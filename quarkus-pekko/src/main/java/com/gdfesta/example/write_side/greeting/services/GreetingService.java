package com.gdfesta.example.write_side.greeting.services;

import com.gdfesta.example.write_side.greeting.aggregate.GreetingActorBehavior;
import com.gdfesta.example.write_side.greeting.aggregate.GreetingCommand;
import com.gdfesta.example.write_side.greeting.aggregate.GreetingState;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import org.apache.pekko.cluster.sharding.typed.javadsl.ClusterSharding;
import org.apache.pekko.pattern.StatusReply;

@ApplicationScoped
public class GreetingService {

    @Inject
    ClusterSharding sharding;

    public Uni<GreetingState> get(String name) {
        var entityRef = sharding.entityRefFor(GreetingActorBehavior.ENTITY_TYPE_KEY, name);
        return Uni.createFrom()
            .completionStage(entityRef.ask(GreetingCommand.Get::new, Duration.ofSeconds(5)));
    }

    public Uni<GreetingState> greet(String name) {
        var entityRef = sharding.entityRefFor(GreetingActorBehavior.ENTITY_TYPE_KEY, name);
        return Uni.createFrom()
            .completionStage(
                entityRef.<StatusReply<GreetingState>>ask(
                    replyTo -> new GreetingCommand.Greet(name, replyTo),
                    Duration.ofSeconds(5)
                )
            )
            .flatMap(this::toUni);
    }

    public Uni<GreetingState> ungreet(String name) {
        var entityRef = sharding.entityRefFor(GreetingActorBehavior.ENTITY_TYPE_KEY, name);
        return Uni.createFrom()
            .completionStage(entityRef.ask(GreetingCommand.UnGreet::new, Duration.ofSeconds(5)))
            .flatMap(this::toUni);
    }

    private Uni<GreetingState> toUni(StatusReply<GreetingState> statusReply) {
        if (statusReply.isSuccess()) {
            var state = statusReply.getValue();
            return Uni.createFrom().item(state);
        } else {
            return Uni.createFrom().failure(statusReply.getError());
        }
    }
}
