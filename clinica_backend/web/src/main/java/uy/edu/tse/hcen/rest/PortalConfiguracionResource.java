package uy.edu.tse.hcen.rest;

import uy.edu.tse.hcen.dto.DTConfiguracionPortal;
import uy.edu.tse.hcen.model.PortalConfiguracion;
import uy.edu.tse.hcen.service.PortalConfiguracionService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.annotation.security.RolesAllowed; // Anotación estándar

@Path("/portal-configuracion")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PortalConfiguracionResource {

    @Inject
    private PortalConfiguracionService configService;

    // -------------------------------------------------------------------------
    // ENDPOINT PÚBLICO (Lectura para el cliente/frontend)
    // -------------------------------------------------------------------------
    
    /**
     * Permite que cualquier usuario (incluso no autenticado o profesional) o el frontend
     * obtenga la configuración de look & feel del tenant actual.
     * La configuración sigue siendo multi-tenant (el TenantAuthFilter establece el ID).
     */
    @GET
    @Path("/public")
    public DTConfiguracionPortal getPublicConfiguracion() {
        PortalConfiguracion config = configService.getConfiguracion();
        
        // Mapeo Entidad a DTO para la respuesta
        DTConfiguracionPortal dto = new DTConfiguracionPortal();
        dto.colorPrimario = config.getColorPrimario();
        dto.colorSecundario = config.getColorSecundario();
        dto.logoUrl = config.getLogoUrl();
        dto.nombrePortal = config.getNombrePortal();
        
        return dto;
    }

    // -------------------------------------------------------------------------
    // ENDPOINT DE ADMINISTRACIÓN (Escritura, requiere rol ADMIN)
    // -------------------------------------------------------------------------

    /**
     * Permite al ADMINISTRADOR modificar la configuración.
     * Requiere que el AuthorizationFilter verifique el rol.
     */
    @PUT
    @RolesAllowed("ADMINISTRADOR") 
    public Response updateConfiguracion(DTConfiguracionPortal dto) {
        PortalConfiguracion updatedConfig = configService.updateConfiguracion(dto);
        return Response.ok(updatedConfig).build();
    }
}
