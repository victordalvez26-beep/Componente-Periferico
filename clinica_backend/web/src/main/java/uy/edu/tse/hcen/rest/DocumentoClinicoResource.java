package uy.edu.tse.hcen.rest;

import uy.edu.tse.hcen.service.DocumentoService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import java.util.List;
import java.net.URI;
import java.util.Map;

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

    @jakarta.inject.Inject
    private uy.edu.tse.hcen.service.HcenClient hcenClient;

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

        // Convert map -> DTO using JSON-B for convenience
        try (jakarta.json.bind.Jsonb jsonb = jakarta.json.bind.JsonbBuilder.create()) {
            String json = jsonb.toJson(datos);
            uy.edu.tse.hcen.dto.DTMetadatos dto = jsonb.fromJson(json, uy.edu.tse.hcen.dto.DTMetadatos.class);

            try {
                hcenClient.registrarMetadatos(dto);
                return Response.status(Response.Status.CREATED).entity(Map.of("status", "registered", "documentoId", dto.getDocumentoId())).build();
            } catch (uy.edu.tse.hcen.exceptions.HcenUnavailableException ex) {
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
            java.util.List<Map<String, Object>> metadatos = hcenClient.consultarMetadatosPaciente(documentoIdPaciente);
            
            // Filtrar metadatos del mismo tenant si hay tenantId en el contexto
            if (currentTenantId != null && !currentTenantId.isBlank()) {
                java.util.List<Map<String, Object>> metadatosFiltrados = new java.util.ArrayList<>();
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
     * Devuelve el contenido directamente con el Content-Type apropiado.
     * 
     * @param id ID del documento (MongoDB _id hex string)
     * @return El contenido del documento
     */
    @GET
    @Path("/{id}/contenido")
    public Response obtenerContenido(@PathParam("id") String id) {
        var doc = documentoService.buscarPorId(id);
        if (doc == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "document not found or invalid id"))
                    .build();
        }
        
        // Extraer contenido del documento
        String contenido = doc.getString("contenido");
        if (contenido == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "document has no content"))
                    .build();
        }
        
        // Determinar Content-Type (puede venir en formato del metadato, pero por ahora usamos text/plain)
        String contentType = jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
        
        // Devolver contenido directamente
        return Response.ok(contenido, contentType)
                .header("Content-Disposition", "inline; filename=\"documento_" + id + ".txt\"")
                .build();
    }
}
