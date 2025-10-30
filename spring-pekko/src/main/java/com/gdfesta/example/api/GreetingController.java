package com.gdfesta.example.api;

import com.gdfesta.example.write_side.greeting.services.GreetingService;
import java.util.concurrent.CompletionStage;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/greetings")
public class GreetingController {

    private final GreetingService greetingService;

    public GreetingController(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    @PostMapping("/{name}")
    public CompletionStage<GreetingResponse> greet(@PathVariable String name) {
        return greetingService
            .greet(name)
            .thenApply(state -> new GreetingResponse(state.getClass().getSimpleName(), state.count()));
    }

    @DeleteMapping("/{name}")
    public CompletionStage<GreetingResponse> unGreet(@PathVariable String name) {
        return greetingService
            .ungreet(name)
            .thenApply(state -> new GreetingResponse(state.getClass().getSimpleName(), state.count()));
    }

    @GetMapping("/{name}")
    public CompletionStage<GreetingResponse> getCount(@PathVariable String name) {
        return greetingService
            .get(name)
            .thenApply(state -> new GreetingResponse(state.getClass().getSimpleName(), state.count()));
    }
}
