package com.gdfesta.example.kafka;

import com.gdfesta.example.kafka.producer.GreetingKafkaProducer;
import com.gdfesta.example.kafka.producer.model.GreetingKafkaMessage.Greeted;
import com.gdfesta.example.write_side.greeting.aggregate.GreetingEvent;
import com.gdfesta.quarkus.pekko.HibernateJdbcSession;
import org.apache.pekko.projection.eventsourced.EventEnvelope;
import org.apache.pekko.projection.jdbc.javadsl.JdbcHandler;

public class GreetingsKafkaHandler
    extends JdbcHandler<EventEnvelope<GreetingEvent>, HibernateJdbcSession> {

    private final GreetingKafkaProducer producer;

    public GreetingsKafkaHandler(GreetingKafkaProducer producer) {
        this.producer = producer;
    }

    @Override
    public void process(HibernateJdbcSession session, EventEnvelope<GreetingEvent> envelope)
        throws Exception {
        switch (envelope.event()) {
            case GreetingEvent.Greeted e -> producer
                .publish(new Greeted(e.name()))
                .toCompletableFuture()
                .get();
            default -> {
                // No action
            }
        }
    }
}
