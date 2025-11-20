package uy.edu.tse.hcen.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
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
     * Maneja peticiones OPTIONS (CORS preflight) para el endpoint de estadísticas.
     */
    @OPTIONS
    @Path("/{id}")
    public Response optionsStats(@Context jakarta.ws.rs.core.HttpHeaders headers) {
        String origin = headers.getRequestHeader("Origin") != null && !headers.getRequestHeader("Origin").isEmpty() 
                ? headers.getRequestHeader("Origin").get(0) 
                : null;
        
        Response.ResponseBuilder responseBuilder = Response.status(Response.Status.OK);
        
        // Agregar headers CORS al preflight
        if (origin != null && (origin.startsWith("http://localhost:3000") || origin.startsWith("http://localhost:3001"))) {
            responseBuilder.header("Access-Control-Allow-Origin", origin);
            responseBuilder.header("Access-Control-Allow-Credentials", "true");
        } else if (origin != null) {
            responseBuilder.header("Access-Control-Allow-Origin", origin);
        } else {
            responseBuilder.header("Access-Control-Allow-Origin", "*");
        }
        responseBuilder.header("Access-Control-Allow-Methods", "GET, OPTIONS");
        responseBuilder.header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Accept, Origin");
        responseBuilder.header("Access-Control-Max-Age", "3600");
        responseBuilder.header("Access-Control-Expose-Headers", "Content-Type, Authorization");
        
        return responseBuilder.build();
    }
    
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

