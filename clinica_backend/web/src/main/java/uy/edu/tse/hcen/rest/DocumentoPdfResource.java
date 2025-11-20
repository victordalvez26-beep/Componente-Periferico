package uy.edu.tse.hcen.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import uy.edu.tse.hcen.multitenancy.TenantContext;
import uy.edu.tse.hcen.service.DocumentoPdfService;
import uy.edu.tse.hcen.service.ProfesionalSaludService;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Recurso REST para manejo de documentos clínicos en formato PDF.
 * 
 * Permite a los profesionales de salud:
 * - Subir PDFs de evaluaciones de pacientes
 * - Generar metadata automáticamente
 * - Sincronizar metadata con el backend HCEN (RNDC)
 * - Servir PDFs para descarga
 */
@Path("/documentos-pdf")
@RequestScoped
public class DocumentoPdfResource {

    private static final Logger LOG = Logger.getLogger(DocumentoPdfResource.class);

    @Inject
    private DocumentoPdfService documentoPdfService;

    @Inject
    private ProfesionalSaludService profesionalSaludService;

    @Context
    private jakarta.ws.rs.core.SecurityContext securityContext;

    /**
     * POST /api/documentos-pdf/upload
     * 
     * Sube un PDF de evaluación de un paciente.
     * 
     * FormData esperado:
     * - archivo: archivo PDF (multipart/form-data)
     * - ciPaciente: CI del paciente
     * - tipoDocumento: tipo de documento (EVALUACION, INFORME, etc.)
     * - descripcion: descripción opcional del documento
     * 
     * @param input Multipart form data con el archivo y metadatos
     * @return Respuesta con el ID del documento creado y metadata
     */
    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("PROFESIONAL")
    public Response subirPdf(MultipartFormDataInput input) {
        try {
            // Obtener información del profesional autenticado
            String profesionalId = null;
            if (securityContext != null && securityContext.getUserPrincipal() != null) {
                profesionalId = securityContext.getUserPrincipal().getName();
            }

            if (profesionalId == null || profesionalId.isBlank()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("error", "Autenticación requerida"))
                        .build();
            }

            // Obtener tenant actual
            String tenantIdStr = TenantContext.getCurrentTenant();
            if (tenantIdStr == null || tenantIdStr.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Tenant no identificado"))
                        .build();
            }
            Long tenantId = Long.parseLong(tenantIdStr);

            // Extraer datos del formulario
            Map<String, List<InputPart>> formDataMap = input.getFormDataMap();
            
            List<InputPart> archivoParts = formDataMap.get("archivo");
            if (archivoParts == null || archivoParts.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Archivo PDF requerido"))
                        .build();
            }
            InputPart archivoPart = archivoParts.get(0);

            List<InputPart> ciPacienteParts = formDataMap.get("ciPaciente");
            if (ciPacienteParts == null || ciPacienteParts.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "CI del paciente requerido"))
                        .build();
            }
            InputPart ciPacientePart = ciPacienteParts.get(0);

            // Leer datos del formulario
            InputStream archivoStream = archivoPart.getBody(InputStream.class, null);
            String ciPaciente = ciPacientePart.getBodyAsString();
            
            String tipoDocumento = "EVALUACION";
            List<InputPart> tipoDocParts = formDataMap.get("tipoDocumento");
            if (tipoDocParts != null && !tipoDocParts.isEmpty()) {
                tipoDocumento = tipoDocParts.get(0).getBodyAsString();
            }
            
            String descripcion = null;
            List<InputPart> descParts = formDataMap.get("descripcion");
            if (descParts != null && !descParts.isEmpty()) {
                descripcion = descParts.get(0).getBodyAsString();
            }

            // Validar que el archivo sea PDF
            String contentType = archivoPart.getHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.equals("application/pdf")) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Solo se permiten archivos PDF"))
                        .build();
            }

            // Procesar y guardar el documento
            Map<String, Object> resultado = documentoPdfService.procesarYGuardarPdf(
                    tenantId,
                    profesionalId,
                    ciPaciente,
                    archivoStream,
                    tipoDocumento,
                    descripcion
            );

            URI location = UriBuilder.fromPath("/api/documentos-pdf/{id}")
                    .build(resultado.get("documentoId"));

            return Response.created(location)
                    .entity(resultado)
                    .build();

        } catch (IllegalArgumentException ex) {
            LOG.error("Error de validación al subir PDF", ex);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        } catch (Exception ex) {
            LOG.error("Error al subir PDF", ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al procesar el documento: " + ex.getMessage()))
                    .build();
        }
    }

    /**
     * GET /api/documentos-pdf/paciente/{ci}
     * 
     * Lista todos los documentos PDF de un paciente por su CI.
     * Solo muestra documentos de la clínica actual (tenant).
     * 
     * @param ci CI del paciente
     * @return Lista de metadatos de documentos
     */
    @GET
    @Path("/paciente/{ci}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("PROFESIONAL")
    public Response listarDocumentosPorPaciente(@PathParam("ci") String ci) {
        try {
            // Obtener tenant actual
            String tenantIdStr = TenantContext.getCurrentTenant();
            if (tenantIdStr == null || tenantIdStr.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Tenant no identificado"))
                        .build();
            }
            Long tenantId = Long.parseLong(tenantIdStr);

            // Validar que el paciente pertenece a esta clínica
            // (esto se valida implícitamente en el servicio al buscar por tenant)
            
            // Listar documentos
            java.util.List<Map<String, Object>> documentos = 
                    documentoPdfService.listarDocumentosPorPaciente(ci, tenantId);

            return Response.ok(documentos).build();

        } catch (Exception ex) {
            LOG.error("Error al listar documentos del paciente", ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al obtener documentos: " + ex.getMessage()))
                    .build();
        }
    }

    /**
     * GET /api/documentos-pdf/{id}
     * 
     * Descarga un PDF por su ID.
     * 
     * @param id ID del documento (MongoDB ObjectId en hex)
     * @return Stream del PDF
     */
    @GET
    @Path("/{id}")
    @Produces("application/pdf")
    // @RolesAllowed("PROFESIONAL") // Temporalmente deshabilitado para pruebas
    public Response descargarPdf(@PathParam("id") String id, @QueryParam("tenantId") Long tenantIdParam) {
        LOG.info(String.format("📥 [BACKEND→PERIFERICO] Petición recibida para descargar PDF - ID: %s, TenantId (query): %s", id, tenantIdParam));
        
        try {
            String tenantIdStr = TenantContext.getCurrentTenant();
            LOG.info(String.format("📋 [PERIFERICO] Tenant en contexto: %s", tenantIdStr));
            
            // Prioridad: 1) Query parameter, 2) Contexto, 3) Fallback
            Long tenantId = null;
            if (tenantIdParam != null) {
                tenantId = tenantIdParam;
                LOG.info(String.format("✅ [PERIFERICO] Usando tenantId del query parameter: %d", tenantId));
            } else if (tenantIdStr != null && !tenantIdStr.isBlank()) {
                tenantId = Long.parseLong(tenantIdStr);
                LOG.info(String.format("✅ [PERIFERICO] Usando tenantId del contexto: %d", tenantId));
            } else {
                // Fallback: usar tenant 1 si no hay información disponible
                LOG.warn("⚠️ [PERIFERICO] Tenant no identificado en contexto ni query parameter, usando tenant 1 como fallback");
                tenantId = 1L;
            }

            LOG.info(String.format("🔍 [PERIFERICO] Buscando PDF en MongoDB - ID: %s, Tenant: %d", id, tenantId));
            byte[] pdfBytes = documentoPdfService.obtenerPdfPorId(id, tenantId);
            
            if (pdfBytes == null) {
                LOG.warn(String.format("❌ [PERIFERICO] PDF no encontrado - ID: %s, Tenant: %d", id, tenantId));
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Documento no encontrado")
                        .build();
            }

            LOG.info(String.format("✅ [PERIFERICO] PDF obtenido de MongoDB - ID: %s, Tamaño: %d bytes", id, pdfBytes.length));
            
            // Verificar que los primeros bytes sean de un PDF válido
            if (pdfBytes.length >= 4) {
                String header = new String(pdfBytes, 0, 4);
                if (!header.startsWith("%PDF")) {
                    LOG.warn(String.format("⚠️ [PERIFERICO] Los primeros bytes no son de un PDF válido: %s", header));
                    LOG.warn(String.format("⚠️ [PERIFERICO] Primeros 200 bytes: %s", 
                            new String(pdfBytes, 0, Math.min(200, pdfBytes.length))));
                } else {
                    LOG.info(String.format("✅ [PERIFERICO] PDF válido detectado - Header: %s", header));
                }
            }
            
            LOG.info(String.format("📤 [PERIFERICO→BACKEND] Enviando PDF al backend HCEN - Tamaño: %d bytes", pdfBytes.length));

            return Response.ok(pdfBytes)
                    .header("Content-Type", "application/pdf")
                    .header("Content-Length", String.valueOf(pdfBytes.length))
                    .header("Content-Disposition", "attachment; filename=\"documento-" + id + ".pdf\"")
                    .build();

        } catch (Exception ex) {
            LOG.error(String.format("❌ [PERIFERICO] Error al descargar PDF - ID: %s", id), ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener el documento: " + ex.getMessage())
                    .build();
        }
    }
}


