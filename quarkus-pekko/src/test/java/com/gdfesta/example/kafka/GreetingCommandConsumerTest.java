package com.gdfesta.example.kafka;

import com.gdfesta.example.kafka.consumer.model.GreetingCommandMessage;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@DisplayName("GreetingCommandConsumer Integration Tests")
class GreetingCommandConsumerTest {

    @Inject
    TestCommandProducer commandProducer;

    private String generateUniqueName() {
        return "cmd-test-" + UUID.randomUUID().toString();
    }

    @Test
    @DisplayName("GreetCommand from Kafka should trigger actor greeting")
    void testGreetCommandFromKafka() {
        String name = generateUniqueName();

        // Publish GreetCommand to Kafka
        var greetCommand = new GreetingCommandMessage.GreetCommand(name);
        commandProducer.publish(greetCommand);

        // Wait for async command processing and verify actor state via REST API
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(200, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                given()
                    .when().get("/greetings/{name}", name)
                    .then()
                        .statusCode(200)
                        .body("status", is("OpenState"))
                        .body("count", is(1));
            });
    }

    @Test
    @DisplayName("Multiple GreetCommands should increment greeting count")
    void testMultipleGreetCommands() {
        String name = generateUniqueName();

        // Publish 3 GreetCommands
        var greetCommand = new GreetingCommandMessage.GreetCommand(name);
        commandProducer.publish(greetCommand);
        commandProducer.publish(greetCommand);
        commandProducer.publish(greetCommand);

        // Wait for async processing and verify count is 3
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(200, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                given()
                    .when().get("/greetings/{name}", name)
                    .then()
                        .statusCode(200)
                        .body("status", is("OpenState"))
                        .body("count", is(3));
            });
    }

    @Test
    @DisplayName("UnGreetCommand from Kafka should trigger actor ungreeting")
    void testUnGreetCommandFromKafka() {
        String name = generateUniqueName();

        // First greet to create actor in OpenState
        var greetCommand = new GreetingCommandMessage.GreetCommand(name);
        commandProducer.publish(greetCommand);

        // Wait for greeting to be processed
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(200, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                given()
                    .when().get("/greetings/{name}", name)
                    .then()
                        .statusCode(200)
                        .body("count", is(1));
            });

        // Now send UnGreetCommand
        var unGreetCommand = new GreetingCommandMessage.UnGreetCommand(name);
        commandProducer.publish(unGreetCommand);

        // Wait for ungreeting to be processed
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(200, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                given()
                    .when().get("/greetings/{name}", name)
                    .then()
                        .statusCode(200)
                        .body("status", is("OpenState"))
                        .body("count", is(0));
            });
    }

    @Test
    @DisplayName("Commands from Kafka should trigger state transition to CloseState")
    void testCommandsReachCloseState() {
        String name = generateUniqueName();

        // Send 5 GreetCommands to reach CloseState
        var greetCommand = new GreetingCommandMessage.GreetCommand(name);
        for (int i = 0; i < 5; i++) {
            commandProducer.publish(greetCommand);
        }

        // Wait for all greetings to be processed and verify CloseState
        await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(200, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                given()
                    .when().get("/greetings/{name}", name)
                    .then()
                        .statusCode(200)
                        .body("status", is("CloseState"))
                        .body("count", is(5));
            });
    }

    @Test
    @DisplayName("Commands for different actors should be processed independently")
    void testMultipleActorsIndependently() {
        String alice = generateUniqueName();
        String bob = generateUniqueName();

        // Send different commands for Alice and Bob
        commandProducer.publish(new GreetingCommandMessage.GreetCommand(alice));
        commandProducer.publish(new GreetingCommandMessage.GreetCommand(alice));
        commandProducer.publish(new GreetingCommandMessage.GreetCommand(bob));

        // Wait for processing and verify Alice has count 2
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(200, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                given()
                    .when().get("/greetings/{name}", alice)
                    .then()
                        .statusCode(200)
                        .body("count", is(2));
            });

        // Verify Bob has count 1
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(200, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                given()
                    .when().get("/greetings/{name}", bob)
                    .then()
                        .statusCode(200)
                        .body("count", is(1));
            });
    }

    @Test
    @DisplayName("Mixed command sequence should be processed correctly")
    void testMixedCommandSequence() {
        String name = generateUniqueName();

        // Send mixed sequence: Greet, Greet, UnGreet, Greet
        commandProducer.publish(new GreetingCommandMessage.GreetCommand(name));
        commandProducer.publish(new GreetingCommandMessage.GreetCommand(name));
        commandProducer.publish(new GreetingCommandMessage.UnGreetCommand(name));
        commandProducer.publish(new GreetingCommandMessage.GreetCommand(name));

        // Final count should be 2 (greet, greet, ungreet, greet = 1+1-1+1 = 2)
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(200, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                given()
                    .when().get("/greetings/{name}", name)
                    .then()
                        .statusCode(200)
                        .body("count", is(2));
            });
    }
}
