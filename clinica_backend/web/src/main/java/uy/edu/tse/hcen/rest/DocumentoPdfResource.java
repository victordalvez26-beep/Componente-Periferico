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
import uy.edu.tse.hcen.service.PoliticasClient;

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

    @Inject
    private PoliticasClient politicasClient;

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

            // Verificar permisos antes de listar documentos
            LOG.info(String.format("🔒 [PERIFERICO] Verificando permisos para listar documentos - Profesional: %s, Paciente: %s", profesionalId, ci));
            boolean tienePermiso = false;
            boolean servicioDisponible = true;
            try {
                tienePermiso = politicasClient.verificarPermiso(profesionalId, ci, null, tenantIdStr);
            } catch (Exception ex) {
                LOG.warnf("No se pudo verificar permisos con el servicio de políticas: %s", ex.getMessage());
                servicioDisponible = false;
                // En modo degradado, permitir acceso si el servicio no está disponible
                // Esto permite que la aplicación siga funcionando aunque el servicio de políticas esté caído
                LOG.warning("⚠️ [PERIFERICO] Servicio de políticas no disponible - Modo degradado: permitiendo acceso temporal");
                tienePermiso = true; // Permitir acceso en modo degradado
            }

            if (!tienePermiso) {
                LOG.warnf("❌ [PERIFERICO] Acceso denegado - Profesional %s no tiene permiso para listar documentos del paciente %s", profesionalId, ci);
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(Map.of("error", "No tiene permisos para acceder a los documentos de este paciente. Debe solicitar acceso primero."))
                        .build();
            }
            
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
     * Descarga un PDF o devuelve contenido de texto por su ID.
     * Busca en ambas colecciones: documentos_pdf y documentos_clinicos
     * 
     * @param id ID del documento (MongoDB ObjectId en hex)
     * @return Stream del PDF o contenido de texto
     */
    @GET
    @Path("/{id}")
    @Produces({"application/pdf", MediaType.APPLICATION_OCTET_STREAM, MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
    @RolesAllowed("PROFESIONAL")
    public Response descargarPdf(@PathParam("id") String id, @QueryParam("tenantId") Long tenantIdParam) {
        LOG.info(String.format("📥 [BACKEND→PERIFERICO] Petición recibida para descargar documento - ID: %s, TenantId (query): %s", id, tenantIdParam));
        
        try {
            // Obtener información del profesional autenticado
            String profesionalId = null;
            if (securityContext != null && securityContext.getUserPrincipal() != null) {
                profesionalId = securityContext.getUserPrincipal().getName();
            }

            if (profesionalId == null || profesionalId.isBlank()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .type(MediaType.APPLICATION_JSON)
                        .entity(Map.of("error", "Autenticación requerida"))
                        .build();
            }

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

            LOG.info(String.format("🔍 [PERIFERICO] Buscando documento en MongoDB - ID: %s, Tenant: %d", id, tenantId));
            
            // Obtener información del documento para verificar permisos
            String ciPaciente = documentoPdfService.obtenerCiPacientePorId(id, tenantId);
            if (ciPaciente == null || ciPaciente.isBlank()) {
                LOG.warn(String.format("⚠️ [PERIFERICO] No se pudo obtener CI del paciente para documento %s", id));
                return Response.status(Response.Status.NOT_FOUND)
                        .type(MediaType.APPLICATION_JSON)
                        .entity(Map.of("error", "Documento no encontrado"))
                        .build();
            }

            // Verificar permisos antes de permitir la descarga
            LOG.info(String.format("🔒 [PERIFERICO] Verificando permisos - Profesional: %s, Paciente: %s", profesionalId, ciPaciente));
            boolean tienePermiso = false;
            try {
                tienePermiso = politicasClient.verificarPermiso(profesionalId, ciPaciente, null, tenantIdStr);
            } catch (Exception ex) {
                LOG.warnf("No se pudo verificar permisos con el servicio de políticas: %s", ex.getMessage());
                // Si el servicio no está disponible, denegar acceso por seguridad
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .type(MediaType.APPLICATION_JSON)
                        .entity(Map.of("error", "No se puede verificar permisos. El servicio de políticas no está disponible."))
                        .build();
            }

            if (!tienePermiso) {
                LOG.warnf("❌ [PERIFERICO] Acceso denegado - Profesional %s no tiene permiso para acceder a documentos del paciente %s", profesionalId, ciPaciente);
                // Obtener el documentoId (UUID) del documento para registrar el acceso denegado
                String documentoId = documentoPdfService.obtenerDocumentoIdPorId(id, tenantId);
                String tipoDocumento = documentoPdfService.obtenerTipoDocumentoPorId(id, tenantId);
                // Registrar el intento de acceso denegado
                try {
                    politicasClient.registrarAcceso(profesionalId, ciPaciente, documentoId, tipoDocumento, false,
                            "No tiene permisos para acceder a este documento", "Intento de descarga desde componente periférico");
                } catch (Exception ex) {
                    LOG.warnf("No se pudo registrar el acceso denegado: %s", ex.getMessage());
                }
                return Response.status(Response.Status.FORBIDDEN)
                        .type(MediaType.APPLICATION_JSON)
                        .entity(Map.of("error", "No tiene permisos para acceder a este documento. Debe solicitar acceso primero."))
                        .build();
            }

            // Obtener el documentoId (UUID) del documento para registrar el acceso
            String documentoId = documentoPdfService.obtenerDocumentoIdPorId(id, tenantId);
            String tipoDocumento = documentoPdfService.obtenerTipoDocumentoPorId(id, tenantId);
            
            // Registrar el acceso permitido
            try {
                politicasClient.registrarAcceso(profesionalId, ciPaciente, documentoId, tipoDocumento, true,
                        null, "Descarga desde componente periférico");
            } catch (Exception ex) {
                LOG.warnf("No se pudo registrar el acceso: %s", ex.getMessage());
            }

            // 1. Intentar obtener PDF
            byte[] pdfBytes = documentoPdfService.obtenerPdfPorId(id, tenantId);
            
            if (pdfBytes != null) {
                LOG.info(String.format("✅ [PERIFERICO] PDF obtenido de MongoDB - ID: %s, Tamaño: %d bytes", id, pdfBytes.length));
                
                // Verificar que los primeros bytes sean de un PDF válido
                if (pdfBytes.length >= 4) {
                    String header = new String(pdfBytes, 0, 4);
                    if (!header.startsWith("%PDF")) {
                        LOG.warn(String.format("⚠️ [PERIFERICO] Los primeros bytes no son de un PDF válido: %s", header));
                    } else {
                        LOG.info(String.format("✅ [PERIFERICO] PDF válido detectado - Header: %s", header));
                    }
                }
                
                LOG.info(String.format("📤 [PERIFERICO→BACKEND] Enviando PDF - Tamaño: %d bytes", pdfBytes.length));

                return Response.ok(pdfBytes)
                        .header("Content-Type", "application/pdf")
                        .header("Content-Length", String.valueOf(pdfBytes.length))
                        .header("Content-Disposition", "attachment; filename=\"documento-" + id + ".pdf\"")
                        .build();
            }
            
            // 2. Si no hay PDF, intentar obtener contenido de texto
            LOG.info(String.format("🔍 [PERIFERICO] PDF no encontrado, buscando contenido de texto - ID: %s", id));
            String contenido = documentoPdfService.obtenerContenidoPorId(id, tenantId);
            
            if (contenido != null && !contenido.isBlank()) {
                LOG.info(String.format("✅ [PERIFERICO] Contenido de texto obtenido - ID: %s, Tamaño: %d caracteres", id, contenido.length()));
                
                return Response.ok(contenido)
                        .header("Content-Type", "text/plain; charset=UTF-8")
                        .header("Content-Length", String.valueOf(contenido.getBytes(java.nio.charset.StandardCharsets.UTF_8).length))
                        .header("Content-Disposition", "inline; filename=\"documento-" + id + ".txt\"")
                        .build();
            }
            
            // 3. Si no se encuentra ni PDF ni contenido
            LOG.warn(String.format("❌ [PERIFERICO] Documento no encontrado - ID: %s, Tenant: %d", id, tenantId));
            return Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of("error", "Documento no encontrado"))
                    .build();

        } catch (Exception ex) {
            LOG.error(String.format("❌ [PERIFERICO] Error al obtener documento - ID: %s", id), ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of("error", "Error al obtener el documento: " + ex.getMessage()))
                    .build();
        }
    }
}


