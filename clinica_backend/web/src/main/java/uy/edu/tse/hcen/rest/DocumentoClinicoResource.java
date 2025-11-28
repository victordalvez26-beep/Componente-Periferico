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
import jakarta.ws.rs.container.ContainerRequestContext;
import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.bson.Document;
import org.bson.types.Binary;
import uy.edu.tse.hcen.multitenancy.TenantContext;
import uy.edu.tse.hcen.service.DocumentoService;

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

    @Inject
    private DocumentoService documentoService;
    
    @Context
    private jakarta.ws.rs.core.SecurityContext securityContext;
    
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
                return DocumentoResponseBuilder.badRequest("Tenant no identificado");
            }
            Long tenantId = Long.parseLong(tenantIdStr);

            // Extraer campos del body
            String ciPaciente = (String) body.get("ciPaciente");
            String contenido = (String) body.get("contenido");
            String tipoDocumento = (String) body.get("tipoDocumento");
            String descripcion = (String) body.get("descripcion");
            String titulo = (String) body.get("titulo");
            String autor = (String) body.get("autor");

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
                    .build(resultado.get("mongoId"));

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
            // Obtener información del profesional autenticado
            String profesionalId = null;
            if (securityContext != null && securityContext.getUserPrincipal() != null) {
                profesionalId = securityContext.getUserPrincipal().getName();
            }

            if (profesionalId == null || profesionalId.isBlank()) {
                return DocumentoResponseBuilder.unauthorized(DocumentoConstants.ERROR_AUTENTICACION_REQUERIDA);
            }

            // Obtener tenant actual
            String tenantIdStr = TenantContext.getCurrentTenant();
            if (tenantIdStr == null || tenantIdStr.isBlank()) {
                return DocumentoResponseBuilder.badRequest("Tenant no identificado");
            }
            Long tenantId = Long.parseLong(tenantIdStr);

            // Extraer datos del formulario
            Map<String, List<InputPart>> formDataMap = input.getFormDataMap();
            
            String contenido = extractField(formDataMap, "contenido");
            String ciPaciente = extractField(formDataMap, "ciPaciente");
            String tipoDocumento = extractField(formDataMap, "tipoDocumento");
            String descripcion = extractField(formDataMap, "descripcion");
            String titulo = extractField(formDataMap, "titulo");
            String autor = extractField(formDataMap, "autor");

            // Validaciones
            if (contenido == null || contenido.isBlank()) {
                return DocumentoResponseBuilder.badRequest(DocumentoConstants.ERROR_CONTENIDO_ES_REQUERIDO);
            }
            if (ciPaciente == null || ciPaciente.isBlank()) {
                return DocumentoResponseBuilder.badRequest("ciPaciente es requerido");
            }
        
            // Extraer archivo adjunto (opcional)
            byte[] archivoBytes = null;
            String nombreArchivo = null;
            String tipoArchivo = null;
            
            List<InputPart> archivoParts = formDataMap.get("archivo");
            if (archivoParts != null && !archivoParts.isEmpty()) {
                InputPart archivoPart = archivoParts.get(0);
                InputStream archivoStream = archivoPart.getBody(InputStream.class, null);
                archivoBytes = archivoStream.readAllBytes();
                
                // Obtener nombre y tipo del archivo desde headers
                String contentDisposition = archivoPart.getHeaders().getFirst("Content-Disposition");
                if (contentDisposition != null && contentDisposition.contains("filename=")) {
                    int start = contentDisposition.indexOf("filename=") + 9;
                    int end = contentDisposition.indexOf("\"", start);
                    if (end == -1) end = contentDisposition.length();
                    nombreArchivo = contentDisposition.substring(start, end).replace("\"", "");
                }

                tipoArchivo = archivoPart.getHeaders().getFirst("Content-Type");
            }

            // Crear documento
            Map<String, Object> resultado = documentoService.crearDocumentoCompletoConArchivo(
                    tenantId,
                    profesionalId,
                    ciPaciente,
                    contenido,
                    tipoDocumento,
                    descripcion,
                    titulo,
                    autor,
                    archivoBytes,
                    nombreArchivo,
                    tipoArchivo
            );

            URI location = UriBuilder.fromPath("/api/documentos/{id}")
                    .build(resultado.get("mongoId"));

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
                return DocumentoResponseBuilder.badRequest("Tenant no identificado");
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
                return DocumentoResponseBuilder.badRequest("Tenant no identificado");
            }
            Long tenantId = Long.parseLong(tenantIdStr);

            Document doc = documentoService.obtenerDocumentoPorId(id, tenantId);
            if (doc == null) {
                return DocumentoResponseBuilder.notFound(DocumentoConstants.ERROR_DOCUMENT_NOT_FOUND);
            }

            Map<String, Object> docInfo = new HashMap<>();
            docInfo.put("mongoId", id);
            docInfo.put("documentoId", doc.getString("documentoId"));
            docInfo.put("ciPaciente", doc.getString("ciPaciente"));
            docInfo.put("tipoDocumento", doc.getString("tipoDocumento"));
            docInfo.put("descripcion", doc.getString("descripcion"));
            docInfo.put("titulo", doc.getString("titulo"));
            docInfo.put("autor", doc.getString("autor"));
            docInfo.put("fechaCreacion", doc.getDate("fechaCreacion"));

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
        LOG.info(String.format("Proxy: Body recibido: %s", body));
        
        try {
            // Validar datos requeridos
            // Validar datos de la solicitud - pacienteCI es requerido, documentoId es opcional (null = todos los documentos)
            if (body == null) {
                LOG.warn("Proxy: Body es null");
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "pacienteCI es requerido"))
                    .build();
            }
            
            if (!body.containsKey("pacienteCI")) {
                LOG.warn(String.format("Proxy: pacienteCI no está en el body. Keys disponibles: %s", body.keySet()));
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "pacienteCI es requerido"))
                    .build();
            }
            
            LOG.info(String.format("Proxy: pacienteCI recibido: %s, documentoId presente: %s", 
                    body.get("pacienteCI"), body.containsKey("documentoId")));
            
            // Obtener información del profesional autenticado
            String profesionalId = null;
            if (securityContext != null && securityContext.getUserPrincipal() != null) {
                profesionalId = securityContext.getUserPrincipal().getName();
            }
            
            if (profesionalId == null || profesionalId.isBlank()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "No se pudo identificar al profesional autenticado"))
                .build();
    }

            // Obtener tenantId de la clínica del profesional autenticado
            String tenantIdStr = TenantContext.getCurrentTenant();
            if (tenantIdStr == null || tenantIdStr.isBlank()) {
                LOG.warn("Proxy: No se pudo obtener tenantId del contexto, la solicitud puede fallar al crear la política");
            } else {
                LOG.info(String.format("Proxy: TenantId obtenido: %s", tenantIdStr));
            }
            
            // URL del backend HCEN Central
            String hcenCentralUrl = System.getenv("HCEN_CENTRAL_URL");
            if (hcenCentralUrl == null || hcenCentralUrl.isEmpty()) {
                hcenCentralUrl = System.getProperty("HCEN_CENTRAL_URL", "http://hcen-backend:8080/api");
            }
            
            String solicitudUrl = hcenCentralUrl + "/metadatos-documento/solicitar-acceso";
            LOG.info(String.format("Proxy: Redirigiendo solicitud a HCEN Central: %s", solicitudUrl));
            
            // Construir payload para HCEN Central (debe incluir el profesionalId y tenantId del usuario autenticado)
            Map<String, Object> payload = new HashMap<>();
            payload.put("pacienteCI", body.get("pacienteCI"));
            
            // Agregar tenantId (clinicaAutorizada) para que se guarde en la solicitud y se use al crear la política
            if (tenantIdStr != null && !tenantIdStr.isBlank()) {
                payload.put("tenantId", tenantIdStr);
                LOG.info(String.format("Proxy: Incluyendo tenantId en payload: %s", tenantIdStr));
            }
            
            // documentoId es opcional - si no se proporciona, es para todos los documentos del paciente
            // NO agregamos documentoId si no está presente o es null
            if (body.containsKey("documentoId") && body.get("documentoId") != null) {
                payload.put("documentoId", body.get("documentoId"));
                LOG.info("Proxy: Incluyendo documentoId en payload");
            } else {
                LOG.info("Proxy: No se incluye documentoId - solicitud para todos los documentos del paciente");
            }
            
            // tipoDocumento es opcional
            if (body.containsKey("tipoDocumento") && body.get("tipoDocumento") != null) {
                payload.put("tipoDocumento", body.get("tipoDocumento"));
    }

            payload.put("motivo", body.getOrDefault("motivo", "Acceso necesario para atención médica"));
            
            LOG.info(String.format("Proxy: Payload final enviado a HCEN Central: %s", payload));
            
            // El backend HCEN Central obtendrá el profesionalId del JWT, pero también podemos enviarlo
            // Nota: El backend HCEN Central extraerá el profesionalId del JWT del usuario
            
            // Hacer proxy al backend HCEN Central
            jakarta.ws.rs.client.Client client = jakarta.ws.rs.client.ClientBuilder.newClient();
            try {
                // Extraer el token JWT del header Authorization del request actual
                String authHeader = null;
                if (httpHeaders != null) {
                    List<String> authHeaders = httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION);
                    if (authHeaders != null && !authHeaders.isEmpty()) {
                        authHeader = authHeaders.get(0);
                        LOG.info("Proxy: Token JWT encontrado en header Authorization");
                    }
                }
                
                jakarta.ws.rs.client.Invocation.Builder requestBuilder = client.target(solicitudUrl)
                    .request(MediaType.APPLICATION_JSON);
                
                // Reenviar el token JWT al backend HCEN Central
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    requestBuilder.header(HttpHeaders.AUTHORIZATION, authHeader);
                    LOG.info("Proxy: Token JWT reenviado al backend HCEN Central");
                } else {
                    LOG.warn("Proxy: No se encontró token JWT para reenviar al backend HCEN Central");
                }
                
                jakarta.ws.rs.core.Response response = requestBuilder
                    .post(jakarta.ws.rs.client.Entity.entity(payload, MediaType.APPLICATION_JSON));
                
                int status = response.getStatus();
                
                LOG.info(String.format("Proxy: Respuesta de HCEN Central - Status: %d", status));
                
                // Leer la respuesta de manera más robusta
                Object responseEntity = null;
                if (response.hasEntity()) {
        try {
                        responseEntity = response.readEntity(Object.class);
                        LOG.info(String.format("Proxy: Respuesta recibida: %s", responseEntity));
                    } catch (Exception e) {
                        LOG.error("Error al leer respuesta de HCEN Central", e);
                        try {
                            String textResponse = response.readEntity(String.class);
                            LOG.warn(String.format("Proxy: Respuesta como texto: %s", textResponse));
                            responseEntity = Map.of("error", textResponse);
                        } catch (Exception e2) {
                            LOG.error("Error al leer respuesta como texto", e2);
                            responseEntity = Map.of("error", "Error desconocido al procesar respuesta");
                        }
                    }
                }
                
                return Response.status(status)
                    .entity(responseEntity != null ? responseEntity : Map.of())
                    .build();
                    
            } finally {
                client.close();
        }
            
        } catch (Exception e) {
            LOG.error("Error en proxy de solicitud de acceso", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Error al procesar la solicitud: " + e.getMessage()))
                .build();
            }
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
