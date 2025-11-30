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
import uy.edu.tse.hcen.client.PoliticasAccesoClient;

import java.io.InputStream;
import java.io.IOException;
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
    
    // Constantes para literales duplicados
    private static final String KEY_ERROR = "error";
    private static final String KEY_ARCHIVO = "archivo";
    private static final String KEY_CI_PACIENTE = "ciPaciente";
    private static final String KEY_TIPO_DOCUMENTO = "tipoDocumento";
    private static final String KEY_DESCRIPCION = "descripcion";
    private static final String KEY_DOCUMENTO_ID = "documentoId";
    private static final String ERROR_TENANT_NO_IDENTIFICADO = "Tenant no identificado";
    private static final String ERROR_AUTENTICACION_REQUERIDA = "Autenticación requerida";
    private static final String ERROR_ARCHIVO_PDF_REQUERIDO = "Archivo PDF requerido";
    private static final String ERROR_CI_PACIENTE_REQUERIDO = "CI del paciente requerido";
    private static final String ERROR_SOLO_PDF = "Solo se permiten archivos PDF";
    private static final String MIME_TYPE_PDF = "application/pdf";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_CONTENT_LENGTH = "Content-Length";
    private static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";

    @Inject
    private DocumentoPdfService documentoPdfService;

    @Inject
    private PoliticasAccesoClient politicasAccesoClient;

    @Inject
    private uy.edu.tse.hcen.service.HcenClient hcenClient;

    @Inject
    private uy.edu.tse.hcen.repository.ProfesionalSaludRepository profesionalSaludRepository;

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
            Response validacion = validarAutenticacionYTenant();
            if (validacion != null) {
                return validacion;
            }

            String profesionalId = obtenerProfesionalId();
            Long tenantId = obtenerTenantId();
            if (tenantId == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(KEY_ERROR, ERROR_TENANT_NO_IDENTIFICADO))
                        .build();
            }

            DatosFormulario datos = extraerDatosFormulario(input);
            Response validacionDatos = validarDatosFormulario(datos);
            if (validacionDatos != null) {
                return validacionDatos;
            }

            Map<String, Object> resultado = documentoPdfService.procesarYGuardarPdf(
                    tenantId,
                    profesionalId,
                    datos.ciPaciente,
                    datos.archivoStream,
                    datos.tipoDocumento,
                    datos.descripcion
            );

            URI location = UriBuilder.fromPath("/api/documentos-pdf/{id}")
                    .build(resultado.get(KEY_DOCUMENTO_ID));

            return Response.created(location)
                    .entity(resultado)
                    .build();

        } catch (IllegalArgumentException ex) {
            LOG.error("Error de validación al subir PDF", ex);
            return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(KEY_ERROR, ex.getMessage()))
                    .build();
        } catch (IOException ex) {
            LOG.error("Error de E/S al subir PDF", ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(Map.of(KEY_ERROR, "Error al procesar el archivo: " + ex.getMessage()))
                    .build();
        } catch (Exception ex) {
            LOG.error("Error al subir PDF", ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(Map.of(KEY_ERROR, "Error al procesar el documento: " + ex.getMessage()))
                    .build();
        }
    }
    
    /**
     * Clase interna para agrupar datos del formulario.
     */
    private static class DatosFormulario {
        InputStream archivoStream;
        String ciPaciente;
        String tipoDocumento;
        String descripcion;
        InputPart archivoPart;
    }
    
    /**
     * Valida autenticación y tenant.
     */
    private Response validarAutenticacionYTenant() {
        String profesionalId = obtenerProfesionalId();
        if (profesionalId == null || profesionalId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of(KEY_ERROR, ERROR_AUTENTICACION_REQUERIDA))
                    .build();
        }
        return null;
    }
    
    /**
     * Obtiene el ID del profesional autenticado.
     */
    private String obtenerProfesionalId() {
        if (securityContext != null && securityContext.getUserPrincipal() != null) {
            return securityContext.getUserPrincipal().getName();
        }
        return null;
    }
    
    /**
     * Obtiene el tenant ID actual.
     */
    private Long obtenerTenantId() {
        String tenantIdStr = TenantContext.getCurrentTenant();
        if (tenantIdStr == null || tenantIdStr.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(tenantIdStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Extrae los datos del formulario multipart.
     */
    private DatosFormulario extraerDatosFormulario(MultipartFormDataInput input) throws IOException {
        DatosFormulario datos = new DatosFormulario();
        Map<String, List<InputPart>> formDataMap = input.getFormDataMap();
        
        datos.archivoPart = obtenerInputPart(formDataMap, KEY_ARCHIVO);
        if (datos.archivoPart != null) {
            datos.archivoStream = datos.archivoPart.getBody(InputStream.class, null);
        }
        
        InputPart ciPacientePart = obtenerInputPart(formDataMap, KEY_CI_PACIENTE);
        if (ciPacientePart != null) {
            datos.ciPaciente = ciPacientePart.getBodyAsString();
        }
        
        datos.tipoDocumento = extraerTipoDocumento(formDataMap);
        datos.descripcion = extraerDescripcion(formDataMap);
        
        return datos;
    }
    
    /**
     * Obtiene un InputPart del formulario.
     */
    private InputPart obtenerInputPart(Map<String, List<InputPart>> formDataMap, String key) {
        List<InputPart> parts = formDataMap.get(key);
        if (parts != null && !parts.isEmpty()) {
            return parts.get(0);
        }
        return null;
    }
    
    /**
     * Extrae el tipo de documento del formulario.
     */
    private String extraerTipoDocumento(Map<String, List<InputPart>> formDataMap) {
        String tipoDocumento = uy.edu.tse.hcen.model.enums.TipoDocumento.CONSULTA_MEDICA.name();
        List<InputPart> tipoDocParts = formDataMap.get(KEY_TIPO_DOCUMENTO);
        if (tipoDocParts != null && !tipoDocParts.isEmpty()) {
            try {
                String tipoRecibido = tipoDocParts.get(0).getBodyAsString();
                tipoDocumento = uy.edu.tse.hcen.model.enums.TipoDocumento.valueOf(tipoRecibido).name();
            } catch (IllegalArgumentException e) {
                LOG.warnf("Tipo de documento desconocido. Usando OTROS.");
                tipoDocumento = uy.edu.tse.hcen.model.enums.TipoDocumento.OTROS.name();
            } catch (IOException e) {
                LOG.warnf("Error al leer tipo de documento del formulario: %s", e.getMessage());
            }
        }
        return tipoDocumento;
    }
    
    /**
     * Extrae la descripción del formulario.
     */
    private String extraerDescripcion(Map<String, List<InputPart>> formDataMap) {
        List<InputPart> descParts = formDataMap.get(KEY_DESCRIPCION);
        if (descParts != null && !descParts.isEmpty()) {
            try {
                return descParts.get(0).getBodyAsString();
            } catch (IOException e) {
                LOG.warnf("Error al leer descripción del formulario: %s", e.getMessage());
                return null;
            }
        }
        return null;
    }
    
    /**
     * Valida los datos extraídos del formulario.
     */
    private Response validarDatosFormulario(DatosFormulario datos) {
        if (datos.archivoPart == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(KEY_ERROR, ERROR_ARCHIVO_PDF_REQUERIDO))
                    .build();
        }
        
        if (datos.ciPaciente == null || datos.ciPaciente.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(KEY_ERROR, ERROR_CI_PACIENTE_REQUERIDO))
                    .build();
        }
        
        String contentType = datos.archivoPart.getHeaders().getFirst(HEADER_CONTENT_TYPE);
        if (contentType == null || !MIME_TYPE_PDF.equals(contentType)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(KEY_ERROR, ERROR_SOLO_PDF))
                    .build();
        }
        
        return null;
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
            // Obtener tenant actual (clínica del profesional)
            String tenantIdStr = TenantContext.getCurrentTenant();
            if (tenantIdStr == null || tenantIdStr.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(KEY_ERROR, ERROR_TENANT_NO_IDENTIFICADO))
                        .build();
            }
            
            // Obtener información del profesional autenticado
            String profesionalId = null;
            if (securityContext != null && securityContext.getUserPrincipal() != null) {
                profesionalId = securityContext.getUserPrincipal().getName();
            }
            
            if (profesionalId == null || profesionalId.isBlank()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of(KEY_ERROR, "No se pudo identificar al profesional autenticado"))
                        .build();
            }
            
            LOG.infof("Listando documentos del paciente %s - Profesional: %s, Clínica: %s", 
                    ci, profesionalId, tenantIdStr);
            
            // Listar documentos de TODAS las clínicas, filtrando por políticas de acceso
            // El registro de acceso se hace en HCEN Central cuando procesa la solicitud
            java.util.List<Map<String, Object>> documentos = 
                    documentoPdfService.listarDocumentosPorPaciente(ci, profesionalId, tenantIdStr);

            return Response.ok(documentos).build();

        } catch (Exception ex) {
            LOG.error("Error al listar documentos del paciente", ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(Map.of(KEY_ERROR, "Error al obtener documentos: " + ex.getMessage()))
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
    @Produces(MIME_TYPE_PDF)
    // @RolesAllowed("PROFESIONAL") // Temporalmente deshabilitado para pruebas
    public Response descargarPdf(@PathParam("id") String id, @QueryParam("tenantId") Long tenantIdParam) {
        LOG.infof("📥 [BACKEND→PERIFERICO] Petición recibida para descargar PDF - ID: %s, TenantId (query): %s", id, tenantIdParam);
        
        try {
            Long tenantId = determinarTenantId(tenantIdParam);
            Map<String, Object> metadata = obtenerYValidarMetadata(id, tenantId);
            if (metadata == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Documento no encontrado")
                        .build();
            }
            
            String pacienteCI = (String) metadata.get(KEY_CI_PACIENTE);
            String tipoDocumento = (String) metadata.get(KEY_TIPO_DOCUMENTO);
            String profesionalId = obtenerProfesionalId();
            boolean esLlamadaDesdeBackendHCEN = esLlamadaDesdeBackendHCEN(profesionalId);
            
            Response validacionPermisos = validarPermisosAcceso(profesionalId, pacienteCI, tipoDocumento, 
                    TenantContext.getCurrentTenant(), esLlamadaDesdeBackendHCEN);
            if (validacionPermisos != null) {
                return validacionPermisos;
            }
            
            byte[] pdfBytes = obtenerYValidarPdf(id, tenantId);
            if (pdfBytes == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Documento no encontrado")
                        .build();
            }
            
            registrarAccesoSiNecesario(profesionalId, pacienteCI, tipoDocumento, id, 
                    TenantContext.getCurrentTenant(), esLlamadaDesdeBackendHCEN);
            
            return construirRespuestaPdf(pdfBytes, id);

        } catch (Exception ex) {
            LOG.errorf(ex, "❌ [PERIFERICO] Error al descargar PDF - ID: %s", id);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener el documento: " + ex.getMessage())
                    .build();
        }
    }
    
    /**
     * Determina el tenant ID a usar (query param, contexto o fallback).
     */
    private Long determinarTenantId(Long tenantIdParam) {
        String tenantIdStr = TenantContext.getCurrentTenant();
        LOG.infof("📋 [PERIFERICO] Tenant en contexto: %s", tenantIdStr);
        
        if (tenantIdParam != null) {
            LOG.infof("✅ [PERIFERICO] Usando tenantId del query parameter: %d", tenantIdParam);
            return tenantIdParam;
        }
        
        if (tenantIdStr != null && !tenantIdStr.isBlank()) {
            Long tenantId = Long.parseLong(tenantIdStr);
            LOG.infof("✅ [PERIFERICO] Usando tenantId del contexto: %d", tenantId);
            return tenantId;
        }
        
        LOG.warn("⚠️ [PERIFERICO] Tenant no identificado en contexto ni query parameter, usando tenant 1 como fallback");
        return 1L;
    }
    
    /**
     * Obtiene y valida la metadata del documento.
     */
    private Map<String, Object> obtenerYValidarMetadata(String id, Long tenantId) {
        LOG.infof("🔍 [PERIFERICO] Obteniendo metadata del documento - ID: %s, Tenant: %d", id, tenantId);
        Map<String, Object> metadata = documentoPdfService.obtenerMetadataPorId(id, tenantId);
        
        if (metadata == null) {
            LOG.warnf("❌ [PERIFERICO] Documento no encontrado - ID: %s, Tenant: %d", id, tenantId);
        }
        
        return metadata;
    }
    
    /**
     * Verifica si la llamada viene del backend HCEN.
     */
    private boolean esLlamadaDesdeBackendHCEN(String profesionalId) {
        if (profesionalId == null) {
            return false;
        }
        return profesionalId.equals("hcen-backend") 
                || profesionalId.startsWith("HCEN-Service")
                || profesionalId.contains("service")
                || profesionalId.contains("backend");
    }
    
    /**
     * Valida los permisos de acceso al documento.
     */
    private Response validarPermisosAcceso(String profesionalId, String pacienteCI, String tipoDocumento,
            String tenantIdStr, boolean esLlamadaDesdeBackendHCEN) {
        if (esLlamadaDesdeBackendHCEN) {
            LOG.infof("✅ [PERIFERICO] Llamada desde backend HCEN detectada (profesionalId: %s), saltando verificación de permisos (ya verificada en backend)", 
                    profesionalId);
            return null;
        }
        
        if (profesionalId == null || profesionalId.isBlank() || pacienteCI == null || pacienteCI.isBlank()) {
            LOG.info("⚠️ [PERIFERICO] No se pudo obtener información del profesional, permitiendo descarga (compatibilidad con HCEN backend)");
            return null;
        }
        
        LOG.infof("🔐 [PERIFERICO] Verificando permisos - Profesional: %s, Paciente: %s, Tipo: %s, Tenant: %s", 
                profesionalId, pacienteCI, tipoDocumento, tenantIdStr);
        
        boolean tienePermiso = politicasAccesoClient.verificarPermiso(
                profesionalId, pacienteCI, tipoDocumento, tenantIdStr);
        
        if (!tienePermiso) {
            LOG.warnf("❌ [PERIFERICO] Acceso denegado - Profesional: %s, Paciente: %s", 
                    profesionalId, pacienteCI);
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("No tiene permiso para acceder a este documento. Se requiere una política de acceso aprobada.")
                    .build();
        }
        
        LOG.infof("✅ [PERIFERICO] Permiso concedido - Profesional: %s, Paciente: %s", 
                profesionalId, pacienteCI);
        return null;
    }
    
    /**
     * Obtiene y valida el PDF del documento.
     */
    private byte[] obtenerYValidarPdf(String id, Long tenantId) {
        LOG.infof("🔍 [PERIFERICO] Obteniendo PDF de MongoDB - ID: %s, Tenant: %d", id, tenantId);
        byte[] pdfBytes = documentoPdfService.obtenerPdfPorId(id, tenantId);
        
        if (pdfBytes == null) {
            LOG.warnf("❌ [PERIFERICO] PDF no encontrado - ID: %s, Tenant: %d", id, tenantId);
            return null;
        }
        
        LOG.infof("✅ [PERIFERICO] PDF obtenido de MongoDB - ID: %s, Tamaño: %d bytes", id, pdfBytes.length);
        validarFormatoPdf(pdfBytes);
        
        return pdfBytes;
    }
    
    /**
     * Valida que el archivo sea un PDF válido.
     */
    private void validarFormatoPdf(byte[] pdfBytes) {
        if (pdfBytes.length >= 4) {
            String header = new String(pdfBytes, 0, 4);
            if (!header.startsWith("%PDF")) {
                LOG.warnf("⚠️ [PERIFERICO] Los primeros bytes no son de un PDF válido: %s", header);
                LOG.warnf("⚠️ [PERIFERICO] Primeros 200 bytes: %s", 
                        new String(pdfBytes, 0, Math.min(200, pdfBytes.length)));
            } else {
                LOG.infof("✅ [PERIFERICO] PDF válido detectado - Header: %s", header);
            }
        }
    }
    
    /**
     * Registra el acceso del profesional si es necesario.
     */
    private void registrarAccesoSiNecesario(String profesionalId, String pacienteCI, String tipoDocumento,
            String documentoId, String tenantIdStr, boolean esLlamadaDesdeBackendHCEN) {
        if (esLlamadaDesdeBackendHCEN || profesionalId == null || profesionalId.isBlank() 
                || pacienteCI == null || pacienteCI.isBlank() || tenantIdStr == null || tenantIdStr.isBlank()) {
            return;
        }
        
        InformacionProfesional infoProf = obtenerInformacionProfesional(profesionalId);
        registrarAccesoEnHCEN(profesionalId, infoProf.nombre, infoProf.especialidad, 
                tenantIdStr, pacienteCI, documentoId, tipoDocumento);
    }
    
    /**
     * Clase interna para agrupar información del profesional.
     */
    private static class InformacionProfesional {
        String nombre;
        String especialidad;
    }
    
    /**
     * Obtiene la información completa del profesional.
     */
    private InformacionProfesional obtenerInformacionProfesional(String profesionalId) {
        InformacionProfesional info = new InformacionProfesional();
        try {
            var profesionalOpt = profesionalSaludRepository.findByNickname(profesionalId);
            if (profesionalOpt.isPresent()) {
                var profesional = profesionalOpt.get();
                if (profesional.getEspecialidad() != null) {
                    info.especialidad = profesional.getEspecialidad().name();
                }
                info.nombre = profesional.getNombre();
                LOG.infof("📝 [PERIFERICO] Información del profesional obtenida - Nombre: %s, Especialidad: %s", 
                        info.nombre, info.especialidad);
            }
        } catch (Exception e) {
            LOG.warnf("⚠️ [PERIFERICO] No se pudo obtener información completa del profesional %s: %s", 
                    profesionalId, e.getMessage());
        }
        return info;
    }
    
    /**
     * Registra el acceso en HCEN Central.
     */
    private void registrarAccesoEnHCEN(String profesionalId, String nombreProfesional, String especialidad,
            String tenantIdStr, String pacienteCI, String documentoId, String tipoDocumento) {
        try {
            hcenClient.registrarAccesoHistoriaClinica(
                    profesionalId, nombreProfesional, especialidad, tenantIdStr,
                    pacienteCI, documentoId, tipoDocumento, true);
            LOG.infof("✅ [PERIFERICO] Acceso registrado para profesional %s, paciente %s, documento %s", 
                    profesionalId, pacienteCI, documentoId);
        } catch (Exception e) {
            LOG.warnf("⚠️ [PERIFERICO] Error al registrar acceso (no crítico): %s", e.getMessage());
        }
    }
    
    /**
     * Construye la respuesta HTTP con el PDF.
     */
    private Response construirRespuestaPdf(byte[] pdfBytes, String id) {
        LOG.infof("📤 [PERIFERICO→BACKEND] Enviando PDF al backend HCEN - Tamaño: %d bytes", pdfBytes.length);
        
        return Response.ok(pdfBytes)
                .header(HEADER_CONTENT_TYPE, MIME_TYPE_PDF)
                .header(HEADER_CONTENT_LENGTH, String.valueOf(pdfBytes.length))
                .header(HEADER_CONTENT_DISPOSITION, "attachment; filename=\"documento-" + id + ".pdf\"")
                .build();
    }
}

