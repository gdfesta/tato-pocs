package com.gdfesta.example.write_side.greeting.aggregate;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OpenState Tests")
class OpenStateTest {

    @Test
    @DisplayName("Should create OpenState with correct name, count and maxCount")
    void testStateCreation() {
        OpenState state = new OpenState(Optional.of("TestName"), 0, 5);

        assertEquals(Optional.of("TestName"), state.name());
        assertEquals(0, state.count());
        assertEquals(5, state.maxCount());
    }

    @Test
    @DisplayName("Should return Greeted event when processing Greet command")
    void testGreetCommand() {
        OpenState state = new OpenState(Optional.of("John"), 0, 5);
        GreetingCommand.Greet command = new GreetingCommand.Greet("John", null);

        List<GreetingEvent> events = state.onCommand(command);

        assertEquals(1, events.size());
        assertInstanceOf(GreetingEvent.Greeted.class, events.getFirst());
        assertEquals("John", ((GreetingEvent.Greeted) events.getFirst()).name());
    }

    @Test
    @DisplayName("Should return UnGreeted event when processing UnGreet command")
    void testUnGreetCommand() {
        OpenState state = new OpenState(Optional.of("John"), 2, 5);
        GreetingCommand.UnGreet command = new GreetingCommand.UnGreet(null);

        List<GreetingEvent> events = state.onCommand(command);

        assertEquals(1, events.size());
        assertInstanceOf(GreetingEvent.UnGreeted.class, events.getFirst());
        assertEquals("John", ((GreetingEvent.UnGreeted) events.getFirst()).name());
    }

    @Test
    @DisplayName("Should increment count when processing Greeted event")
    void testGreetedEventIncrement() {
        OpenState state = new OpenState(Optional.empty(), 0, 5);
        GreetingEvent.Greeted event = new GreetingEvent.Greeted("Alice");

        GreetingState newState = state.onEvent(event);

        assertInstanceOf(OpenState.class, newState);
        assertEquals(Optional.of("Alice"), newState.name());
        assertEquals(1, newState.count());

        // Test another increment
        GreetingState nextState = newState.onEvent(event);
        assertInstanceOf(OpenState.class, nextState);
        assertEquals(Optional.of("Alice"), nextState.name());
        assertEquals(2, nextState.count());
    }

    @Test
    @DisplayName("Should transition to CloseState when count reaches maxCount")
    void testGreetedEventTransitionToClosed() {
        OpenState state = new OpenState(Optional.of("Bob"), 4, 5);
        GreetingEvent.Greeted event = new GreetingEvent.Greeted("Bob");

        GreetingState newState = state.onEvent(event);

        assertInstanceOf(CloseState.class, newState);
        assertEquals(Optional.of("Bob"), newState.name());
        assertEquals(5, newState.count());
    }

    @Test
    @DisplayName("Should decrement count when processing UnGreeted event")
    void testUnGreetedEventDecrement() {
        OpenState state = new OpenState(Optional.of("TestName"), 3, 5);
        GreetingEvent.UnGreeted event = new GreetingEvent.UnGreeted("TestName");

        GreetingState newState = state.onEvent(event);

        assertInstanceOf(OpenState.class, newState);
        assertEquals(Optional.of("TestName"), newState.name());
        assertEquals(2, newState.count());

        // Test another decrement
        GreetingState nextState = newState.onEvent(event);
        assertInstanceOf(OpenState.class, nextState);
        assertEquals(Optional.of("TestName"), nextState.name());
        assertEquals(1, nextState.count());
    }

    @Test
    @DisplayName("Should not decrement count below zero")
    void testUnGreetedEventFloorAtZero() {
        OpenState state = new OpenState(Optional.of("TestName"), 0, 5);
        GreetingEvent.UnGreeted event = new GreetingEvent.UnGreeted("TestName");

        GreetingState newState = state.onEvent(event);

        assertInstanceOf(OpenState.class, newState);
        assertEquals(Optional.of("TestName"), newState.name());
        assertEquals(0, newState.count());
    }

    @Test
    @DisplayName("Should return new state instances (immutability)")
    void testStateImmutability() {
        OpenState originalState = new OpenState(Optional.of("Charlie"), 2, 5);
        GreetingEvent.Greeted event = new GreetingEvent.Greeted("Charlie");

        GreetingState newState = originalState.onEvent(event);

        assertNotSame(originalState, newState);
        assertEquals(2, originalState.count()); // Original unchanged
        assertEquals(3, newState.count()); // New state modified
    }

    @Test
    @DisplayName("Should throw IllegalStateException when Greet command has different name")
    void testGreetCommandWithDifferentName() {
        OpenState state = new OpenState(Optional.of("John"), 0, 5);
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
        OpenState state = new OpenState(Optional.empty(), 2, 5);
        GreetingCommand.UnGreet command = new GreetingCommand.UnGreet(null);

        List<GreetingEvent> events = state.onCommand(command);

        // Should be a no-op - return empty list since there's nothing to ungreet
        assertEquals(0, events.size());
    }
}
