package com.gdfesta.example.read_side.greetings_count;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "greetings_count")
public class GreetingsCountModel {

    @Id
    public String name;

    @Column(name = "greeting_count")
    public Integer greetingCount;

    @Column(name = "last_greeted_at")
    public Instant lastGreetedAt;

    public GreetingsCountModel() {}

    public GreetingsCountModel(String name, Integer greetingCount, Instant lastGreetedAt) {
        this.name = name;
        this.greetingCount = greetingCount;
        this.lastGreetedAt = lastGreetedAt;
    }
}
