package com.gdfesta.example.read_side.greetings_count;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
@DisplayName("GreetingsCountReadSideHandler Integration Tests")
class GreetingsCountReadSideHandlerTest {

    @Inject
    GreetingsCountRepository repository;

    private String generateUniqueName() {
        return "handler-test-" + UUID.randomUUID();
    }

    @Transactional
    GreetingsCountModel findById(String name) {
        return repository.findById(name);
    }

    @Test
    @DisplayName("Greeted event should update database via projection")
    void testGreetedEventUpdatesDatabase() {
        String name = generateUniqueName();

        // POST greeting via REST API (triggers actor + event + projection)
        given()
            .when()
            .post("/greetings/{name}", name)
            .then()
            .statusCode(200)
            .body("status", is("OpenState"))
            .body("count", is(1));

        // Wait for async projection to process and update read-side database
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(100, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                GreetingsCountModel model = findById(name);
                assertNotNull(model, "Read model should exist in database");
                assertEquals(1, model.count, "Count should be 1 after first greeting");
            });

        // Verify final state
        GreetingsCountModel model = findById(name);
        assertNotNull(model);
        assertEquals(name, model.name);
        assertEquals(1, model.count);
        assertNotNull(model.lastGreetedAt);
    }

    @Test
    @DisplayName("Multiple greetings should accumulate in database")
    void testMultipleGreetingsAccumulate() {
        String name = generateUniqueName();

        // POST 3 greetings
        for (int i = 0; i < 3; i++) {
            given().when().post("/greetings/{name}", name).then().statusCode(200);
        }

        // Wait for projection to process all 3 events
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(100, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                GreetingsCountModel model = findById(name);
                assertNotNull(model, "Read model should exist");
                assertEquals(3, model.count, "Count should be 3 after three greetings");
            });
    }

    @Test
    @DisplayName("Timestamp should update after each greeting")
    void testTimestampUpdates() throws InterruptedException {
        String name = generateUniqueName();

        // First greeting
        given().when().post("/greetings/{name}", name).then().statusCode(200);

        // Wait for first projection
        await().atMost(10, TimeUnit.SECONDS).until(() -> findById(name) != null);

        GreetingsCountModel first = findById(name);
        assertNotNull(first.lastGreetedAt);

        // Wait a bit to ensure timestamp difference
        Thread.sleep(50);

        // Second greeting
        given().when().post("/greetings/{name}", name).then().statusCode(200);

        // Wait for second projection
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(100, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                GreetingsCountModel model = findById(name);
                assertEquals(2, model.count);
                assertTrue(
                    model.lastGreetedAt.isAfter(first.lastGreetedAt),
                    "Timestamp should be updated after second greeting"
                );
            });
    }

    @Test
    @DisplayName("UnGreeted event should decrement database count")
    void testUnGreetedEventDecrementsCount() {
        String name = generateUniqueName();

        // POST 3 greetings
        for (int i = 0; i < 3; i++) {
            given().when().post("/greetings/{name}", name).then().statusCode(200);
        }

        // Wait for projections
        await()
            .atMost(10, TimeUnit.SECONDS)
            .until(() -> {
                GreetingsCountModel model = findById(name);
                return model != null && model.count == 3;
            });

        GreetingsCountModel before = findById(name);
        assertEquals(3, before.count);

        // DELETE (UnGreet) - should decrement read-side database
        given().when().delete("/greetings/{name}", name).then().statusCode(200);

        // Wait for projection to process UnGreeted event
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(100, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                GreetingsCountModel model = findById(name);
                assertNotNull(model, "Read model should still exist");
                assertEquals(2, model.count, "UnGreeted event should decrement count to 2");
            });

        // Verify final state
        GreetingsCountModel after = findById(name);
        assertNotNull(after);
        assertEquals(2, after.count);
        assertTrue(
            after.lastGreetedAt.isAfter(before.lastGreetedAt) ||
                after.lastGreetedAt.equals(before.lastGreetedAt),
            "Timestamp should be updated or remain the same"
        );
    }

    @Test
    @DisplayName("Different names should create separate database rows")
    void testDifferentNamesCreateSeparateRows() {
        String alice = generateUniqueName();
        String bob = generateUniqueName();
        String charlie = generateUniqueName();

        // Greet Alice twice
        given().when().post("/greetings/{name}", alice).then().statusCode(200);
        given().when().post("/greetings/{name}", alice).then().statusCode(200);

        // Greet Bob once
        given().when().post("/greetings/{name}", bob).then().statusCode(200);

        // Greet Charlie three times
        for (int i = 0; i < 3; i++) {
            given().when().post("/greetings/{name}", charlie).then().statusCode(200);
        }

        // Wait for all projections
        await()
            .atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                GreetingsCountModel aliceModel = findById(alice);
                GreetingsCountModel bobModel = findById(bob);
                GreetingsCountModel charlieModel = findById(charlie);

                assertNotNull(aliceModel, "Alice should have a record");
                assertNotNull(bobModel, "Bob should have a record");
                assertNotNull(charlieModel, "Charlie should have a record");

                assertEquals(2, aliceModel.count, "Alice count should be 2");
                assertEquals(1, bobModel.count, "Bob count should be 1");
                assertEquals(3, charlieModel.count, "Charlie count should be 3");
            });
    }

    @Test
    @DisplayName("Projection should handle rapid successive greetings")
    void testRapidSuccessiveGreetings() {
        String name = generateUniqueName();
        int totalGreetings = 5;

        // Send 5 greetings rapidly (reaches CloseState at 5)
        for (int i = 0; i < totalGreetings; i++) {
            given().when().post("/greetings/{name}", name).then().statusCode(200);
        }

        // Wait for all projections to process
        await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(200, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                GreetingsCountModel model = findById(name);
                assertNotNull(model, "Read model should exist");
                assertEquals(
                    totalGreetings,
                    model.count,
                    "All greetings should be counted even when sent rapidly"
                );
            });
    }

    @Test
    @DisplayName("UnGreeted should decrement count to zero but not below")
    void testUnGreetedDecrementsToZero() {
        String name = generateUniqueName();

        // POST 1 greeting
        given().when().post("/greetings/{name}", name).then().statusCode(200);

        // Wait for projection
        await()
            .atMost(10, TimeUnit.SECONDS)
            .until(() -> {
                GreetingsCountModel model = findById(name);
                return model != null && model.count == 1;
            });

        // DELETE (UnGreet) once
        given().when().delete("/greetings/{name}", name).then().statusCode(200);

        // Wait for projection - count should be 0
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(100, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                GreetingsCountModel model = findById(name);
                assertNotNull(model, "Read model should still exist");
                assertEquals(0, model.count, "Count should be decremented to 0");
            });

        // DELETE (UnGreet) again - count should remain at 0
        given().when().delete("/greetings/{name}", name).then().statusCode(200);

        // Wait for projection - count should still be 0 (not negative)
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(100, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                GreetingsCountModel model = findById(name);
                assertNotNull(model, "Read model should still exist");
                assertEquals(0, model.count, "Count should not go below 0");
            });
    }

    @Test
    @DisplayName("Multiple UnGreeted events should properly decrement count")
    void testMultipleUnGreetedEvents() {
        String name = generateUniqueName();

        // POST 5 greetings
        for (int i = 0; i < 5; i++) {
            given().when().post("/greetings/{name}", name).then().statusCode(200);
        }

        // Wait for all greetings to be projected
        await()
            .atMost(10, TimeUnit.SECONDS)
            .until(() -> {
                GreetingsCountModel model = findById(name);
                return model != null && model.count == 5;
            });

        // DELETE (UnGreet) 3 times
        for (int i = 0; i < 3; i++) {
            given().when().delete("/greetings/{name}", name).then().statusCode(200);
        }

        // Wait for UnGreeted events to be processed - count should be 2
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(100, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                GreetingsCountModel model = findById(name);
                assertNotNull(model, "Read model should exist");
                assertEquals(2, model.count, "Count should be decremented to 2 after 3 ungreets");
            });
    }

    @Test
    @DisplayName("Interleaved Greet and UnGreet events should be handled correctly")
    void testInterleavedGreetAndUnGreet() {
        String name = generateUniqueName();

        // POST 2 greetings
        given().when().post("/greetings/{name}", name).then().statusCode(200);
        given().when().post("/greetings/{name}", name).then().statusCode(200);

        // Wait for projection
        await()
            .atMost(10, TimeUnit.SECONDS)
            .until(() -> {
                GreetingsCountModel model = findById(name);
                return model != null && model.count == 2;
            });

        // DELETE (UnGreet) once
        given().when().delete("/greetings/{name}", name).then().statusCode(200);

        // Wait for projection - count should be 1
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(100, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                GreetingsCountModel model = findById(name);
                assertEquals(1, model.count);
            });

        // POST 2 more greetings
        given().when().post("/greetings/{name}", name).then().statusCode(200);
        given().when().post("/greetings/{name}", name).then().statusCode(200);

        // Wait for projection - count should be 3
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(100, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                GreetingsCountModel model = findById(name);
                assertEquals(3, model.count);
            });

        // DELETE (UnGreet) once
        given().when().delete("/greetings/{name}", name).then().statusCode(200);

        // Wait for projection - count should be 2
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(100, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                GreetingsCountModel model = findById(name);
                assertEquals(2, model.count, "Final count should be 2 after interleaved operations");
            });
    }
}
