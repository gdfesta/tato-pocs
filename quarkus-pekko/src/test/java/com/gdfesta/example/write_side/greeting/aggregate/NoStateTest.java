package com.gdfesta.example.write_side.greeting.aggregate;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NoState Tests")
class NoStateTest {

    @Test
    @DisplayName("Should create NoState with correct maxCount and count of 0")
    void testStateCreation() {
        NoState state = new NoState(5);

        assertEquals(0, state.count());
        assertEquals(5, state.maxCount());
    }

    @Test
    @DisplayName("Should return Greeted event when processing Greet command")
    void testGreetCommand() {
        NoState state = new NoState(5);
        GreetingCommand.Greet command = new GreetingCommand.Greet("John", null);

        List<GreetingEvent> events = state.onCommand(command);

        assertEquals(1, events.size());
        assertInstanceOf(GreetingEvent.Greeted.class, events.getFirst());
        assertEquals("John", ((GreetingEvent.Greeted) events.getFirst()).name());
    }

    @Test
    @DisplayName("Should return empty events when processing UnGreet command (no-op)")
    void testUnGreetCommand() {
        NoState state = new NoState(5);
        GreetingCommand.UnGreet command = new GreetingCommand.UnGreet(null);

        List<GreetingEvent> events = state.onCommand(command);

        // Should be a no-op - return empty list since there's nothing to ungreet
        assertEquals(0, events.size());
    }

    @Test
    @DisplayName("Should transition to OpenState when processing Greeted event")
    void testGreetedEventTransitionToOpen() {
        NoState state = new NoState(5);
        GreetingEvent.Greeted event = new GreetingEvent.Greeted("Alice");

        GreetingState newState = state.onEvent(event);

        assertInstanceOf(OpenState.class, newState);
        assertEquals(Optional.of("Alice"), ((OpenState) newState).name());
        assertEquals(1, newState.count());
        assertEquals(5, ((OpenState) newState).maxCount());
    }

    @Test
    @DisplayName("Should transition to CloseState when processing Greeted event and maxCount is 1")
    void testGreetedEventTransitionToCloseWhenMaxCountOne() {
        NoState state = new NoState(1);
        GreetingEvent.Greeted event = new GreetingEvent.Greeted("Bob");

        GreetingState newState = state.onEvent(event);

        assertInstanceOf(CloseState.class, newState);
        assertEquals(Optional.of("Bob"), ((CloseState) newState).name());
        assertEquals(1, newState.count());
    }

    @Test
    @DisplayName("Should stay in NoState when processing UnGreeted event")
    void testUnGreetedEventStaysInNoState() {
        NoState state = new NoState(5);
        GreetingEvent.UnGreeted event = new GreetingEvent.UnGreeted("TestName");

        GreetingState newState = state.onEvent(event);

        assertInstanceOf(NoState.class, newState);
        assertEquals(0, newState.count());
        assertEquals(5, ((NoState) newState).maxCount());
    }

    @Test
    @DisplayName("Should return same instance when processing UnGreeted event (immutability)")
    void testUnGreetedEventReturnsSameInstance() {
        NoState state = new NoState(5);
        GreetingEvent.UnGreeted event = new GreetingEvent.UnGreeted("TestName");

        GreetingState newState = state.onEvent(event);

        assertSame(state, newState); // Same instance since state doesn't change
    }

    @Test
    @DisplayName("Should create new instance when processing Greeted event (immutability)")
    void testGreetedEventCreatesNewInstance() {
        NoState state = new NoState(5);
        GreetingEvent.Greeted event = new GreetingEvent.Greeted("Charlie");

        GreetingState newState = state.onEvent(event);

        assertNotSame(state, newState);
        assertEquals(0, state.count()); // Original unchanged
        assertEquals(1, newState.count()); // New state modified
    }
}
