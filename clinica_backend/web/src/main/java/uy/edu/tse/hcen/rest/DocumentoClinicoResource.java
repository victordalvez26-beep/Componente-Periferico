package uy.edu.tse.hcen.rest;

import uy.edu.tse.hcen.service.DocumentoService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import jakarta.ws.rs.core.MultivaluedMap;

import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import java.util.Map;
import uy.edu.tse.hcen.service.HcenClient;
import uy.edu.tse.hcen.dto.DTMetadatos;
import uy.edu.tse.hcen.exceptions.HcenUnavailableException;
import jakarta.ws.rs.core.Context;
import org.jboss.logging.Logger;
import uy.edu.tse.hcen.multitenancy.TenantContext;


/**
 * Recurso REST para manejo básico de documentos clínicos en MongoDB.
 *
 * Flujo esperado:
 * 1) El profesional crea los metadatos mediante POST /api/documentos/metadatos (enviarMetadatos) — aún no implementado.
 * 2) Luego sube el contenido del documento mediante POST /api/documentos.
 * 
 * Nota: El path base "/api" está definido en RestApplication, por lo que este recurso usa "/documentos"
 */
@Path("/documentos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class DocumentoClinicoResource {

    @Inject
    private DocumentoService documentoService;

    @Inject
    private HcenClient hcenClient;

    @Inject
    private uy.edu.tse.hcen.service.PoliticasClient politicasClient;

    @Inject
    private uy.edu.tse.hcen.service.ProfesionalSaludService profesionalSaludService;

    @Inject
    private uy.edu.tse.hcen.service.OpenAIService openAIService;

    @Context
    private jakarta.ws.rs.core.SecurityContext securityContext;

    private static final Logger LOG = Logger.getLogger(DocumentoClinicoResource.class);

    public DocumentoClinicoResource() {
    }

    /**
     * Endpoint placeholder para enviar metadatos al microservicio. No implementado por ahora.
     */
    @POST
    @Path("/metadatos")
    public Response enviarMetadatos(Map<String, Object> datos) {
        if (datos == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "payload required")).build();
        }

        if (datos.get("documentIdPaciente") == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "documentIdPaciente required")).build();
        }
        
        // Convert map -> DTO using JSON-B for convenience
        try (Jsonb jsonb = JsonbBuilder.create()) {
            String json = jsonb.toJson(datos);
            DTMetadatos dto = jsonb.fromJson(json, DTMetadatos.class);

            try {
                hcenClient.registrarMetadatos(dto);
                return Response.status(Response.Status.CREATED).entity(Map.of("status", "registered", "documentoId", dto.getDocumentoId())).build();
            } catch (HcenUnavailableException ex) {
                // Central no disponible -> retry strategy could be implemented
                return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(Map.of("error", "HCEN unavailable", "detail", ex.getMessage())).build();
            }
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", "failed to map payload", "detail", ex.getMessage())).build();
        }
    }

    /**
     * Guarda el contenido de un documento. Requiere rol de profesional.
     * Espera un JSON como { "documentoId": "<id>", "contenido": "<contenido>" }
     */
    @POST
    @RolesAllowed("PROFESIONAL")
    public Response guardarContenido(Map<String, String> body) {
        if (body == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "Request body required")).build();
        }

        String documentoId = body.get("documentoId");
        String contenido = body.get("contenido");

        if (documentoId == null || documentoId.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "documentoId required")).build();
        }
        if (contenido == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "contenido required")).build();
        }

        try {
            var saved = documentoService.guardarContenido(documentoId, contenido);
            // Build a Location/URI if desired (not exposing real DB id for now)
            URI location = UriBuilder.fromPath("/api/documentos/{documentoId}").build(documentoId);
            return Response.created(location).entity(saved.toJson()).build();
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", ex.getMessage())).build();
        } catch (SecurityException ex) {
            // Tenant mismatch or unauthorized
            return Response.status(Response.Status.FORBIDDEN).entity(Map.of("error", ex.getMessage())).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", "server error" , "detail", ex.getMessage())).build();
        }
    }

    /**
     * Recupera un documento de MongoDB por su id (_id hex string).
     * Accesible públicamente para lectura (sin autenticación requerida).
     */
    @GET
    @Path("/{id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM})
    public Response obtenerPorId(@PathParam("id") String id) {
        var doc = documentoService.buscarPorId(id);
        if (doc == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", "document not found or invalid id")).build();
        }
        
        // Si existe PDF, devolverlo como binario (para compatibilidad con RNDC)
        org.bson.types.Binary pdfBinary = doc.get("pdf", org.bson.types.Binary.class);
        if (pdfBinary != null && pdfBinary.getData() != null && pdfBinary.getData().length > 0) {
            byte[] pdfBytes = pdfBinary.getData();
            String documentoId = doc.getString("documentoId");
            String fileName = "documento_" + id + ".pdf";
            if (documentoId != null && !documentoId.isBlank()) {
                fileName = "documento_" + documentoId + ".pdf";
            }
            return Response.ok(pdfBytes, MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .build();
        }
        
        // Si no hay PDF, devolver JSON (comportamiento original)
        return Response.ok(doc.toJson()).build();
    }

    /**
     * Devuelve los ids (hex) de los documentos asociados a un documentoId (p. ej. cédula del paciente).
     */
    @GET
    @Path("/ids/{documentoId}")
    @RolesAllowed("PROFESIONAL")
    public Response obtenerIdsPorDocumento(@PathParam("documentoId") String documentoId) {
        if (documentoId == null || documentoId.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "documentoId required")).build();
        }
        List<String> ids = documentoService.buscarIdsPorDocumentoPaciente(documentoId);
        if (ids == null || ids.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", "no documents found for the given documentoId")).build();
        }
        return Response.ok(ids).build();
    }

    /**
     * Crea un documento clínico completo con archivo adjunto: guarda el contenido en MongoDB, 
     * convierte a PDF, guarda el archivo adjunto y envía los metadatos al HCEN central.
     * Acepta multipart/form-data con los siguientes campos:
     * - contenido: texto del documento (requerido)
     * - documentoIdPaciente: CI del paciente (requerido)
     * - titulo: título del documento (opcional)
     * - autor: autor del documento (opcional)
     * - especialidad: especialidad (opcional)
     * - archivo: archivo adjunto (opcional)
     * 
     * El documentoId se genera automáticamente si no se proporciona.
     */
    @POST
    @Path("/completo-con-archivo")
    @RolesAllowed("PROFESIONAL")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response crearDocumentoCompletoConArchivo(MultipartFormDataInput input) {
        try {
            // Extraer campos del formulario
            String contenido = null;
            String documentoIdPaciente = null;
            String titulo = null;
            String autor = null;
            String especialidad = null;
            
            // Leer campos de texto
            if (input.getFormDataMap().containsKey("contenido")) {
                List<InputPart> contenidoParts = input.getFormDataMap().get("contenido");
                if (!contenidoParts.isEmpty()) {
                    contenido = contenidoParts.get(0).getBodyAsString();
                }
            }
            if (input.getFormDataMap().containsKey("documentoIdPaciente")) {
                List<InputPart> pacienteParts = input.getFormDataMap().get("documentoIdPaciente");
                if (!pacienteParts.isEmpty()) {
                    documentoIdPaciente = pacienteParts.get(0).getBodyAsString();
                }
            }
            if (input.getFormDataMap().containsKey("titulo")) {
                List<InputPart> tituloParts = input.getFormDataMap().get("titulo");
                if (!tituloParts.isEmpty()) {
                    titulo = tituloParts.get(0).getBodyAsString();
                }
            }
            if (input.getFormDataMap().containsKey("autor")) {
                List<InputPart> autorParts = input.getFormDataMap().get("autor");
                if (!autorParts.isEmpty()) {
                    autor = autorParts.get(0).getBodyAsString();
                }
            }
            if (input.getFormDataMap().containsKey("especialidad")) {
                List<InputPart> especialidadParts = input.getFormDataMap().get("especialidad");
                if (!especialidadParts.isEmpty()) {
                    especialidad = especialidadParts.get(0).getBodyAsString();
                }
            }

            if (contenido == null || contenido.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "contenido es requerido"))
                        .build();
            }
            if (documentoIdPaciente == null || documentoIdPaciente.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "documentoIdPaciente es requerido"))
                        .build();
            }

            // Leer archivo adjunto si está presente
            byte[] archivoBytes = null;
            String nombreArchivo = null;
            String tipoArchivo = null;
            
            if (input.getFormDataMap().containsKey("archivo")) {
                List<InputPart> archivoParts = input.getFormDataMap().get("archivo");
                if (!archivoParts.isEmpty()) {
                    InputPart archivoPart = archivoParts.get(0);
                    archivoBytes = archivoPart.getBody(byte[].class, null);
                    
                    // Obtener información del archivo desde los headers
                    MultivaluedMap<String, String> headers = archivoPart.getHeaders();
                    String contentDisposition = headers.getFirst("Content-Disposition");
                    tipoArchivo = archivoPart.getMediaType() != null ? archivoPart.getMediaType().toString() : null;
                    
                    // Extraer el nombre del archivo del header Content-Disposition
                    if (contentDisposition != null) {
                        // Formato: Content-Disposition: form-data; name="archivo"; filename="nombre_archivo.ext"
                        String[] parts = contentDisposition.split(";");
                        for (String part : parts) {
                            part = part.trim();
                            if (part.startsWith("filename")) {
                                String[] keyValue = part.split("=");
                                if (keyValue.length == 2) {
                                    nombreArchivo = keyValue[1].trim();
                                    // Remover comillas si las hay
                                    if (nombreArchivo.startsWith("\"") && nombreArchivo.endsWith("\"")) {
                                        nombreArchivo = nombreArchivo.substring(1, nombreArchivo.length() - 1);
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    
                    if (nombreArchivo != null && archivoBytes != null) {
                        LOG.infof("Archivo adjunto recibido: %s, tamaño: %d bytes, tipo: %s", 
                                nombreArchivo, archivoBytes.length, tipoArchivo);
                    }
                }
            }

            // Construir map de metadatos
            Map<String, Object> metadatos = new java.util.HashMap<>();
            if (titulo != null && !titulo.isBlank()) {
                metadatos.put("titulo", titulo);
            }
            if (autor != null && !autor.isBlank()) {
                metadatos.put("autor", autor);
            }
            if (especialidad != null && !especialidad.isBlank()) {
                metadatos.put("especialidad", especialidad);
            }

            var resultado = documentoService.crearDocumentoCompletoConArchivo(
                    contenido, documentoIdPaciente, archivoBytes, nombreArchivo, tipoArchivo, metadatos);
            
            URI location = UriBuilder.fromPath("/api/documentos/{documentoId}").build(resultado.get("documentoId"));
            return Response.created(location).entity(resultado).build();
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", ex.getMessage())).build();
        } catch (SecurityException ex) {
            return Response.status(Response.Status.FORBIDDEN).entity(Map.of("error", ex.getMessage())).build();
        } catch (uy.edu.tse.hcen.exceptions.MetadatosSyncException ex) {
            LOG.warnf("Documento guardado localmente pero sin sincronización en HCEN: %s", ex.getMessage());
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Documento guardado localmente pero sin sincronización en HCEN", 
                            "detail", ex.getMessage()))
                    .build();
        } catch (Exception ex) {
            LOG.error("Error al crear documento con archivo", ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al crear documento", "detail", ex.getMessage()))
                    .build();
        }
    }

    /**
     * Crea un documento clínico completo: guarda el contenido en MongoDB y envía los metadatos al HCEN central.
     * Espera un JSON con "contenido" y los campos de metadatos (documentoIdPaciente, especialidad, etc.)
     * El documentoId se genera automáticamente si no se proporciona.
     * Convierte automáticamente el contenido a PDF y lo guarda como binario.
     */
    @POST
    @Path("/completo")
    @RolesAllowed("PROFESIONAL")
    public Response crearDocumentoCompleto(Map<String, Object> body) {
        if (body == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "Request body required")).build();
        }
        if (body.get("documentoIdPaciente") == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "documentoIdPaciente required")).build();
        }

        try {
            var resultado = documentoService.crearDocumentoCompleto(body);
            URI location = UriBuilder.fromPath("/api/documentos/{documentoId}").build(resultado.get("documentoId"));
            return Response.created(location).entity(resultado).build();
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", ex.getMessage())).build();
        } catch (SecurityException ex) {
            return Response.status(Response.Status.FORBIDDEN).entity(Map.of("error", ex.getMessage())).build();
        } catch (uy.edu.tse.hcen.exceptions.MetadatosSyncException ex) {
            // El documento se guardó localmente pero no se sincronizó con HCEN
            // Retornar éxito con advertencia
            LOG.warnf("Documento guardado localmente pero sin sincronización en HCEN: %s", ex.getMessage());
            // Intentar obtener el resultado del documento guardado
            try {
                // El documento ya está guardado, intentar construir respuesta básica
                String documentoId = (String) body.get("documentoId");
                if (documentoId == null || documentoId.isBlank()) {
                    documentoId = java.util.UUID.randomUUID().toString();
                }
                String documentoIdPaciente = (String) body.get("documentoIdPaciente");
                Map<String, Object> resultado = Map.of(
                    "documentoId", documentoId,
                    "documentoIdPaciente", documentoIdPaciente != null ? documentoIdPaciente : "",
                    "warning", "Documento guardado localmente pero sin sincronización en HCEN"
                );
                URI location = UriBuilder.fromPath("/api/documentos/{documentoId}").build(documentoId);
                return Response.status(Response.Status.CREATED).entity(resultado).location(location).build();
            } catch (Exception e) {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity(Map.of("error", "Documento guardado localmente pero sin sincronización en HCEN", "detail", ex.getMessage()))
                        .build();
            }
        } catch (RuntimeException ex) {
            // Puede contener HcenUnavailableException envuelta
            if (ex.getMessage() != null && ex.getMessage().contains("HCEN unavailable")) {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(Map.of("error", "HCEN unavailable", "detail", ex.getMessage())).build();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", "server error", "detail", ex.getMessage())).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", "server error", "detail", ex.getMessage())).build();
        }
    }

    /**
     * GET /api/documentos/paciente/{documentoIdPaciente}/metadatos
     * 
     * Obtiene todos los metadatos de documentos de un paciente.
     * Si los documentos son del mismo tenant, los busca localmente (desde RNDC vía HCEN).
     * Si son de otros tenants, los consulta desde HCEN central.
     * 
     * @param documentoIdPaciente CI o documento de identidad del paciente
     * @return Lista de metadatos de documentos del paciente
     */
    @GET
    @Path("/paciente/{documentoIdPaciente}/metadatos")
    @RolesAllowed("PROFESIONAL")
    public Response obtenerMetadatosPaciente(@PathParam("documentoIdPaciente") String documentoIdPaciente) {
        if (documentoIdPaciente == null || documentoIdPaciente.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "documentoIdPaciente es requerido"))
                    .build();
        }

        try {
            // Obtener tenantId del contexto actual
            String currentTenantId = TenantContext.getCurrentTenant();
            
            // Consultar metadatos desde HCEN central (que consulta RNDC)
            // HCEN central maneja la lógica de filtrar por tenant si es necesario
            List<Map<String, Object>> metadatos = hcenClient.consultarMetadatosPaciente(documentoIdPaciente);
            
            // Filtrar metadatos del mismo tenant si hay tenantId en el contexto
            if (currentTenantId != null && !currentTenantId.isBlank()) {
                List<Map<String, Object>> metadatosFiltrados = new ArrayList<>();
                for (Map<String, Object> meta : metadatos) {
                    String tenantId = (String) meta.get("tenantId");
                    // Incluir documentos del mismo tenant
                    if (currentTenantId.equals(tenantId)) {
                        metadatosFiltrados.add(meta);
                    }
                }
                return Response.ok(metadatosFiltrados).build();
            } else {
                // Si no hay tenantId en contexto, devolver todos los metadatos
                // (esto puede ser útil para administradores o consultas globales)
                return Response.ok(metadatos).build();
            }
            
        } catch (uy.edu.tse.hcen.exceptions.HcenUnavailableException ex) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "HCEN no disponible", "detail", ex.getMessage()))
                    .build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al obtener metadatos", "detail", ex.getMessage()))
                    .build();
        }
    }

    /**
     * GET /api/documentos/{id}/contenido
     * 
     * Endpoint mejorado para descargar el contenido de un documento.
     * Verifica permisos antes de devolver el contenido.
     * Registra el acceso para auditoría.
     * 
     * PERMITE ACCESO DEL PACIENTE A SUS PROPIOS DOCUMENTOS:
     * - Si el header X-Paciente-CI coincide con el CI del documento, se permite acceso sin verificación de políticas.
     * - Para profesionales, se verifica mediante políticas de acceso.
     * 
     * @param id ID del documento (MongoDB _id hex string)
     * @param pacienteCI CI del paciente (header opcional, para acceso directo del paciente)
     * @return El contenido del documento
     */
    @GET
    @Path("/{id}/contenido")
    @RolesAllowed("PROFESIONAL")
    public Response obtenerContenido(
            @PathParam("id") String id,
            @HeaderParam("X-Paciente-CI") String pacienteCI) {
        if (id == null || id.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "id required")).build();
        }

        // 1) Buscar documento en MongoDB
        var doc = documentoService.buscarPorId(id);
        if (doc == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "document not found or invalid id"))
                    .build();
        }
        
        // 2) Obtener información del usuario desde el token
        String usuarioId = null;
        if (securityContext != null && securityContext.getUserPrincipal() != null) {
            usuarioId = securityContext.getUserPrincipal().getName();
        }
        
        if (usuarioId == null || usuarioId.isBlank()) {
            // Registrar acceso denegado
            politicasClient.registrarAcceso(null, null, null, null, false, 
                "Usuario no identificado", "Intento de acceso sin autenticación");
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Autenticación requerida"))
                    .build();
        }
        
        // 3) Obtener metadatos del documento para obtener documentoIdPaciente
        String documentoId = doc.getString("documentoId");
        String codDocumPaciente = null;
        String tipoDocumento = null;
        boolean accesoPermitido = false;
        String motivoRechazo = null;
        boolean esAccesoPaciente = false;
        
        if (documentoId != null && !documentoId.isBlank()) {
            // Consultar metadatos para obtener información del paciente
            Map<String, Object> metadatos = documentoService.obtenerMetadatosPorDocumentoId(documentoId);
            if (metadatos != null) {
                codDocumPaciente = (String) metadatos.get("documentoIdPaciente");
                // Intentar obtener tipoDocumento primero (tipo de documento clínico)
                tipoDocumento = (String) metadatos.get("tipoDocumento");
                // Si no hay tipoDocumento, usar formato como fallback (aunque no es ideal)
                if (tipoDocumento == null || tipoDocumento.isBlank()) {
                    tipoDocumento = (String) metadatos.get("formato");
                }
            }
        }
        
        // Si no hay metadatos, intentar obtener pacienteDoc del documento directamente
        if (codDocumPaciente == null || codDocumPaciente.isBlank()) {
            codDocumPaciente = doc.getString("pacienteDoc");
        }
        
        // 4) Verificar si es acceso del paciente a sus propios documentos
        if (codDocumPaciente != null && !codDocumPaciente.isBlank()) {
            // Si se proporciona el header X-Paciente-CI y coincide con el CI del documento, permitir acceso
            if (pacienteCI != null && !pacienteCI.isBlank() && pacienteCI.equals(codDocumPaciente)) {
                accesoPermitido = true;
                esAccesoPaciente = true;
                LOG.infof("Acceso permitido: Paciente %s accediendo a su propio documento %s", pacienteCI, id);
            } else {
                // Verificar permisos para profesionales mediante políticas
                String tenantId = TenantContext.getCurrentTenant();
                accesoPermitido = politicasClient.verificarPermiso(usuarioId, codDocumPaciente, tipoDocumento, tenantId);
                if (!accesoPermitido) {
                    motivoRechazo = "No tiene permisos para acceder a este documento";
                    // Logging de auditoría para acceso denegado
                    uy.edu.tse.hcen.common.security.SecurityAuditLogger.logFailedAccess(
                        usuarioId, "/api/documentos/" + id, motivoRechazo);
                } else {
                    // Logging de auditoría para acceso exitoso a documento sensible
                    uy.edu.tse.hcen.common.security.SecurityAuditLogger.logSensitiveAccess(
                        usuarioId, "/api/documentos/" + id);
                }
            }
        } else {
            // Si no podemos determinar el paciente, permitir acceso pero registrar advertencia
            accesoPermitido = true; // Por compatibilidad, permitir si no hay metadatos
            LOG.warnf("No se pudo determinar paciente para documento %s, permitiendo acceso", id);
        }
        
        // 5) Registrar acceso (exitoso o no)
        String referencia = esAccesoPaciente ? "Acceso del paciente a su propio documento" : "Acceso desde componente periférico";
        politicasClient.registrarAcceso(usuarioId, codDocumPaciente, documentoId, tipoDocumento,
                accesoPermitido, motivoRechazo, referencia);
        
        // 6) Si no tiene permiso, denegar acceso
        if (!accesoPermitido) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", "No tiene permisos para acceder a este documento",
                                 "detail", motivoRechazo))
                    .build();
        }
        
        // 7) Verificar si existe PDF y devolverlo si está disponible
        org.bson.types.Binary pdfBinary = doc.get("pdf", org.bson.types.Binary.class);
        if (pdfBinary != null && pdfBinary.getData() != null && pdfBinary.getData().length > 0) {
            // Devolver PDF binario
            byte[] pdfBytes = pdfBinary.getData();
            String fileName = "documento_" + id + ".pdf";
            if (documentoId != null && !documentoId.isBlank()) {
                fileName = "documento_" + documentoId + ".pdf";
            }
            return Response.ok(pdfBytes, MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .build();
        }
        
        // 8) Si no hay PDF, devolver contenido de texto
        String contenido = doc.getString("contenido");
        if (contenido == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "document has no content"))
                    .build();
        }
        
        // 9) Determinar Content-Type para texto
        String contentType = tipoDocumento != null && !tipoDocumento.isBlank() 
                ? tipoDocumento 
                : MediaType.TEXT_PLAIN;
        
        // 10) Devolver contenido de texto
        return Response.ok(contenido, contentType)
                .header("Content-Disposition", "inline; filename=\"documento_" + id + ".txt\"")
                .build();
    }

    /**
     * POST /api/documentos/solicitar-acceso
     * 
     * Permite a un profesional solicitar acceso a documentos de un paciente.
     * 
     * @param body JSON con codDocumPaciente, tipoDocumento (opcional), documentoId (opcional), razonSolicitud
     * @return ID de la solicitud creada
     */
    @POST
    @Path("/solicitar-acceso")
    @RolesAllowed("PROFESIONAL")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response solicitarAcceso(Map<String, Object> body) {
        // 1) Obtener información del profesional desde el token
        String profesionalId = null;
        if (securityContext != null && securityContext.getUserPrincipal() != null) {
            profesionalId = securityContext.getUserPrincipal().getName();
        }
        
        if (profesionalId == null || profesionalId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Autenticación requerida"))
                    .build();
        }
        
        // 2) Validar campos requeridos
        String codDocumPaciente = (String) body.get("codDocumPaciente");
        if (codDocumPaciente == null || codDocumPaciente.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "codDocumPaciente es requerido"))
                    .build();
        }
        
        String tipoDocumento = (String) body.get("tipoDocumento");
        String documentoId = (String) body.get("documentoId");
        String razonSolicitud = (String) body.get("razonSolicitud");
        
        // 3) Obtener especialidad del profesional (opcional, puede venir en el body)
        String especialidad = (String) body.get("especialidad");
        if (especialidad == null || especialidad.isBlank()) {
            // Intentar obtener desde el contexto del usuario si está disponible
            especialidad = "General"; // Default
        }
        
        // 4) Crear solicitud de acceso
        Long solicitudId = politicasClient.crearSolicitudAcceso(
                profesionalId, especialidad, codDocumPaciente,
                tipoDocumento, documentoId, razonSolicitud);
        
        if (solicitudId == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al crear solicitud de acceso"))
                    .build();
        }
        
        return Response.status(Response.Status.CREATED)
                .entity(Map.of(
                    "solicitudId", solicitudId,
                    "profesionalId", profesionalId,
                    "codDocumPaciente", codDocumPaciente,
                    "estado", "PENDIENTE",
                    "mensaje", "Solicitud de acceso creada exitosamente"
                ))
                .build();
    }

    /**
     * POST /api/documentos/solicitar-acceso-historia-clinica
     * 
     * Permite a un profesional solicitar acceso a toda la historia clínica de un paciente.
     * Este endpoint es diferente de /solicitar-acceso porque no requiere documentoId ni tipoDocumento,
     * indicando que se solicita acceso a todos los documentos del paciente.
     * 
     * @param body JSON con codDocumPaciente, razonSolicitud (opcional), especialidad (opcional)
     * @return ID de la solicitud creada
     */
    @POST
    @Path("/solicitar-acceso-historia-clinica")
    @RolesAllowed("PROFESIONAL")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response solicitarAccesoHistoriaClinica(Map<String, Object> body) {
        // 1) Obtener información del profesional desde el token
        String profesionalId = null;
        if (securityContext != null && securityContext.getUserPrincipal() != null) {
            profesionalId = securityContext.getUserPrincipal().getName();
        }
        
        if (profesionalId == null || profesionalId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Autenticación requerida"))
                    .build();
        }
        
        // 2) Validar campos requeridos
        String codDocumPaciente = (String) body.get("codDocumPaciente");
        if (codDocumPaciente == null || codDocumPaciente.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "codDocumPaciente es requerido"))
                    .build();
        }
        
        // 3) Para solicitud de historia clínica completa, no se especifica documentoId ni tipoDocumento
        String razonSolicitud = (String) body.get("razonSolicitud");
        if (razonSolicitud == null || razonSolicitud.isBlank()) {
            razonSolicitud = "Solicitud de acceso a toda la historia clínica del paciente";
        }
        
        // 4) Obtener especialidad del profesional (opcional)
        String especialidad = (String) body.get("especialidad");
        if (especialidad == null || especialidad.isBlank()) {
            especialidad = "General"; // Default
        }
        
        // 5) Crear solicitud de acceso a toda la historia clínica
        // tipoDocumento y documentoId son null para indicar acceso a todos los documentos
        Long solicitudId = politicasClient.crearSolicitudAcceso(
                profesionalId, especialidad, codDocumPaciente,
                null, // tipoDocumento = null indica todos los tipos
                null, // documentoId = null indica todos los documentos
                razonSolicitud);
        
        if (solicitudId == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al crear solicitud de acceso"))
                    .build();
        }
        
        return Response.status(Response.Status.CREATED)
                .entity(Map.of(
                    "solicitudId", solicitudId,
                    "profesionalId", profesionalId,
                    "codDocumPaciente", codDocumPaciente,
                    "tipoSolicitud", "HISTORIA_CLINICA_COMPLETA",
                    "estado", "PENDIENTE",
                    "mensaje", "Solicitud de acceso a historia clínica completa creada exitosamente"
                ))
                .build();
    }

    /**
     * POST /api/documentos/solicitudes/{id}/aprobar
     * 
     * Permite a un paciente o administrador aprobar una solicitud de acceso pendiente.
     * 
     * FLUJO:
     * - Un profesional crea un documento clínico de un paciente.
     * - Otro profesional intenta acceder y solicita autorización.
     * - EL PACIENTE es quien debe aprobar la solicitud.
     * 
     * Nota: Este endpoint permite que cualquier usuario autenticado apruebe solicitudes.
     * En producción, se debería validar que el usuario sea el paciente correspondiente.
     * 
     * @param id ID de la solicitud a aprobar
     * @param body JSON con resueltoPor (opcional), comentario (opcional), pacienteCI (opcional para validación)
     * @return Solicitud aprobada
     */
    @POST
    @Path("/solicitudes/{id}/aprobar")
    // Permitir a cualquier usuario autenticado aprobar (incluye pacientes)
    @Consumes({MediaType.APPLICATION_JSON, MediaType.WILDCARD})
    public Response aprobarSolicitud(@PathParam("id") Long id, Map<String, Object> body) {
        // 1) Obtener información del usuario desde el token
        String resueltoPor = null;
        if (securityContext != null && securityContext.getUserPrincipal() != null) {
            resueltoPor = securityContext.getUserPrincipal().getName();
        }
        
        // Si viene en el body, usar ese valor
        if (body != null && body.containsKey("resueltoPor")) {
            resueltoPor = (String) body.get("resueltoPor");
        }
        
        if (resueltoPor == null || resueltoPor.isBlank()) {
            resueltoPor = "paciente"; // Default
        }
        
        String comentario = body != null ? (String) body.get("comentario") : null;
        
        // 2) Aprobar la solicitud
        boolean exito = politicasClient.aprobarSolicitud(id, resueltoPor, comentario);
        
        if (!exito) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al aprobar la solicitud"))
                    .build();
        }
        
        return Response.ok(Map.of(
            "solicitudId", id,
            "estado", "APROBADA",
            "resueltoPor", resueltoPor,
            "mensaje", "Solicitud aprobada exitosamente"
        )).build();
    }

    /**
     * POST /api/documentos/solicitudes/{id}/rechazar
     * 
     * Permite a un paciente o administrador rechazar una solicitud de acceso pendiente.
     * 
     * FLUJO:
     * - Un profesional crea un documento clínico de un paciente.
     * - Otro profesional intenta acceder y solicita autorización.
     * - EL PACIENTE es quien debe rechazar la solicitud si no desea otorgar acceso.
     * 
     * Nota: Este endpoint permite que cualquier usuario autenticado rechace solicitudes.
     * En producción, se debería validar que el usuario sea el paciente correspondiente.
     * 
     * @param id ID de la solicitud a rechazar
     * @param body JSON con resueltoPor (opcional), comentario (opcional), pacienteCI (opcional para validación)
     * @return Solicitud rechazada
     */
    @POST
    @Path("/solicitudes/{id}/rechazar")
    // Permitir a cualquier usuario autenticado rechazar (incluye pacientes)
    @Consumes({MediaType.APPLICATION_JSON, MediaType.WILDCARD})
    public Response rechazarSolicitud(@PathParam("id") Long id, Map<String, Object> body) {
        // 1) Obtener información del usuario desde el token
        String resueltoPor = null;
        if (securityContext != null && securityContext.getUserPrincipal() != null) {
            resueltoPor = securityContext.getUserPrincipal().getName();
        }
        
        // Si viene en el body, usar ese valor
        if (body != null && body.containsKey("resueltoPor")) {
            resueltoPor = (String) body.get("resueltoPor");
        }
        
        if (resueltoPor == null || resueltoPor.isBlank()) {
            resueltoPor = "paciente"; // Default
        }
        
        String comentario = body != null ? (String) body.get("comentario") : null;
        
        // 2) Rechazar la solicitud
        boolean exito = politicasClient.rechazarSolicitud(id, resueltoPor, comentario);
        
        if (!exito) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al rechazar la solicitud"))
                    .build();
        }
        
        return Response.ok(Map.of(
            "solicitudId", id,
            "estado", "RECHAZADA",
            "resueltoPor", resueltoPor,
            "mensaje", "Solicitud rechazada exitosamente"
        )).build();
    }

    // ==================== ENDPOINTS DE CONFIGURACIÓN DE POLÍTICAS ====================

    /**
     * POST /api/documentos/politicas
     * 
     * Crea una política de acceso para un profesional específico y un paciente.
     * 
     * @param body JSON con los parámetros de la política
     * @return Política creada
     */
    @POST
    @Path("/politicas")
    @RolesAllowed({"ADMINISTRADOR", "PROFESIONAL"})
    public Response crearPolitica(Map<String, Object> body) {
        try {
            String alcance = (String) body.getOrDefault("alcance", "TODOS_LOS_DOCUMENTOS");
            String duracion = (String) body.getOrDefault("duracion", "INDEFINIDA");
            String gestion = (String) body.getOrDefault("gestion", "AUTOMATICA");
            String codDocumPaciente = (String) body.get("codDocumPaciente");
            String profesionalAutorizado = (String) body.get("profesionalAutorizado");
            String tipoDocumento = (String) body.get("tipoDocumento");
            String fechaVencimiento = (String) body.get("fechaVencimiento");
            String referencia = (String) body.get("referencia");

            if (profesionalAutorizado == null || profesionalAutorizado.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "profesionalAutorizado es requerido"))
                        .build();
            }

            Long politicaId = politicasClient.crearPolitica(
                    alcance, duracion, gestion,
                    codDocumPaciente, profesionalAutorizado,
                    tipoDocumento, fechaVencimiento, referencia);

            if (politicaId == null) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(Map.of("error", "Error al crear la política"))
                        .build();
            }

            return Response.status(Response.Status.CREATED)
                    .entity(Map.of(
                            "politicaId", politicaId,
                            "mensaje", "Política creada exitosamente",
                            "alcance", alcance,
                            "duracion", duracion,
                            "gestion", gestion
                    ))
                    .build();
        } catch (Exception e) {
            LOG.error("Error creando política", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al crear la política: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * POST /api/documentos/politicas/global
     * 
     * Crea una política global que permite a un profesional acceder a TODOS los pacientes.
     * Nota: Para políticas globales, codDocumPaciente debe ser null o "*".
     * 
     * @param body JSON con los parámetros de la política global
     * @return Política creada
     */
    @POST
    @Path("/politicas/global")
    @RolesAllowed("ADMINISTRADOR")
    public Response crearPoliticaGlobal(Map<String, Object> body) {
        try {
            String alcance = (String) body.getOrDefault("alcance", "TODOS_LOS_DOCUMENTOS");
            String duracion = (String) body.getOrDefault("duracion", "INDEFINIDA");
            String gestion = (String) body.getOrDefault("gestion", "AUTOMATICA");
            String profesionalAutorizado = (String) body.get("profesionalAutorizado");
            String tipoDocumento = (String) body.get("tipoDocumento");
            String fechaVencimiento = (String) body.get("fechaVencimiento");
            String referencia = (String) body.getOrDefault("referencia", "Política global");

            if (profesionalAutorizado == null || profesionalAutorizado.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "profesionalAutorizado es requerido"))
                        .build();
            }

            // Para políticas globales, codDocumPaciente es null
            // Nota: El servicio de políticas puede requerir un valor especial para políticas globales
            // Por ahora, usamos un valor especial "*" que el servicio puede interpretar
            Long politicaId = politicasClient.crearPolitica(
                    alcance, duracion, gestion,
                    "*", // Valor especial para políticas globales
                    profesionalAutorizado,
                    tipoDocumento, fechaVencimiento, referencia);

            if (politicaId == null) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(Map.of("error", "Error al crear la política global"))
                        .build();
            }

            return Response.status(Response.Status.CREATED)
                    .entity(Map.of(
                            "politicaId", politicaId,
                            "mensaje", "Política global creada exitosamente",
                            "tipo", "GLOBAL",
                            "profesionalAutorizado", profesionalAutorizado,
                            "alcance", alcance
                    ))
                    .build();
        } catch (Exception e) {
            LOG.error("Error creando política global", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al crear la política global: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * POST /api/documentos/politicas/especialidad
     * 
     * Crea políticas de acceso para todos los profesionales de una especialidad específica
     * y un paciente. Crea una política por cada profesional de esa especialidad.
     * 
     * @param body JSON con los parámetros
     * @return Resumen de políticas creadas
     */
    @POST
    @Path("/politicas/especialidad")
    @RolesAllowed("ADMINISTRADOR")
    public Response crearPoliticasPorEspecialidad(Map<String, Object> body) {
        try {
            String especialidad = (String) body.get("especialidad");
            String codDocumPaciente = (String) body.get("codDocumPaciente");
            String alcance = (String) body.getOrDefault("alcance", "TODOS_LOS_DOCUMENTOS");
            String duracion = (String) body.getOrDefault("duracion", "INDEFINIDA");
            String gestion = (String) body.getOrDefault("gestion", "AUTOMATICA");
            String tipoDocumento = (String) body.get("tipoDocumento");
            String fechaVencimiento = (String) body.get("fechaVencimiento");
            String referencia = (String) body.getOrDefault("referencia", 
                    "Política por especialidad: " + especialidad);

            if (especialidad == null || especialidad.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "especialidad es requerida"))
                        .build();
            }

            if (codDocumPaciente == null || codDocumPaciente.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "codDocumPaciente es requerido"))
                        .build();
            }

            // Buscar todos los profesionales de esa especialidad
            List<uy.edu.tse.hcen.model.ProfesionalSalud> profesionales = 
                    profesionalSaludService.findByEspecialidad(especialidad);

            if (profesionales.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "No se encontraron profesionales con la especialidad: " + especialidad))
                        .build();
            }

            // Crear una política para cada profesional
            List<Map<String, Object>> politicasCreadas = new ArrayList<>();
            int exitosas = 0;
            int fallidas = 0;

            for (uy.edu.tse.hcen.model.ProfesionalSalud profesional : profesionales) {
                String profesionalNickname = profesional.getNickname();
                Long politicaId = politicasClient.crearPolitica(
                        alcance, duracion, gestion,
                        codDocumPaciente, profesionalNickname,
                        tipoDocumento, fechaVencimiento, referencia);

                if (politicaId != null) {
                    exitosas++;
                    politicasCreadas.add(Map.of(
                            "politicaId", politicaId,
                            "profesionalId", profesionalNickname,
                            "profesionalNombre", profesional.getNombre()
                    ));
                } else {
                    fallidas++;
                }
            }

            return Response.status(Response.Status.CREATED)
                    .entity(Map.of(
                            "mensaje", "Políticas creadas por especialidad",
                            "especialidad", especialidad,
                            "codDocumPaciente", codDocumPaciente,
                            "totalProfesionales", profesionales.size(),
                            "politicasExitosas", exitosas,
                            "politicasFallidas", fallidas,
                            "politicas", politicasCreadas
                    ))
                    .build();
        } catch (Exception e) {
            LOG.error("Error creando políticas por especialidad", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al crear políticas por especialidad: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * GET /api/documentos/politicas
     * 
     * Lista todas las políticas de acceso.
     * 
     * @return Lista de políticas
     */
    @GET
    @Path("/politicas")
    @RolesAllowed({"ADMINISTRADOR", "PROFESIONAL"})
    public Response listarPoliticas() {
        try {
            List<Map<String, Object>> politicas = politicasClient.listarPoliticas();
            if (politicas == null) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(Map.of("error", "Error al listar políticas"))
                        .build();
            }
            return Response.ok(politicas).build();
        } catch (Exception e) {
            LOG.error("Error listando políticas", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al listar políticas: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * GET /api/documentos/politicas/paciente/{ci}
     * 
     * Lista las políticas de acceso de un paciente específico.
     * 
     * @param ci CI del paciente
     * @return Lista de políticas del paciente
     */
    @GET
    @Path("/politicas/paciente/{ci}")
    @RolesAllowed({"ADMINISTRADOR", "PROFESIONAL"})
    public Response listarPoliticasPorPaciente(@PathParam("ci") String ci) {
        try {
            List<Map<String, Object>> politicas = politicasClient.listarPoliticasPorPaciente(ci);
            if (politicas == null) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(Map.of("error", "Error al listar políticas del paciente"))
                        .build();
            }
            return Response.ok(politicas).build();
        } catch (Exception e) {
            LOG.error("Error listando políticas por paciente", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al listar políticas del paciente: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * GET /api/documentos/politicas/profesional/{id}
     * 
     * Lista las políticas de acceso de un profesional específico.
     * 
     * @param id ID o nickname del profesional
     * @return Lista de políticas del profesional
     */
    @GET
    @Path("/politicas/profesional/{id}")
    @RolesAllowed({"ADMINISTRADOR", "PROFESIONAL"})
    public Response listarPoliticasPorProfesional(@PathParam("id") String id) {
        try {
            List<Map<String, Object>> politicas = politicasClient.listarPoliticasPorProfesional(id);
            if (politicas == null) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(Map.of("error", "Error al listar políticas del profesional"))
                        .build();
            }
            return Response.ok(politicas).build();
        } catch (Exception e) {
            LOG.error("Error listando políticas por profesional", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al listar políticas del profesional: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * DELETE /api/documentos/politicas/{id}
     * 
     * Elimina una política de acceso.
     * 
     * @param id ID de la política a eliminar
     * @return Respuesta de eliminación
     */
    @DELETE
    @Path("/politicas/{id}")
    @RolesAllowed("ADMINISTRADOR")
    public Response eliminarPolitica(@PathParam("id") Long id) {
        try {
            boolean exito = politicasClient.eliminarPolitica(id);
            if (!exito) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Política no encontrada o error al eliminar"))
                        .build();
            }
            return Response.ok(Map.of(
                    "politicaId", id,
                    "mensaje", "Política eliminada exitosamente"
            )).build();
        } catch (Exception e) {
            LOG.error("Error eliminando política", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al eliminar la política: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * GET /api/documentos/paciente/{documentoIdPaciente}/resumen
     * 
     * Genera un resumen de la historia clínica completa del paciente usando IA.
     * Requiere que el profesional tenga permisos para acceder a TODA la historia clínica.
     * 
     * @param documentoIdPaciente CI o documento de identidad del paciente
     * @return Resumen generado por IA de la historia clínica
     */
    @GET
    @Path("/paciente/{documentoIdPaciente}/resumen")
    @RolesAllowed("PROFESIONAL")
    public Response generarResumenHistoriaClinica(@PathParam("documentoIdPaciente") String documentoIdPaciente) {
        if (documentoIdPaciente == null || documentoIdPaciente.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "documentoIdPaciente es requerido"))
                    .build();
        }

        try {
            // 1) Obtener información del profesional desde el token
            String profesionalId = null;
            if (securityContext != null && securityContext.getUserPrincipal() != null) {
                profesionalId = securityContext.getUserPrincipal().getName();
            }
            
            if (profesionalId == null || profesionalId.isBlank()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("error", "Autenticación requerida"))
                        .build();
            }

            // 2) Verificar permisos para acceder a TODA la historia clínica
            // Verificar sin tipoDocumento específico para verificar acceso general
            String tenantId = TenantContext.getCurrentTenant();
            boolean tienePermiso = politicasClient.verificarPermiso(profesionalId, documentoIdPaciente, null, tenantId);
            
            if (!tienePermiso) {
                // Registrar acceso denegado
                uy.edu.tse.hcen.common.security.SecurityAuditLogger.logFailedAccess(
                    profesionalId, 
                    "/api/documentos/paciente/" + documentoIdPaciente + "/resumen",
                    "No tiene permisos para acceder a la historia clínica completa");
                
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(Map.of("error", "No tiene permisos para acceder a la historia clínica completa del paciente"))
                        .build();
            }

            // 3) Obtener todos los contenidos de documentos del paciente
            List<String> contenidos = documentoService.obtenerContenidosPorPaciente(documentoIdPaciente);
            
            if (contenidos == null || contenidos.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "No se encontraron documentos para el paciente"))
                        .build();
            }

            // 4) Combinar todos los contenidos en un solo texto
            StringBuilder historiaClinicaCompleta = new StringBuilder();
            for (int i = 0; i < contenidos.size(); i++) {
                historiaClinicaCompleta.append("=== Documento ").append(i + 1).append(" ===\n");
                historiaClinicaCompleta.append(contenidos.get(i));
                historiaClinicaCompleta.append("\n\n");
            }

            // 5) Generar resumen usando OpenAI
            String resumen = openAIService.generarResumenHistoriaClinica(historiaClinicaCompleta.toString());

            // 6) Registrar acceso exitoso
            uy.edu.tse.hcen.common.security.SecurityAuditLogger.logSensitiveAccess(
                profesionalId, 
                "/api/documentos/paciente/" + documentoIdPaciente + "/resumen");

            return Response.ok(Map.of(
                "paciente", documentoIdPaciente,
                "resumen", resumen,
                "documentosProcesados", contenidos.size()
            )).build();

        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        } catch (RuntimeException ex) {
            LOG.error("Error generando resumen de historia clínica", ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al generar resumen: " + ex.getMessage()))
                    .build();
        } catch (Exception ex) {
            LOG.error("Error inesperado generando resumen", ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error inesperado al generar resumen"))
                    .build();
        }
    }
}
