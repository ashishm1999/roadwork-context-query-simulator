package org.coaas.roadwork.acoca;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Blocking + async client for CAPME. Talks to /api/poa on the CAPME Flask
 * app defined in ashishm1999/ACOCA-P.
 */
public final class CAPMEClient {

    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();

    public CAPMEClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public CompletableFuture<Double> getPoAAsync(String itemId, Map<String, Double> attributes) {
        String payload = buildPayload(itemId, attributes);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/poa"))
            .timeout(Duration.ofMillis(500))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(resp -> parseScore(resp.body(), itemId));
    }

    public double getPoA(String itemId, Map<String, Double> attributes) throws Exception {
        return getPoAAsync(itemId, attributes).get();
    }

    private static String buildPayload(String itemId, Map<String, Double> attributes) {
        StringBuilder sb = new StringBuilder("{\"items\":{\"").append(itemId).append("\":{");
        boolean first = true;
        for (Map.Entry<String, Double> e : attributes.entrySet()) {
            if (!first) sb.append(',');
            sb.append('"').append(e.getKey()).append("\":").append(e.getValue());
            first = false;
        }
        sb.append("}}}");
        return sb.toString();
    }

    private static double parseScore(String body, String itemId) {
        int i = body.indexOf("\"" + itemId + "\"");
        if (i < 0) return 0.5;
        int colon = body.indexOf(':', i);
        int end = -1;
        for (int j = colon + 1; j < body.length(); j++) {
            char c = body.charAt(j);
            if (c == ',' || c == '}') { end = j; break; }
        }
        if (end < 0) return 0.5;
        try {
            return Double.parseDouble(body.substring(colon + 1, end).trim());
        } catch (NumberFormatException e) {
            return 0.5;
        }
    }
}
