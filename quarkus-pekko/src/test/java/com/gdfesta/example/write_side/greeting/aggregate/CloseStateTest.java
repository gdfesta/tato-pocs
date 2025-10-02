package com.gdfesta.example.write_side.greeting.aggregate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CloseState Tests")
class CloseStateTest {

    @Test
    @DisplayName("Should create CloseState with correct count")
    void testStateCreation() {
        CloseState state = new CloseState(5);

        assertEquals(5, state.count());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when processing Greet command")
    void testGreetCommandThrowsException() {
        CloseState state = new CloseState(5);
        GreetingCommand.Greet command = new GreetingCommand.Greet("John", null);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> state.onCommand(command)
        );

        assertTrue(exception.getMessage().contains("Cannot greet more than 5 times"));
    }

    @Test
    @DisplayName("Should return UnGreeted event when processing UnGreet command")
    void testUnGreetCommand() {
        CloseState state = new CloseState(5);
        GreetingCommand.UnGreet command = new GreetingCommand.UnGreet(null);

        List<GreetingEvent> events = state.onCommand(command);

        assertEquals(1, events.size());
        assertInstanceOf(GreetingEvent.UnGreeted.class, events.get(0));
    }

    @Test
    @DisplayName("Should throw IllegalStateException when processing Greeted event")
    void testGreetedEventThrowsException() {
        CloseState state = new CloseState(5);
        GreetingEvent.Greeted event = new GreetingEvent.Greeted("Alice");

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> state.onEvent(event)
        );

        assertTrue(exception.getMessage().contains("Cannot greet more than 5 times"));
    }

    @Test
    @DisplayName("Should transition to OpenState when processing UnGreeted event")
    void testUnGreetedEventTransitionToOpen() {
        CloseState state = new CloseState(5);
        GreetingEvent.UnGreeted event = new GreetingEvent.UnGreeted();

        GreetingState newState = state.onEvent(event);

        assertInstanceOf(OpenState.class, newState);
        assertEquals(4, newState.count());

        // Verify it's OpenState with correct maxCount
        OpenState openState = (OpenState) newState;
        assertEquals(5, openState.maxCount());
    }
}
