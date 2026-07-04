package org.coaas.roadwork.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Small HTTP handler used by every query submission path.
 *
 * Wraps the JDK 11 {@link HttpClient} with the connect + read timeouts and
 * the default headers the CoaaS Query Engine expects, and exposes a
 * fire-and-forget helper that swallows errors on the caller's behalf so
 * the scheduler doesn't get blocked on a slow downstream.
 */
public final class HttpHandler {

    private static final Logger log = Logger.getLogger(HttpHandler.class.getName());

    private static final String DEFAULT_COAAS_ENDPOINT =
        System.getenv().getOrDefault("COAAS_URL", "http://localhost:8080") + "/api/query";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(500))
        .build();

    private HttpHandler() { /* no instances */ }

    /** Fire-and-forget context query submission — matches Shakthi's async pattern. */
    public static void makeContextQuery(String queryBody, String authToken) {
        makeContextQuery(DEFAULT_COAAS_ENDPOINT, queryBody, authToken);
    }

    public static void makeContextQuery(String endpoint, String queryBody, String authToken) {
        try {
            HttpRequest request = buildRequest(endpoint, queryBody, authToken, "plain/text");
            CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception ex) {
            log.severe("Error making context query request: " + ex.getMessage());
        }
    }

    /** Blocking JSON call — used by the ACOCA-P adapter to read component responses. */
    public static HttpResponse<String> jsonPost(String endpoint, String body) throws Exception {
        HttpRequest request = buildRequest(endpoint, body, null, "application/json");
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /** Non-blocking JSON call — used when the caller just wants to fan out. */
    public static CompletableFuture<HttpResponse<String>> jsonPostAsync(String endpoint, String body) {
        HttpRequest request = buildRequest(endpoint, body, null, "application/json");
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    /** JSON GET helper. */
    public static HttpResponse<String> jsonGet(String endpoint) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .timeout(Duration.ofSeconds(2))
            .header("Accept", "application/json")
            .GET()
            .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpRequest buildRequest(String endpoint, String body, String authToken, String contentType) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(Objects.requireNonNull(endpoint, "endpoint")))
            .timeout(Duration.ofSeconds(2))
            .header("Content-Type", contentType);
        if (authToken != null && !authToken.isBlank()) {
            builder.header("Authorization", authToken);
        }
        for (Map.Entry<String, String> h : defaultHeaders().entrySet()) {
            builder.header(h.getKey(), h.getValue());
        }
        return builder.POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body)).build();
    }

    private static Map<String, String> defaultHeaders() {
        Map<String, String> h = new HashMap<>();
        h.put("X-ACOCA-Client", "roadwork-context-query-simulator/1.0");
        return h;
    }
}
