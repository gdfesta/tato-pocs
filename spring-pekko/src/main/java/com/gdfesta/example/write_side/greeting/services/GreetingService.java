package com.gdfesta.example.write_side.greeting.services;

import com.gdfesta.example.write_side.greeting.aggregate.GreetingActorBehavior;
import com.gdfesta.example.write_side.greeting.aggregate.GreetingCommand;
import com.gdfesta.example.write_side.greeting.aggregate.GreetingState;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.cluster.sharding.typed.javadsl.ClusterSharding;
import org.apache.pekko.pattern.StatusReply;
import org.springframework.stereotype.Service;

@Service
public class GreetingService {

    private final ClusterSharding sharding;

    public GreetingService(ClusterSharding sharding) {
        this.sharding = sharding;
    }

    public CompletionStage<GreetingState> get(String name) {
        var entityRef = sharding.entityRefFor(GreetingActorBehavior.ENTITY_TYPE_KEY, name);
        return entityRef.ask(GreetingCommand.Get::new, Duration.ofSeconds(5));
    }

    public CompletionStage<GreetingState> greet(String name) {
        var entityRef = sharding.entityRefFor(GreetingActorBehavior.ENTITY_TYPE_KEY, name);
        return entityRef
            .<StatusReply<GreetingState>>ask(
                replyTo -> new GreetingCommand.Greet(name, replyTo),
                Duration.ofSeconds(5)
            )
            .thenCompose(this::toCompletionStage);
    }

    public CompletionStage<GreetingState> ungreet(String name) {
        var entityRef = sharding.entityRefFor(GreetingActorBehavior.ENTITY_TYPE_KEY, name);
        return entityRef
            .<StatusReply<GreetingState>>ask(
                replyTo -> new GreetingCommand.UnGreet(replyTo),
                Duration.ofSeconds(5)
            )
            .thenCompose(this::toCompletionStage);
    }

    private CompletionStage<GreetingState> toCompletionStage(StatusReply<GreetingState> statusReply) {
        if (statusReply.isSuccess()) {
            var state = statusReply.getValue();
            return CompletableFuture.completedFuture(state);
        } else {
            return CompletableFuture.failedFuture(statusReply.getError());
        }
    }
}
