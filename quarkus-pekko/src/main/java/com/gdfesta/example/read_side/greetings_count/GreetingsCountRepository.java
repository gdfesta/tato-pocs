package com.gdfesta.example.read_side.greetings_count;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Optional;

@ApplicationScoped
public class GreetingsCountRepository
    implements PanacheRepositoryBase<GreetingsCountModel, String> {

    @Transactional
    public void upsertGreeting(String name) {
        GreetingsCountModel greeting = Optional.ofNullable(findById(name))
            .map(
                existing -> new GreetingsCountModel(name, existing.count + 1, Instant.now())
            )
            .orElseGet(() -> new GreetingsCountModel(name, 1, Instant.now()));
        getEntityManager().merge(greeting);
        flush();
    }

    @Transactional
    public void decrementGreeting(String name) {
        Optional.ofNullable(findById(name))
            .ifPresent(existing -> {
                int newCount = Math.max(existing.greetingCount - 1, 0);
                GreetingsCountModel updated = new GreetingsCountModel(
                    name,
                    newCount,
                    Instant.now()
                );
                getEntityManager().merge(updated);
                flush();
            });
    }
}
