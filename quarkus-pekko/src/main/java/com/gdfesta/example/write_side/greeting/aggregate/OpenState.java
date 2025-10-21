package com.gdfesta.example.write_side.greeting.aggregate;

import java.util.List;
import java.util.Optional;

public record OpenState(Optional<String> name, int count, int maxCount) implements GreetingState {
    @Override
    public List<GreetingEvent> onCommand(GreetingCommand.NonGet command) {
        return switch (command) {
            case GreetingCommand.Greet greet -> {
                if (name.isPresent() && !name.get().equals(greet.name())) {
                    throw new IllegalStateException(
                        "Cannot greet different name. Expected: " + name.get() + ", got: " + greet.name()
                    );
                }
                yield List.of(new GreetingEvent.Greeted(greet.name()));
            }
            case GreetingCommand.UnGreet unGreet -> {
                // If no name is set, nothing to ungreet - return empty events (no-op)
                if (name.isEmpty()) {
                    yield List.of();
                }
                yield List.of(new GreetingEvent.UnGreeted(name.get()));
            }
        };
    }

    @Override
    public GreetingState onEvent(GreetingEvent event) {
        return switch (event) {
            case GreetingEvent.Greeted greeted -> incremented(greeted.name());
            case GreetingEvent.UnGreeted unGreeted -> decremented(unGreeted.name());
        };
    }

    private GreetingState incremented(String fallbackName) {
        Optional<String> stateName = name.isEmpty() ? Optional.of(fallbackName) : name;
        return (count + 1 == maxCount)
            ? new CloseState(stateName, count + 1)
            : new OpenState(stateName, count + 1, maxCount);
    }

    private GreetingState decremented(String fallbackName) {
        Optional<String> stateName = name.isEmpty() ? Optional.of(fallbackName) : name;
        int newCount = Math.max(count - 1, 0);
        return new OpenState(stateName, newCount, maxCount);
    }
}
