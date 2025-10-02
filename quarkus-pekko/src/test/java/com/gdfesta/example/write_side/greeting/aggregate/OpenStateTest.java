package com.gdfesta.example.write_side.greeting.aggregate;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OpenState Tests")
class OpenStateTest {

    @Test
    @DisplayName("Should create OpenState with correct count and maxCount")
    void testStateCreation() {
        OpenState state = new OpenState(0, 5);

        assertEquals(0, state.count());
        assertEquals(5, state.maxCount());
    }

    @Test
    @DisplayName("Should return Greeted event when processing Greet command")
    void testGreetCommand() {
        OpenState state = new OpenState(0, 5);
        GreetingCommand.Greet command = new GreetingCommand.Greet("John", null);

        List<GreetingEvent> events = state.onCommand(command);

        assertEquals(1, events.size());
        assertInstanceOf(GreetingEvent.Greeted.class, events.getFirst());
        assertEquals("John", ((GreetingEvent.Greeted) events.getFirst()).name());
    }

    @Test
    @DisplayName("Should return UnGreeted event when processing UnGreet command")
    void testUnGreetCommand() {
        OpenState state = new OpenState(2, 5);
        GreetingCommand.UnGreet command = new GreetingCommand.UnGreet(null);

        List<GreetingEvent> events = state.onCommand(command);

        assertEquals(1, events.size());
        assertInstanceOf(GreetingEvent.UnGreeted.class, events.getFirst());
    }

    @Test
    @DisplayName("Should increment count when processing Greeted event")
    void testGreetedEventIncrement() {
        OpenState state = new OpenState(0, 5);
        GreetingEvent.Greeted event = new GreetingEvent.Greeted("Alice");

        GreetingState newState = state.onEvent(event);

        assertInstanceOf(OpenState.class, newState);
        assertEquals(1, newState.count());

        // Test another increment
        GreetingState nextState = newState.onEvent(event);
        assertInstanceOf(OpenState.class, nextState);
        assertEquals(2, nextState.count());
    }

    @Test
    @DisplayName("Should transition to CloseState when count reaches maxCount")
    void testGreetedEventTransitionToClosed() {
        OpenState state = new OpenState(4, 5);
        GreetingEvent.Greeted event = new GreetingEvent.Greeted("Bob");

        GreetingState newState = state.onEvent(event);

        assertInstanceOf(CloseState.class, newState);
        assertEquals(5, newState.count());
    }

    @Test
    @DisplayName("Should decrement count when processing UnGreeted event")
    void testUnGreetedEventDecrement() {
        OpenState state = new OpenState(3, 5);
        GreetingEvent.UnGreeted event = new GreetingEvent.UnGreeted();

        GreetingState newState = state.onEvent(event);

        assertInstanceOf(OpenState.class, newState);
        assertEquals(2, newState.count());

        // Test another decrement
        GreetingState nextState = newState.onEvent(event);
        assertInstanceOf(OpenState.class, nextState);
        assertEquals(1, nextState.count());
    }

    @Test
    @DisplayName("Should not decrement count below zero")
    void testUnGreetedEventFloorAtZero() {
        OpenState state = new OpenState(0, 5);
        GreetingEvent.UnGreeted event = new GreetingEvent.UnGreeted();

        GreetingState newState = state.onEvent(event);

        assertInstanceOf(OpenState.class, newState);
        assertEquals(0, newState.count());
    }

    @Test
    @DisplayName("Should return new state instances (immutability)")
    void testStateImmutability() {
        OpenState originalState = new OpenState(2, 5);
        GreetingEvent.Greeted event = new GreetingEvent.Greeted("Charlie");

        GreetingState newState = originalState.onEvent(event);

        assertNotSame(originalState, newState);
        assertEquals(2, originalState.count()); // Original unchanged
        assertEquals(3, newState.count()); // New state modified
    }
}
