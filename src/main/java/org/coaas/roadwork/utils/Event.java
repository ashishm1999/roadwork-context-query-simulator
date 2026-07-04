package org.coaas.roadwork.utils;

import java.util.Objects;

/**
 * Named event, typed by {@link Operation}. Used to keep pub/sub topics
 * consistent across the simulator (QUERY_SUBMITTED, POA_UPDATE, etc.).
 */
public final class Event {

    private final Operation operation;
    private final String topic;

    public Event(Operation operation, String topic) {
        this.operation = Objects.requireNonNull(operation);
        this.topic = Objects.requireNonNull(topic);
    }

    public Operation getOperation() {
        return operation;
    }

    public String getTopic() {
        return topic;
    }

    public String qualifiedName() {
        return topic + "." + operation.name().toLowerCase();
    }
}
