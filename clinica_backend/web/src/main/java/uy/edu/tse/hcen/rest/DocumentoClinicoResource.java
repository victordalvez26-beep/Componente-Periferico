package uy.edu.tse.hcen.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.HttpHeaders;
import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.bson.Document;
import org.bson.types.Binary;
import uy.edu.tse.hcen.multitenancy.TenantContext;
import uy.edu.tse.hcen.service.DocumentoService;
import uy.edu.tse.hcen.service.OpenAIService;
import jakarta.ws.rs.core.SecurityContext;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Recurso REST para manejo de documentos clínicos completos.
 *
 * Permite a los profesionales de salud:
 * - Crear documentos clínicos completos con contenido de texto (se convierte a PDF al descargarse)
 * - Crear documentos con archivos adjuntos
 * - Obtener documentos por ID
 * - Obtener contenido y PDF de documentos
 */
@Path("/documentos")
@RequestScoped
public class DocumentoClinicoResource {

    private static final Logger LOG = Logger.getLogger(DocumentoClinicoResource.class);
    
    // Constantes para literales duplicados
    private static final String ERROR_TENANT_NO_IDENTIFICADO = "Tenant no identificado";
    private static final String KEY_CI_PACIENTE = "ciPaciente";
    private static final String KEY_TIPO_DOCUMENTO = "tipoDocumento";
    private static final String KEY_DESCRIPCION = "descripcion";
    private static final String KEY_TITULO = "titulo";
    private static final String KEY_AUTOR = "autor";
    private static final String KEY_MONGO_ID = "mongoId";
    private static final String KEY_DOCUMENTO_ID = "documentoId";
    private static final String KEY_FECHA_CREACION = "fechaCreacion";
    private static final String KEY_ERROR = "error";
    private static final String KEY_PACIENTE_CI = "pacienteCI";

    @Inject
    private DocumentoService documentoService;

    @Inject
    private OpenAIService openAIService;

    @Inject
    private uy.edu.tse.hcen.client.PoliticasAccesoClient politicasAccesoClient;
    
    @Context
    private SecurityContext securityContext;
    
    @Context
    private HttpHeaders httpHeaders;


    /**
     * POST /api/documentos/completo
     * 
     * Crea un documento clínico completo con contenido de texto.
     * El contenido se guarda en MongoDB y el PDF se genera on-demand al descargarse.
     * 
     * Body JSON esperado:
     * {
     *   "ciPaciente": "12345678",
     *   "contenido": "Contenido del documento...",
     *   "tipoDocumento": "EVALUACION",
     *   "descripcion": "Descripción opcional",
     *   "titulo": "Título opcional",
     *   "autor": "Autor opcional"
     * }
     * 
     * @param body JSON con los datos del documento
     * @return Respuesta con el ID del documento creado
     */
    @POST
    @Path("/completo")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("PROFESIONAL")
    public Response crearDocumentoCompleto(Map<String, Object> body) {
        try {
            LOG.info("=== crearDocumentoCompleto INICIO ===");
            // Validar body
            if (body == null) {
                LOG.info("crearDocumentoCompleto: body es null - retornando 400");
                return DocumentoResponseBuilder.badRequest(DocumentoConstants.ERROR_REQUEST_BODY_REQUIRED);
            }

            // Obtener información del profesional autenticado
            String profesionalId = null;
            if (securityContext != null && securityContext.getUserPrincipal() != null) {
                profesionalId = securityContext.getUserPrincipal().getName();
            }

            LOG.infof("crearDocumentoCompleto: profesionalId=%s, securityContext=%s", profesionalId, securityContext);
            if (profesionalId == null || profesionalId.isBlank()) {
                LOG.info("crearDocumentoCompleto: profesionalId es null o vacío - retornando 401");
                return DocumentoResponseBuilder.unauthorized(DocumentoConstants.ERROR_AUTENTICACION_REQUERIDA);
            }

            // Obtener tenant actual
            String tenantIdStr = TenantContext.getCurrentTenant();
            LOG.infof("crearDocumentoCompleto: tenantIdStr=%s", tenantIdStr);
            if (tenantIdStr == null || tenantIdStr.isBlank()) {
                LOG.info("crearDocumentoCompleto: tenantIdStr es null o vacío - retornando 400");
                return DocumentoResponseBuilder.badRequest(ERROR_TENANT_NO_IDENTIFICADO);
            }
            Long tenantId = Long.parseLong(tenantIdStr);

            // Extraer campos del body
            String ciPaciente = (String) body.get(KEY_CI_PACIENTE);
            String contenido = (String) body.get("contenido");
            String tipoDocumento = (String) body.get(KEY_TIPO_DOCUMENTO);
            String descripcion = (String) body.get(KEY_DESCRIPCION);
            String titulo = (String) body.get(KEY_TITULO);
            String autor = (String) body.get(KEY_AUTOR);

            // Validaciones
            LOG.infof("crearDocumentoCompleto: ciPaciente=%s, contenido=%s", ciPaciente, contenido != null ? "presente" : "null");
            if (ciPaciente == null || ciPaciente.isBlank()) {
                LOG.info("crearDocumentoCompleto: ciPaciente es null o vacío");
                return DocumentoResponseBuilder.badRequest("ciPaciente es requerido");
            }
            if (contenido == null || contenido.isBlank()) {
                LOG.info("crearDocumentoCompleto: contenido es null o vacío");
                return DocumentoResponseBuilder.badRequest(DocumentoConstants.ERROR_CONTENIDO_ES_REQUERIDO);
            }

            // Crear documento
            Map<String, Object> resultado = documentoService.crearDocumentoCompleto(
                    tenantId,
                    profesionalId,
                    ciPaciente,
                    contenido,
                    tipoDocumento,
                    descripcion,
                    titulo,
                    autor
            );

            URI location = UriBuilder.fromPath("/api/documentos/{id}")
                    .build(resultado.get(KEY_MONGO_ID));

            return Response.created(location)
                    .entity(resultado)
                    .build();

        } catch (IllegalArgumentException ex) {
            LOG.error("Error de validación al crear documento", ex);
            return DocumentoResponseBuilder.badRequest(ex.getMessage());
        } catch (Exception ex) {
            LOG.error("Error al crear documento completo", ex);
            return DocumentoResponseBuilder.internalServerError("Error al crear documento: " + ex.getMessage());
        }
    }

