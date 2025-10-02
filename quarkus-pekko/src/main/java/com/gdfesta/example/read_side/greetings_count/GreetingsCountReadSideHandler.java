package com.gdfesta.example.read_side.greetings_count;

import org.apache.pekko.projection.jdbc.javadsl.JdbcHandler;

import com.gdfesta.example.write_side.greeting.aggregate.GreetingEvent;
import com.gdfesta.quarkus.pekko.HibernateJdbcSession;

import org.apache.pekko.projection.eventsourced.EventEnvelope;

public class GreetingsCountReadSideHandler extends JdbcHandler<EventEnvelope<GreetingEvent>, HibernateJdbcSession> {

    private final GreetingsCountRepository greetingsCountRepository;

    public GreetingsCountReadSideHandler(GreetingsCountRepository greetingsCountRepository) {
        this.greetingsCountRepository = greetingsCountRepository;
    }

    @Override
    public void process(HibernateJdbcSession session, EventEnvelope<GreetingEvent> envelope) throws Exception {
        switch (envelope.event()) {
            case GreetingEvent.Greeted greeted -> {
                greetingsCountRepository.upsertGreeting(greeted.name());
            }
            case GreetingEvent.UnGreeted unGreeted -> {
                // No read-side update needed
            }
        }
    }
}