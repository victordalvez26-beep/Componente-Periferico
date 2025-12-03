package uy.edu.tse.hcen.rest;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonbExceptionMapperTest {

    private JsonbExceptionMapper mapper = new JsonbExceptionMapper();

    @Test
    void testToResponseWithFechaNacimientoError() {
        ProcessingException exception = new ProcessingException("Unable to deserialize property fechaNacimiento");
        
        Response response = mapper.toResponse(exception);
        
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());
    }

    @Test
    void testToResponseWithFechaNacimientoInRootCause() {
        RuntimeException rootCause = new RuntimeException("could not be parsed fechaNacimiento");
        ProcessingException exception = new ProcessingException("Error", rootCause);
        
        Response response = mapper.toResponse(exception);
        
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testToResponseWithJsonbException() {
        ProcessingException exception = new ProcessingException("JSON Binding deserialization error");
        
        Response response = mapper.toResponse(exception);
        
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());
    }

    @Test
    void testToResponseWithGenericProcessingException() {
        ProcessingException exception = new ProcessingException("Generic processing error");
        
        Response response = mapper.toResponse(exception);
        
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());
    }

    @Test
    void testToResponseWithNullMessage() {
        ProcessingException exception = new ProcessingException((String) null);
        
        Response response = mapper.toResponse(exception);
        
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testErrorResponse() {
        JsonbExceptionMapper.ErrorResponse errorResponse = new JsonbExceptionMapper.ErrorResponse("Test error");
        
        assertEquals("Test error", errorResponse.getError());
        
        errorResponse.setError("New error");
        assertEquals("New error", errorResponse.getError());
    }
}

