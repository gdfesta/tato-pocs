package com.gdfesta.example;

import com.gdfesta.example.write_side.greeting.aggregate.GreetingActorBehavior;
import com.gdfesta.example.write_side.greeting.aggregate.GreetingCommand;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.pekko.cluster.sharding.typed.javadsl.ClusterSharding;
import org.apache.pekko.cluster.sharding.typed.javadsl.Entity;
import org.apache.pekko.persistence.typed.ReplicaId;
import org.apache.pekko.persistence.typed.ReplicationId;
import org.apache.pekko.persistence.typed.javadsl.ReplicatedEventSourcing;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ShardsCreator {

    private final ClusterSharding sharding;
    private final String replicaIdString;

    public ShardsCreator(
        ClusterSharding sharding,
        @Value("${pekko.replicated-event-sourcing.replica-id:region-1}") String replicaId
    ) {
        this.sharding = sharding;
        this.replicaIdString = replicaId;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(1)
    public void onApplicationReady() {
        log.info("Initializing Replicated Shards for replica: {}", replicaIdString);

        // Define all replicas in the system
        // These should match the replicas configured in application.conf
        Set<ReplicaId> allReplicaIds = Set.of(
            new ReplicaId("region-1"),
            new ReplicaId("region-2"),
            new ReplicaId("region-3")
        );

        ReplicaId thisReplicaId = new ReplicaId(replicaIdString);

        // Initialize sharding with replicated event sourcing
        // Each entity will use ReplicatedEventSourcing for cross-region replication
        sharding.init(
            Entity.of(
                GreetingActorBehavior.ENTITY_TYPE_KEY,
                entityContext -> {
                    // Use ReplicatedEventSourcing factory for per-replica journal config
                    // Each replica has its own PostgreSQL database
                    return ReplicatedEventSourcing.perReplicaJournalConfig(
                        new ReplicationId(
                            "greeting-aggregate",
                            entityContext.getEntityId(),
                            thisReplicaId
                        ),
                        allReplicaIds.stream()
                            .collect(
                                java.util.stream.Collectors.toMap(
                                    r -> r,
                                    r -> "jdbc-read-journal"
                                )
                            ),
                        GreetingActorBehavior::new
                    );
                }
            )
        );

        log.info("Replicated sharding initialized successfully for replica: {}", replicaIdString);
    }
}
