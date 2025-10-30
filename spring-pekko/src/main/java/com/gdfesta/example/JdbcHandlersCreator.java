package com.gdfesta.example;

import com.gdfesta.example.kafka.GreetingsKafkaHandler;
import com.gdfesta.example.kafka.producer.GreetingKafkaProducer;
import com.gdfesta.example.read_side.greetings_count.GreetingsCountReadSideHandler;
import com.gdfesta.example.read_side.greetings_count.GreetingsCountRepository;
import com.gdfesta.example.write_side.greeting.aggregate.GreetingActorBehavior;
import com.gdfesta.springboot.pekko.HibernateSessionFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.cluster.sharding.typed.javadsl.ShardedDaemonProcess;
import org.apache.pekko.persistence.jdbc.query.javadsl.JdbcReadJournal;
import org.apache.pekko.projection.ProjectionBehavior;
import org.apache.pekko.projection.ProjectionId;
import org.apache.pekko.projection.eventsourced.javadsl.EventSourcedProvider;
import org.apache.pekko.projection.jdbc.javadsl.JdbcProjection;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JdbcHandlersCreator {

    private final HibernateSessionFactory sessionProvider;
    private final GreetingsCountRepository greetingsCountRepository;
    private final GreetingKafkaProducer greetingKafkaProducer;
    private final ActorSystem<Void> actorSystem;

    public JdbcHandlersCreator(
        HibernateSessionFactory sessionProvider,
        GreetingsCountRepository greetingsCountRepository,
        GreetingKafkaProducer greetingKafkaProducer,
        ActorSystem<Void> actorSystem
    ) {
        this.sessionProvider = sessionProvider;
        this.greetingsCountRepository = greetingsCountRepository;
        this.greetingKafkaProducer = greetingKafkaProducer;
        this.actorSystem = actorSystem;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(2)
    public void onApplicationReady() {
        log.info("Initializing JdbcHandlers...");

        ShardedDaemonProcess.get(actorSystem).init(
            ProjectionBehavior.Command.class,
            "greetings-count-readside-daemon",
            GreetingActorBehavior.tags.size(),
            id -> {
                var tag = GreetingActorBehavior.tags.get(id);
                return ProjectionBehavior.create(
                    JdbcProjection.exactlyOnce(
                        ProjectionId.of("greetings-count-read-side-projection", tag),
                        EventSourcedProvider.eventsByTag(
                            actorSystem,
                            JdbcReadJournal.Identifier(),
                            tag
                        ),
                        sessionProvider::newInstance,
                        () -> new GreetingsCountReadSideHandler(greetingsCountRepository),
                        actorSystem
                    )
                );
            },
            ProjectionBehavior.stopMessage()
        );

        ShardedDaemonProcess.get(actorSystem).init(
            ProjectionBehavior.Command.class,
            "greetings-kafka-daemon",
            GreetingActorBehavior.tags.size(),
            id -> {
                var tag = GreetingActorBehavior.tags.get(id);
                return ProjectionBehavior.create(
                    JdbcProjection.exactlyOnce(
                        ProjectionId.of("greetings-kafka-projection", tag),
                        EventSourcedProvider.eventsByTag(
                            actorSystem,
                            JdbcReadJournal.Identifier(),
                            tag
                        ),
                        sessionProvider::newInstance,
                        () -> new GreetingsKafkaHandler(greetingKafkaProducer),
                        actorSystem
                    )
                );
            },
            ProjectionBehavior.stopMessage()
        );
    }
}
