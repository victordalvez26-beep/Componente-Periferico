package uy.edu.tse.hcen.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import uy.edu.tse.hcen.multitenancy.TenantContext;
import uy.edu.tse.hcen.repository.UsuarioSaludRepository;
import uy.edu.tse.hcen.repository.ProfesionalSaludRepository;
import jakarta.ejb.EJB;

import java.util.Map;

/**
 * Resource para obtener estadísticas del dashboard de la clínica.
 */
@Path("/stats")
@Produces(MediaType.APPLICATION_JSON)
public class StatsResource {

    private static final Logger LOG = Logger.getLogger(StatsResource.class);

    @EJB
    private UsuarioSaludRepository usuarioSaludRepository;

    @EJB
    private ProfesionalSaludRepository profesionalRepository;

    /**
     * Obtiene estadísticas generales de la clínica.
     * 
     * @param tenantId ID de la clínica
     * @return Estadísticas (total pacientes, total profesionales, etc.)
     */
    @GET
    @Path("/{tenantId}")
    public Response getStats(@PathParam("tenantId") String tenantId) {
        LOG.infof("Received GET stats request for tenant: %s", tenantId);
        
        try {
            if (tenantId == null || tenantId.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "tenantId is required"))
                        .build();
            }
            
            // Establecer el tenant context
            TenantContext.setCurrentTenant(tenantId);
            
            try {
                Long tenantIdLong = Long.parseLong(tenantId);
                
                // Obtener estadísticas
                int totalPacientes = usuarioSaludRepository.findByTenant(tenantIdLong).size();
                int totalProfesionales = profesionalRepository.findAll().size();
                
                Map<String, Object> stats = Map.of(
                    "tenantId", tenantId,
                    "totalPacientes", totalPacientes,
                    "totalProfesionales", totalProfesionales,
                    "totalDocumentos", 0 // TODO: Implementar cuando haya endpoint de documentos
                );
                
                return Response.ok(stats).build();
                
            } finally {
                TenantContext.clear();
            }
            
        } catch (NumberFormatException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "tenantId must be a valid number"))
                    .build();
        } catch (Exception ex) {
            LOG.error("Error getting stats", ex);
            return Response.serverError()
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }
}
