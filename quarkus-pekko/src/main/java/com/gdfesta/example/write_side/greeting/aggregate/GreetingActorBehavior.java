package com.gdfesta.example.write_side.greeting.aggregate;

import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.cluster.sharding.typed.javadsl.EntityTypeKey;
import org.apache.pekko.cluster.sharding.typed.javadsl.EntityContext;
import org.apache.pekko.pattern.StatusReply;
import org.apache.pekko.persistence.typed.PersistenceId;
import org.apache.pekko.persistence.typed.javadsl.CommandHandler;
import org.apache.pekko.persistence.typed.javadsl.EventHandler;
import org.apache.pekko.persistence.typed.javadsl.EventSourcedBehavior;
import org.apache.pekko.persistence.typed.javadsl.RetentionCriteria;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class GreetingActorBehavior
        extends EventSourcedBehavior<GreetingCommand, GreetingEvent, GreetingState> {

    public static final EntityTypeKey<GreetingCommand> ENTITY_TYPE_KEY = EntityTypeKey.create(GreetingCommand.class,
            "greeting-aggregate");

    public static final List<String> tags = Collections
            .unmodifiableList(Arrays.asList("greeting-0", "greeting-1", "greeting-2", "greeting-3", "greeting-4"));

    public static final Behavior<GreetingCommand> create(EntityContext<GreetingCommand> entityContext) {
        return new GreetingActorBehavior(
                PersistenceId.of(entityContext.getEntityTypeKey().name(), entityContext.getEntityId()));
    }

    private GreetingActorBehavior(PersistenceId persistenceId) {
        super(persistenceId);
    }

    @Override
    public GreetingState emptyState() {
        return new OpenState(0, 5);
    }

    @Override
    public CommandHandler<GreetingCommand, GreetingEvent, GreetingState> commandHandler() {
        return newCommandHandlerBuilder()
                .forAnyState()
                .onCommand(GreetingCommand.Get.class, (state, command) -> Effect().reply(command.replyTo(), state))
                .onCommand(GreetingCommand.NonGet.class,
                        (state, command) -> {
                            try {
                                var events = state.onCommand(command);

                                var effect = events.isEmpty() ? Effect().none() : Effect().persist(events);

                                return effect.thenRun(newState -> {
                                    command.replyTo().tell(StatusReply.success(newState));
                                });
                            } catch (IllegalStateException e) {
                                return Effect().reply(command.replyTo(), StatusReply.error(e));
                            }
                        })
                .build();
    }

    @Override
    public EventHandler<GreetingState, GreetingEvent> eventHandler() {
        return newEventHandlerBuilder()
                .forAnyState()
                .onAnyEvent((state, event) -> state.onEvent(event));
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