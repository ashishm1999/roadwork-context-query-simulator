package org.coaas.roadwork.utils.pubsub;

import java.util.Objects;
import java.util.logging.Logger;

import org.coaas.roadwork.utils.Message;
import org.coaas.roadwork.utils.OnMessage;

/**
 * Convenience wrapper around {@link Publisher} — mirrors Shakthi's
 * Subscriber pattern so the receiving side can be tested independently
 * of the publisher.
 */
public final class Subscriber {

    private static final Logger log = Logger.getLogger(Subscriber.class.getName());

    private final String topic;
    private final OnMessage callback;
    private boolean subscribed = false;

    public Subscriber(String topic, OnMessage callback) {
        this.topic = Objects.requireNonNull(topic);
        this.callback = Objects.requireNonNull(callback);
    }

    public synchronized void start() {
        if (subscribed) return;
        Publisher.getInstance().subscribe(topic, this::receive);
        subscribed = true;
        log.info("Subscribed to " + topic);
    }

    public synchronized void stop() {
        if (!subscribed) return;
        Publisher.getInstance().unsubscribe(topic, this::receive);
        subscribed = false;
        log.info("Unsubscribed from " + topic);
    }

    private void receive(Message message) {
        try {
            callback.handle(message);
        } catch (RuntimeException ex) {
            log.warning("subscriber callback threw for " + topic + ": " + ex.getMessage());
        }
    }
}
