package com.gdfesta.example.write_side.greeting.aggregate;

import java.util.List;

public record OpenState(int count, int maxCount) implements GreetingState {
    @Override
    public List<GreetingEvent> onCommand(GreetingCommand.NonGet command) {
        return switch (command) {
            case GreetingCommand.Greet greet -> List.of(new GreetingEvent.Greeted(greet.name()));
            case GreetingCommand.UnGreet ignored -> List.of(new GreetingEvent.UnGreeted());
        };
    }

    @Override
    public GreetingState onEvent(GreetingEvent event) {
        return switch (event) {
            case GreetingEvent.Greeted ignored -> incremented();
            case GreetingEvent.UnGreeted ignored -> decremented();
        };
    }

    private GreetingState incremented() {
        return (count + 1 == maxCount)
                ? new CloseState(count + 1)
                : new OpenState(count + 1, maxCount);
    }

    private GreetingState decremented() {
        int newCount = Math.max(count - 1, 0);
        return new OpenState(newCount, maxCount);
    }

}