package uy.edu.tse.hcen.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import uy.edu.tse.hcen.messaging.RabbitSenderLocal;

import java.util.Map;

@Path("/rabbit")
@Consumes(MediaType.APPLICATION_JSON)
public class RabbitResource {

    @Inject
    RabbitSenderLocal sender;

    @POST
    public Response send(Map<String, String> payload) {
        String queue = payload.getOrDefault("queue", "queue://localhost/testQueue");
        String message = payload.getOrDefault("message", "hello");
        try {
            sender.send(queue, message);
            return Response.ok().entity("sent").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }
}
