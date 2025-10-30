package com.gdfesta.example;

import com.gdfesta.example.write_side.greeting.aggregate.GreetingActorBehavior;
import lombok.extern.slf4j.Slf4j;
import org.apache.pekko.cluster.sharding.typed.javadsl.ClusterSharding;
import org.apache.pekko.cluster.sharding.typed.javadsl.Entity;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ShardsCreator {

    private final ClusterSharding sharding;

    public ShardsCreator(ClusterSharding sharding) {
        this.sharding = sharding;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(1)
    public void onApplicationReady() {
        log.info("Initializing Shards...");

        // Initialize the sharding for GreetingActor
        sharding.init(
            Entity.of(GreetingActorBehavior.ENTITY_TYPE_KEY, GreetingActorBehavior::create)
        );
    }
}
