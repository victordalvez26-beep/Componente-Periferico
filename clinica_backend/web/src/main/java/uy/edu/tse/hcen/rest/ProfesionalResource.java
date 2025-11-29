package uy.edu.tse.hcen.rest;

import uy.edu.tse.hcen.client.PoliticasAccesoClient;
import uy.edu.tse.hcen.multitenancy.TenantContext;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * Recurso REST para operaciones relacionadas con profesionales de salud.
 * Endpoints específicos para profesionales (no CRUD completo).
 */
@Path("/profesional")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class ProfesionalResource {

    private static final Logger LOG = Logger.getLogger(ProfesionalResource.class);

    @Inject
    private PoliticasAccesoClient politicasAccesoClient;
    
    @Context
    private SecurityContext securityContext;

    /**
     * GET /api/profesional/verificar
     * 
     * Verifica si un profesional tiene permiso para acceder a documentos de un paciente.
     * 
     * @param profesionalId ID del profesional (nickname) - opcional, se usa el autenticado si no se proporciona
     * @param pacienteCI CI del paciente
     * @param tipoDoc Tipo de documento (opcional)
     * @return JSON con { tienePermiso: true/false }
     */
    @GET
    @Path("/verificar")
    @RolesAllowed({"PROFESIONAL", "ADMINISTRADOR"})
    public Response verificarPermiso(
            @QueryParam("profesionalId") String profesionalId,
            @QueryParam("pacienteCI") String pacienteCI,
            @QueryParam("tipoDoc") String tipoDoc) {
        
        try {
            // Obtener tenant actual
            String tenantId = TenantContext.getCurrentTenant();
            if (tenantId == null || tenantId.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Tenant no identificado"))
                    .build();
            }
            
            // Si no se proporciona profesionalId, usar el del usuario autenticado
            if (profesionalId == null || profesionalId.isBlank()) {
                if (securityContext != null && securityContext.getUserPrincipal() != null) {
                    profesionalId = securityContext.getUserPrincipal().getName();
                } else {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "profesionalId es requerido"))
                        .build();
                }
            }
            
            if (pacienteCI == null || pacienteCI.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "pacienteCI es requerido"))
                    .build();
            }
            
            LOG.info(String.format("Verificando permiso - Profesional: %s, Paciente: %s, TipoDoc: %s, Tenant: %s", 
                    profesionalId, pacienteCI, tipoDoc, tenantId));
            
            boolean tienePermiso = politicasAccesoClient.verificarPermiso(
                    profesionalId, 
                    pacienteCI, 
                    tipoDoc, 
                    tenantId);
            
            return Response.ok(Map.of("tienePermiso", tienePermiso)).build();
            
        } catch (Exception e) {
            LOG.error("Error al verificar permiso", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Error al verificar permiso: " + e.getMessage()))
                .build();
        }
    }
}

