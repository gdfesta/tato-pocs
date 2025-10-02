package com.gdfesta.example.kafka;

import com.gdfesta.example.kafka.producer.model.GreetingKafkaMessage;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@DisplayName("GreetingsKafkaHandler Integration Tests")
class GreetingsKafkaHandlerTest {

    private String generateUniqueName() {
        return "kafka-test-" + UUID.randomUUID().toString();
    }

    @Test
    @DisplayName("Greeted event should publish message to Kafka")
    void testGreetedEventPublishesToKafka() {
        String name = generateUniqueName();

        // Clear any previous messages
        TestKafkaConsumer.clear();

        // POST greeting via REST API (triggers actor + event + Kafka projection)
        given()
            .when().post("/greetings/{name}", name)
            .then()
                .statusCode(200)
                .body("status", is("OpenState"))
                .body("count", is(1));

        // Wait for async Kafka message to be published and consumed
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(100, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                List<GreetingKafkaMessage> messages = TestKafkaConsumer.getMessages();
                assertFalse(messages.isEmpty(), "At least one Kafka message should be published");

                // Find message for our specific name
                boolean found = messages.stream()
                    .anyMatch(msg -> msg instanceof GreetingKafkaMessage.Greeted greeted
                        && greeted.name().equals(name));
                assertTrue(found, "Kafka message should contain greeting for " + name);
            });
    }

    @Test
    @DisplayName("Multiple greetings should publish multiple Kafka messages")
    void testMultipleGreetingsPublishMultipleMessages() {
        String alice = generateUniqueName();
        String bob = generateUniqueName();
        String charlie = generateUniqueName();

        TestKafkaConsumer.clear();

        // Greet 3 different people
        given().when().post("/greetings/{name}", alice).then().statusCode(200);
        given().when().post("/greetings/{name}", bob).then().statusCode(200);
        given().when().post("/greetings/{name}", charlie).then().statusCode(200);

        // Wait for 3 Kafka messages
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(100, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                List<GreetingKafkaMessage> messages = TestKafkaConsumer.getMessages();

                long aliceCount = messages.stream()
                    .filter(msg -> msg instanceof GreetingKafkaMessage.Greeted greeted && greeted.name().equals(alice))
                    .count();
                long bobCount = messages.stream()
                    .filter(msg -> msg instanceof GreetingKafkaMessage.Greeted greeted && greeted.name().equals(bob))
                    .count();
                long charlieCount = messages.stream()
                    .filter(msg -> msg instanceof GreetingKafkaMessage.Greeted greeted && greeted.name().equals(charlie))
                    .count();

                assertEquals(1, aliceCount, "Alice should have 1 message");
                assertEquals(1, bobCount, "Bob should have 1 message");
                assertEquals(1, charlieCount, "Charlie should have 1 message");
            });
    }

    @Test
    @DisplayName("UnGreeted event should NOT publish to Kafka")
    void testUnGreetedEventNotPublished() {
        String name = generateUniqueName();

        TestKafkaConsumer.clear();

        // First greet to create the actor
        given().when().post("/greetings/{name}", name).then().statusCode(200);

        // Wait for greeting message
        await()
            .atMost(10, TimeUnit.SECONDS)
            .until(() -> TestKafkaConsumer.getMessages().stream()
                .anyMatch(msg -> msg instanceof GreetingKafkaMessage.Greeted greeted && greeted.name().equals(name)));

        int messagesAfterGreeting = TestKafkaConsumer.getMessages().size();

        // DELETE (UnGreet) - should not produce Kafka message
        given()
            .when().delete("/greetings/{name}", name)
            .then()
                .statusCode(200);

        // Wait a bit to ensure no message is published
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Count should not increase (UnGreeted events are not published to Kafka)
        int messagesAfterUnGreeting = TestKafkaConsumer.getMessages().size();
        assertEquals(messagesAfterGreeting, messagesAfterUnGreeting,
            "UnGreeted event should not produce Kafka messages");
    }

    @Test
    @DisplayName("Rapid successive greetings should publish all messages")
    void testRapidSuccessiveGreetingsPublishAll() {
        String name = generateUniqueName();
        int totalGreetings = 5;

        TestKafkaConsumer.clear();

        // Send 5 greetings rapidly (will reach CloseState at 5)
        for (int i = 0; i < totalGreetings; i++) {
            given().when().post("/greetings/{name}", name).then().statusCode(200);
        }

        // Wait for all 5 messages to be published
        await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(200, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                long count = TestKafkaConsumer.getMessages().stream()
                    .filter(msg -> msg instanceof GreetingKafkaMessage.Greeted greeted && greeted.name().equals(name))
                    .count();
                assertEquals(totalGreetings, count,
                    "All " + totalGreetings + " greetings should produce Kafka messages");
            });
    }

    @Test
    @DisplayName("Kafka messages should be published for all successful greetings")
    void testOnlySuccessfulGreetingsPublishToKafka() {
        String name = generateUniqueName();

        TestKafkaConsumer.clear();

        // Greet 5 times to reach CloseState
        for (int i = 0; i < 5; i++) {
            given().when().post("/greetings/{name}", name).then().statusCode(200);
        }

        // Wait for 5 messages
        await()
            .atMost(10, TimeUnit.SECONDS)
            .until(() -> TestKafkaConsumer.getMessages().stream()
                .filter(msg -> msg instanceof GreetingKafkaMessage.Greeted greeted && greeted.name().equals(name))
                .count() == 5);

        // Try 6th greeting (should fail with 500)
        given()
            .when().post("/greetings/{name}", name)
            .then()
                .statusCode(500);

        // Wait a bit to ensure no additional message
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Should still have only 5 messages (failed greeting doesn't produce event)
        long count = TestKafkaConsumer.getMessages().stream()
            .filter(msg -> msg instanceof GreetingKafkaMessage.Greeted greeted && greeted.name().equals(name))
            .count();
        assertEquals(5, count, "Only successful greetings should produce Kafka messages");
    }

}
