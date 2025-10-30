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
    @JsonSubTypes.Type(value = GreetingKafkaMessage.Greeted.class, name = "Greeted"),
    @JsonSubTypes.Type(value = GreetingKafkaMessage.UnGreeted.class, name = "UnGreeted")
})
public sealed interface GreetingKafkaMessage permits GreetingKafkaMessage.Greeted, GreetingKafkaMessage.UnGreeted {
    String name();

    @JsonTypeName("Greeted")
    record Greeted(String name) implements GreetingKafkaMessage {}

    @JsonTypeName("UnGreeted")
    record UnGreeted(String name) implements GreetingKafkaMessage {}
}