    /**
     * POST /api/documentos/completo-con-archivo
     * 
     * Crea un documento clínico completo con contenido de texto y archivo adjunto.
     * 
     * FormData esperado:
     * - contenido: texto del documento (requerido)
     * - ciPaciente: CI del paciente (requerido)
     * - tipoDocumento: tipo de documento (opcional)
     * - descripcion: descripción (opcional)
     * - titulo: título (opcional)
     * - autor: autor (opcional)
     * - archivo: archivo adjunto (opcional)
     * 
     * @param input Multipart form data
     * @return Respuesta con el ID del documento creado
     */
    @POST
    @Path("/completo-con-archivo")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("PROFESIONAL")
    public Response crearDocumentoCompletoConArchivo(MultipartFormDataInput input) {
        try {
            Response validacion = validarAutenticacionYTenant();
            if (validacion != null) {
                return validacion;
            }

            String profesionalId = getUsuarioId();
            Long tenantId = obtenerTenantId();
            if (tenantId == null) {
                return DocumentoResponseBuilder.badRequest(ERROR_TENANT_NO_IDENTIFICADO);
            }

            DatosDocumentoConArchivo datos = extraerDatosDocumentoConArchivo(input);
            Response validacionDatos = validarDatosDocumentoConArchivo(datos);
            if (validacionDatos != null) {
                return validacionDatos;
            }

            Map<String, Object> resultado = documentoService.crearDocumentoCompletoConArchivo(
                    tenantId, profesionalId, datos.ciPaciente, datos.contenido,
                    datos.tipoDocumento, datos.descripcion, datos.titulo, datos.autor,
                    datos.archivoBytes, datos.nombreArchivo, datos.tipoArchivo
            );

            URI location = UriBuilder.fromPath("/api/documentos/{id}")
                    .build(resultado.get(KEY_MONGO_ID));

            return Response.created(location)
                    .entity(resultado)
                    .build();

        } catch (IllegalArgumentException ex) {
            LOG.error("Error de validación al crear documento con archivo", ex);
            return DocumentoResponseBuilder.badRequest(ex.getMessage());
        } catch (Exception ex) {
            LOG.error("Error al crear documento completo con archivo", ex);
            return DocumentoResponseBuilder.internalServerError("Error al crear documento: " + ex.getMessage());
        }
    }
    
    /**
     * Clase interna para agrupar datos del documento con archivo.
     */
    private static class DatosDocumentoConArchivo {
        String contenido;
        String ciPaciente;
        String tipoDocumento;
        String descripcion;
        String titulo;
        String autor;
        byte[] archivoBytes;
        String nombreArchivo;
        String tipoArchivo;
    }
    
