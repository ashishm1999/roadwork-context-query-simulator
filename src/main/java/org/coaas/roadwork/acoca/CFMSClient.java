package org.coaas.roadwork.acoca;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/** Blocking + async client for CFMS. */
public final class CFMSClient {

    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();

    public CFMSClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public CompletableFuture<Double> getCfAsync(String itemId) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/freshness/" + itemId))
            .timeout(Duration.ofMillis(500))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{\"context_type\":\"roadwork\"}"))
            .build();
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(resp -> {
                if (resp.statusCode() == 404) return 0.0;
                return extract(resp.body(), "cf", 0.0);
            });
    }

    static double extract(String body, String field, double defaultValue) {
        int i = body.indexOf("\"" + field + "\"");
        if (i < 0) return defaultValue;
        int colon = body.indexOf(':', i);
        int end = -1;
        for (int j = colon + 1; j < body.length(); j++) {
            char c = body.charAt(j);
            if (c == ',' || c == '}') { end = j; break; }
        }
        if (end < 0) return defaultValue;
        try {
            return Double.parseDouble(body.substring(colon + 1, end).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
