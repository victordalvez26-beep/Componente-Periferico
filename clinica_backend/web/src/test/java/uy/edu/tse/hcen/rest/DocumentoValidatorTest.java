package uy.edu.tse.hcen.rest;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DocumentoValidatorTest {

    @Test
    void testValidateBodyNull() {
        Response response = DocumentoValidator.validateBody(null);
        
        assertNotNull(response);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testValidateBodyValid() {
        Map<String, Object> body = new HashMap<>();
        Response response = DocumentoValidator.validateBody(body);
        
        assertNull(response);
    }

    @Test
    void testValidateIdNull() {
        Response response = DocumentoValidator.validateId(null);
        
        assertNotNull(response);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testValidateIdBlank() {
        Response response = DocumentoValidator.validateId("   ");
        
        assertNotNull(response);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testValidateIdValid() {
        Response response = DocumentoValidator.validateId("doc-123");
        
        assertNull(response);
    }

    @Test
    void testValidateDocumentoIdPacienteNull() {
        Response response = DocumentoValidator.validateDocumentoIdPaciente(null);
        
        assertNotNull(response);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testValidateDocumentoIdPacienteValid() {
        Response response = DocumentoValidator.validateDocumentoIdPaciente("12345678");
        
        assertNull(response);
    }

    @Test
    void testValidateContenidoNull() {
        Response response = DocumentoValidator.validateContenido(null);
        
        assertNotNull(response);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testValidateContenidoValid() {
        Response response = DocumentoValidator.validateContenido("Contenido");
        
        assertNull(response);
    }

    @Test
    void testValidateCodDocumPacienteNull() {
        Response response = DocumentoValidator.validateCodDocumPaciente(null);
        
        assertNotNull(response);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testValidateCodDocumPacienteValid() {
        Response response = DocumentoValidator.validateCodDocumPaciente("12345678");
        
        assertNull(response);
    }

    @Test
    void testValidateUsuarioIdNull() {
        Response response = DocumentoValidator.validateUsuarioId(null);
        
        assertNotNull(response);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    void testValidateUsuarioIdValid() {
        Response response = DocumentoValidator.validateUsuarioId("user-123");
        
        assertNull(response);
    }
}

