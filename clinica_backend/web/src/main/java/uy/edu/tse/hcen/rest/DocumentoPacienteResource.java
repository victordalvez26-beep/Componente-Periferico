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

    private final HcenClient hcenClient;
    private final DocumentoService documentoService;
    private final PoliticasClient politicasClient;
    private final OpenAIService openAIService;
    private final SecurityContext securityContext;

    @Inject
    public DocumentoPacienteResource(HcenClient hcenClient,
                                    DocumentoService documentoService,
                                    PoliticasClient politicasClient,
                                    OpenAIService openAIService,
                                    SecurityContext securityContext) {
        this.hcenClient = hcenClient;
        this.documentoService = documentoService;
        this.politicasClient = politicasClient;
        this.openAIService = openAIService;
        this.securityContext = securityContext;
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

            String tenantId = TenantContext.getCurrentTenant();
            boolean tienePermiso = politicasClient.verificarPermiso(profesionalId, documentoIdPaciente, null, tenantId);

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
            String resumen = openAIService.generarResumenHistoriaClinica(historiaClinicaCompleta);

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
}

