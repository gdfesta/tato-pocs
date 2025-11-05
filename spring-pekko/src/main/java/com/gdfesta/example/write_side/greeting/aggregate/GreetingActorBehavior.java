package com.gdfesta.example.write_side.greeting.aggregate;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.cluster.sharding.typed.javadsl.EntityContext;
import org.apache.pekko.cluster.sharding.typed.javadsl.EntityTypeKey;
import org.apache.pekko.pattern.StatusReply;
import org.apache.pekko.persistence.typed.ReplicationId;
import org.apache.pekko.persistence.typed.javadsl.CommandHandler;
import org.apache.pekko.persistence.typed.javadsl.EventHandler;
import org.apache.pekko.persistence.typed.javadsl.ReplicatedEventSourcedBehavior;
import org.apache.pekko.persistence.typed.javadsl.ReplicationContext;
import org.apache.pekko.persistence.typed.javadsl.RetentionCriteria;

public class GreetingActorBehavior
    extends ReplicatedEventSourcedBehavior<GreetingCommand, GreetingEvent, GreetingState> {

    public static final EntityTypeKey<GreetingCommand> ENTITY_TYPE_KEY = EntityTypeKey.create(
        GreetingCommand.class,
        "greeting-aggregate"
    );

    public static final List<String> tags = List.of(
        "greeting-0",
        "greeting-1",
        "greeting-2",
        "greeting-3",
        "greeting-4"
    );

    public static Behavior<GreetingCommand> create(ReplicationContext replicationContext) {
        return new GreetingActorBehavior(replicationContext);
    }

    public GreetingActorBehavior(ReplicationContext replicationContext) {
        super(replicationContext);
    }

    @Override
    public GreetingState emptyState() {
        return new NoState(5);
    }

    @Override
    public CommandHandler<GreetingCommand, GreetingEvent, GreetingState> commandHandler() {
        return newCommandHandlerBuilder()
            .forAnyState()
            .onCommand(
                GreetingCommand.Get.class,
                (state, command) -> Effect().reply(command.replyTo(), state)
            )
            .onCommand(GreetingCommand.NonGet.class, (state, command) -> {
                try {
                    var events = state.onCommand(command);

                    var effect = events.isEmpty() ? Effect().none() : Effect().persist(events);

                    return effect.thenRun(
                        newState -> command.replyTo().tell(StatusReply.success(newState))
                    );
                } catch (IllegalStateException e) {
                    return Effect().reply(command.replyTo(), StatusReply.error(e));
                }
            })
            .build();
    }

    @Override
    public EventHandler<GreetingState, GreetingEvent> eventHandler() {
        return newEventHandlerBuilder().forAnyState().onAnyEvent(GreetingState::onEvent);
    }

    @Override
    public RetentionCriteria retentionCriteria() {
        return RetentionCriteria.snapshotEvery(100, 3);
    }

    @Override
    public Set<String> tagsFor(GreetingEvent event) {
        int n = Math.abs(this.persistenceId().entityId().hashCode() % tags.size());
        String selectedTag = tags.get(n);
        return Collections.singleton(selectedTag);
    }
}
