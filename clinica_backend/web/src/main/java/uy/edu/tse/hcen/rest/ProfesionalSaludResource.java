package uy.edu.tse.hcen.rest;

import uy.edu.tse.hcen.dto.DTProfesionalSalud;
import uy.edu.tse.hcen.dto.ProfesionalResponse;
import uy.edu.tse.hcen.model.ProfesionalSalud;
import uy.edu.tse.hcen.service.ProfesionalSaludService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;
import java.util.Optional;

@Path("/profesionales")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMINISTRADOR")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@RequestScoped
public class ProfesionalSaludResource {

    @jakarta.ejb.EJB
    private ProfesionalSaludService profesionalService;

    @GET
    public Response listAll() {
        List<ProfesionalSalud> all = profesionalService.findAllInCurrentTenant();
        List<ProfesionalResponse> resp = all.stream().map(ProfesionalResponse::fromEntity).toList();
        return Response.ok(resp).build();
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") Long id) {
        Optional<ProfesionalSalud> opt = profesionalService.findById(id);
        if (opt.isPresent()) {
            return Response.ok(ProfesionalResponse.fromEntity(opt.get())).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @POST
    public Response create(DTProfesionalSalud dto) {
        // Basic validation
        if (dto == null || dto.getNickname() == null || dto.getNickname().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("nickname required").build();
        }
        if (dto.getNombre() == null || dto.getNombre().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("nombre required").build();
        }
        if (dto.getEmail() == null || dto.getEmail().isBlank() || !dto.getEmail().contains("@")) {
            return Response.status(Response.Status.BAD_REQUEST).entity("valid email required").build();
        }

        ProfesionalSalud saved = profesionalService.create(dto);
        URI location = UriBuilder.fromPath("/api/profesionales/{id}").build(saved.getId());
        return Response.created(location).entity(ProfesionalResponse.fromEntity(saved)).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, DTProfesionalSalud dto) {
        try {
            ProfesionalSalud merged = profesionalService.update(id, dto);
            return Response.ok(ProfesionalResponse.fromEntity(merged)).build();
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.NOT_FOUND).entity(ex.getMessage()).build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        try {
            profesionalService.delete(id);
            return Response.noContent().build();
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.NOT_FOUND).entity(ex.getMessage()).build();
        }
    }
}