    /**
     * Valida autenticación y tenant.
     */
    private Response validarAutenticacionYTenant() {
        String profesionalId = getUsuarioId();
        if (profesionalId == null || profesionalId.isBlank()) {
            return DocumentoResponseBuilder.unauthorized(DocumentoConstants.ERROR_AUTENTICACION_REQUERIDA);
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
    private DatosDocumentoConArchivo extraerDatosDocumentoConArchivo(MultipartFormDataInput input) throws java.io.IOException {
        DatosDocumentoConArchivo datos = new DatosDocumentoConArchivo();
        Map<String, List<InputPart>> formDataMap = input.getFormDataMap();
        
        datos.contenido = extractField(formDataMap, "contenido");
        datos.ciPaciente = extractField(formDataMap, KEY_CI_PACIENTE);
        datos.tipoDocumento = extractField(formDataMap, KEY_TIPO_DOCUMENTO);
        datos.descripcion = extractField(formDataMap, KEY_DESCRIPCION);
        datos.titulo = extractField(formDataMap, KEY_TITULO);
        datos.autor = extractField(formDataMap, KEY_AUTOR);
        
        DatosArchivo archivo = extraerArchivoAdjunto(formDataMap);
        datos.archivoBytes = archivo.bytes;
        datos.nombreArchivo = archivo.nombre;
        datos.tipoArchivo = archivo.tipo;
        
        return datos;
    }
    
    /**
     * Clase interna para datos del archivo adjunto.
     */
    private static class DatosArchivo {
        byte[] bytes;
        String nombre;
        String tipo;
    }
    
    /**
     * Extrae el archivo adjunto del formulario.
     */
    private DatosArchivo extraerArchivoAdjunto(Map<String, List<InputPart>> formDataMap) throws java.io.IOException {
        DatosArchivo archivo = new DatosArchivo();
        List<InputPart> archivoParts = formDataMap.get("archivo");
        
        if (archivoParts != null && !archivoParts.isEmpty()) {
            InputPart archivoPart = archivoParts.get(0);
            InputStream archivoStream = archivoPart.getBody(InputStream.class, null);
            archivo.bytes = archivoStream.readAllBytes();
            
            String contentDisposition = archivoPart.getHeaders().getFirst("Content-Disposition");
            if (contentDisposition != null && contentDisposition.contains("filename=")) {
                int start = contentDisposition.indexOf("filename=") + 9;
                int end = contentDisposition.indexOf("\"", start);
                if (end == -1) {
                    end = contentDisposition.length();
                }
                archivo.nombre = contentDisposition.substring(start, end).replace("\"", "");
            }
            
            archivo.tipo = archivoPart.getHeaders().getFirst("Content-Type");
        }
        
        return archivo;
    }
    
    /**
     * Valida los datos del documento con archivo.
     */
    private Response validarDatosDocumentoConArchivo(DatosDocumentoConArchivo datos) {
        if (datos.contenido == null || datos.contenido.isBlank()) {
            return DocumentoResponseBuilder.badRequest(DocumentoConstants.ERROR_CONTENIDO_ES_REQUERIDO);
        }
        if (datos.ciPaciente == null || datos.ciPaciente.isBlank()) {
            return DocumentoResponseBuilder.badRequest("ciPaciente es requerido");
        }
        return null;
    }

    /**
     * GET /api/documentos/{id}/contenido
     * 
     * Obtiene el contenido de texto de un documento.
     * 
     * @param id ID de MongoDB (ObjectId en hex)
     * @return Contenido de texto del documento
     */
    @GET
    @Path("/{id}/contenido")
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed("PROFESIONAL")
    public Response obtenerContenido(@PathParam("id") String id) {
        try {
            String tenantIdStr = TenantContext.getCurrentTenant();
            if (tenantIdStr == null || tenantIdStr.isBlank()) {
                return DocumentoResponseBuilder.badRequest(ERROR_TENANT_NO_IDENTIFICADO);
            }
            Long tenantId = Long.parseLong(tenantIdStr);

            String contenido = documentoService.obtenerContenido(id, tenantId);
            if (contenido == null) {
                return DocumentoResponseBuilder.notFound(DocumentoConstants.ERROR_DOCUMENT_NOT_FOUND);
            }

            return Response.ok(contenido, MediaType.TEXT_PLAIN).build();

        } catch (Exception ex) {
            LOG.error("Error al obtener contenido del documento", ex);
            return DocumentoResponseBuilder.internalServerError("Error al obtener contenido: " + ex.getMessage());
        }
    }

    /**
     * GET /api/documentos/{id}/pdf
     * 
     * Obtiene el PDF de un documento.
     * 
     * @param id ID de MongoDB (ObjectId en hex)
     * @return PDF del documento
     */
    @GET
    @Path("/{id}/pdf")
    @Produces("application/pdf")
    // @RolesAllowed("PROFESIONAL") // Temporalmente sin autenticación para que HCEN central pueda descargarlo
    public Response obtenerPdf(@PathParam("id") String id, @QueryParam("tenantId") Long tenantIdParam) {
        try {
            // Prioridad: 1) Query parameter, 2) Contexto
            Long tenantId = null;
            if (tenantIdParam != null) {
                tenantId = tenantIdParam;
                // Establecer el tenantId en el contexto para que el servicio lo use
                TenantContext.setCurrentTenant(String.valueOf(tenantId));
                LOG.info(String.format("TenantId establecido desde query parameter: %d", tenantId));
            } else {
                String tenantIdStr = TenantContext.getCurrentTenant();
                if (tenantIdStr != null && !tenantIdStr.isBlank()) {
                    tenantId = Long.parseLong(tenantIdStr);
                } else {
                    LOG.warn("No se encontró tenantId en query parameter ni en contexto. Intentando con tenantId=1");
                    tenantId = 1L;
                    TenantContext.setCurrentTenant("1");
                }
            }

            byte[] pdfBytes = documentoService.obtenerPdf(id, tenantId);
            if (pdfBytes == null || pdfBytes.length == 0) {
                return DocumentoResponseBuilder.notFound("PDF no encontrado");
            }
        
            return Response.ok(pdfBytes, "application/pdf")
                    .header("Content-Disposition", "inline; filename=\"documento_" + id + ".pdf\"")
                    .build();

        } catch (Exception ex) {
            LOG.error("Error al obtener PDF del documento", ex);
            return DocumentoResponseBuilder.internalServerError("Error al obtener PDF: " + ex.getMessage());
        }
    }

    /**
     * GET /api/documentos/{id}
     * 
     * Obtiene información de un documento (sin el contenido completo).
     * 
     * @param id ID de MongoDB (ObjectId en hex)
     * @return Información del documento
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("PROFESIONAL")
    public Response obtenerDocumento(@PathParam("id") String id) {
        try {
            String tenantIdStr = TenantContext.getCurrentTenant();
            if (tenantIdStr == null || tenantIdStr.isBlank()) {
                return DocumentoResponseBuilder.badRequest(ERROR_TENANT_NO_IDENTIFICADO);
            }
            Long tenantId = Long.parseLong(tenantIdStr);

            Document doc = documentoService.obtenerDocumentoPorId(id, tenantId);
            if (doc == null) {
                return DocumentoResponseBuilder.notFound(DocumentoConstants.ERROR_DOCUMENT_NOT_FOUND);
            }

            Map<String, Object> docInfo = new HashMap<>();
            docInfo.put(KEY_MONGO_ID, id);
            docInfo.put(KEY_DOCUMENTO_ID, doc.getString(KEY_DOCUMENTO_ID));
            docInfo.put(KEY_CI_PACIENTE, doc.getString(KEY_CI_PACIENTE));
            docInfo.put(KEY_TIPO_DOCUMENTO, doc.getString(KEY_TIPO_DOCUMENTO));
            docInfo.put(KEY_DESCRIPCION, doc.getString(KEY_DESCRIPCION));
            docInfo.put(KEY_TITULO, doc.getString(KEY_TITULO));
            docInfo.put(KEY_AUTOR, doc.getString(KEY_AUTOR));
            // Convertir fecha: MongoDB guarda en UTC, pero debemos mostrarla en hora de Uruguay
            // La fecha guardada representa el instante correcto, pero al serializarse a JSON se interpreta como UTC
            // Para corregir esto, ajustamos el instante para que cuando se interprete como UTC, 
            // represente la misma hora local que queremos mostrar en Uruguay
            java.util.Date fechaUtc = doc.getDate(KEY_FECHA_CREACION);
            if (fechaUtc != null) {
                java.time.ZoneId uruguayZone = java.time.ZoneId.of("America/Montevideo");
                java.time.Instant instant = fechaUtc.toInstant();
                // Convertir a hora de Uruguay para obtener la hora local correcta y el offset
                java.time.ZonedDateTime zonedDateTime = instant.atZone(uruguayZone);
                // Ajustar: restar el offset de Uruguay para que cuando se interprete como UTC, muestre la hora correcta
                // Esto es necesario porque JSON serializa Date como UTC, pero queremos que muestre la hora de Uruguay
                int offsetSegundos = zonedDateTime.getOffset().getTotalSeconds();
                java.time.Instant instanteAjustado = instant.minusSeconds(offsetSegundos);
                docInfo.put(KEY_FECHA_CREACION, java.util.Date.from(instanteAjustado));
            } else {
                docInfo.put(KEY_FECHA_CREACION, null);
            }

            // Verificar si tiene PDF
            Binary pdfBinary = doc.get("pdfBytes", Binary.class);
            boolean tienePdf = pdfBinary != null && pdfBinary.getData() != null && pdfBinary.getData().length > 0;
            docInfo.put("tienePdf", tienePdf);

            // Verificar si tiene archivo adjunto
            Binary archivoAdjuntoBinary = doc.get("archivoAdjunto", Binary.class);
            boolean tieneArchivoAdjunto = archivoAdjuntoBinary != null && 
                    archivoAdjuntoBinary.getData() != null && archivoAdjuntoBinary.getData().length > 0;
            docInfo.put("tieneArchivoAdjunto", tieneArchivoAdjunto);

            if (tieneArchivoAdjunto) {
                docInfo.put("nombreArchivoAdjunto", doc.getString("nombreArchivoAdjunto"));
                docInfo.put("tipoArchivoAdjunto", doc.getString("tipoArchivoAdjunto"));
            }

            return DocumentoResponseBuilder.ok(docInfo);

        } catch (Exception ex) {
            LOG.error("Error al obtener documento", ex);
            return DocumentoResponseBuilder.internalServerError("Error al obtener documento: " + ex.getMessage());
        }
    }

    /**
     * POST /api/documentos/solicitar-acceso
     * 
     * Proxy para solicitar acceso a un documento.
     * El componente periférico hace proxy al backend HCEN Central.
     * 
     * @param body Mapa con: pacienteCI, documentoId, tipoDocumento (opcional), motivo (opcional)
     * @return Respuesta del backend HCEN Central
     */
    @POST
    @Path("/solicitar-acceso")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("PROFESIONAL")
    public Response solicitarAcceso(Map<String, Object> body) {
        LOG.info("Proxy: Solicitud de acceso recibida en componente periférico");
        LOG.infof("Proxy: Body recibido: %s", body);
        
        try {
            Response validacion = validarBodySolicitud(body);
            if (validacion != null) {
                return validacion;
            }
            
            Response validacionProfesional = validarProfesionalAutenticado();
            if (validacionProfesional != null) {
                return validacionProfesional;
            }
            
            String tenantIdStr = obtenerYLoggearTenantId();
            String solicitudUrl = construirUrlSolicitud();
            Map<String, Object> payload = construirPayload(body, tenantIdStr);
            
            return realizarPeticionProxy(solicitudUrl, payload);
            
        } catch (Exception e) {
            LOG.error("Error en proxy de solicitud de acceso", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of(KEY_ERROR, "Error al procesar la solicitud: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Valida el body de la solicitud.
     */
    private Response validarBodySolicitud(Map<String, Object> body) {
        if (body == null) {
            LOG.warn("Proxy: Body es null");
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of(KEY_ERROR, "pacienteCI es requerido"))
                .build();
        }
        
        if (!body.containsKey(KEY_PACIENTE_CI)) {
            LOG.warnf("Proxy: pacienteCI no está en el body. Keys disponibles: %s", body.keySet());
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of(KEY_ERROR, "pacienteCI es requerido"))
                .build();
        }
        
        LOG.infof("Proxy: pacienteCI recibido: %s, documentoId presente: %s", 
                body.get(KEY_PACIENTE_CI), body.containsKey(KEY_DOCUMENTO_ID));
        return null;
    }
    
    /**
     * Valida que el profesional esté autenticado.
     */
    private Response validarProfesionalAutenticado() {
        String profesionalId = getUsuarioId();
        if (profesionalId == null || profesionalId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Map.of(KEY_ERROR, "No se pudo identificar al profesional autenticado"))
                .build();
        }
        return null;
    }
    
    /**
     * Obtiene y registra el tenant ID.
     */
    private String obtenerYLoggearTenantId() {
        String tenantIdStr = TenantContext.getCurrentTenant();
        if (tenantIdStr == null || tenantIdStr.isBlank()) {
            LOG.warn("Proxy: No se pudo obtener tenantId del contexto, la solicitud puede fallar al crear la política");
        } else {
            LOG.infof("Proxy: TenantId obtenido: %s", tenantIdStr);
        }
        return tenantIdStr;
    }
    
    /**
     * Construye la URL de la solicitud.
     */
    private String construirUrlSolicitud() {
        String solicitudUrl = uy.edu.tse.hcen.utils.HcenCentralUrlUtil.buildApiUrl("/metadatos-documento/solicitar-acceso");
        LOG.infof("Proxy: Redirigiendo solicitud a HCEN Central: %s", solicitudUrl);
        return solicitudUrl;
    }
    
    /**
     * Construye el payload para HCEN Central.
     */
    private Map<String, Object> construirPayload(Map<String, Object> body, String tenantIdStr) {
        Map<String, Object> payload = new HashMap<>();
        
        Object pacienteCI = body.get(KEY_PACIENTE_CI);
        if (pacienteCI != null) {
            payload.put(KEY_PACIENTE_CI, pacienteCI.toString());
        } else {
            LOG.warn("Proxy: pacienteCI es null, no se incluirá en el payload");
        }
        
        if (tenantIdStr != null && !tenantIdStr.isBlank()) {
            payload.put("tenantId", tenantIdStr);
            LOG.infof("Proxy: Incluyendo tenantId en payload: %s", tenantIdStr);
        }
        
        agregarDocumentoIdAlPayload(body, payload);
        agregarTipoDocumentoAlPayload(body, payload);
        agregarMotivoAlPayload(body, payload);
        
        LOG.infof("Proxy: Payload final enviado a HCEN Central: %s", payload);
        return payload;
    }
    
    /**
     * Agrega documentoId al payload si está presente.
     */
    private void agregarDocumentoIdAlPayload(Map<String, Object> body, Map<String, Object> payload) {
        if (body.containsKey(KEY_DOCUMENTO_ID) && body.get(KEY_DOCUMENTO_ID) != null) {
            Object documentoId = body.get(KEY_DOCUMENTO_ID);
            payload.put(KEY_DOCUMENTO_ID, documentoId != null ? documentoId.toString() : null);
            LOG.info("Proxy: Incluyendo documentoId en payload");
        } else {
            LOG.info("Proxy: No se incluye documentoId - solicitud para todos los documentos del paciente");
        }
    }
    
    /**
     * Agrega tipoDocumento al payload si está presente.
     */
    private void agregarTipoDocumentoAlPayload(Map<String, Object> body, Map<String, Object> payload) {
        if (body.containsKey(KEY_TIPO_DOCUMENTO) && body.get(KEY_TIPO_DOCUMENTO) != null) {
            Object tipoDocumento = body.get(KEY_TIPO_DOCUMENTO);
            if (tipoDocumento != null) {
                payload.put(KEY_TIPO_DOCUMENTO, tipoDocumento.toString());
            }
        }
    }
    
    /**
     * Agrega motivo al payload.
     */
    private void agregarMotivoAlPayload(Map<String, Object> body, Map<String, Object> payload) {
        Object motivo = body.getOrDefault("motivo", "Acceso necesario para atención médica");
        payload.put("motivo", motivo != null ? motivo.toString() : "Acceso necesario para atención médica");
    }
    
    /**
     * Realiza la petición proxy al backend HCEN Central.
     */
    private Response realizarPeticionProxy(String solicitudUrl, Map<String, Object> payload) {
        jakarta.ws.rs.client.Client client = jakarta.ws.rs.client.ClientBuilder.newClient();
        try {
            String authHeader = extraerTokenJWT();
            jakarta.ws.rs.client.Invocation.Builder requestBuilder = client.target(solicitudUrl)
                .request(MediaType.APPLICATION_JSON);
            
            agregarHeaderAutorizacion(requestBuilder, authHeader);
            
            jakarta.ws.rs.core.Response response = requestBuilder
                .post(jakarta.ws.rs.client.Entity.entity(payload, MediaType.APPLICATION_JSON));
            
            return procesarRespuestaProxy(response, client);
                    
        } finally {
            cerrarCliente(client);
        }
    }
    
    /**
     * Extrae el token JWT del header Authorization.
     */
    private String extraerTokenJWT() {
        if (httpHeaders != null) {
            List<String> authHeaders = httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION);
            if (authHeaders != null && !authHeaders.isEmpty()) {
                LOG.info("Proxy: Token JWT encontrado en header Authorization");
                return authHeaders.get(0);
            }
        }
        return null;
    }
    
    /**
     * Agrega el header de autorización si existe.
     */
    private void agregarHeaderAutorizacion(jakarta.ws.rs.client.Invocation.Builder requestBuilder, String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, authHeader);
            LOG.info("Proxy: Token JWT reenviado al backend HCEN Central");
        } else {
            LOG.warn("Proxy: No se encontró token JWT para reenviar al backend HCEN Central");
        }
    }
    
