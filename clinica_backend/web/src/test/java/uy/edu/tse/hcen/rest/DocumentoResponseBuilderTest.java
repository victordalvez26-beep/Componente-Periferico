package uy.edu.tse.hcen.rest;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentoResponseBuilderTest {

    @Test
    void testBadRequest() {
        Response response = DocumentoResponseBuilder.badRequest("Error message");
        
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());
    }

    @Test
    void testBadRequestWithDetail() {
        Response response = DocumentoResponseBuilder.badRequest("Error", "Detail");
        
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testUnauthorized() {
        Response response = DocumentoResponseBuilder.unauthorized("Unauthorized");
        
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    void testForbidden() {
        Response response = DocumentoResponseBuilder.forbidden("Forbidden");
        
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    void testForbiddenWithDetail() {
        Response response = DocumentoResponseBuilder.forbidden("Forbidden", "Detail");
        
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    void testNotFound() {
        Response response = DocumentoResponseBuilder.notFound("Not found");
        
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void testServiceUnavailable() {
        Response response = DocumentoResponseBuilder.serviceUnavailable("Unavailable");
        
        assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
    }

    @Test
    void testServiceUnavailableWithDetail() {
        Response response = DocumentoResponseBuilder.serviceUnavailable("Unavailable", "Detail");
        
        assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
    }

    @Test
    void testInternalServerError() {
        Response response = DocumentoResponseBuilder.internalServerError("Error");
        
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }

    @Test
    void testInternalServerErrorWithDetail() {
        Response response = DocumentoResponseBuilder.internalServerError("Error", "Detail");
        
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }

    @Test
    void testOk() {
        Object entity = new Object();
        Response response = DocumentoResponseBuilder.ok(entity);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(entity, response.getEntity());
    }

    @Test
    void testCreated() {
        java.net.URI location = java.net.URI.create("/api/documentos/123");
        Object entity = new Object();
        Response response = DocumentoResponseBuilder.created(location, entity);
        
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals(location, response.getLocation());
    }
}

