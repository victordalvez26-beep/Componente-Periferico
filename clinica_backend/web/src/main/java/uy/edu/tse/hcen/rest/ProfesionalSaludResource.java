package uy.edu.tse.hcen.rest;

import uy.edu.tse.hcen.dto.DTProfesionalSalud;
import uy.edu.tse.hcen.dto.ProfesionalResponse;
import uy.edu.tse.hcen.model.ProfesionalSalud;
import uy.edu.tse.hcen.service.ProfesionalSaludService;
import uy.edu.tse.hcen.multitenancy.TenantContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Path("/profesionales")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMINISTRADOR")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@RequestScoped
public class ProfesionalSaludResource {

    private static final String ERROR_ID_REQUIRED = "id is required";

    @EJB
    private ProfesionalSaludService profesionalService;

    @GET
    public Response listAll(@QueryParam("tenantId") String tenantId) {
        try {
            // Establecer el tenant context si se proporciona como query parameter
            if (tenantId != null && !tenantId.isBlank()) {
                TenantContext.setCurrentTenant(tenantId);
            }
            
            List<ProfesionalSalud> all = profesionalService.findAllInCurrentTenant();
            List<ProfesionalResponse> resp = all.stream().map(ProfesionalResponse::fromEntity).toList();
            return Response.ok(resp).build();
        } finally {
            // Limpiar el tenant context al finalizar
            TenantContext.clear();
        }
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") Long id) {
        if (id == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ERROR_ID_REQUIRED).build();
        }
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

        // El tenantId ya está configurado en TenantContext por el AuthTokenFilter
        // Si viene en el body del request, lo establecemos explícitamente
        String tenantId = null;
        if (dto != null && dto.getTenantId() != null) {
            tenantId = dto.getTenantId();
            TenantContext.setCurrentTenant(tenantId);
        } else {
            // Usar el tenantId del contexto actual (configurado por AuthTokenFilter)
            tenantId = TenantContext.getCurrentTenant();
        }

        try {
            ProfesionalSalud saved = profesionalService.create(dto);
            URI location = UriBuilder.fromPath("/api/profesionales/{id}").build(saved.getId());
            return Response.created(location).entity(ProfesionalResponse.fromEntity(saved)).build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(Map.of("error", e.getMessage())).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, DTProfesionalSalud dto) {
        if (id == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ERROR_ID_REQUIRED).build();
        }
        if (dto == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("dto is required").build();
        }
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
        if (id == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ERROR_ID_REQUIRED).build();
        }
        try {
            profesionalService.delete(id);
            return Response.noContent().build();
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.NOT_FOUND).entity(ex.getMessage()).build();
        }
    }
}