    /**
     * Procesa la respuesta del proxy.
     */
    private Response procesarRespuestaProxy(jakarta.ws.rs.core.Response response, jakarta.ws.rs.client.Client client) {
        int status = response.getStatus();
        LOG.infof("Proxy: Respuesta de HCEN Central - Status: %d", status);
        
        if (status == 404) {
            return manejarError404(response, client);
        }
        
        Object responseEntity = leerRespuesta(response);
        return Response.status(status)
            .entity(responseEntity != null ? responseEntity : Map.of("status", status))
            .build();
    }
    
    /**
     * Maneja el error 404 del servicio.
     */
    private Response manejarError404(jakarta.ws.rs.core.Response response, jakarta.ws.rs.client.Client client) {
        String errorDetail = leerErrorDetail(response);
        LOG.warnf("Proxy: Endpoint no encontrado en HCEN Central (404). El servicio puede no estar disponible. Detalle: %s", errorDetail);
        cerrarCliente(client);
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
            .entity(Map.of(
                KEY_ERROR, "El servicio de solicitud de acceso no está disponible",
                "detalle", "El endpoint solicitado no fue encontrado en HCEN Central. Es posible que el servicio no esté configurado o no esté disponible en este momento."
            ))
            .build();
    }
    
    /**
     * Lee el detalle del error de la respuesta.
     */
    private String leerErrorDetail(jakarta.ws.rs.core.Response response) {
        try {
            if (response.hasEntity()) {
                return response.readEntity(String.class);
            }
        } catch (Exception e) {
            // Ignorar errores al leer el cuerpo del 404
        }
        return "";
    }
    
