package com.clinica.resource;

import com.clinica.multitenant.TenantContext;
import com.clinica.model.Profesional;
import com.clinica.repository.ProfesionalRepository;
// import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProfesionalResource {

    @Inject
    private TenantContext tenantContext;

    @Inject
    private ProfesionalRepository profesionalRepository;

    // Rutas para el portal de clínica/profesional
    // - POST /v1/clinica/profesionales  (PROFESIONAL)
    // - GET  /v1/clinica/profesionales  (ADMIN_CLINICA, PROFESIONAL)

    /**
     * (PROFESIONAL) Crea un nuevo profesional y asigna el tenantId actual.
     */
    @POST
    @Path("/clinica/profesionales")
    // @RolesAllowed("PROFESIONAL")
    public Response crearProfesionalClinica(Profesional nuevoProfesional) {
        String currentTenantId = tenantContext.getCurrentTenantId();
        nuevoProfesional.setTenantId(currentTenantId);

        profesionalRepository.save(nuevoProfesional);
        return Response.status(Response.Status.CREATED).entity(nuevoProfesional).build();
    }

    @GET
    @Path("/clinica/profesionales")
    // @RolesAllowed({"ADMIN_CLINICA", "PROFESIONAL"})
    public List<Profesional> obtenerProfesionalesClinica() {
        String currentTenantId = tenantContext.getCurrentTenantId();
        return profesionalRepository.findByTenantId(currentTenantId);
    }

    // Rutas administrativas (solo ADMIN_CLINICA)
    // - POST /v1/admin/profesionales
    // - GET  /v1/admin/profesionales

    /**
     * (ADMIN_CLINICA) Crea un nuevo profesional y le asigna el tenantId de la sesión actual (CU 7).
     */
    @POST
    @Path("/admin/profesionales")
    // @RolesAllowed("ADMIN_CLINICA")
    public Response crearProfesionalAdmin(Profesional nuevoProfesional) {
        // AISLAMIENTO DE ESCRITURA: El tenantId de la clínica logueada se asigna al profesional
        String currentTenantId = tenantContext.getCurrentTenantId();
        nuevoProfesional.setTenantId(currentTenantId);

        // Se deben hashear las credenciales antes de guardar.
        // nuevoProfesional.setPasswordHash(PasswordUtil.hash(nuevoProfesional.getPasswordHash()));

        profesionalRepository.save(nuevoProfesional);
        return Response.status(Response.Status.CREATED).entity(nuevoProfesional).build();
    }

    @GET
    @Path("/admin/profesionales")
    // @RolesAllowed("ADMIN_CLINICA")
    public List<Profesional> obtenerProfesionalesAdmin() {
        String currentTenantId = tenantContext.getCurrentTenantId();
        return profesionalRepository.findByTenantId(currentTenantId);
    }

    // ... Implementación de PUT (Modificación) y DELETE (Baja) para admin/clinic (comentar/implementar según necesidad)
}
