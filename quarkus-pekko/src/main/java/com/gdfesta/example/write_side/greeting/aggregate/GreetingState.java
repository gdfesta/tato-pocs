package com.gdfesta.example.write_side.greeting.aggregate;

import com.gdfesta.example.write_side.JacksonJsonSerialization;
import java.util.List;
import java.util.Optional;

public sealed interface GreetingState
    extends JacksonJsonSerialization
    permits OpenState, CloseState {
    Optional<String> name();

    int count();

    List<GreetingEvent> onCommand(GreetingCommand.NonGet command);

    GreetingState onEvent(GreetingEvent event);
}
