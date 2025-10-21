package com.gdfesta.example.read_side.greetings_count;

import com.gdfesta.example.write_side.greeting.aggregate.GreetingEvent;
import com.gdfesta.quarkus.pekko.HibernateJdbcSession;
import org.apache.pekko.projection.eventsourced.EventEnvelope;
import org.apache.pekko.projection.jdbc.javadsl.JdbcHandler;

public class GreetingsCountReadSideHandler
    extends JdbcHandler<EventEnvelope<GreetingEvent>, HibernateJdbcSession> {

    private final GreetingsCountRepository greetingsCountRepository;

    public GreetingsCountReadSideHandler(GreetingsCountRepository greetingsCountRepository) {
        this.greetingsCountRepository = greetingsCountRepository;
    }

    @Override
    public void process(HibernateJdbcSession session, EventEnvelope<GreetingEvent> envelope) {
        switch (envelope.event()) {
            case GreetingEvent.Greeted greeted -> greetingsCountRepository.upsertGreeting(
                greeted.name()
            );
            case GreetingEvent.UnGreeted ignored -> {
                String name = extractEntityIdFromPersistenceId(envelope.persistenceId());
                greetingsCountRepository.decrementGreeting(name);
            }
        }
    }

    private String extractEntityIdFromPersistenceId(String persistenceId) {
        // PersistenceId format: "entityType|entityId"
        int separatorIndex = persistenceId.indexOf('|');
        return separatorIndex >= 0 ? persistenceId.substring(separatorIndex + 1) : persistenceId;
    }
}
