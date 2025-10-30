package com.gdfesta.example.kafka.producer;

import com.gdfesta.example.kafka.producer.model.GreetingKafkaMessage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GreetingKafkaProducer {

    private final KafkaTemplate<String, GreetingKafkaMessage> kafkaTemplate;
    private static final String TOPIC = "event.greeting";
    private static final String FAILURE_MESSAGE =
        "Failed to publish a <GreetingKafkaMessage> message";

    public GreetingKafkaProducer(KafkaTemplate<String, GreetingKafkaMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletionStage<Object> publish(GreetingKafkaMessage event) {
        return kafkaTemplate
            .send(TOPIC, event.name(), event)
            .thenApply(result -> {
                log.info("Successfully published a <GreetingKafkaMessage> message");
                return null;
            })
            .exceptionally(ex -> {
                log.error(FAILURE_MESSAGE, ex);
                return null;
            });
    }
}
