package com.gdfesta.example.read_side.greetings_count;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;

@ApplicationScoped
public class GreetingsCountRepository implements PanacheRepositoryBase<GreetingsCountModel, String> {

    @Transactional
    public void upsertGreeting(String name) {
        GreetingsCountModel greeting = findById(name);

        if (greeting == null) {
            greeting = new GreetingsCountModel(name, 0, Instant.now());
        }

        greeting.name = name;
        greeting.greetingCount = greeting.greetingCount + 1;
        greeting.lastGreetedAt = Instant.now();

        persistAndFlush(greeting);
    }
}