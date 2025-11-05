package uy.edu.tse.hcen.resource;

import uy.edu.tse.hcen.dto.DTProfesionalSalud;
import uy.edu.tse.hcen.service.ProfesionalSaludService;
import uy.edu.tse.hcen.model.ProfesionalSalud;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@RolesAllowed("ADMINISTRADOR")
@Path("/profesionales")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProfesionalResource {

    @Inject
    private ProfesionalSaludService profesionalService;
    private static final String ERROR_KEY = "error";

    // Public no-arg constructor so RESTEasy/Weld can instantiate this resource
    public ProfesionalResource() {
    }

    // CREATE (POST /profesionales)
    @POST
    public Response createProfesional(DTProfesionalSalud dto) {
        try {
            ProfesionalSalud nuevoProfesional = profesionalService.create(dto);
            return Response.status(Response.Status.CREATED).entity(nuevoProfesional).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(java.util.Map.of(ERROR_KEY, e.getMessage())).build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(java.util.Map.of(ERROR_KEY, e.getMessage())).build();
        }
    }

    // READ ALL (GET /profesionales)
    @GET
    public List<ProfesionalSalud> getAllProfesionales() {
        return profesionalService.findAllInCurrentTenant();
    }

    // UPDATE (PUT /profesionales/{id})
    @PUT
    @Path("/{id}")
    public Response updateProfesional(@PathParam("id") Long id, DTProfesionalSalud dto) {
        try {
            ProfesionalSalud updatedProfesional = profesionalService.update(id, dto);
            return Response.ok(updatedProfesional).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(java.util.Map.of(ERROR_KEY, e.getMessage())).build();
        }
    }

    // DELETE (DELETE /profesionales/{id})
    @DELETE
    @Path("/{id}")
    public Response deleteProfesional(@PathParam("id") Long id) {
        try {
            profesionalService.delete(id);
            return Response.noContent().build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(java.util.Map.of(ERROR_KEY, e.getMessage())).build();
        }
    }
}
