package uy.edu.tse.hcen.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.jboss.logging.Logger;
import uy.edu.tse.hcen.exceptions.HcenUnavailableException;
import uy.edu.tse.hcen.multitenancy.TenantContext;
import uy.edu.tse.hcen.rest.DocumentoResponseBuilder;
import uy.edu.tse.hcen.rest.DocumentoValidator;
import uy.edu.tse.hcen.service.DocumentoService;
import uy.edu.tse.hcen.service.HcenClient;
import uy.edu.tse.hcen.service.OpenAIService;
import uy.edu.tse.hcen.service.PoliticasClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Recurso REST para operaciones relacionadas con pacientes y sus documentos.
 */
@Path("/documentos/paciente")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class DocumentoPacienteResource {

    private static final Logger LOG = Logger.getLogger(DocumentoPacienteResource.class);

    @Inject
    private HcenClient hcenClient;
    
    @Inject
    private DocumentoService documentoService;
    
    @Inject
    private PoliticasClient politicasClient;
    
    @Inject
    private OpenAIService openAIService;
    
    @Inject
    private SecurityContext securityContext;

    // Constructor sin parámetros requerido por RESTEasy
    public DocumentoPacienteResource() {
    }

    /**
     * GET /api/documentos/paciente/{documentoIdPaciente}/metadatos
     * 
     * Obtiene todos los metadatos de documentos de un paciente.
     */
    @GET
    @Path("/{documentoIdPaciente}/metadatos")
    @RolesAllowed("PROFESIONAL")
    public Response obtenerMetadatosPaciente(@PathParam("documentoIdPaciente") String documentoIdPaciente) {
        Response validation = DocumentoValidator.validateDocumentoIdPaciente(documentoIdPaciente);
        if (validation != null) {
            return validation;
        }

        try {
            String currentTenantId = TenantContext.getCurrentTenant();
            List<Map<String, Object>> metadatos = hcenClient.consultarMetadatosPaciente(documentoIdPaciente);

            if (currentTenantId != null && !currentTenantId.isBlank()) {
                List<Map<String, Object>> metadatosFiltrados = new ArrayList<>();
                for (Map<String, Object> meta : metadatos) {
                    String tenantId = (String) meta.get("tenantId");
                    if (currentTenantId.equals(tenantId)) {
                        metadatosFiltrados.add(meta);
                    }
                }
                return DocumentoResponseBuilder.ok(metadatosFiltrados);
            } else {
                return DocumentoResponseBuilder.ok(metadatos);
            }

        } catch (HcenUnavailableException ex) {
            return DocumentoResponseBuilder.serviceUnavailable("HCEN no disponible", ex.getMessage());
        } catch (Exception ex) {
            return DocumentoResponseBuilder.internalServerError("Error al obtener metadatos", ex.getMessage());
        }
    }

    /**
     * GET /api/documentos/paciente/{documentoIdPaciente}/resumen
     * 
     * Genera un resumen de la historia clínica completa del paciente usando IA.
     */
    @GET
    @Path("/{documentoIdPaciente}/resumen")
    @RolesAllowed("PROFESIONAL")
    public Response generarResumenHistoriaClinica(@PathParam("documentoIdPaciente") String documentoIdPaciente) {
        Response validation = DocumentoValidator.validateDocumentoIdPaciente(documentoIdPaciente);
        if (validation != null) {
            return validation;
        }

        try {
            String profesionalId = getUsuarioId();
            Response usuarioValidation = DocumentoValidator.validateUsuarioId(profesionalId);
            if (usuarioValidation != null) {
                return usuarioValidation;
            }

            // Verificar permisos (opcional - si el servicio de políticas no está disponible, continuar)
            String tenantId = TenantContext.getCurrentTenant();
            boolean tienePermiso = true; // Por defecto permitir si no se puede verificar
            try {
                tienePermiso = politicasClient.verificarPermiso(profesionalId, documentoIdPaciente, null, tenantId);
            } catch (Exception ex) {
                LOG.warnf("No se pudo verificar permisos con el servicio de políticas (continuando): %s", ex.getMessage());
                // Continuar sin verificación de permisos si el servicio no está disponible
            }

            if (!tienePermiso) {
                uy.edu.tse.hcen.common.security.SecurityAuditLogger.logFailedAccess(
                        profesionalId,
                        "/api/documentos/paciente/" + documentoIdPaciente + "/resumen",
                        "No tiene permisos para acceder a la historia clínica completa");

                return DocumentoResponseBuilder.forbidden(
                        "No tiene permisos para acceder a la historia clínica completa del paciente");
            }

            List<String> contenidos = documentoService.obtenerContenidosPorPaciente(documentoIdPaciente);

            if (contenidos == null || contenidos.isEmpty()) {
                return DocumentoResponseBuilder.notFound("No se encontraron documentos para el paciente");
            }

            String historiaClinicaCompleta = construirHistoriaClinicaCompleta(contenidos);
            
            // Intentar generar resumen con OpenAI
            String resumen;
            try {
                resumen = openAIService.generarResumenHistoriaClinica(historiaClinicaCompleta);
            } catch (RuntimeException ex) {
                LOG.warnf("No se pudo generar resumen con OpenAI, usando fallback: %s", ex.getMessage());
                resumen = generarResumenFallback(contenidos);
            }

            uy.edu.tse.hcen.common.security.SecurityAuditLogger.logSensitiveAccess(
                    profesionalId,
                    "/api/documentos/paciente/" + documentoIdPaciente + "/resumen");

            return DocumentoResponseBuilder.ok(Map.of(
                    "paciente", documentoIdPaciente,
                    "resumen", resumen,
                    "documentosProcesados", contenidos.size()
            ));

        } catch (IllegalArgumentException ex) {
            return DocumentoResponseBuilder.badRequest(ex.getMessage());
        } catch (RuntimeException ex) {
            LOG.error("Error generando resumen de historia clínica", ex);
            return DocumentoResponseBuilder.internalServerError("Error al generar resumen: " + ex.getMessage());
        } catch (Exception ex) {
            LOG.error("Error inesperado generando resumen", ex);
            return DocumentoResponseBuilder.internalServerError("Error inesperado al generar resumen");
        }
    }

    private String getUsuarioId() {
        if (securityContext != null && securityContext.getUserPrincipal() != null) {
            return securityContext.getUserPrincipal().getName();
        }
        return null;
    }

    private String construirHistoriaClinicaCompleta(List<String> contenidos) {
        StringBuilder historiaClinicaCompleta = new StringBuilder();
        for (int i = 0; i < contenidos.size(); i++) {
            historiaClinicaCompleta.append("=== Documento ").append(i + 1).append(" ===\n");
            historiaClinicaCompleta.append(contenidos.get(i));
            historiaClinicaCompleta.append("\n\n");
        }
        return historiaClinicaCompleta.toString();
    }

    private String generarResumenFallback(List<String> contenidos) {
        StringBuilder builder = new StringBuilder();
        builder.append("Resumen automático (sin servicio de IA)\n");
        builder.append("Documentos procesados: ").append(contenidos.size()).append("\n\n");

        for (int i = 0; i < contenidos.size(); i++) {
            String texto = contenidos.get(i);
            if (texto == null || texto.isBlank()) {
                continue;
            }
            builder.append("Documento ").append(i + 1).append(":\n");
            String snippet = texto.trim();
            if (snippet.length() > 400) {
                snippet = snippet.substring(0, 400) + "...";
            }
            builder.append(snippet).append("\n\n");
            if (i >= 2) {
                builder.append("... (").append(contenidos.size() - 3).append(" documentos adicionales)\n");
                break;
            }
        }

        if (builder.length() == 0) {
            builder.append("No hay contenido clínico para resumir.");
        }

        builder.append("\nEste resumen fue generado automáticamente debido a que el servicio de IA no está disponible.");
        return builder.toString();
    }
}

