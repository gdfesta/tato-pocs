package com.gdfesta.example.kafka.producer;

import java.util.concurrent.CompletionStage;

import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;

import com.gdfesta.example.kafka.producer.model.GreetingKafkaMessage;

@Slf4j
@ApplicationScoped
public class GreetingKafkaProducer {

    @Inject
    @Channel("greeting-events")
    @OnOverflow(value = OnOverflow.Strategy.BUFFER, bufferSize = 100)
    MutinyEmitter<GreetingKafkaMessage> emitter;

    private static final String FAILURE_MESSAGE = "Failed to publish a <GreetingKafkaMessage> message";

    public CompletionStage<Void> publish(GreetingKafkaMessage event) {
        var message = Message.of(event);

        var metadata = OutgoingKafkaRecordMetadata
            .<String>builder()
            .withKey(event.name())
            .build();

        message = message.addMetadata(metadata);

        return emitter
            .sendMessage(message)
            .invoke(() -> log.info("Successfully published a <GreetingKafkaMessage> message"))
            .onFailure()
            .invoke(failure -> log.error(FAILURE_MESSAGE, failure))
            .subscribeAsCompletionStage();
    }
}
