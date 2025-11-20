package uy.edu.tse.hcen.rest;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

/**
 * Helper para construir respuestas HTTP consistentes.
 */
public final class DocumentoResponseBuilder {

    private DocumentoResponseBuilder() {
        // Clase de utilidad, no instanciable
    }

    public static Response badRequest(String error) {
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(DocumentoConstants.FIELD_ERROR, error))
                .build();
    }

    public static Response badRequest(String error, String detail) {
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        DocumentoConstants.FIELD_ERROR, error,
                        DocumentoConstants.FIELD_DETAIL, detail))
                .build();
    }

    public static Response unauthorized(String error) {
        return Response.status(Response.Status.UNAUTHORIZED)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(DocumentoConstants.FIELD_ERROR, error))
                .build();
    }

    public static Response forbidden(String error) {
        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(DocumentoConstants.FIELD_ERROR, error))
                .build();
    }

    public static Response forbidden(String error, String detail) {
        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        DocumentoConstants.FIELD_ERROR, error,
                        DocumentoConstants.FIELD_DETAIL, detail))
                .build();
    }

    public static Response notFound(String error) {
        return Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(DocumentoConstants.FIELD_ERROR, error))
                .build();
    }

    public static Response serviceUnavailable(String error) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(DocumentoConstants.FIELD_ERROR, error))
                .build();
    }

    public static Response serviceUnavailable(String error, String detail) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        DocumentoConstants.FIELD_ERROR, error,
                        DocumentoConstants.FIELD_DETAIL, detail))
                .build();
    }

    public static Response internalServerError(String error) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(DocumentoConstants.FIELD_ERROR, error))
                .build();
    }

    public static Response internalServerError(String error, String detail) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        DocumentoConstants.FIELD_ERROR, error,
                        DocumentoConstants.FIELD_DETAIL, detail))
                .build();
    }

    public static Response ok(Object entity) {
        return Response.ok(entity, MediaType.APPLICATION_JSON).build();
    }

    public static Response created(java.net.URI location, Object entity) {
        return Response.created(location).entity(entity).build();
    }
}



