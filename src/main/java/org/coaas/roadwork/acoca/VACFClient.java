package org.coaas.roadwork.acoca;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/** Blocking + async client for VACF. */
public final class VACFClient {

    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();

    public VACFClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public static final class Policy {
        public final long ttlMs;
        public final int replicas;
        public Policy(long ttlMs, int replicas) { this.ttlMs = ttlMs; this.replicas = replicas; }
    }

    public CompletableFuture<Policy> policyAsync(String itemId, double cvi, double cf, double poa, int slaMs) {
        String body = "{\"itemId\":\"" + itemId + "\","
                    + "\"cvi\":" + cvi + ",\"cf\":" + cf + ",\"poa\":" + poa + ","
                    + "\"slaMs\":" + slaMs + "}";
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/policy"))
            .timeout(Duration.ofMillis(500))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(resp -> {
                long ttl = (long) CFMSClient.extract(resp.body(), "ttl_ms", 30_000);
                int replicas = (int) CFMSClient.extract(resp.body(), "replicas", 1);
                return new Policy(ttl, replicas);
            });
    }
}
