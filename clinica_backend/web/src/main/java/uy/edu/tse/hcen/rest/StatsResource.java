package uy.edu.tse.hcen.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import uy.edu.tse.hcen.multitenancy.TenantContext;
import uy.edu.tse.hcen.service.StatsService;

import java.util.Map;

/**
 * Resource para obtener estadísticas de una clínica.
 */
@Path("/stats")
@Produces(MediaType.APPLICATION_JSON)
public class StatsResource {

    private static final Logger LOG = Logger.getLogger(StatsResource.class);

    @EJB
    private StatsService statsService;

    /**
     * Obtiene las estadísticas de una clínica.
     * 
     * @param tenantId ID de la clínica
     * @return Estadísticas: profesionales, usuarios, documentos, consultas
     */
    @GET
    @Path("/{id}")
    public Response getStats(@PathParam("id") Long tenantId) {
        try {
            // Establecer el tenant context
            TenantContext.setCurrentTenant(String.valueOf(tenantId));
            
            Map<String, Long> stats = statsService.getStats(tenantId);
            
            return Response.ok(stats).build();
        } catch (Exception ex) {
            LOG.error("Error obteniendo estadísticas", ex);
            return Response.serverError()
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        } finally {
            TenantContext.clear();
        }
    }
}

