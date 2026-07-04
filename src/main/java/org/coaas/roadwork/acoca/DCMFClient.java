package org.coaas.roadwork.acoca;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/** Blocking + async client for DCMF. */
public final class DCMFClient {

    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();

    public DCMFClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public static final class Decision {
        public final boolean cache;
        public final double belief;
        public Decision(boolean cache, double belief) { this.cache = cache; this.belief = belief; }
    }

    public CompletableFuture<Decision> decideAsync(String itemId, double cf, double poa) {
        String body = "{\"itemId\":\"" + itemId + "\",\"cf\":" + cf + ",\"poa\":" + poa + ",\"quality\":1.0}";
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/decide"))
            .timeout(Duration.ofMillis(500))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(resp -> {
                boolean cache = resp.body().contains("\"cache\":true");
                double belief = CFMSClient.extract(resp.body(), "betP", 0.0);
                return new Decision(cache, belief);
            });
    }
}
