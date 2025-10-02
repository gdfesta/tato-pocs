package com.gdfesta.example.kafka.producer.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "messageType"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = GreetingKafkaMessage.Greeted.class, name = "Greeted")
})
public sealed interface GreetingKafkaMessage {
    String name();

    @JsonTypeName("Greeted")
    public final record Greeted(String name) implements GreetingKafkaMessage {
    }
}