package org.coaas.roadwork;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.coaas.roadwork.utils.Message;
import org.coaas.roadwork.utils.Operation;
import org.coaas.roadwork.utils.pubsub.Publisher;
import org.coaas.roadwork.utils.pubsub.Subscriber;
import org.junit.jupiter.api.Test;

class PublisherTest {

    @Test
    void publishReachesEverySubscriber() {
        AtomicInteger delivered = new AtomicInteger();
        Subscriber a = new Subscriber("topic.a", msg -> delivered.incrementAndGet());
        Subscriber b = new Subscriber("topic.a", msg -> delivered.incrementAndGet());
        a.start();
        b.start();
        Publisher.getInstance().publish("topic.a", new Message(Operation.HEARTBEAT, "hi"));
        assertEquals(2, delivered.get());
        a.stop();
        b.stop();
    }

    @Test
    void publishToUnknownTopicDoesNotThrow() {
        int delivered = Publisher.getInstance().publish("topic.does-not-exist",
            new Message(Operation.HEARTBEAT, ""));
        assertEquals(0, delivered);
    }

    @Test
    void subscriberCountsCorrectly() {
        Subscriber s = new Subscriber("topic.count", msg -> {});
        s.start();
        assertTrue(Publisher.getInstance().subscriberCount("topic.count") >= 1);
        s.stop();
    }
}
