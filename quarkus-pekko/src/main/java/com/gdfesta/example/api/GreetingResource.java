package com.gdfesta.example.api;

import com.gdfesta.example.write_side.greeting.services.GreetingService;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/greetings")
public class GreetingResource {

    @Inject
    GreetingService greetingService;

    @POST
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<GreetingResponse> greet(@PathParam("name") String name) {
        return greetingService.greet(name)
                .map(state -> new GreetingResponse(state.getClass().getSimpleName(), state.count()));
    }

    @DELETE
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<GreetingResponse> unGreet(@PathParam("name") String name) {
        return greetingService.ungreet(name)
                .map(state -> new GreetingResponse(state.getClass().getSimpleName(), state.count()));
    }

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<GreetingResponse> getCount(@PathParam("name") String name) {
        return greetingService.get(name)
                .map(state -> new GreetingResponse(state.getClass().getSimpleName(), state.count()));
    }
}
