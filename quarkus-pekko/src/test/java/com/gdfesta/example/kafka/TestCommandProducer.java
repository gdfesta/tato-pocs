package com.gdfesta.example.kafka;

import com.gdfesta.example.kafka.consumer.model.GreetingCommandMessage;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.concurrent.CompletionStage;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;

/**
 * Test command producer that publishes commands to Kafka for integration testing.
 * ApplicationScoped so it's shared across all tests.
 */
@Slf4j
@ApplicationScoped
public class TestCommandProducer {

    @Inject
    @Channel("greeting-commands-test")
    @OnOverflow(value = OnOverflow.Strategy.BUFFER, bufferSize = 100)
    MutinyEmitter<GreetingCommandMessage> emitter;

    private static final String FAILURE_MESSAGE =
        "Failed to publish a <GreetingCommandMessage> command";

    public CompletionStage<Void> publish(GreetingCommandMessage command) {
        var message = Message.of(command);

        var metadata = OutgoingKafkaRecordMetadata.<String>builder()
            .withKey(command.name())
            .build();

        message = message.addMetadata(metadata);

        return emitter
            .sendMessage(message)
            .invoke(
                () ->
                    log.info(
                        "Successfully published a <GreetingCommandMessage> command: {}",
                        command
                    )
            )
            .onFailure()
            .invoke(failure -> log.error(FAILURE_MESSAGE, failure))
            .subscribeAsCompletionStage();
    }
}
