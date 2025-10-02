package com.gdfesta.example.write_side.greeting.aggregate;

import com.gdfesta.example.write_side.JacksonJsonSerialization;

public sealed interface GreetingEvent extends JacksonJsonSerialization{
    public static final record Greeted(String name) implements GreetingEvent {
    }

    public static final record UnGreeted() implements GreetingEvent {
    }
}
