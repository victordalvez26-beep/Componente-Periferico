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

import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import java.util.Map;
import uy.edu.tse.hcen.service.HcenClient;
import uy.edu.tse.hcen.dto.DTMetadatos;
import uy.edu.tse.hcen.exceptions.HcenUnavailableException;
import jakarta.ws.rs.core.Context;
import org.jboss.logging.Logger;


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
    public Response obtenerPorId(@PathParam("id") String id) {
        var doc = documentoService.buscarPorId(id);
        if (doc == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", "document not found or invalid id")).build();
        }
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
     * Crea un documento clínico completo: guarda el contenido en MongoDB y envía los metadatos al HCEN central.
     * Espera un JSON con "contenido" y los campos de metadatos (documentoIdPaciente, especialidad, etc.)
     * El documentoId se genera automáticamente si no se proporciona.
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
            String currentTenantId = uy.edu.tse.hcen.multitenancy.TenantContext.getCurrentTenant();
            
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
     * @param id ID del documento (MongoDB _id hex string)
     * @return El contenido del documento
     */
    @GET
    @Path("/{id}/contenido")
    @RolesAllowed("PROFESIONAL")
    public Response obtenerContenido(@PathParam("id") String id) {
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
        
        // 2) Obtener información del profesional desde el token
        String profesionalId = null;
        if (securityContext != null && securityContext.getUserPrincipal() != null) {
            profesionalId = securityContext.getUserPrincipal().getName();
        }
        
        if (profesionalId == null || profesionalId.isBlank()) {
            // Registrar acceso denegado
            politicasClient.registrarAcceso(null, null, null, null, false, 
                "Profesional no identificado", "Intento de acceso sin autenticación");
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
        
        if (documentoId != null && !documentoId.isBlank()) {
            // Consultar metadatos para obtener información del paciente
            Map<String, Object> metadatos = documentoService.obtenerMetadatosPorDocumentoId(documentoId);
            if (metadatos != null) {
                codDocumPaciente = (String) metadatos.get("documentoIdPaciente");
                tipoDocumento = (String) metadatos.get("formato");
            }
        }
        
        // Si no hay metadatos, intentar obtener pacienteDoc del documento directamente
        if (codDocumPaciente == null || codDocumPaciente.isBlank()) {
            codDocumPaciente = doc.getString("pacienteDoc");
        }
        
        // 4) Verificar permisos si tenemos información del paciente
        if (codDocumPaciente != null && !codDocumPaciente.isBlank()) {
            accesoPermitido = politicasClient.verificarPermiso(profesionalId, codDocumPaciente, tipoDocumento);
            if (!accesoPermitido) {
                motivoRechazo = "No tiene permisos para acceder a este documento";
            }
        } else {
            // Si no podemos determinar el paciente, permitir acceso pero registrar advertencia
            accesoPermitido = true; // Por compatibilidad, permitir si no hay metadatos
            LOG.warnf("No se pudo determinar paciente para documento %s, permitiendo acceso", id);
        }
        
        // 5) Registrar acceso (exitoso o no)
        politicasClient.registrarAcceso(profesionalId, codDocumPaciente, documentoId, tipoDocumento,
                accesoPermitido, motivoRechazo, "Acceso desde componente periférico");
        
        // 6) Si no tiene permiso, denegar acceso
        if (!accesoPermitido) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", "No tiene permisos para acceder a este documento",
                                 "detail", motivoRechazo))
                    .build();
        }
        
        // 7) Extraer contenido del documento
        String contenido = doc.getString("contenido");
        if (contenido == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "document has no content"))
                    .build();
        }
        
        // 8) Determinar Content-Type
        String contentType = tipoDocumento != null && !tipoDocumento.isBlank() 
                ? tipoDocumento 
                : MediaType.TEXT_PLAIN;
        
        // 9) Devolver contenido
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
}
