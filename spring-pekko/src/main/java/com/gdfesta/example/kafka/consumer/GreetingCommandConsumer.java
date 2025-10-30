package com.gdfesta.example.kafka.consumer;

import com.gdfesta.example.kafka.consumer.model.GreetingCommandMessage;
import com.gdfesta.example.write_side.greeting.services.GreetingService;
import java.util.concurrent.CompletionStage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GreetingCommandConsumer {

    private final GreetingService greetingService;

    public GreetingCommandConsumer(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    @KafkaListener(topics = "command.greeting", groupId = "greeting-command-consumer-group")
    public void consume(GreetingCommandMessage command) {
        log.info("Received command: {}", command);

        switch (command) {
            case GreetingCommandMessage.GreetCommand greet -> processGreet(greet);
            case GreetingCommandMessage.UnGreetCommand unGreet -> processUnGreet(unGreet);
        }
    }

    private void processGreet(GreetingCommandMessage.GreetCommand command) {
        log.info("Processing Greet command for: {}", command.name());
        greetingService
            .greet(command.name())
            .thenAccept(state -> log.info("Greet successful for {}: {}", command.name(), state))
            .exceptionally(failure -> {
                log.error("Failed to greet {}", command.name(), failure);
                return null;
            });
    }

    private void processUnGreet(GreetingCommandMessage.UnGreetCommand command) {
        log.info("Processing UnGreet command for: {}", command.name());
        greetingService
            .ungreet(command.name())
            .thenAccept(state -> log.info("UnGreet successful for {}: {}", command.name(), state))
            .exceptionally(failure -> {
                log.error("Failed to ungreet {}", command.name(), failure);
                return null;
            });
    }
}
