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
 */
@Path("/api/documentos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class DocumentoClinicoResource {

    @Inject
    private DocumentoService documentoService;

    public DocumentoClinicoResource() {
    }

    /**
     * Endpoint placeholder para enviar metadatos al microservicio. No implementado por ahora.
     */
    @POST
    @Path("/metadatos")
    public Response enviarMetadatos(Map<String, Object> datos) {
        return Response.status(Response.Status.NOT_IMPLEMENTED)
                .entity(Map.of("error", "enviarMetadatos no implementado aún"))
                .build();
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
     */
    @GET
    @Path("/{id}")
    @RolesAllowed("PROFESIONAL")
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
}
