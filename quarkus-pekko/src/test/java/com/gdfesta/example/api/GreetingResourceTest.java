package com.gdfesta.example.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
@DisplayName("GreetingResource Integration Tests")
public class GreetingResourceTest {

    private String generateUniqueName() {
        return "test-" + UUID.randomUUID();
    }

    @Test
    @DisplayName("POST /greetings/{name} - First greeting returns OpenState with count=1")
    void testFirstGreeting() {
        String name = generateUniqueName();

        given()
            .when()
            .post("/greetings/{name}", name)
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("status", is("OpenState"))
            .body("count", is(1));
    }

    @Test
    @DisplayName("POST /greetings/{name} - Multiple greetings increment count")
    void testMultipleGreetings() {
        String name = generateUniqueName();

        // First greeting -> count=1
        given()
            .when()
            .post("/greetings/{name}", name)
            .then()
            .statusCode(200)
            .body("status", is("OpenState"))
            .body("count", is(1));

        // Second greeting -> count=2
        given()
            .when()
            .post("/greetings/{name}", name)
            .then()
            .statusCode(200)
            .body("status", is("OpenState"))
            .body("count", is(2));

        // Third greeting -> count=3
        given()
            .when()
            .post("/greetings/{name}", name)
            .then()
            .statusCode(200)
            .body("status", is("OpenState"))
            .body("count", is(3));

        // Fourth greeting -> count=4
        given()
            .when()
            .post("/greetings/{name}", name)
            .then()
            .statusCode(200)
            .body("status", is("OpenState"))
            .body("count", is(4));
    }

    @Test
    @DisplayName("POST /greetings/{name} - Transition to CloseState at maxCount")
    void testTransitionToCloseState() {
        String name = generateUniqueName();

        // Greet 4 times to reach count=4 (still OpenState)
        for (int i = 0; i < 4; i++) {
            given()
                .when()
                .post("/greetings/{name}", name)
                .then()
                .statusCode(200)
                .body("status", is("OpenState"));
        }

        // Fifth greeting -> count=5, transitions to CloseState
        given()
            .when()
            .post("/greetings/{name}", name)
            .then()
            .statusCode(200)
            .body("status", is("CloseState"))
            .body("count", is(5));
    }

    @Test
    @DisplayName("POST /greetings/{name} - Closed state rejects further greetings")
    void testClosedStateRejectsGreeting() {
        String name = generateUniqueName();

        // Greet 5 times to reach CloseState
        for (int i = 0; i < 5; i++) {
            given().when().post("/greetings/{name}", name).then().statusCode(200);
        }

        // Verify we're at CloseState(5)
        given()
            .when()
            .get("/greetings/{name}", name)
            .then()
            .statusCode(200)
            .body("status", is("CloseState"))
            .body("count", is(5));

        // Sixth greeting should fail with 500 error
        // Note: Error message format differs between dev (@QuarkusTest) and packaged (@QuarkusIntegrationTest) modes
        given().when().post("/greetings/{name}", name).then().statusCode(500);
    }

    @Test
    @DisplayName("DELETE /greetings/{name} - Ungreet decrements count")
    void testUngreetDecrementsCount() {
        String name = generateUniqueName();

        // Greet 5 times to reach CloseState with count=5
        for (int i = 0; i < 5; i++) {
            given().when().post("/greetings/{name}", name).then().statusCode(200);
        }

        // Verify we're at CloseState(5)
        given()
            .when()
            .get("/greetings/{name}", name)
            .then()
            .statusCode(200)
            .body("status", is("CloseState"))
            .body("count", is(5));

        // Ungreet once -> should transition to OpenState(4)
        given()
            .when()
            .delete("/greetings/{name}", name)
            .then()
            .statusCode(200)
            .body("status", is("OpenState"))
            .body("count", is(4));

        // Ungreet again -> OpenState(3)
        given()
            .when()
            .delete("/greetings/{name}", name)
            .then()
            .statusCode(200)
            .body("status", is("OpenState"))
            .body("count", is(3));

        // Ungreet again -> OpenState(2)
        given()
            .when()
            .delete("/greetings/{name}", name)
            .then()
            .statusCode(200)
            .body("status", is("OpenState"))
            .body("count", is(2));
    }

