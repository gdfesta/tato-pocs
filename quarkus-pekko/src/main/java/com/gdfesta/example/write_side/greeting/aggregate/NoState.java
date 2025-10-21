package com.gdfesta.example.write_side.greeting.aggregate;

import java.util.List;

public record NoState(int maxCount) implements GreetingState {
    @Override
    public int count() {
        return 0;
    }

    @Override
    public List<GreetingEvent> onCommand(GreetingCommand.NonGet command) {
        return switch (command) {
            case GreetingCommand.Greet greet -> List.of(new GreetingEvent.Greeted(greet.name()));
            case GreetingCommand.UnGreet ignored -> List.of(); // No-op: nothing to ungreet
        };
    }

    @Override
    public GreetingState onEvent(GreetingEvent event) {
        return switch (event) {
            case GreetingEvent.Greeted greeted -> {
                // Transition to OpenState with count=1 and the greeted name
                int newCount = 1;
                yield (newCount == maxCount)
                    ? new CloseState(java.util.Optional.of(greeted.name()), newCount)
                    : new OpenState(java.util.Optional.of(greeted.name()), newCount, maxCount);
            }
            case GreetingEvent.UnGreeted ignored -> this; // Stay in NoState
        };
    }
}
