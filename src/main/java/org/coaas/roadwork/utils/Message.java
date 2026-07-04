package org.coaas.roadwork.utils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Lightweight message envelope shared across the pub/sub layer.
 *
 * Every message carries an operation, a body, and a bag of headers so
 * subscribers can filter without decoding the payload.
 */
public final class Message {

    private final Operation operation;
    private final String body;
    private final Map<String, String> headers;
    private final Instant createdAt;

    public Message(Operation operation, String body) {
        this(operation, body, new HashMap<>(), Instant.now());
    }

    public Message(Operation operation, String body, Map<String, String> headers, Instant createdAt) {
        this.operation = Objects.requireNonNull(operation);
        this.body = body == null ? "" : body;
        this.headers = new HashMap<>(headers);
        this.createdAt = createdAt;
    }

    public Operation getOperation() {
        return operation;
    }

    public String getBody() {
        return body;
    }

    public Map<String, String> getHeaders() {
        return new HashMap<>(headers);
    }

    public Message withHeader(String key, String value) {
        Map<String, String> merged = new HashMap<>(headers);
        merged.put(key, value);
        return new Message(operation, body, merged, createdAt);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Message{" + operation + ", body.len=" + body.length() + ", headers=" + headers.size() + "}";
    }
}
