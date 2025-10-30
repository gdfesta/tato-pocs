package com.gdfesta.example.read_side.greetings_count;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface GreetingsCountRepository extends JpaRepository<GreetingsCountModel, String> {
    @Transactional
    default void upsertGreeting(String name) {
        GreetingsCountModel greeting = Optional.ofNullable(findById(name).orElse(null))
            .map(existing -> new GreetingsCountModel(name, existing.count + 1, Instant.now()))
            .orElseGet(() -> new GreetingsCountModel(name, 1, Instant.now()));
        saveAndFlush(greeting);
    }

    @Transactional
    default void decrementGreeting(String name) {
        findById(name).ifPresent(existing -> {
            int newCount = Math.max(existing.count - 1, 0);
            GreetingsCountModel updated = new GreetingsCountModel(name, newCount, Instant.now());
            saveAndFlush(updated);
        });
    }
}
