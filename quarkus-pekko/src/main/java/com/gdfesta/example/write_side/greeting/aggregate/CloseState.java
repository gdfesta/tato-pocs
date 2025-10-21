package com.gdfesta.example.write_side.greeting.aggregate;

import java.util.List;
import java.util.Optional;

public record CloseState(Optional<String> name, int count) implements GreetingState {
    @Override
    public List<GreetingEvent> onCommand(GreetingCommand.NonGet command) {
        switch (command) {
            case GreetingCommand.UnGreet unGreet -> {
                if (name.isEmpty()) {
                    throw new IllegalStateException(
                        "Cannot ungreet when state has no name"
                    );
                }
                return List.of(new GreetingEvent.UnGreeted(name.get()));
            }
            case GreetingCommand.Greet greet -> {
                if (name.isPresent() && !name.get().equals(greet.name())) {
                    throw new IllegalStateException(
                        "Cannot greet different name. Expected: " + name.get() + ", got: " + greet.name()
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
                Optional<String> stateName = name.isEmpty() ? Optional.of(unGreeted.name()) : name;
                return new OpenState(stateName, count - 1, count);
            }
            case GreetingEvent.Greeted ignored -> throw new IllegalStateException(
                "Cannot greet more than " + count + " times"
            );
        }
    }
}
