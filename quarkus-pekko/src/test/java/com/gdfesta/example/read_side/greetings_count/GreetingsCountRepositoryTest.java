package com.gdfesta.example.read_side.greetings_count;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@DisplayName("GreetingsCountRepository Integration Tests")
class GreetingsCountRepositoryTest {

    @Inject
    GreetingsCountRepository repository;

    @Inject
    EntityManager entityManager;

    private String generateUniqueName() {
        return "test-" + UUID.randomUUID().toString();
    }

    /**
     * Clears the JPA first-level cache (persistence context).
     * This forces subsequent findById() calls to fetch fresh data from the database.
     *
     * Why needed: In tests, the persistence context spans the entire test method,
     * so without clearing, findById() returns cached entities instead of
     * querying the database to verify actual persistence.
     */
    private void clearCache() {
        entityManager.clear();
    }

    @AfterEach
    @Transactional
    void cleanup() {
        // Clean up test data after each test
        repository.deleteAll();
    }

    @Test
    @DisplayName("Should create new record with count=1 for first greeting")
    void testFirstGreeting() {
        String name = generateUniqueName();

        repository.upsertGreeting(name);

        GreetingsCountModel result = repository.findById(name);
        assertNotNull(result);
        assertEquals(name, result.name);
        assertEquals(1, result.greetingCount);
        assertNotNull(result.lastGreetedAt);
        assertTrue(result.lastGreetedAt.isBefore(Instant.now().plusSeconds(1)));
    }

    @Test
    @DisplayName("Should increment count for subsequent greetings")
    void testSubsequentGreetings() {
        String name = generateUniqueName();

        // First greeting
        repository.upsertGreeting(name);
        clearCache();
        GreetingsCountModel first = repository.findById(name);
        assertEquals(1, first.greetingCount);

        // Second greeting
        repository.upsertGreeting(name);
        clearCache();
        GreetingsCountModel second = repository.findById(name);
        assertEquals(2, second.greetingCount);

        // Third greeting
        repository.upsertGreeting(name);
        clearCache();
        GreetingsCountModel third = repository.findById(name);
        assertEquals(3, third.greetingCount);
    }

    @Test
    @DisplayName("Should update lastGreetedAt timestamp on each greeting")
    void testTimestampUpdates() throws InterruptedException {
        String name = generateUniqueName();

        // First greeting
        repository.upsertGreeting(name);
        clearCache();
        GreetingsCountModel first = repository.findById(name);
        Instant firstTimestamp = first.lastGreetedAt;
        assertNotNull(firstTimestamp);

        // Wait a bit to ensure timestamp difference
        Thread.sleep(10);

        // Second greeting
        repository.upsertGreeting(name);
        clearCache();
        GreetingsCountModel second = repository.findById(name);
        Instant secondTimestamp = second.lastGreetedAt;
        assertNotNull(secondTimestamp);
        assertTrue(secondTimestamp.isAfter(firstTimestamp),
            "Second timestamp should be after first timestamp");

        // Wait a bit more
        Thread.sleep(10);

        // Third greeting
        repository.upsertGreeting(name);
        clearCache();
        GreetingsCountModel third = repository.findById(name);
        Instant thirdTimestamp = third.lastGreetedAt;
        assertNotNull(thirdTimestamp);
        assertTrue(thirdTimestamp.isAfter(secondTimestamp),
            "Third timestamp should be after second timestamp");
    }

    @Test
    @DisplayName("Should persist data and allow querying by ID")
    void testDatabasePersistence() {
        String name = generateUniqueName();

        repository.upsertGreeting(name);

        // Query by ID
        GreetingsCountModel found = repository.findById(name);
        assertNotNull(found);
        assertEquals(name, found.name);
        assertEquals(1, found.greetingCount);

        // Verify it's in the database by counting
        long count = repository.count("name = ?1", name);
        assertEquals(1, count);
    }

    @Test
    @DisplayName("Should properly commit transaction")
    void testTransactionBehavior() {
        String name = generateUniqueName();

        // Upsert is @Transactional, should auto-commit
        repository.upsertGreeting(name);
        clearCache();

        // Verify data is committed and visible in new query
        GreetingsCountModel result = repository.findById(name);
        assertNotNull(result);
        assertEquals(1, result.greetingCount);

        // Update again
        repository.upsertGreeting(name);
        clearCache();

        // Verify update is committed
        GreetingsCountModel updated = repository.findById(name);
        assertEquals(2, updated.greetingCount);
    }

    @Test
    @DisplayName("Should handle multiple different names independently")
    void testMultipleNames() {
        String alice = generateUniqueName();
        String bob = generateUniqueName();
        String charlie = generateUniqueName();

        // Greet Alice twice
        repository.upsertGreeting(alice);
        repository.upsertGreeting(alice);

        // Greet Bob once
        repository.upsertGreeting(bob);

        // Greet Charlie three times
        repository.upsertGreeting(charlie);
        repository.upsertGreeting(charlie);
        repository.upsertGreeting(charlie);

        // Verify each has independent count
        assertEquals(2, repository.findById(alice).greetingCount);
        assertEquals(1, repository.findById(bob).greetingCount);
        assertEquals(3, repository.findById(charlie).greetingCount);
    }

    @Test
    @DisplayName("Should return null for non-existent name")
    void testNonExistentName() {
        String nonExistent = generateUniqueName();

        GreetingsCountModel result = repository.findById(nonExistent);

        assertNull(result);
    }
}
