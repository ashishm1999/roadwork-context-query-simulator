package org.coaas.roadwork.server;

import java.nio.file.Path;
import java.util.logging.Logger;

import org.coaas.roadwork.acoca.AcocaAdapter;
import org.coaas.roadwork.utils.Message;
import org.coaas.roadwork.utils.Operation;
import org.coaas.roadwork.utils.pubsub.Publisher;
import org.coaas.roadwork.utils.pubsub.Subscriber;

/**
 * Roadwork query generating server — matches the Shakthi
 * QueryGeneratingServer entry point but wires our Quartz-based
 * scheduler + pub/sub bus + ACOCA-P adapter.
 *
 * <pre>
 *     java -jar target/roadwork-context-query-simulator-1.0.0.jar server \
 *         --queries sample/diurnal-queries.json \
 *         --coaas   http://coaas:8080
 * </pre>
 */
public class QueryGeneratingServer {

    private static final Logger log = Logger.getLogger(QueryGeneratingServer.class.getName());

    public static void main(String[] args) throws Exception {
        log.info("Starting roadwork context query generating server");

        // Subscribe a small monitor to the topic bus so operators see the query flow.
        Subscriber monitor = new Subscriber("roadwork.query.submitted", QueryGeneratingServer::onSubmitted);
        monitor.start();
        Subscriber decision = new Subscriber("roadwork.acoca.decision", QueryGeneratingServer::onDecision);
        decision.start();

        Path queriesPath = args.length > 0 ? Path.of(args[0]) : Path.of("sample/diurnal-queries.json");
        QueryScheduler scheduler = QueryScheduler.getInstance();
        scheduler.fetchSchedule(queriesPath);
        scheduler.start();

        Publisher.getInstance().publish("roadwork.system", new Message(Operation.HEARTBEAT, "server-started"));
        log.info("Roadwork Context Query Simulator started -- ACOCA-P adapter=" + AcocaAdapter.isEnabled());

        // Keep the JVM alive; Quartz manages its own executor threads.
        Thread.currentThread().join();
    }

    private static void onSubmitted(Message message) {
        log.fine("QUERY_SUBMITTED " + message.getHeaders());
    }

    private static void onDecision(Message message) {
        log.fine("DECISION_MADE " + message.getBody());
    }
}
