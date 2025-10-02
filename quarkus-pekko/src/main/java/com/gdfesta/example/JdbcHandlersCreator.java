package com.gdfesta.example;

import com.gdfesta.example.kafka.GreetingsKafkaHandler;
import com.gdfesta.example.kafka.producer.GreetingKafkaProducer;
import com.gdfesta.example.read_side.greetings_count.GreetingsCountReadSideHandler;
import com.gdfesta.example.read_side.greetings_count.GreetingsCountRepository;
import com.gdfesta.example.write_side.greeting.aggregate.GreetingActorBehavior;
import com.gdfesta.quarkus.pekko.HibernateSessionFactory;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.cluster.sharding.typed.javadsl.ShardedDaemonProcess;
import org.apache.pekko.persistence.jdbc.query.javadsl.JdbcReadJournal;
import org.apache.pekko.projection.ProjectionBehavior;
import org.apache.pekko.projection.ProjectionId;
import org.apache.pekko.projection.eventsourced.javadsl.EventSourcedProvider;
import org.apache.pekko.projection.jdbc.javadsl.JdbcProjection;
import org.jboss.logging.Logger;

@ApplicationScoped
public class JdbcHandlersCreator {

    private static final Logger LOG = Logger.getLogger(JdbcHandlersCreator.class);

    @Inject
    HibernateSessionFactory sessionProvider;

    @Inject
    GreetingsCountRepository greetingsCountRepository;

    @Inject
    GreetingKafkaProducer greetingKafkaProducer;

    @Inject
    ActorSystem<Void> actorSystem;

    void onStart(@Observes @Priority(1002) StartupEvent event) {
        LOG.info("Initializing JdbcHandlers...");

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
