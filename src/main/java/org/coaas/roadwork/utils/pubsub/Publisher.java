package org.coaas.roadwork.utils.pubsub;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import org.coaas.roadwork.utils.Message;
import org.coaas.roadwork.utils.OnMessage;

/**
 * Very small in-process publisher. Used by the roadwork query simulator so
 * that the same topic naming scheme (roadwork.query.submitted,
 * roadwork.poa.update, roadwork.cvi.update, ...) can be swapped for a
 * real broker (RabbitMQ or Kafka) without rewriting call sites.
 */
public final class Publisher {

    private static final Logger log = Logger.getLogger(Publisher.class.getName());
    private static final Publisher INSTANCE = new Publisher();

    private final Map<String, List<OnMessage>> subscribers = new ConcurrentHashMap<>();

    private Publisher() {}

    public static Publisher getInstance() {
        return INSTANCE;
    }

    public void subscribe(String topic, OnMessage callback) {
        subscribers.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>()).add(callback);
    }

    public void unsubscribe(String topic, OnMessage callback) {
        List<OnMessage> topicSubs = subscribers.get(topic);
        if (topicSubs != null) topicSubs.remove(callback);
    }

    public int publish(String topic, Message message) {
        List<OnMessage> callbacks = subscribers.getOrDefault(topic, Collections.emptyList());
        int delivered = 0;
        for (OnMessage cb : callbacks) {
            try {
                cb.handle(message);
                delivered++;
            } catch (RuntimeException ex) {
                log.warning("subscriber for " + topic + " threw: " + ex.getMessage());
            }
        }
        return delivered;
    }

    public int subscriberCount(String topic) {
        return subscribers.getOrDefault(topic, Collections.emptyList()).size();
    }
}
