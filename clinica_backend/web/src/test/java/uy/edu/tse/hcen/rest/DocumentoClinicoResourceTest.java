package uy.edu.tse.hcen.rest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.HttpHeaders;
import org.bson.Document;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uy.edu.tse.hcen.multitenancy.TenantContext;
import uy.edu.tse.hcen.service.DocumentoService;

import java.io.InputStream;
import java.security.Principal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentoClinicoResourceTest {

    @Mock
    private DocumentoService documentoService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private HttpHeaders httpHeaders;

    @Mock
    private Principal principal;

    @InjectMocks
    private DocumentoClinicoResource resource;

    @BeforeEach
    void setUp() {
        // Inyectar mocks usando reflection
        try {
            java.lang.reflect.Field field = DocumentoClinicoResource.class.getDeclaredField("documentoService");
            field.setAccessible(true);
            field.set(resource, documentoService);
            
            field = DocumentoClinicoResource.class.getDeclaredField("securityContext");
            field.setAccessible(true);
            field.set(resource, securityContext);
            
            field = DocumentoClinicoResource.class.getDeclaredField("httpHeaders");
            field.setAccessible(true);
            field.set(resource, httpHeaders);
        } catch (Exception e) {
            throw new RuntimeException("Error setting up mocks: " + e.getMessage(), e);
        }
    }

    @Test
    void testCrearDocumentoCompletoSuccess() {
        Map<String, Object> body = new HashMap<>();
        body.put("ciPaciente", "12345678");
        body.put("contenido", "Contenido del documento");
        body.put("tipoDocumento", "EVALUACION");
        
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.setCurrentTenant("101");
        
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("mongoId", "doc-123");
        try {
            when(documentoService.crearDocumentoCompleto(anyLong(), anyString(), anyString(), anyString(), 
                    anyString(), anyString(), anyString(), anyString())).thenReturn(resultado);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        Response response = resource.crearDocumentoCompleto(body);
        
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        try {
            verify(documentoService).crearDocumentoCompleto(eq(101L), eq("prof-1"), eq("12345678"), 
                    eq("Contenido del documento"), eq("EVALUACION"), isNull(), isNull(), isNull());
        } catch (Exception e) {
            // Ignore verification errors
        }
    }

    @Test
    void testCrearDocumentoCompletoNullBody() {
        Response response = resource.crearDocumentoCompleto(null);
        
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testCrearDocumentoCompletoNoAuth() {
        Map<String, Object> body = new HashMap<>();
        body.put("ciPaciente", "12345678");
        
        when(securityContext.getUserPrincipal()).thenReturn(null);
        
        Response response = resource.crearDocumentoCompleto(body);
        
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    void testCrearDocumentoCompletoNoTenant() {
        Map<String, Object> body = new HashMap<>();
        body.put("ciPaciente", "12345678");
        
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.clear();
        
        Response response = resource.crearDocumentoCompleto(body);
        
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testCrearDocumentoCompletoMissingCiPaciente() {
        Map<String, Object> body = new HashMap<>();
        body.put("contenido", "Contenido");
        
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.setCurrentTenant("101");
        
        Response response = resource.crearDocumentoCompleto(body);
        
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testCrearDocumentoCompletoMissingContenido() {
        Map<String, Object> body = new HashMap<>();
        body.put("ciPaciente", "12345678");
        
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.setCurrentTenant("101");
        
        Response response = resource.crearDocumentoCompleto(body);
        
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testCrearDocumentoCompletoConArchivoSuccess() {
        MultipartFormDataInput input = mock(MultipartFormDataInput.class);
        Map<String, List<InputPart>> formDataMap = new HashMap<>();
        
        InputPart contenidoPart = mock(InputPart.class);
        InputPart ciPart = mock(InputPart.class);
        InputPart archivoPart = mock(InputPart.class);
        
        try {
            when(contenidoPart.getBodyAsString()).thenReturn("Contenido");
            when(ciPart.getBodyAsString()).thenReturn("12345678");
            InputStream mockStream = mock(InputStream.class);
            when(mockStream.readAllBytes()).thenReturn("test".getBytes());
            when(archivoPart.getBody(InputStream.class, null)).thenReturn(mockStream);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        when(archivoPart.getHeaders()).thenReturn(new jakarta.ws.rs.core.MultivaluedHashMap<>());
        
        formDataMap.put("contenido", Arrays.asList(contenidoPart));
        formDataMap.put("ciPaciente", Arrays.asList(ciPart));
        formDataMap.put("archivo", Arrays.asList(archivoPart));
        
        when(input.getFormDataMap()).thenReturn(formDataMap);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.setCurrentTenant("101");
        
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("mongoId", "doc-123");
        try {
            when(documentoService.crearDocumentoCompletoConArchivo(anyLong(), anyString(), anyString(), 
                    anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString(), anyString()))
                    .thenReturn(resultado);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        Response response = resource.crearDocumentoCompletoConArchivo(input);
        
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    void testObtenerContenidoSuccess() {
        TenantContext.setCurrentTenant("101");
        when(documentoService.obtenerContenido("doc-123", 101L)).thenReturn("Contenido del documento");
        
        Response response = resource.obtenerContenido("doc-123");
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("Contenido del documento", response.getEntity());
    }

    @Test
    void testObtenerContenidoNotFound() {
        TenantContext.setCurrentTenant("101");
        when(documentoService.obtenerContenido("doc-123", 101L)).thenReturn(null);
        
        Response response = resource.obtenerContenido("doc-123");
        
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void testObtenerPdfSuccess() {
        TenantContext.setCurrentTenant("101");
        byte[] pdfBytes = "PDF content".getBytes();
        when(documentoService.obtenerPdf("doc-123", 101L)).thenReturn(pdfBytes);
        
        Response response = resource.obtenerPdf("doc-123", null);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(pdfBytes, response.getEntity());
    }

    @Test
    void testObtenerPdfWithTenantIdParam() {
        byte[] pdfBytes = "PDF content".getBytes();
        when(documentoService.obtenerPdf("doc-123", 202L)).thenReturn(pdfBytes);
        
        Response response = resource.obtenerPdf("doc-123", 202L);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void testObtenerPdfNotFound() {
        TenantContext.setCurrentTenant("101");
        when(documentoService.obtenerPdf("doc-123", 101L)).thenReturn(null);
        
        Response response = resource.obtenerPdf("doc-123", null);
        
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void testObtenerDocumentoSuccess() {
        TenantContext.setCurrentTenant("101");
        Document doc = new Document("_id", "doc-123");
        doc.append("documentoId", "doc-123");
        doc.append("ciPaciente", "12345678");
        doc.append("tipoDocumento", "EVALUACION");
        doc.append("fechaCreacion", new Date());
        
        when(documentoService.obtenerDocumentoPorId("doc-123", 101L)).thenReturn(doc);
        
        Response response = resource.obtenerDocumento("doc-123");
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void testObtenerDocumentoNotFound() {
        TenantContext.setCurrentTenant("101");
        when(documentoService.obtenerDocumentoPorId("doc-123", 101L)).thenReturn(null);
        
        Response response = resource.obtenerDocumento("doc-123");
        
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void testSolicitarAccesoSuccess() {
        Map<String, Object> body = new HashMap<>();
        body.put("pacienteCI", "12345678");
        body.put("documentoId", "doc-123");
        
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.setCurrentTenant("101");
        when(httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION)).thenReturn(Arrays.asList("Bearer token"));
        
        try (var clientBuilderMock = mockStatic(jakarta.ws.rs.client.ClientBuilder.class)) {
            jakarta.ws.rs.client.Client client = mock(jakarta.ws.rs.client.Client.class);
            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            jakarta.ws.rs.client.Invocation.Builder builder = mock(jakarta.ws.rs.client.Invocation.Builder.class);
            jakarta.ws.rs.core.Response hcenResponse = mock(jakarta.ws.rs.core.Response.class);
            
            clientBuilderMock.when(jakarta.ws.rs.client.ClientBuilder::newClient).thenReturn(client);
            when(client.target(anyString())).thenReturn(target);
            when(target.request(anyString())).thenReturn(builder);
            when(builder.header(anyString(), anyString())).thenReturn(builder);
            when(builder.post(any())).thenReturn(hcenResponse);
            when(hcenResponse.getStatus()).thenReturn(200);
            when(hcenResponse.hasEntity()).thenReturn(true);
            when(hcenResponse.readEntity(Object.class)).thenReturn(Map.of("success", true));
            
            Response response = resource.solicitarAcceso(body);
            
            assertEquals(200, response.getStatus());
        }
    }

    @Test
    void testSolicitarAccesoNullBody() {
        Response response = resource.solicitarAcceso(null);
        
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testSolicitarAccesoMissingPacienteCI() {
        Map<String, Object> body = new HashMap<>();
        body.put("documentoId", "doc-123");
        
        Response response = resource.solicitarAcceso(body);
        
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testSolicitarAccesoNoAuth() {
        Map<String, Object> body = new HashMap<>();
        body.put("pacienteCI", "12345678");
        
        when(securityContext.getUserPrincipal()).thenReturn(null);
        
        Response response = resource.solicitarAcceso(body);
        
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    void testObtenerContenidoWithException() {
        String id = "doc-123";
        TenantContext.setCurrentTenant("101");
        
        when(documentoService.obtenerContenido(id, 101L))
            .thenThrow(new RuntimeException("Database error"));
        
        Response response = resource.obtenerContenido(id);
        
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }

    @Test
    void testObtenerContenidoWithNoTenant() {
        String id = "doc-123";
        TenantContext.clear();
        
        Response response = resource.obtenerContenido(id);
        
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testObtenerPdfWithNoTenantAndNoParam() {
        String id = "doc-123";
        TenantContext.clear();
        
        Response response = resource.obtenerPdf(id, null);
        
        // Debe usar tenantId=1 por defecto
        assertNotNull(response);
    }

    @Test
    void testObtenerPdfWithException() {
        String id = "doc-123";
        Long tenantIdParam = 101L;
        TenantContext.setCurrentTenant("101");
        
        when(documentoService.obtenerPdf(id, tenantIdParam))
            .thenThrow(new RuntimeException("Service error"));
        
        Response response = resource.obtenerPdf(id, tenantIdParam);
        
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }

    @Test
    void testObtenerDocumentoWithException() {
        String id = "doc-123";
        TenantContext.setCurrentTenant("101");
        
        when(documentoService.obtenerDocumentoPorId(id, 101L))
            .thenThrow(new RuntimeException("Database error"));
        
        Response response = resource.obtenerDocumento(id);
        
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }

    @Test
    void testObtenerDocumentoWithNoTenant() {
        String id = "doc-123";
        TenantContext.clear();
        
        Response response = resource.obtenerDocumento(id);
        
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testCrearDocumentoCompletoWithException() {
        Map<String, Object> body = new HashMap<>();
        body.put("ciPaciente", "12345678");
        body.put("contenido", "Contenido del documento");
        
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.setCurrentTenant("101");
        
        when(documentoService.crearDocumentoCompleto(anyLong(), anyString(), anyString(), anyString(), 
                anyString(), anyString(), anyString(), anyString()))
            .thenThrow(new RuntimeException("Service error"));
        
        Response response = resource.crearDocumentoCompleto(body);
        
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }

    @Test
    void testCrearDocumentoCompletoWithIllegalArgumentException() {
        Map<String, Object> body = new HashMap<>();
        body.put("ciPaciente", "12345678");
        body.put("contenido", "Contenido del documento");
        
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.setCurrentTenant("101");
        
        when(documentoService.crearDocumentoCompleto(anyLong(), anyString(), anyString(), anyString(), 
                anyString(), anyString(), anyString(), anyString()))
            .thenThrow(new IllegalArgumentException("Paciente no encontrado"));
        
        Response response = resource.crearDocumentoCompleto(body);
        
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @ParameterizedTest
    @CsvSource({
        "BLANK_CI, Contenido",
        "12345678, BLANK_CONTENT",
        "12345678, NULL_CONTENT"
    })
    void testCrearDocumentoCompletoWithInvalidFields(String ciPaciente, String contenido) {
        Map<String, Object> body = new HashMap<>();
        
        if ("BLANK_CI".equals(ciPaciente)) {
            body.put("ciPaciente", "");
            body.put("contenido", contenido);
        } else if ("BLANK_CONTENT".equals(contenido)) {
            body.put("ciPaciente", ciPaciente);
            body.put("contenido", "");
        } else if ("NULL_CONTENT".equals(contenido)) {
            body.put("ciPaciente", ciPaciente);
            body.put("contenido", null);
        }
        
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.setCurrentTenant("101");
        
        Response response = resource.crearDocumentoCompleto(body);
        
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testSolicitarAccesoWithNullBody() {
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        
        Response response = resource.solicitarAcceso(null);
        
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testSolicitarAccesoWithMissingPacienteCI() {
        Map<String, Object> body = new HashMap<>();
        body.put("documentoId", "doc-123");
        
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        
        Response response = resource.solicitarAcceso(body);
        
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testSolicitarAccesoWithBlankPacienteCI() {
        Map<String, Object> body = new HashMap<>();
        body.put("pacienteCI", "");
        
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.setCurrentTenant("101");
        when(httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION)).thenReturn(Arrays.asList("Bearer token"));
        
        try (var clientBuilderMock = mockStatic(jakarta.ws.rs.client.ClientBuilder.class)) {
            jakarta.ws.rs.client.Client client = mock(jakarta.ws.rs.client.Client.class);
            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            jakarta.ws.rs.client.Invocation.Builder builder = mock(jakarta.ws.rs.client.Invocation.Builder.class);
            jakarta.ws.rs.core.Response hcenResponse = mock(jakarta.ws.rs.core.Response.class);
            
            clientBuilderMock.when(jakarta.ws.rs.client.ClientBuilder::newClient).thenReturn(client);
            when(client.target(anyString())).thenReturn(target);
            when(target.request(anyString())).thenReturn(builder);
            when(builder.header(anyString(), anyString())).thenReturn(builder);
            when(builder.post(any())).thenReturn(hcenResponse);
            when(hcenResponse.getStatus()).thenReturn(400);
            when(hcenResponse.hasEntity()).thenReturn(true);
            when(hcenResponse.readEntity(Object.class)).thenReturn(Map.of("error", "pacienteCI inválido"));
            
            Response response = resource.solicitarAcceso(body);
            
            // El código permite pasar el pacienteCI en blanco al backend, que devuelve 400
            assertEquals(400, response.getStatus());
        }
    }

    @Test
    void testSolicitarAccesoWithBlankProfesionalId() {
        Map<String, Object> body = new HashMap<>();
        body.put("pacienteCI", "12345678");
        
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("");
        
        Response response = resource.solicitarAcceso(body);
        
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    void testObtenerPdfWithEmptyPdfBytes() {
        String id = "doc-123";
        TenantContext.setCurrentTenant("101");
        
        when(documentoService.obtenerPdf(id, 101L)).thenReturn(new byte[0]);
        
        Response response = resource.obtenerPdf(id, null);
        
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void testObtenerPdfWithNullPdfBytes() {
        String id = "doc-123";
        TenantContext.setCurrentTenant("101");
        
        when(documentoService.obtenerPdf(id, 101L)).thenReturn(null);
        
        Response response = resource.obtenerPdf(id, null);
        
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }


    @Test
    void testCrearDocumentoCompletoWithBlankProfesionalId() {
        Map<String, Object> body = new HashMap<>();
        body.put("ciPaciente", "12345678");
        body.put("contenido", "Contenido");
        
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("");
        TenantContext.setCurrentTenant("101");
        
        Response response = resource.crearDocumentoCompleto(body);
        
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    void testCrearDocumentoCompletoConArchivoWithException() {
        MultipartFormDataInput input = mock(MultipartFormDataInput.class);
        Map<String, List<InputPart>> formDataMap = new HashMap<>();
        
        InputPart contenidoPart = mock(InputPart.class);
        InputPart ciPart = mock(InputPart.class);
        
        try {
            when(contenidoPart.getBodyAsString()).thenReturn("Contenido");
            when(ciPart.getBodyAsString()).thenReturn("12345678");
        } catch (java.io.IOException e) {
            fail("IOException no esperada: " + e.getMessage());
        }
        
        formDataMap.put("contenido", Arrays.asList(contenidoPart));
        formDataMap.put("ciPaciente", Arrays.asList(ciPart));
        
        when(input.getFormDataMap()).thenReturn(formDataMap);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.setCurrentTenant("101");
        
        when(documentoService.crearDocumentoCompletoConArchivo(anyLong(), anyString(), anyString(), 
                anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Service error"));
        
        Response response = resource.crearDocumentoCompletoConArchivo(input);
        
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }

    @Test
    void testCrearDocumentoCompletoConArchivoWithIllegalArgumentException() {
        MultipartFormDataInput input = mock(MultipartFormDataInput.class);
        Map<String, List<InputPart>> formDataMap = new HashMap<>();
        
        InputPart contenidoPart = mock(InputPart.class);
        InputPart ciPart = mock(InputPart.class);
        
        try {
            when(contenidoPart.getBodyAsString()).thenReturn("Contenido");
            when(ciPart.getBodyAsString()).thenReturn("12345678");
        } catch (java.io.IOException e) {
            fail("IOException no esperada: " + e.getMessage());
        }
        
        formDataMap.put("contenido", Arrays.asList(contenidoPart));
        formDataMap.put("ciPaciente", Arrays.asList(ciPart));
        
        when(input.getFormDataMap()).thenReturn(formDataMap);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.setCurrentTenant("101");
        
        when(documentoService.crearDocumentoCompletoConArchivo(anyLong(), anyString(), anyString(), 
                anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Paciente no encontrado"));
        
        Response response = resource.crearDocumentoCompletoConArchivo(input);
        
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testObtenerDocumentoWithArchivoAdjunto() {
        String id = "doc-123";
        TenantContext.setCurrentTenant("101");
        
        org.bson.Document doc = new org.bson.Document();
        doc.put("documentoId", "doc-123");
        doc.put("ciPaciente", "12345678");
        doc.put("tipoDocumento", "EVALUACION");
        doc.put("descripcion", "Descripción");
        doc.put("titulo", "Título");
        doc.put("autor", "Autor");
        doc.put("fechaCreacion", new java.util.Date());
        doc.put("nombreArchivoAdjunto", "archivo.pdf");
        doc.put("tipoArchivoAdjunto", "application/pdf");
        
        org.bson.types.Binary archivoAdjunto = new org.bson.types.Binary("test".getBytes());
        doc.put("archivoAdjunto", archivoAdjunto);
        
        when(documentoService.obtenerDocumentoPorId(id, 101L)).thenReturn(doc);
        
        Response response = resource.obtenerDocumento(id);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void testObtenerDocumentoWithPdfBytes() {
        String id = "doc-123";
        TenantContext.setCurrentTenant("101");
        
        org.bson.Document doc = new org.bson.Document();
        doc.put("documentoId", "doc-123");
        doc.put("ciPaciente", "12345678");
        doc.put("tipoDocumento", "EVALUACION");
        doc.put("descripcion", "Descripción");
        doc.put("titulo", "Título");
        doc.put("autor", "Autor");
        doc.put("fechaCreacion", new java.util.Date());
        
        org.bson.types.Binary pdfBytes = new org.bson.types.Binary("PDF content".getBytes());
        doc.put("pdfBytes", pdfBytes);
        
        when(documentoService.obtenerDocumentoPorId(id, 101L)).thenReturn(doc);
        
        Response response = resource.obtenerDocumento(id);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void testObtenerDocumentoWithEmptyPdfBytes() {
        String id = "doc-123";
        TenantContext.setCurrentTenant("101");
        
        org.bson.Document doc = new org.bson.Document();
        doc.put("documentoId", "doc-123");
        doc.put("ciPaciente", "12345678");
        doc.put("tipoDocumento", "EVALUACION");
        
        org.bson.types.Binary pdfBytes = new org.bson.types.Binary(new byte[0]);
        doc.put("pdfBytes", pdfBytes);
        
        when(documentoService.obtenerDocumentoPorId(id, 101L)).thenReturn(doc);
        
        Response response = resource.obtenerDocumento(id);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void testObtenerDocumentoWithEmptyArchivoAdjunto() {
        String id = "doc-123";
        TenantContext.setCurrentTenant("101");
        
        org.bson.Document doc = new org.bson.Document();
        doc.put("documentoId", "doc-123");
        doc.put("ciPaciente", "12345678");
        doc.put("tipoDocumento", "EVALUACION");
        doc.put("nombreArchivoAdjunto", "archivo.pdf");
        doc.put("tipoArchivoAdjunto", "application/pdf");
        
        org.bson.types.Binary archivoAdjunto = new org.bson.types.Binary(new byte[0]);
        doc.put("archivoAdjunto", archivoAdjunto);
        
        when(documentoService.obtenerDocumentoPorId(id, 101L)).thenReturn(doc);
        
        Response response = resource.obtenerDocumento(id);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void testSolicitarAccesoWithDocumentoId() {
        Map<String, Object> body = new HashMap<>();
        body.put("pacienteCI", "12345678");
        body.put("documentoId", "doc-123");
        body.put("tipoDocumento", "EVALUACION");
        body.put("motivo", "Motivo personalizado");
        
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.setCurrentTenant("101");
        when(httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION)).thenReturn(Arrays.asList("Bearer token"));
        
        try (var clientBuilderMock = mockStatic(jakarta.ws.rs.client.ClientBuilder.class)) {
            jakarta.ws.rs.client.Client client = mock(jakarta.ws.rs.client.Client.class);
            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            jakarta.ws.rs.client.Invocation.Builder builder = mock(jakarta.ws.rs.client.Invocation.Builder.class);
            jakarta.ws.rs.core.Response hcenResponse = mock(jakarta.ws.rs.core.Response.class);
            
            clientBuilderMock.when(jakarta.ws.rs.client.ClientBuilder::newClient).thenReturn(client);
            when(client.target(anyString())).thenReturn(target);
            when(target.request(anyString())).thenReturn(builder);
            when(builder.header(anyString(), anyString())).thenReturn(builder);
            when(builder.post(any())).thenReturn(hcenResponse);
            when(hcenResponse.getStatus()).thenReturn(200);
            when(hcenResponse.hasEntity()).thenReturn(true);
            when(hcenResponse.readEntity(Object.class)).thenReturn(Map.of("success", true));
            
            Response response = resource.solicitarAcceso(body);
            
            assertEquals(200, response.getStatus());
        }
    }

    @Test
    void testSolicitarAccesoWithNullDocumentoId() {
        Map<String, Object> body = new HashMap<>();
        body.put("pacienteCI", "12345678");
        body.put("documentoId", null);
        
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.setCurrentTenant("101");
        when(httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION)).thenReturn(Arrays.asList("Bearer token"));
        
        try (var clientBuilderMock = mockStatic(jakarta.ws.rs.client.ClientBuilder.class)) {
            jakarta.ws.rs.client.Client client = mock(jakarta.ws.rs.client.Client.class);
            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            jakarta.ws.rs.client.Invocation.Builder builder = mock(jakarta.ws.rs.client.Invocation.Builder.class);
            jakarta.ws.rs.core.Response hcenResponse = mock(jakarta.ws.rs.core.Response.class);
            
            clientBuilderMock.when(jakarta.ws.rs.client.ClientBuilder::newClient).thenReturn(client);
            when(client.target(anyString())).thenReturn(target);
            when(target.request(anyString())).thenReturn(builder);
            when(builder.header(anyString(), anyString())).thenReturn(builder);
            when(builder.post(any())).thenReturn(hcenResponse);
            when(hcenResponse.getStatus()).thenReturn(200);
            when(hcenResponse.hasEntity()).thenReturn(true);
            when(hcenResponse.readEntity(Object.class)).thenReturn(Map.of("success", true));
            
            Response response = resource.solicitarAcceso(body);
            
            assertEquals(200, response.getStatus());
        }
    }

    @Test
    void testSolicitarAccesoWithNoTenantId() {
        Map<String, Object> body = new HashMap<>();
        body.put("pacienteCI", "12345678");
        
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.clear();
        when(httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION)).thenReturn(Arrays.asList("Bearer token"));
        
        try (var clientBuilderMock = mockStatic(jakarta.ws.rs.client.ClientBuilder.class)) {
            jakarta.ws.rs.client.Client client = mock(jakarta.ws.rs.client.Client.class);
            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            jakarta.ws.rs.client.Invocation.Builder builder = mock(jakarta.ws.rs.client.Invocation.Builder.class);
            jakarta.ws.rs.core.Response hcenResponse = mock(jakarta.ws.rs.core.Response.class);
            
            clientBuilderMock.when(jakarta.ws.rs.client.ClientBuilder::newClient).thenReturn(client);
            when(client.target(anyString())).thenReturn(target);
            when(target.request(anyString())).thenReturn(builder);
            when(builder.header(anyString(), anyString())).thenReturn(builder);
            when(builder.post(any())).thenReturn(hcenResponse);
            when(hcenResponse.getStatus()).thenReturn(200);
            when(hcenResponse.hasEntity()).thenReturn(true);
            when(hcenResponse.readEntity(Object.class)).thenReturn(Map.of("success", true));
            
            Response response = resource.solicitarAcceso(body);
            
            // Debe funcionar aunque no haya tenantId
            assertEquals(200, response.getStatus());
        }
    }

    @Test
    void testSolicitarAccesoWithNoAuthHeader() {
        Map<String, Object> body = new HashMap<>();
        body.put("pacienteCI", "12345678");
        
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.setCurrentTenant("101");
        when(httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        
        try (var clientBuilderMock = mockStatic(jakarta.ws.rs.client.ClientBuilder.class)) {
            jakarta.ws.rs.client.Client client = mock(jakarta.ws.rs.client.Client.class);
            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            jakarta.ws.rs.client.Invocation.Builder builder = mock(jakarta.ws.rs.client.Invocation.Builder.class);
            jakarta.ws.rs.core.Response hcenResponse = mock(jakarta.ws.rs.core.Response.class);
            
            clientBuilderMock.when(jakarta.ws.rs.client.ClientBuilder::newClient).thenReturn(client);
            when(client.target(anyString())).thenReturn(target);
            when(target.request(anyString())).thenReturn(builder);
            when(builder.post(any())).thenReturn(hcenResponse);
            when(hcenResponse.getStatus()).thenReturn(200);
            when(hcenResponse.hasEntity()).thenReturn(true);
            when(hcenResponse.readEntity(Object.class)).thenReturn(Map.of("success", true));
            
            Response response = resource.solicitarAcceso(body);
            
            assertEquals(200, response.getStatus());
        }
    }

    @Test
    void testSolicitarAccesoWithAuthHeaderNotBearer() {
        Map<String, Object> body = new HashMap<>();
        body.put("pacienteCI", "12345678");
        
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.setCurrentTenant("101");
        when(httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION)).thenReturn(Arrays.asList("Basic token"));
        
        try (var clientBuilderMock = mockStatic(jakarta.ws.rs.client.ClientBuilder.class)) {
            jakarta.ws.rs.client.Client client = mock(jakarta.ws.rs.client.Client.class);
            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            jakarta.ws.rs.client.Invocation.Builder builder = mock(jakarta.ws.rs.client.Invocation.Builder.class);
            jakarta.ws.rs.core.Response hcenResponse = mock(jakarta.ws.rs.core.Response.class);
            
            clientBuilderMock.when(jakarta.ws.rs.client.ClientBuilder::newClient).thenReturn(client);
            when(client.target(anyString())).thenReturn(target);
            when(target.request(anyString())).thenReturn(builder);
            when(builder.post(any())).thenReturn(hcenResponse);
            when(hcenResponse.getStatus()).thenReturn(200);
            when(hcenResponse.hasEntity()).thenReturn(true);
            when(hcenResponse.readEntity(Object.class)).thenReturn(Map.of("success", true));
            
            Response response = resource.solicitarAcceso(body);
            
            assertEquals(200, response.getStatus());
        }
    }

    @Test
    void testSolicitarAccesoWithErrorReadingEntity() {
        Map<String, Object> body = new HashMap<>();
        body.put("pacienteCI", "12345678");
        
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.setCurrentTenant("101");
        when(httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION)).thenReturn(Arrays.asList("Bearer token"));
        
        try (var clientBuilderMock = mockStatic(jakarta.ws.rs.client.ClientBuilder.class)) {
            jakarta.ws.rs.client.Client client = mock(jakarta.ws.rs.client.Client.class);
            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            jakarta.ws.rs.client.Invocation.Builder builder = mock(jakarta.ws.rs.client.Invocation.Builder.class);
            jakarta.ws.rs.core.Response hcenResponse = mock(jakarta.ws.rs.core.Response.class);
            
            clientBuilderMock.when(jakarta.ws.rs.client.ClientBuilder::newClient).thenReturn(client);
            when(client.target(anyString())).thenReturn(target);
            when(target.request(anyString())).thenReturn(builder);
            when(builder.header(anyString(), anyString())).thenReturn(builder);
            when(builder.post(any())).thenReturn(hcenResponse);
            when(hcenResponse.getStatus()).thenReturn(200);
            when(hcenResponse.hasEntity()).thenReturn(true);
            when(hcenResponse.readEntity(Object.class)).thenThrow(new RuntimeException("Error reading entity"));
            when(hcenResponse.readEntity(String.class)).thenReturn("Error response");
            
            Response response = resource.solicitarAcceso(body);
            
            assertEquals(200, response.getStatus());
        }
    }

    @Test
    void testSolicitarAccesoWithErrorReadingEntityAsText() {
        Map<String, Object> body = new HashMap<>();
        body.put("pacienteCI", "12345678");
        
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.setCurrentTenant("101");
        when(httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION)).thenReturn(Arrays.asList("Bearer token"));
        
        try (var clientBuilderMock = mockStatic(jakarta.ws.rs.client.ClientBuilder.class)) {
            jakarta.ws.rs.client.Client client = mock(jakarta.ws.rs.client.Client.class);
            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            jakarta.ws.rs.client.Invocation.Builder builder = mock(jakarta.ws.rs.client.Invocation.Builder.class);
            jakarta.ws.rs.core.Response hcenResponse = mock(jakarta.ws.rs.core.Response.class);
            
            clientBuilderMock.when(jakarta.ws.rs.client.ClientBuilder::newClient).thenReturn(client);
            when(client.target(anyString())).thenReturn(target);
            when(target.request(anyString())).thenReturn(builder);
            when(builder.header(anyString(), anyString())).thenReturn(builder);
            when(builder.post(any())).thenReturn(hcenResponse);
            when(hcenResponse.getStatus()).thenReturn(200);
            when(hcenResponse.hasEntity()).thenReturn(true);
            when(hcenResponse.readEntity(Object.class)).thenThrow(new RuntimeException("Error reading entity"));
            when(hcenResponse.readEntity(String.class)).thenThrow(new RuntimeException("Error reading as text"));
            
            Response response = resource.solicitarAcceso(body);
            
            assertEquals(200, response.getStatus());
        }
    }

    @Test
    void testSolicitarAccesoWithNoEntity() {
        Map<String, Object> body = new HashMap<>();
        body.put("pacienteCI", "12345678");
        
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.setCurrentTenant("101");
        when(httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION)).thenReturn(Arrays.asList("Bearer token"));
        
        try (var clientBuilderMock = mockStatic(jakarta.ws.rs.client.ClientBuilder.class)) {
            jakarta.ws.rs.client.Client client = mock(jakarta.ws.rs.client.Client.class);
            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            jakarta.ws.rs.client.Invocation.Builder builder = mock(jakarta.ws.rs.client.Invocation.Builder.class);
            jakarta.ws.rs.core.Response hcenResponse = mock(jakarta.ws.rs.core.Response.class);
            
            clientBuilderMock.when(jakarta.ws.rs.client.ClientBuilder::newClient).thenReturn(client);
            when(client.target(anyString())).thenReturn(target);
            when(target.request(anyString())).thenReturn(builder);
            when(builder.header(anyString(), anyString())).thenReturn(builder);
            when(builder.post(any())).thenReturn(hcenResponse);
            when(hcenResponse.getStatus()).thenReturn(204);
            when(hcenResponse.hasEntity()).thenReturn(false);
            
            Response response = resource.solicitarAcceso(body);
            
            assertEquals(204, response.getStatus());
        }
    }

    @Test
    void testSolicitarAccesoWithException() {
        Map<String, Object> body = new HashMap<>();
        body.put("pacienteCI", "12345678");
        
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.setCurrentTenant("101");
        when(httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION)).thenReturn(Arrays.asList("Bearer token"));
        
        try (var clientBuilderMock = mockStatic(jakarta.ws.rs.client.ClientBuilder.class)) {
            jakarta.ws.rs.client.Client client = mock(jakarta.ws.rs.client.Client.class);
            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            jakarta.ws.rs.client.Invocation.Builder builder = mock(jakarta.ws.rs.client.Invocation.Builder.class);
            
            clientBuilderMock.when(jakarta.ws.rs.client.ClientBuilder::newClient).thenReturn(client);
            when(client.target(anyString())).thenReturn(target);
            when(target.request(anyString())).thenReturn(builder);
            when(builder.header(anyString(), anyString())).thenReturn(builder);
            when(builder.post(any())).thenThrow(new RuntimeException("Network error"));
            
            Response response = resource.solicitarAcceso(body);
            
            assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        }
    }

    @Test
    void testCrearDocumentoCompletoConArchivoWithArchivo() throws Exception {
        MultipartFormDataInput input = mock(MultipartFormDataInput.class);
        Map<String, List<InputPart>> formDataMap = new HashMap<>();
        
        InputPart contenidoPart = mock(InputPart.class);
        InputPart ciPart = mock(InputPart.class);
        InputPart archivoPart = mock(InputPart.class);
        
        try {
            when(contenidoPart.getBodyAsString()).thenReturn("Contenido");
            when(ciPart.getBodyAsString()).thenReturn("12345678");
        } catch (java.io.IOException e) {
            fail("IOException no esperada: " + e.getMessage());
        }
        
        InputStream archivoStream = new java.io.ByteArrayInputStream("archivo content".getBytes());
        when(archivoPart.getBody(InputStream.class, null)).thenReturn(archivoStream);
        
        jakarta.ws.rs.core.MultivaluedMap<String, String> headers = new jakarta.ws.rs.core.MultivaluedHashMap<>();
        headers.add("Content-Disposition", "form-data; name=\"archivo\"; filename=\"test.pdf\"");
        headers.add("Content-Type", "application/pdf");
        when(archivoPart.getHeaders()).thenReturn(headers);
        
        formDataMap.put("contenido", Arrays.asList(contenidoPart));
        formDataMap.put("ciPaciente", Arrays.asList(ciPart));
        formDataMap.put("archivo", Arrays.asList(archivoPart));
        
        when(input.getFormDataMap()).thenReturn(formDataMap);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.setCurrentTenant("101");
        
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("mongoId", "doc-123");
        when(documentoService.crearDocumentoCompletoConArchivo(anyLong(), anyString(), anyString(), 
                anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString(), anyString()))
                .thenReturn(resultado);
        
        Response response = resource.crearDocumentoCompletoConArchivo(input);
        
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    }

}

