package com.gdfesta.example;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.apache.pekko.cluster.sharding.typed.javadsl.ClusterSharding;
import org.apache.pekko.cluster.sharding.typed.javadsl.Entity;
import org.jboss.logging.Logger;

import com.gdfesta.example.write_side.greeting.aggregate.GreetingActorBehavior;

import jakarta.annotation.Priority;


@ApplicationScoped
public class ShardsCreator {

    private static final Logger LOG = Logger.getLogger(ShardsCreator.class);

    @Inject
    ClusterSharding sharding;

    void onStart(@Observes @Priority(1002) StartupEvent event) {
        LOG.info("Initializing Shards...");

        // Initialize the sharding for GreetingActor
        sharding.init(Entity.of(
                GreetingActorBehavior.ENTITY_TYPE_KEY,
                GreetingActorBehavior::create
        ));
    }
}