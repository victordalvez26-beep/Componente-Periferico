package uy.edu.tse.hcen.rest;

import jakarta.ws.rs.core.Response;
import java.lang.reflect.Field;
import static org.mockito.ArgumentMatchers.isNull;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.HttpHeaders;
import org.bson.Document;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uy.edu.tse.hcen.multitenancy.TenantContext;
import uy.edu.tse.hcen.service.DocumentoService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
        // Limpiar y configurar TenantContext
        TenantContext.clear();
        TenantContext.setCurrentTenant("101");
        
        // Inyectar mocks usando reflection
        try {
            Field field = DocumentoClinicoResource.class.getDeclaredField("documentoService");
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
void testCrearDocumentoCompletoSuccess() throws Exception { // Lanza la excepción del servicio para no ocultarla
    // ARRANGE (Setup)
    Map<String, Object> body = new HashMap<>();
    body.put("ciPaciente", "12345678");
    body.put("contenido", "Contenido del documento");
    body.put("tipoDocumento", "EVALUACION");
    // Agregamos los campos opcionales como null para claridad, aunque no es estrictamente necesario
    body.put("descripcion", null); 
    body.put("titulo", null);
    body.put("autor", null); 
    
    // Mocks de seguridad y tenant
    when(securityContext.getUserPrincipal()).thenReturn(principal);
    when(principal.getName()).thenReturn("prof-1");
    // TenantContext se configura en @BeforeEach, pero lo reafirmamos por claridad
    TenantContext.setCurrentTenant("101"); 
    
    // Configuración del resultado esperado del servicio
    Map<String, Object> resultado = Map.of("mongoId", "doc-123");
    
    // STUBBING: Usar any() o isNull() para los argumentos que pueden ser null
    when(documentoService.crearDocumentoCompleto(
             eq(101L), 
             eq("prof-1"), 
             eq("12345678"), 
             eq("Contenido del documento"), 
             eq("EVALUACION"), 
             isNull(), 
             isNull(), 
             isNull()
         )).thenReturn(resultado);
    
    // ACT (Ejecución)
    Response response = resource.crearDocumentoCompleto(body);
    
    // ASSERT (Verificación)
    assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus(),
                 "Debe retornar 201 CREATED si la solicitud es exitosa.");
    
    // Verificación de la llamada al servicio con los argumentos esperados
    verify(documentoService, times(1)).crearDocumentoCompleto(
        eq(101L), 
        eq("prof-1"), 
        eq("12345678"), 
        eq("Contenido del documento"), 
        eq("EVALUACION"), 
        isNull(), 
        isNull(), 
        isNull()
    );
    
    // Verificación del cuerpo de la respuesta
    Map<String, Object> responseEntity = (Map<String, Object>) response.getEntity();
    assertNotNull(responseEntity);
    assertEquals("doc-123", responseEntity.get("mongoId"));
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
    void testCrearDocumentoCompletoConArchivoSuccess() throws Exception {
        // ARRANGE: Setup del formulario multipart
        MultipartFormDataInput input = mock(MultipartFormDataInput.class);
        Map<String, List<InputPart>> formDataMap = new HashMap<>();
        
        // Mocks de las partes (datos simples)
        InputPart contenidoPart = mock(InputPart.class);
        InputPart ciPart = mock(InputPart.class);
        InputPart archivoPart = mock(InputPart.class);
        
        // 1. Datos del contenido y CI
        final String contenidoStr = "Contenido";
        final String ciStr = "12345678";
        when(contenidoPart.getBodyAsString()).thenReturn(contenidoStr);
        when(ciPart.getBodyAsString()).thenReturn(ciStr);
        
        // 2. Datos del archivo
        final byte[] archivoData = "test".getBytes();
        InputStream mockStream = new ByteArrayInputStream(archivoData);
        
        // Usamos 'any()' para el tipo de InputStream y 'isNull()' para el media type, como es usual en Resteasy
        when(archivoPart.getBody(eq(InputStream.class), isNull())).thenReturn(mockStream); 
        
        // 3. Headers del archivo
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add("Content-Type", "application/pdf");
        headers.add("Content-Disposition", "form-data; name=\"archivo\"; filename=\"test.pdf\"");
        when(archivoPart.getHeaders()).thenReturn(headers);
        
        // 4. Llenar el mapa del formulario
        formDataMap.put("contenido", Arrays.asList(contenidoPart));
        formDataMap.put("ciPaciente", Arrays.asList(ciPart));
        formDataMap.put("archivo", Arrays.asList(archivoPart));
        
        // Incluir campos opcionales nulos para que el extractField no lance NPE si se intenta acceder a ellos
        formDataMap.put("tipoDocumento", null); 
        formDataMap.put("descripcion", null);
        formDataMap.put("titulo", null);
        formDataMap.put("autor", null);
    
        when(input.getFormDataMap()).thenReturn(formDataMap);
        
        // Mocks de seguridad y tenant
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.setCurrentTenant("101");
        
        // Configuración del resultado esperado del servicio
        Map<String, Object> resultado = Map.of("mongoId", "doc-123");
        
        // 5. STUBBING CORREGIDO: Usamos isNull() para los 4 campos de texto opcionales y any(byte[].class) para el archivo
        when(documentoService.crearDocumentoCompletoConArchivo(
            eq(101L),                         // tenantId
            eq("prof-1"),                     // profesionalId
            eq(ciStr),                        // ciPaciente
            eq(contenidoStr),                 // contenido
            isNull(String.class),             // tipoDocumento 
            isNull(String.class),             // descripcion
            isNull(String.class),             // titulo
            isNull(String.class),             // autor
            any(byte[].class),                // archivoBytes (byte[] real)
            eq("test.pdf"),                   // nombreArchivo
            eq("application/pdf")             // tipoArchivo
        )).thenReturn(resultado);
        
        // ACT (Ejecución)
        Response response = resource.crearDocumentoCompletoConArchivo(input);
        
        // ASSERT (Verificación)
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus(),
                     "Debe retornar 201 CREATED si la solicitud es exitosa.");
        
        // Verificación de la llamada
        verify(documentoService, times(1)).crearDocumentoCompletoConArchivo(
            eq(101L), eq("prof-1"), eq(ciStr), eq(contenidoStr), 
            isNull(), isNull(), isNull(), isNull(), 
            any(byte[].class), eq("test.pdf"), eq("application/pdf")
        );
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
    void testObtenerContenidoWithException() throws Exception {
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
    void testObtenerPdfWithException() throws Exception {
        String id = "doc-123";
        Long tenantIdParam = 101L;
        TenantContext.setCurrentTenant("101");
        
        when(documentoService.obtenerPdf(id, tenantIdParam))
            .thenThrow(new RuntimeException("Service error"));
        
        Response response = resource.obtenerPdf(id, tenantIdParam);
        
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }

    @Test
    void testObtenerDocumentoWithException() throws Exception {
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
    void testCrearDocumentoCompletoWithException() throws Exception {
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
    void testCrearDocumentoCompletoWithIllegalArgumentException() throws Exception {
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

    @Test
    void testCrearDocumentoCompletoWithBlankCiPaciente() {
        Map<String, Object> body = new HashMap<>();
        body.put("ciPaciente", "");
        body.put("contenido", "Contenido");
        
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
    void testObtenerPdfWithEmptyPdfBytes() throws Exception {
        String id = "doc-123";
        TenantContext.setCurrentTenant("101");
        
        when(documentoService.obtenerPdf(id, 101L)).thenReturn(new byte[0]);
        
        Response response = resource.obtenerPdf(id, null);
        
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void testObtenerPdfWithNullPdfBytes() throws Exception {
        String id = "doc-123";
        TenantContext.setCurrentTenant("101");
        
        when(documentoService.obtenerPdf(id, 101L)).thenReturn(null);
        
        Response response = resource.obtenerPdf(id, null);
        
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void testCrearDocumentoCompletoWithBlankContenido() {
        Map<String, Object> body = new HashMap<>();
        body.put("ciPaciente", "12345678");
        body.put("contenido", "");
        
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.setCurrentTenant("101");
        
        Response response = resource.crearDocumentoCompleto(body);
        
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testCrearDocumentoCompletoWithNullContenido() {
        Map<String, Object> body = new HashMap<>();
        body.put("ciPaciente", "12345678");
        body.put("contenido", null);
        
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.setCurrentTenant("101");
        
        Response response = resource.crearDocumentoCompleto(body);
        
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
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
    void testCrearDocumentoCompletoConArchivoWithException() throws Exception {
        MultipartFormDataInput input = mock(MultipartFormDataInput.class);
        Map<String, List<InputPart>> formDataMap = new HashMap<>();
        
        InputPart contenidoPart = mock(InputPart.class);
        InputPart ciPart = mock(InputPart.class);
        
        when(contenidoPart.getBodyAsString()).thenReturn("Contenido");
        when(ciPart.getBodyAsString()).thenReturn("12345678");
        
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
    void testCrearDocumentoCompletoConArchivoWithIllegalArgumentException() throws Exception {
        MultipartFormDataInput input = mock(MultipartFormDataInput.class);
        Map<String, List<InputPart>> formDataMap = new HashMap<>();
        
        InputPart contenidoPart = mock(InputPart.class);
        InputPart ciPart = mock(InputPart.class);
        
        when(contenidoPart.getBodyAsString()).thenReturn("Contenido");
        when(ciPart.getBodyAsString()).thenReturn("12345678");
        
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
    void testObtenerDocumentoWithArchivoAdjunto() throws Exception {
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
    void testObtenerDocumentoWithPdfBytes() throws Exception {
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
    void testObtenerDocumentoWithEmptyPdfBytes() throws Exception {
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
    void testObtenerDocumentoWithEmptyArchivoAdjunto() throws Exception {
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
        
        when(contenidoPart.getBodyAsString()).thenReturn("Contenido");
        when(ciPart.getBodyAsString()).thenReturn("12345678");
        
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

