package org.coaas.roadwork.generator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.coaas.roadwork.model.CDQLQuery;
import org.coaas.roadwork.model.QueryTemplate;

import picocli.CommandLine;

/**
 * Stage 2 of the two-stage generator: read the template CSV produced by
 * {@link QueryGenerator} and fold each row into an executable CDQL query.
 */
@CommandLine.Command(name = "cdql", mixinStandardHelpOptions = true,
    description = "Fold template rows into executable CDQL queries.")
public class CDQLTransformer implements Callable<Integer> {

    @CommandLine.Option(names = "--input", required = true)
    private Path input;

    @CommandLine.Option(names = "--output", required = true)
    private Path output;

    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public Integer call() throws Exception {
        Files.createDirectories(output.getParent());
        try (BufferedReader br = Files.newBufferedReader(input);
             BufferedWriter bw = Files.newBufferedWriter(output)) {
            String header = br.readLine();   // skip header
            if (header == null) return 0;
            bw.write("[");
            bw.newLine();
            int count = 0;
            String row;
            while ((row = br.readLine()) != null) {
                QueryTemplate t = parseRow(row);
                CDQLQuery q = new CDQLQuery(
                    "q-" + String.format("%09d", count),
                    t.submitAtMs,
                    t.slaMs == null ? 250 : t.slaMs,
                    render(t),
                    t
                );
                if (count > 0) bw.write(",\n");
                bw.write(JSON.writeValueAsString(q));
                count++;
                if (count % 20_000 == 0) System.out.printf("... %,d CDQL queries written%n", count);
            }
            bw.newLine();
            bw.write("]");
        }
        System.out.printf("CDQL queries written to %s%n", output);
        return 0;
    }

    static String render(QueryTemplate t) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("prefix schema:http//schema.org\n");
        sb.append("pull (targetRoadwork.congestion, targetRoadwork.detour, targetRoadwork.time_delay)\n");
        sb.append("when distance(consumerCar.location, targetRoadwork.location) <= {\"value\": ");
        sb.append(t.maxDistanceM == null ? 5000 : t.maxDistanceM).append(", \"unit\":\"m\"}\n");
        sb.append("define\n");
        sb.append("  entity consumerCar    is from schema:Vehicle  where consumerCar.vin = \"")
          .append(t.vehicleId).append("\",\n");
        sb.append("  entity targetRoadwork is from coaas:Roadwork  where\n");
        sb.append("      targetRoadwork.site_id = \"").append(t.targetSite).append("\"");
        if (t.minFreshness != null) {
            sb.append("\n      and targetRoadwork.freshness >= ").append(String.format("%.2f", t.minFreshness));
        }
        if (t.minQoc != null) {
            sb.append("\n      and targetRoadwork.qoc >= ").append(String.format("%.2f", t.minQoc));
        }
        if (t.maxDelayMin != null) {
            sb.append("\n      and targetRoadwork.time_delay < {\"value\": ")
              .append(t.maxDelayMin).append(", \"unit\":\"min\"}");
        }
        if (t.slaMs != null) {
            sb.append("\n      and targetRoadwork.sla_ms <= ").append(t.slaMs);
        }
        return sb.toString();
    }

    private static QueryTemplate parseRow(String row) {
        String[] c = row.split(",", -1);
        QueryTemplate t = new QueryTemplate();
        t.vehicleId = c[0];
        t.locationLat = Double.parseDouble(c[1]);
        t.locationLng = Double.parseDouble(c[2]);
        t.targetSite = c[3];
        t.maxDelayMin  = c[4].isEmpty() ? null : Integer.parseInt(c[4]);
        t.minFreshness = c[5].isEmpty() ? null : Double.parseDouble(c[5]);
        t.minQoc       = c[6].isEmpty() ? null : Double.parseDouble(c[6]);
        t.maxDistanceM = c[7].isEmpty() ? null : Integer.parseInt(c[7]);
        t.routePreference = c[8].isEmpty() ? null : c[8];
        t.contextType = c[9];
        t.day = c[10];
        t.hour = Integer.parseInt(c[11]);
        t.minute = Integer.parseInt(c[12]);
        t.second = Integer.parseInt(c[13]);
        t.slaMs = c[14].isEmpty() ? null : Integer.parseInt(c[14]);
        t.submitAtMs = Long.parseLong(c[15]);
        return t;
    }

    public static void main(String[] args) {
        int rc = new CommandLine(new CDQLTransformer()).execute(args);
        System.exit(rc);
    }
}
