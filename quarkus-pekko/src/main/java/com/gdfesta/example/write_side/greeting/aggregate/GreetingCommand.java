package com.gdfesta.example.write_side.greeting.aggregate;

import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.pattern.StatusReply;

import com.gdfesta.example.write_side.JacksonJsonSerialization;

public sealed interface GreetingCommand extends JacksonJsonSerialization {

    public static final record Get(ActorRef<GreetingState> replyTo) implements GreetingCommand {
    }
    
    public static sealed interface NonGet extends GreetingCommand {
        ActorRef<StatusReply<GreetingState>> replyTo();
    }

    public static final record Greet(String name, ActorRef<StatusReply<GreetingState>> replyTo) implements NonGet {

    }

    public static final record UnGreet(ActorRef<StatusReply<GreetingState>> replyTo) implements NonGet {
    }
}
