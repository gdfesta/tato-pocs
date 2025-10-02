package com.gdfesta.example.write_side.greeting.aggregate;

import java.util.List;

import com.gdfesta.example.write_side.JacksonJsonSerialization;

public sealed interface GreetingState extends JacksonJsonSerialization permits OpenState, CloseState {
    int count();

    List<GreetingEvent> onCommand(GreetingCommand.NonGet command);

    GreetingState onEvent(GreetingEvent event);
}
