package uy.edu.tse.hcen.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import uy.edu.tse.hcen.service.ClinicaProducer;

@Path("/api/clinica")
public class ClinicaResource {

    @Inject
    private ClinicaProducer producer;

    @POST
    @Path("/alta")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response alta(AltaClinicaRequest req) {
        if (req == null || req.payload == null) return Response.status(Response.Status.BAD_REQUEST).entity("payload required").build();
        // payload should be a JSON object representing the clinic data
        try {
            producer.enqueueAltaClinica(req.payload);
            // Return 202 Accepted to indicate asynchronous processing
            return Response.accepted().build();
        } catch (Exception ex) {
            return Response.serverError().entity(ex.getMessage()).build();
        }
    }
}
