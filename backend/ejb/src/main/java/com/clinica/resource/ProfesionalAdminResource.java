package com.clinica.resource;

import com.clinica.model.Profesional;
import com.clinica.repository.ProfesionalRepository;
import com.clinica.multitenant.TenantContext; // El contexto obtiene el tenantId del JWT
// import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/v1/admin/profesionales")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
// @RolesAllowed("ADMIN_CLINICA") // Sólo el Administrador Clínica puede gestionar 
public class ProfesionalAdminResource {

    @Inject
    private ProfesionalRepository profesionalRepository; 

    @Inject
    private TenantContext tenantContext; 

    /**
     * Crea un nuevo profesional y le asigna el tenantId de la sesión actual (CU 7).
     */
    @POST
    public Response crearProfesional(Profesional nuevoProfesional) {
        // AISLAMIENTO DE ESCRITURA: El tenantId de la clínica logueada se asigna al profesional
        String currentTenantId = tenantContext.getCurrentTenantId();
        nuevoProfesional.setTenantId(currentTenantId);

        // Se deben hashear las credenciales antes de guardar.
        // nuevoProfesional.setPasswordHash(PasswordUtil.hash(nuevoProfesional.getPasswordHash())); 
        
        profesionalRepository.save(nuevoProfesional);
        return Response.status(Response.Status.CREATED).entity(nuevoProfesional).build();
    }
    
    /**
     * Obtiene la lista de profesionales de SU clínica (CU 7).
     */
    @GET
    public List<Profesional> obtenerTodosLosProfesionales() {
        // AISLAMIENTO DE LECTURA: La consulta en el repositorio filtrará por el tenantId
        String currentTenantId = tenantContext.getCurrentTenantId();
        return profesionalRepository.findByTenantId(currentTenantId);
    }
    
    // ... Implementación de PUT (Modificación) y DELETE (Baja) [cite: 1134]
}
