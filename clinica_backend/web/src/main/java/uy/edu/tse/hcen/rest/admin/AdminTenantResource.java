package uy.edu.tse.hcen.rest.admin;

import uy.edu.tse.hcen.service.TenantAdminService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriBuilder;
import java.net.URI;

@Path("/admin/tenants")
public class AdminTenantResource {

    @Inject
    private TenantAdminService tenantAdminService;

    public static class TenantCreateRequest {
        private String tenantId; // numeric suffix used in schema name (e.g., 103)
        private String nombrePortal;
        private String colorPrimario;

        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }

        public String getNombrePortal() { return nombrePortal; }
        public void setNombrePortal(String nombrePortal) { this.nombrePortal = nombrePortal; }

        public String getColorPrimario() { return colorPrimario; }
        public void setColorPrimario(String colorPrimario) { this.colorPrimario = colorPrimario; }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createTenant(TenantCreateRequest req, @Context SecurityContext sc) {
        // Basic role check - require ADMINISTRADOR
        if (sc == null || !sc.isUserInRole("ADMINISTRADOR")) {
            return Response.status(Response.Status.FORBIDDEN).entity("Access denied").build();
        }

        if (req == null || req.getTenantId() == null || req.getTenantId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("tenantId required").build();
        }

        String schema = "schema_clinica_" + req.getTenantId();
        try {
            tenantAdminService.createTenantSchema(schema, req.getColorPrimario(), req.getNombrePortal());
        } catch (Exception ex) {
            return Response.serverError().entity(ex.getMessage()).build();
        }

        URI location = UriBuilder.fromPath("/api/admin/tenants/{id}").build(req.getTenantId());
        return Response.created(location).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listTenants(@Context SecurityContext sc) {
        if (sc == null || !sc.isUserInRole("ADMINISTRADOR")) {
            return Response.status(Response.Status.FORBIDDEN).entity("Access denied").build();
        }

        try {
            java.util.List<java.util.Map<String, Object>> tenants = tenantAdminService.listTenants();
            return Response.ok(tenants).build();
        } catch (Exception ex) {
            return Response.serverError().entity(ex.getMessage()).build();
        }
    }
}
