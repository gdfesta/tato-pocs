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

    @Column(name = "count")
    public Integer count;

    @Column(name = "last_greeted_at")
    public Instant lastGreetedAt;

    public GreetingsCountModel() {}

    public GreetingsCountModel(String name, Integer count, Instant lastGreetedAt) {
        this.name = name;
        this.count = count;
        this.lastGreetedAt = lastGreetedAt;
    }
}
