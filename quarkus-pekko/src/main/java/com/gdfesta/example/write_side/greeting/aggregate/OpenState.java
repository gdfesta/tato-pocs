package com.gdfesta.example.write_side.greeting.aggregate;

import java.util.List;

public record OpenState(String name, int count, int maxCount) implements GreetingState {
    @Override
    public List<GreetingEvent> onCommand(GreetingCommand.NonGet command) {
        return switch (command) {
            case GreetingCommand.Greet greet -> {
                if (name != null && !name.equals(greet.name())) {
                    throw new IllegalStateException(
                        "Cannot greet different name. Expected: " + name + ", got: " + greet.name()
                    );
                }
                yield List.of(new GreetingEvent.Greeted(greet.name()));
            }
            case GreetingCommand.UnGreet unGreet -> {
                if (name == null) {
                    throw new IllegalStateException(
                        "Cannot ungreet when state has no name"
                    );
                }
                yield List.of(new GreetingEvent.UnGreeted(name));
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

    private GreetingState incremented(String eventName) {
        String stateName = (name == null) ? eventName : name;
        return (count + 1 == maxCount)
            ? new CloseState(stateName, count + 1)
            : new OpenState(stateName, count + 1, maxCount);
    }

    private GreetingState decremented(String eventName) {
        String stateName = (name == null) ? eventName : name;
        int newCount = Math.max(count - 1, 0);
        return new OpenState(stateName, newCount, maxCount);
    }
}