    @Test
    @DisplayName("DELETE /greetings/{name} - Ungreet from CloseState transitions to OpenState")
    void testUngreetFromCloseState() {
        String name = generateUniqueName();

        // Greet 5 times to reach CloseState(5)
        for (int i = 0; i < 5; i++) {
            given().when().post("/greetings/{name}", name).then().statusCode(200);
        }

        // Verify CloseState(5)
        given()
            .when()
            .get("/greetings/{name}", name)
            .then()
            .statusCode(200)
            .body("status", is("CloseState"))
            .body("count", is(5));

        // Ungreet should transition to OpenState(4, 5)
        given()
            .when()
            .delete("/greetings/{name}", name)
            .then()
            .statusCode(200)
            .body("status", is("OpenState"))
            .body("count", is(4));
    }

    @Test
    @DisplayName("DELETE /greetings/{name} - Ungreet at zero keeps count at zero")
    void testUngreetAtZero() {
        String name = generateUniqueName();

        // Start with OpenState(0, 5)
        given()
            .when()
            .get("/greetings/{name}", name)
            .then()
            .statusCode(200)
            .body("status", is("OpenState"))
            .body("count", is(0));

        // Ungreet at zero should stay at zero
        given()
            .when()
            .delete("/greetings/{name}", name)
            .then()
            .statusCode(200)
            .body("status", is("OpenState"))
            .body("count", is(0));
    }

    @Test
    @DisplayName("GET /greetings/{name} - Returns current state")
    void testGetReturnsCurrentState() {
        String name = generateUniqueName();

        // Initially should be OpenState(0)
        given()
            .when()
            .get("/greetings/{name}", name)
            .then()
            .statusCode(200)
            .body("status", is("OpenState"))
            .body("count", is(0));

        // Greet twice
        given().when().post("/greetings/{name}", name).then().statusCode(200);
        given().when().post("/greetings/{name}", name).then().statusCode(200);

        // Get should return OpenState(2)
        given()
            .when()
            .get("/greetings/{name}", name)
            .then()
            .statusCode(200)
            .body("status", is("OpenState"))
            .body("count", is(2));
    }

    @Test
    @DisplayName("GET /greetings/{name} - Non-existent entity returns OpenState(0)")
    void testGetNonExistentEntity() {
        String name = generateUniqueName();

        // Getting a name that was never greeted should return initial state
        given()
            .when()
            .get("/greetings/{name}", name)
            .then()
            .statusCode(200)
            .body("status", is("OpenState"))
            .body("count", is(0));
    }

    @Test
    @DisplayName("Response structure verification")
    void testResponseStructure() {
        String name = generateUniqueName();

        // Verify JSON response structure
        given()
            .when()
            .post("/greetings/{name}", name)
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("status", is("OpenState"))
            .body("count", is(1));
    }

    @Test
    @DisplayName("Full workflow: greet to max, ungreet, and greet again")
    void testFullWorkflow() {
        String name = generateUniqueName();

        // Greet 5 times to close
        for (int i = 0; i < 5; i++) {
            given().when().post("/greetings/{name}", name).then().statusCode(200);
        }

        // Verify closed
        given()
            .when()
            .get("/greetings/{name}", name)
            .then()
            .body("status", is("CloseState"))
            .body("count", is(5));

        // Ungreet to open
        given()
            .when()
            .delete("/greetings/{name}", name)
            .then()
            .body("status", is("OpenState"))
            .body("count", is(4));

        // Can greet again now
        given()
            .when()
            .post("/greetings/{name}", name)
            .then()
            .statusCode(200)
            .body("status", is("CloseState"))
            .body("count", is(5));
    }
}
