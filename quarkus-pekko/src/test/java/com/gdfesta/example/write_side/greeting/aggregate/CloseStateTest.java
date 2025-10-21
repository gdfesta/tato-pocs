package com.gdfesta.example.write_side.greeting.aggregate;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CloseState Tests")
class CloseStateTest {

    @Test
    @DisplayName("Should create CloseState with correct name and count")
    void testStateCreation() {
        CloseState state = new CloseState(Optional.of("TestName"), 5);

        assertEquals(Optional.of("TestName"), state.name());
        assertEquals(5, state.count());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when processing Greet command")
    void testGreetCommandThrowsException() {
        CloseState state = new CloseState(Optional.of("John"), 5);
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
        CloseState state = new CloseState(Optional.of("John"), 5);
        GreetingCommand.UnGreet command = new GreetingCommand.UnGreet(null);

        List<GreetingEvent> events = state.onCommand(command);

        assertEquals(1, events.size());
        assertInstanceOf(GreetingEvent.UnGreeted.class, events.getFirst());
        assertEquals("John", ((GreetingEvent.UnGreeted) events.getFirst()).name());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when processing Greeted event")
    void testGreetedEventThrowsException() {
        CloseState state = new CloseState(Optional.of("Alice"), 5);
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
        CloseState state = new CloseState(Optional.of("TestName"), 5);
        GreetingEvent.UnGreeted event = new GreetingEvent.UnGreeted("TestName");

        GreetingState newState = state.onEvent(event);

        assertInstanceOf(OpenState.class, newState);
        OpenState openState = (OpenState) newState;
        assertEquals(Optional.of("TestName"), openState.name());
        assertEquals(4, newState.count());

        // Verify it's OpenState with correct maxCount
        assertEquals(5, openState.maxCount());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when Greet command has different name")
    void testGreetCommandWithDifferentName() {
        CloseState state = new CloseState(Optional.of("John"), 5);
        GreetingCommand.Greet command = new GreetingCommand.Greet("Jane", null);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> state.onCommand(command)
        );

        assertTrue(exception.getMessage().contains("Cannot greet different name"));
        assertTrue(exception.getMessage().contains("Expected: John"));
        assertTrue(exception.getMessage().contains("got: Jane"));
    }

    @Test
    @DisplayName("Should return empty events when UnGreet command and state has no name (no-op)")
    void testUnGreetCommandWithNoName() {
        CloseState state = new CloseState(Optional.empty(), 5);
        GreetingCommand.UnGreet command = new GreetingCommand.UnGreet(null);

        List<GreetingEvent> events = state.onCommand(command);

        // Should be a no-op - return empty list since there's nothing to ungreet
        assertEquals(0, events.size());
    }
}
