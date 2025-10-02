package com.gdfesta.example.write_side.greeting.aggregate;

import java.util.List;

public final record CloseState(int count) implements GreetingState {

    @Override
    public List<GreetingEvent> onCommand(GreetingCommand.NonGet command) {
        switch (command) {
            case GreetingCommand.UnGreet unGreet -> {
                return List.of(new GreetingEvent.UnGreeted());
            }
            case GreetingCommand.Greet greet -> {
                throw new IllegalStateException("Cannot greet more than " + count + " times");
            }
        }
    }

    @Override
    public GreetingState onEvent(GreetingEvent event) {
        switch (event) {
            case GreetingEvent.UnGreeted e -> {
                return new OpenState(count - 1, count);
            }
            case GreetingEvent.Greeted e -> {
                throw new IllegalStateException("Cannot greet more than " + count + " times");
            }
        }
    }
}