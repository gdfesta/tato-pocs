package com.gdfesta.example.write_side.greeting.aggregate;

import com.gdfesta.example.write_side.JacksonJsonSerialization;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.pattern.StatusReply;

public sealed interface GreetingCommand extends JacksonJsonSerialization {
    record Get(ActorRef<GreetingState> replyTo) implements GreetingCommand {}

    sealed interface NonGet extends GreetingCommand {
        ActorRef<StatusReply<GreetingState>> replyTo();
    }

    record Greet(String name, ActorRef<StatusReply<GreetingState>> replyTo) implements NonGet {}

    record UnGreet(ActorRef<StatusReply<GreetingState>> replyTo) implements NonGet {}
}
