package com.gdfesta.example.write_side.greeting.aggregate;

import java.util.List;

public record OpenState(int count, int maxCount) implements GreetingState {
    @Override
    public List<GreetingEvent> onCommand(GreetingCommand.NonGet command) {
        return switch (command) {
            case GreetingCommand.Greet greet -> List.of(new GreetingEvent.Greeted(greet.name()));
            case GreetingCommand.UnGreet unGreet -> List.of(new GreetingEvent.UnGreeted());
        };
    }

    @Override
    public GreetingState onEvent(GreetingEvent event) {
        return switch (event) {
            case GreetingEvent.Greeted e -> incremented();
            case GreetingEvent.UnGreeted e -> decremented();
        };
    }

    private GreetingState incremented() {
        int newCount = count + 1;
        if (newCount == maxCount) {
            return new CloseState(newCount);
        }
        return new OpenState(newCount, maxCount);
    }

    private GreetingState decremented() {
        int newCount = count - 1;
        if (newCount < 0) {
            newCount = 0;
        }
        return new OpenState(newCount, maxCount);
    }

}