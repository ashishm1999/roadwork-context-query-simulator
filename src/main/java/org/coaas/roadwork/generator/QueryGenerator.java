package org.coaas.roadwork.generator;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.Callable;

import org.coaas.roadwork.config.Sites;
import org.coaas.roadwork.model.QueryTemplate;

import picocli.CommandLine;

/**
 * Stage 1 of the two-stage generator: draw {@link QueryTemplate} rows from
 * the parameter distributions defined in {@code Context Query Templates.csv}
 * and write them as a template CSV that Stage 2 (CDQLTransformer) consumes.
 */
@CommandLine.Command(name = "generator", mixinStandardHelpOptions = true,
    description = "Sample QueryTemplate rows for the roadwork scenario.")
public class QueryGenerator implements Callable<Integer> {

    @CommandLine.Option(names = "--templates", required = true,
        description = "Path to the Context Query Templates CSV describing distributions.")
    private Path templates;

    @CommandLine.Option(names = "--count", defaultValue = "280000")
    private int count;

    @CommandLine.Option(names = "--horizon", defaultValue = "24",
        description = "Wall-clock hours the query load spans.")
    private int horizonHours;

    @CommandLine.Option(names = "--seed", defaultValue = "20260703")
    private long seed;

    @CommandLine.Option(names = "--output", required = true,
        description = "Where to write the generated CSV.")
    private Path output;

    @Override
    public Integer call() throws Exception {
        Random rng = new Random(seed);
        long horizonMs = horizonHours * 3_600_000L;
        long stepMs = Math.max(1L, horizonMs / count);

        Files.createDirectories(output.getParent());
        try (BufferedWriter bw = Files.newBufferedWriter(output)) {
            writeHeader(bw);
            for (int i = 0; i < count; i++) {
                QueryTemplate t = sample(rng, i, stepMs);
                writeRow(bw, t);
                if ((i + 1) % 20_000 == 0) {
                    System.out.printf("... %,d template rows written%n", i + 1);
                }
            }
        }
        System.out.printf("Wrote %,d rows to %s%n", count, output);
        return 0;
    }

    private QueryTemplate sample(Random rng, int seq, long stepMs) {
        QueryTemplate t = new QueryTemplate();

        Sites.Site site = pickSite(rng);
        t.targetSite = site.siteId;
        t.locationLat = site.lat + rng.nextGaussian() * 0.010;
        t.locationLng = site.lng + rng.nextGaussian() * 0.015;
        t.vehicleId = randomVin(rng);
        t.contextType = pick(rng,
            new String[]{"roadwork", "incident", "weather", "aggregated"},
            new double[]{0.55, 0.20, 0.15, 0.10});

        if (rng.nextDouble() < 0.85) {
            t.maxDelayMin = 2 + rng.nextInt(18);
        }
        if (rng.nextDouble() < 0.75) {
            t.minFreshness = 0.20 + rng.nextDouble() * 0.60;
        }
        if (rng.nextDouble() < 0.35) {
            t.minQoc = 0.50 + rng.nextDouble() * 0.45;
        }
        if (rng.nextDouble() < 0.55) {
            t.maxDistanceM = pickInt(rng,
                new int[]{500, 1000, 2500, 5000, 10000},
                new double[]{0.25, 0.20, 0.20, 0.20, 0.15});
        }
        if (rng.nextDouble() < 0.60) {
            t.routePreference = pick(rng,
                new String[]{"fastest", "shortest", "scenic"},
                new double[]{0.65, 0.25, 0.10});
        }
        if (rng.nextDouble() < 0.85) {
            t.slaMs = pickInt(rng,
                new int[]{100, 250, 500, 1000},
                new double[]{0.30, 0.45, 0.20, 0.05});
        }

        long submitAtMs = (long) seq * stepMs;
        submitAtMs += diurnalJitter(rng, submitAtMs);
        t.submitAtMs = Math.max(0, submitAtMs);

        long inHourMs = t.submitAtMs % 3_600_000L;
        t.hour = (int) ((t.submitAtMs / 3_600_000L) % 24);
        t.minute = (int) (inHourMs / 60_000L);
        t.second = (int) ((inHourMs % 60_000L) / 1000L);
        t.day = weekday(t.hour + (t.submitAtMs / 3_600_000L / 24L));
        return t;
    }

    private Sites.Site pickSite(Random rng) {
        double r = rng.nextDouble();
        double acc = 0.0;
        for (Sites.Site s : Sites.ALL) {
            acc += s.weight;
            if (r <= acc) return s;
        }
        return Sites.ALL.get(Sites.ALL.size() - 1);
    }

    private static String randomVin(Random rng) {
        String alpha = "ABCDEFGHJKLMNPRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(17);
        for (int i = 0; i < 17; i++) sb.append(alpha.charAt(rng.nextInt(alpha.length())));
        return sb.toString();
    }

    private static String pick(Random rng, String[] values, double[] weights) {
        double r = rng.nextDouble();
        double acc = 0.0;
        for (int i = 0; i < values.length; i++) {
            acc += weights[i];
            if (r <= acc) return values[i];
        }
        return values[values.length - 1];
    }

    private static int pickInt(Random rng, int[] values, double[] weights) {
        double r = rng.nextDouble();
        double acc = 0.0;
        for (int i = 0; i < values.length; i++) {
            acc += weights[i];
            if (r <= acc) return values[i];
        }
        return values[values.length - 1];
    }

    private static long diurnalJitter(Random rng, long submitAtMs) {
        double hour = (submitAtMs / 3_600_000.0) % 24.0;
        double morning = Math.exp(-0.5 * Math.pow((hour - 7.5) / 1.2, 2));
        double evening = Math.exp(-0.5 * Math.pow((hour - 17.5) / 1.4, 2));
        double load = 0.15 + 0.85 * (morning + evening);
        double variance = 1.0 - Math.min(0.9, load);
        return (long) (rng.nextGaussian() * variance * 5_000);
    }

    private static String weekday(long dayCount) {
        String[] days = {"monday","tuesday","wednesday","thursday","friday","saturday","sunday"};
        return days[(int) Math.floorMod(dayCount, 7)];
    }

    private static void writeHeader(BufferedWriter bw) throws Exception {
        bw.write("vehicle_id,location_lat,location_lng,target_site,max_delay_min," +
                 "min_freshness,min_qoc,max_distance_m,route_preference,context_type," +
                 "day,hour,minute,second,sla_ms,submit_at_ms");
        bw.newLine();
    }

    private static void writeRow(BufferedWriter bw, QueryTemplate t) throws Exception {
        bw.write(String.join(",",
            t.vehicleId,
            String.format("%.6f", t.locationLat),
            String.format("%.6f", t.locationLng),
            t.targetSite,
            s(t.maxDelayMin), s(t.minFreshness), s(t.minQoc), s(t.maxDistanceM),
            s(t.routePreference), t.contextType, t.day,
            String.valueOf(t.hour), String.valueOf(t.minute), String.valueOf(t.second),
            s(t.slaMs), String.valueOf(t.submitAtMs)
        ));
        bw.newLine();
    }

    private static String s(Object o) {
        return o == null ? "" : o.toString();
    }

    public static void main(String[] args) {
        int rc = new CommandLine(new QueryGenerator()).execute(args);
        System.exit(rc);
    }
}
