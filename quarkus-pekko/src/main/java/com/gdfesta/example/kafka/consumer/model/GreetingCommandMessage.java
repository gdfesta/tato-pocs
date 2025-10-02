package com.gdfesta.example.kafka.consumer.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "commandType"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = GreetingCommandMessage.GreetCommand.class, name = "Greet"),
    @JsonSubTypes.Type(value = GreetingCommandMessage.UnGreetCommand.class, name = "UnGreet")
})
public sealed interface GreetingCommandMessage {
    String name();

    @JsonTypeName("Greet")
    record GreetCommand(String name) implements GreetingCommandMessage {
    }

    @JsonTypeName("UnGreet")
    record UnGreetCommand(String name) implements GreetingCommandMessage {
    }
}
