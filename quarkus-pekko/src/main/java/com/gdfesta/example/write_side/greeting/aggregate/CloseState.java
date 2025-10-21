package com.gdfesta.example.write_side.greeting.aggregate;

import java.util.List;

public record CloseState(String name, int count) implements GreetingState {
    @Override
    public List<GreetingEvent> onCommand(GreetingCommand.NonGet command) {
        switch (command) {
            case GreetingCommand.UnGreet unGreet -> {
                if (name == null) {
                    throw new IllegalStateException(
                        "Cannot ungreet when state has no name"
                    );
                }
                return List.of(new GreetingEvent.UnGreeted(name));
            }
            case GreetingCommand.Greet greet -> {
                if (name != null && !name.equals(greet.name())) {
                    throw new IllegalStateException(
                        "Cannot greet different name. Expected: " + name + ", got: " + greet.name()
                    );
                }
                throw new IllegalStateException(
                    "Cannot greet more than " + count + " times"
                );
            }
        }
    }

    @Override
    public GreetingState onEvent(GreetingEvent event) {
        switch (event) {
            case GreetingEvent.UnGreeted unGreeted -> {
                String stateName = (name == null) ? unGreeted.name() : name;
                return new OpenState(stateName, count - 1, count);
            }
            case GreetingEvent.Greeted ignored -> throw new IllegalStateException(
                "Cannot greet more than " + count + " times"
            );
        }
    }
}
