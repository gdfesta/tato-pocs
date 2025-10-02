package com.gdfesta.example.kafka;

import com.gdfesta.example.kafka.producer.model.GreetingKafkaMessage;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Test Kafka consumer that captures messages for verification.
 * ApplicationScoped so it's shared across all tests.
 */
@ApplicationScoped
public class TestKafkaConsumer {
    private static final List<GreetingKafkaMessage> capturedMessages = new CopyOnWriteArrayList<>();

    @Incoming("greeting-events-test")
    public void consume(GreetingKafkaMessage message) {
        capturedMessages.add(message);
    }

    public static List<GreetingKafkaMessage> getMessages() {
        return capturedMessages;
    }

    public static void clear() {
        capturedMessages.clear();
    }
}
