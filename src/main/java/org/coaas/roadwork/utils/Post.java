package org.coaas.roadwork.utils;

import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;

/**
 * Thin wrapper for one-shot POST calls, kept separate from HttpHandler so
 * the fire-and-forget vs blocking modes stay explicit at every call site.
 */
public final class Post {

    private static final Logger log = Logger.getLogger(Post.class.getName());

    public static int send(String url, String body, String contentType) {
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(1))
                .build();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(3))
                .header("Content-Type", contentType == null ? "application/json" : contentType)
                .POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body))
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode();
        } catch (Exception ex) {
            log.warning("POST " + url + " failed: " + ex.getMessage());
            return -1;
        }
    }

    /** Streaming POST — used when the body may be many megabytes. */
    public static int sendStreamed(String url, byte[] payload, String contentType) {
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(1))
                .build();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", contentType == null ? "application/octet-stream" : contentType)
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .build();
            return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch (Exception ex) {
            log.warning("streamed POST " + url + " failed: " + ex.getMessage());
            return -1;
        }
    }

    private Post() {}
}
