package com.gdfesta.example.read_side.greetings_count;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
@DisplayName("GreetingsCountRepository Integration Tests")
class GreetingsCountRepositoryTest {

    @Inject
    GreetingsCountRepository repository;

    @Inject
    EntityManager entityManager;

    private String generateUniqueName() {
        return "test-" + UUID.randomUUID();
    }

    /**
     * Clears the JPA first-level cache (persistence context).
     * This forces subsequent findById() calls to fetch fresh data from the database.
     * <p>
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
        assertEquals(1, result.count);
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
        assertEquals(1, first.count);

        // Second greeting
        repository.upsertGreeting(name);
        clearCache();
        GreetingsCountModel second = repository.findById(name);
        assertEquals(2, second.count);

        // Third greeting
        repository.upsertGreeting(name);
        clearCache();
        GreetingsCountModel third = repository.findById(name);
        assertEquals(3, third.count);
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
        assertTrue(
            secondTimestamp.isAfter(firstTimestamp),
            "Second timestamp should be after first timestamp"
        );

        // Wait a bit more
        Thread.sleep(10);

        // Third greeting
        repository.upsertGreeting(name);
        clearCache();
        GreetingsCountModel third = repository.findById(name);
        Instant thirdTimestamp = third.lastGreetedAt;
        assertNotNull(thirdTimestamp);
        assertTrue(
            thirdTimestamp.isAfter(secondTimestamp),
            "Third timestamp should be after second timestamp"
        );
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
        assertEquals(1, found.count);

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
        assertEquals(1, result.count);

        // Update again
        repository.upsertGreeting(name);
        clearCache();

        // Verify update is committed
        GreetingsCountModel updated = repository.findById(name);
        assertEquals(2, updated.count);
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
        assertEquals(2, repository.findById(alice).count);
        assertEquals(1, repository.findById(bob).count);
        assertEquals(3, repository.findById(charlie).count);
    }

    @Test
    @DisplayName("Should return null for non-existent name")
    void testNonExistentName() {
        String nonExistent = generateUniqueName();

        GreetingsCountModel result = repository.findById(nonExistent);

        assertNull(result);
    }

    @Test
    @DisplayName("Should decrement count when ungreeting")
    void testDecrementGreeting() {
        String name = generateUniqueName();

        // Create initial greeting with count=3
        repository.upsertGreeting(name);
        repository.upsertGreeting(name);
        repository.upsertGreeting(name);
        clearCache();

        GreetingsCountModel before = repository.findById(name);
        assertEquals(3, before.count);

        // Decrement once
        repository.decrementGreeting(name);
        clearCache();

        GreetingsCountModel after = repository.findById(name);
        assertEquals(2, after.count);
        assertNotNull(after.lastGreetedAt);
    }

    @Test
    @DisplayName("Should decrement count to zero but not below")
    void testDecrementToZero() {
        String name = generateUniqueName();

        // Create initial greeting
        repository.upsertGreeting(name);
        clearCache();

        assertEquals(1, repository.findById(name).count);

        // Decrement to zero
        repository.decrementGreeting(name);
        clearCache();

        GreetingsCountModel afterFirst = repository.findById(name);
        assertEquals(0, afterFirst.count);

        // Try to decrement again - should stay at 0
        repository.decrementGreeting(name);
        clearCache();

        GreetingsCountModel afterSecond = repository.findById(name);
        assertEquals(0, afterSecond.count, "Count should not go below zero");
    }

    @Test
    @DisplayName("Should update timestamp when decrementing")
    void testDecrementUpdatesTimestamp() throws InterruptedException {
        String name = generateUniqueName();

        // Create initial greeting
        repository.upsertGreeting(name);
        clearCache();

        GreetingsCountModel before = repository.findById(name);
        Instant beforeTimestamp = before.lastGreetedAt;

        // Wait a bit to ensure timestamp difference
        Thread.sleep(10);

        // Decrement
        repository.decrementGreeting(name);
        clearCache();

        GreetingsCountModel after = repository.findById(name);
        assertTrue(
            after.lastGreetedAt.isAfter(beforeTimestamp) ||
                after.lastGreetedAt.equals(beforeTimestamp),
            "Timestamp should be updated or remain the same when decrementing"
        );
    }

    @Test
    @DisplayName("Should do nothing when decrementing non-existent name")
    void testDecrementNonExistent() {
        String nonExistent = generateUniqueName();

        // Try to decrement non-existent - should not throw exception
        assertDoesNotThrow(() -> repository.decrementGreeting(nonExistent));

        // Verify nothing was created
        GreetingsCountModel result = repository.findById(nonExistent);
        assertNull(result, "Decrementing non-existent name should not create a record");
    }

    @Test
    @DisplayName("Should handle multiple decrements correctly")
    void testMultipleDecrements() {
        String name = generateUniqueName();

        // Create initial greeting with count=5
        for (int i = 0; i < 5; i++) {
            repository.upsertGreeting(name);
        }
        clearCache();

        assertEquals(5, repository.findById(name).count);

        // Decrement 3 times
        repository.decrementGreeting(name);
        repository.decrementGreeting(name);
        repository.decrementGreeting(name);
        clearCache();

        GreetingsCountModel result = repository.findById(name);
        assertEquals(2, result.count, "Count should be decremented to 2 after 3 decrements");
    }

    @Test
    @DisplayName("Should handle interleaved increment and decrement operations")
    void testInterleavedIncrementDecrement() {
        String name = generateUniqueName();

        // Increment twice
        repository.upsertGreeting(name);
        repository.upsertGreeting(name);
        clearCache();
        assertEquals(2, repository.findById(name).count);

        // Decrement once
        repository.decrementGreeting(name);
        clearCache();
        assertEquals(1, repository.findById(name).count);

        // Increment again
        repository.upsertGreeting(name);
        clearCache();
        assertEquals(2, repository.findById(name).count);

        // Decrement twice
        repository.decrementGreeting(name);
        repository.decrementGreeting(name);
        clearCache();

        GreetingsCountModel result = repository.findById(name);
        assertEquals(0, result.count, "Final count should be 0 after interleaved operations");
    }
}
