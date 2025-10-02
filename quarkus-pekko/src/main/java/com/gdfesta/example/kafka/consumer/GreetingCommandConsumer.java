package com.gdfesta.example.kafka.consumer;

import com.gdfesta.example.kafka.consumer.model.GreetingCommandMessage;
import com.gdfesta.example.write_side.greeting.services.GreetingService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.concurrent.CompletionStage;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@Slf4j
@ApplicationScoped
public class GreetingCommandConsumer {

    @Inject
    GreetingService greetingService;

    @Incoming("greeting-commands")
    public CompletionStage<Void> consume(GreetingCommandMessage command) {
        log.info("Received command: {}", command);

        return switch (command) {
            case GreetingCommandMessage.GreetCommand greet -> processGreet(greet);
            case GreetingCommandMessage.UnGreetCommand unGreet -> processUnGreet(unGreet);
        };
    }

    private CompletionStage<Void> processGreet(GreetingCommandMessage.GreetCommand command) {
        log.info("Processing Greet command for: {}", command.name());
        return greetingService
            .greet(command.name())
            .invoke(state -> log.info("Greet successful for {}: {}", command.name(), state))
            .onFailure()
            .invoke(failure -> log.error("Failed to greet {}", command.name(), failure))
            .replaceWithVoid()
            .subscribeAsCompletionStage();
    }

    private CompletionStage<Void> processUnGreet(GreetingCommandMessage.UnGreetCommand command) {
        log.info("Processing UnGreet command for: {}", command.name());
        return greetingService
            .ungreet(command.name())
            .invoke(state -> log.info("UnGreet successful for {}: {}", command.name(), state))
            .onFailure()
            .invoke(failure -> log.error("Failed to ungreet {}", command.name(), failure))
            .replaceWithVoid()
            .subscribeAsCompletionStage();
    }
}
