package uy.edu.tse.hcen.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.annotation.security.RolesAllowed;
import uy.edu.tse.hcen.service.StatsService;
import uy.edu.tse.hcen.multitenancy.TenantContext;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * REST Resource para obtener estadísticas del tenant.
 */
@Path("/stats")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StatsResource {

    private static final Logger LOGGER = Logger.getLogger(StatsResource.class.getName());

    @EJB
    private StatsService statsService;

    /**
     * GET /api/stats/{tenantId}
     * 
     * Obtiene las estadísticas del tenant especificado.
     * 
     * @param tenantId ID del tenant (clínica)
     * @return Estadísticas: profesionales, usuarios, documentos, consultas
     */
    @GET
    @Path("/{tenantId}")
    @RolesAllowed({"ADMINISTRADOR", "PROFESIONAL"})
    public Response obtenerEstadisticas(@PathParam("tenantId") String tenantId) {
        LOGGER.info(String.format("Solicitando estadísticas para tenant: %s", tenantId));
        
        try {
            // Validar que el tenantId del path coincida con el tenant del usuario autenticado
            String tenantActual = TenantContext.getCurrentTenant();
            if (tenantActual == null || !tenantActual.equals(tenantId)) {
                LOGGER.warning(String.format("Tenant del usuario (%s) no coincide con tenant solicitado (%s)", tenantActual, tenantId));
                return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", "No tiene permiso para acceder a las estadísticas de este tenant"))
                    .build();
            }
            
            Map<String, Object> stats = statsService.obtenerEstadisticas(tenantId);
            return Response.ok(stats).build();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format("Error al obtener estadísticas para tenant %s", tenantId), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Error al obtener estadísticas: " + e.getMessage()))
                .build();
        }
    }

    /**
     * GET /api/stats/{tenantId}/actividad-reciente
     * 
     * Obtiene la actividad reciente del tenant especificado.
     * 
     * @param tenantId ID del tenant (clínica)
     * @param limite Número máximo de actividades a devolver (default: 10)
     * @return Lista de actividades recientes ordenadas por fecha descendente
     */
    @GET
    @Path("/{tenantId}/actividad-reciente")
    @RolesAllowed({"ADMINISTRADOR", "PROFESIONAL"})
    public Response obtenerActividadReciente(
            @PathParam("tenantId") String tenantId,
            @QueryParam("limite") @DefaultValue("10") int limite) {
        LOGGER.info(String.format("Solicitando actividad reciente para tenant: %s (limite: %d)", tenantId, limite));
        
        try {
            // Validar que el tenantId del path coincida con el tenant del usuario autenticado
            String tenantActual = TenantContext.getCurrentTenant();
            if (tenantActual == null || !tenantActual.equals(tenantId)) {
                LOGGER.warning(String.format("Tenant del usuario (%s) no coincide con tenant solicitado (%s)", tenantActual, tenantId));
                return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", "No tiene permiso para acceder a la actividad de este tenant"))
                    .build();
            }
            
            // Limitar el máximo a 50 para evitar sobrecarga
            if (limite > 50) {
                limite = 50;
            }
            
            List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);
            return Response.ok(actividades).build();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format("Error al obtener actividad reciente para tenant %s", tenantId), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Error al obtener actividad reciente: " + e.getMessage()))
                .build();
        }
    }
}