    /**
     * Lee la respuesta de manera robusta.
     */
    private Object leerRespuesta(jakarta.ws.rs.core.Response response) {
        if (!response.hasEntity()) {
            return null;
        }
        
        try {
            String textResponse = response.readEntity(String.class);
            LOG.infof("Proxy: Respuesta recibida (texto): %s", textResponse);
            
            if (textResponse.trim().startsWith("{") || textResponse.trim().startsWith("[")) {
                return Map.of("mensaje", textResponse);
            } else {
                return Map.of(KEY_ERROR, textResponse.contains("404") ? 
                    "El servicio no está disponible" : textResponse);
            }
        } catch (Exception e) {
            LOG.error("Error al leer respuesta de HCEN Central", e);
            return Map.of(KEY_ERROR, "Error al procesar respuesta del servidor");
        }
    }
    
    /**
     * Cierra el cliente HTTP de forma segura.
     */
    private void cerrarCliente(jakarta.ws.rs.client.Client client) {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                LOG.warn("Error al cerrar cliente HTTP", e);
            }
        }
    }

    /**
     * GET /api/documentos/{documentoIdPaciente}/resumen
     * 
     * Genera un resumen de la historia clínica completa de un paciente.
     * 
     * @param documentoIdPaciente CI del paciente
     * @return Resumen de la historia clínica generado con IA
     */
    @GET
    @Path("/{documentoIdPaciente}/resumen")
    @Produces(MediaType.APPLICATION_JSON)
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

            // Verificar permisos usando el servicio de políticas
            String tenantIdStr = TenantContext.getCurrentTenant();
            if (tenantIdStr == null || tenantIdStr.isBlank()) {
                return DocumentoResponseBuilder.badRequest(ERROR_TENANT_NO_IDENTIFICADO);
            }
            
            boolean tienePermiso = verificarPermisosConFallback(profesionalId, documentoIdPaciente, tenantIdStr);

            if (!tienePermiso) {
                LOG.warnf("Acceso denegado - Profesional: %s, Paciente: %s, Endpoint: /api/documentos/%s/resumen", 
                        profesionalId, documentoIdPaciente, documentoIdPaciente);
                return DocumentoResponseBuilder.forbidden(
                        "No tiene permisos para acceder a la historia clínica completa del paciente");
            }

            // Obtener contenidos de todos los documentos del paciente
            List<String> contenidos = documentoService.obtenerContenidosPorPaciente(documentoIdPaciente);
            if (contenidos == null || contenidos.isEmpty()) {
                return DocumentoResponseBuilder.notFound("No se encontraron documentos para el paciente");
            }

            // Construir historia clínica completa
            String historiaClinicaCompleta = construirHistoriaClinicaCompleta(contenidos);
            
            // Intentar generar resumen con OpenAI
            String resumen = generarResumenConFallback(historiaClinicaCompleta, contenidos);

            // Registrar acceso sensible
            LOG.infof("Acceso sensible registrado - Profesional: %s, Paciente: %s, Endpoint: /api/documentos/%s/resumen", 
                    profesionalId, documentoIdPaciente, documentoIdPaciente);
            
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

    /**
     * Obtiene el ID del usuario autenticado.
     */
    private String getUsuarioId() {
        if (securityContext != null && securityContext.getUserPrincipal() != null) {
            return securityContext.getUserPrincipal().getName();
        }
        return null;
    }

    /**
     * Verifica permisos con fallback si falla el servicio.
     */
    private boolean verificarPermisosConFallback(String profesionalId, String documentoIdPaciente, String tenantIdStr) {
        try {
            return politicasAccesoClient.verificarPermiso(profesionalId, documentoIdPaciente, null, tenantIdStr);
        } catch (Exception ex) {
            LOG.warnf("No se pudo verificar permisos con el servicio de políticas (continuando): %s", ex.getMessage());
            return true; // Por defecto permitir si no se puede verificar
        }
    }
    
    /**
     * Genera resumen con fallback si OpenAI falla.
     */
    private String generarResumenConFallback(String historiaClinicaCompleta, List<String> contenidos) {
        try {
            return openAIService.generarResumenHistoriaClinica(historiaClinicaCompleta);
        } catch (RuntimeException ex) {
            LOG.warnf("No se pudo generar resumen con OpenAI, usando fallback: %s", ex.getMessage());
            return generarResumenFallback(contenidos);
        }
    }
    
    /**
     * Construye una historia clínica completa concatenando todos los contenidos.
     */
    private String construirHistoriaClinicaCompleta(List<String> contenidos) {
        StringBuilder historiaClinicaCompleta = new StringBuilder();
        for (int i = 0; i < contenidos.size(); i++) {
            historiaClinicaCompleta.append("=== Documento ").append(i + 1).append(" ===\n");
            historiaClinicaCompleta.append(contenidos.get(i));
            historiaClinicaCompleta.append("\n\n");
        }
        return historiaClinicaCompleta.toString();
    }

    /**
     * Genera un resumen básico cuando el servicio de IA no está disponible.
     */
    private String generarResumenFallback(List<String> contenidos) {
        StringBuilder builder = new StringBuilder();
        builder.append("Resumen automático (sin servicio de IA)\n");
        builder.append("Documentos procesados: ").append(contenidos.size()).append("\n\n");
        
        int documentosProcesados = 0;
        for (int i = 0; i < contenidos.size() && documentosProcesados < 3; i++) {
            String texto = contenidos.get(i);
            if (texto != null && !texto.isBlank()) {
                builder.append("Documento ").append(i + 1).append(":\n");
                String snippet = texto.trim();
                if (snippet.length() > 400) {
                    snippet = snippet.substring(0, 400) + "...";
                }
                builder.append(snippet).append("\n\n");
                documentosProcesados++;
            }
        }
        
        if (contenidos.size() > 3) {
            builder.append("... (").append(contenidos.size() - 3).append(" documentos adicionales)\n");
        }
        
        if (builder.isEmpty()) {
            builder.append("No hay contenido clínico para resumir.");
        }
        
        builder.append("\nEste resumen fue generado automáticamente debido a que el servicio de IA no está disponible.");
        return builder.toString();
    }

    /**
     * Helper para extraer un campo de texto del multipart form data.
     */
    private String extractField(Map<String, List<InputPart>> formDataMap, String fieldName) {
        List<InputPart> parts = formDataMap.get(fieldName);
        if (parts != null && !parts.isEmpty()) {
            try {
                return parts.get(0).getBodyAsString();
            } catch (Exception ex) {
                LOG.warn("Error al extraer campo " + fieldName, ex);
                return null;
            }
        }
        return null;
    }


    
}
