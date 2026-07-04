package org.coaas.roadwork.replay;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

import org.coaas.roadwork.model.CDQLQuery;

import picocli.CommandLine;

/**
 * Replays a CDQL query load against a CoaaS endpoint at the pace of the
 * captured workload. The pace can be compressed or expanded ({@code --pace}).
 */
@CommandLine.Command(name = "replay", mixinStandardHelpOptions = true,
    description = "Replay a CDQL query load against a CoaaS endpoint.")
public class LoadReplayServer implements Callable<Integer> {

    @CommandLine.Option(names = "--queries", required = true)
    private Path queries;

    @CommandLine.Option(names = "--coaas", defaultValue = "http://localhost:8080")
    private URI coaasBase;

    @CommandLine.Option(names = "--pace", defaultValue = "realtime",
        description = "realtime | 2x | 10x | burst")
    private String pace;

    @CommandLine.Option(names = "--concurrency", defaultValue = "16")
    private int concurrency;

    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public Integer call() throws Exception {
        List<CDQLQuery> load = JSON.readValue(Files.newBufferedReader(queries), new TypeReference<>() {});
        System.out.printf("Loaded %,d queries. Pace=%s, Concurrency=%d%n", load.size(), pace, concurrency);

        double compression = switch (pace) {
            case "2x"       -> 2.0;
            case "10x"      -> 10.0;
            case "burst"    -> Double.POSITIVE_INFINITY;
            default         -> 1.0;
        };

        long startedAtNs = System.nanoTime();
        AtomicInteger sent = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        ScheduledExecutorService exec = Executors.newScheduledThreadPool(concurrency);

        try (CloseableHttpClient http = HttpClients.createDefault()) {
            for (CDQLQuery q : load) {
                long dispatchAtNs = startedAtNs +
                    (long) (q.submitAtMs * 1_000_000.0 / compression);
                long delayNs = Math.max(0L, dispatchAtNs - System.nanoTime());
                exec.schedule(() -> {
                    try {
                        HttpPost post = new HttpPost(coaasBase.resolve("/api/query"));
                        String body = JSON.writeValueAsString(q);
                        post.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
                        int code = http.execute(post, resp -> resp.getCode());
                        if (code >= 400) failed.incrementAndGet();
                        sent.incrementAndGet();
                        if (sent.get() % 5000 == 0) {
                            System.out.printf("... %,d queries dispatched (%,d failed)%n",
                                sent.get(), failed.get());
                        }
                    } catch (Exception e) {
                        failed.incrementAndGet();
                    }
                }, delayNs, TimeUnit.NANOSECONDS);
            }
        }

        exec.shutdown();
        exec.awaitTermination(1, TimeUnit.HOURS);
        System.out.printf("Done. %,d dispatched, %,d failed.%n", sent.get(), failed.get());
        return 0;
    }

    public static void main(String[] args) {
        int rc = new CommandLine(new LoadReplayServer()).execute(args);
        System.exit(rc);
    }
}
