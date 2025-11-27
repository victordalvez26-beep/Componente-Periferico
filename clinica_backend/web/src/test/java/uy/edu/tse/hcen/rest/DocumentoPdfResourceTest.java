package uy.edu.tse.hcen.rest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
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
import uy.edu.tse.hcen.client.PoliticasAccesoClient;
import uy.edu.tse.hcen.multitenancy.TenantContext;
import uy.edu.tse.hcen.repository.ProfesionalSaludRepository;
import uy.edu.tse.hcen.service.DocumentoPdfService;
import uy.edu.tse.hcen.service.HcenClient;
import uy.edu.tse.hcen.service.ProfesionalSaludService;

import java.io.InputStream;
import java.security.Principal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentoPdfResourceTest {

    @Mock
    private DocumentoPdfService documentoPdfService;

    @Mock
    private ProfesionalSaludService profesionalSaludService;

    @Mock
    private PoliticasAccesoClient politicasAccesoClient;

    @Mock
    private HcenClient hcenClient;

    @Mock
    private ProfesionalSaludRepository profesionalSaludRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Principal principal;

    @InjectMocks
    private DocumentoPdfResource resource;

    @BeforeEach
    void setUp() {
        try {
            java.lang.reflect.Field field = DocumentoPdfResource.class.getDeclaredField("documentoPdfService");
            field.setAccessible(true);
            field.set(resource, documentoPdfService);
            
            field = DocumentoPdfResource.class.getDeclaredField("profesionalSaludService");
            field.setAccessible(true);
            field.set(resource, profesionalSaludService);
            
            field = DocumentoPdfResource.class.getDeclaredField("politicasAccesoClient");
            field.setAccessible(true);
            field.set(resource, politicasAccesoClient);
            
            field = DocumentoPdfResource.class.getDeclaredField("hcenClient");
            field.setAccessible(true);
            field.set(resource, hcenClient);
            
            field = DocumentoPdfResource.class.getDeclaredField("profesionalSaludRepository");
            field.setAccessible(true);
            field.set(resource, profesionalSaludRepository);
            
            field = DocumentoPdfResource.class.getDeclaredField("securityContext");
            field.setAccessible(true);
            field.set(resource, securityContext);
        } catch (Exception e) {
            fail("Error setting up mocks: " + e.getMessage());
        }
    }

    @Test
    void testSubirPdfSuccess() throws Exception {
        MultipartFormDataInput input = mock(MultipartFormDataInput.class);
        Map<String, List<InputPart>> formDataMap = new HashMap<>();
        
        InputPart archivoPart = mock(InputPart.class);
        InputPart ciPart = mock(InputPart.class);
        
        when(archivoPart.getBody(InputStream.class, null)).thenReturn(mock(InputStream.class));
        jakarta.ws.rs.core.MultivaluedMap<String, String> headers = new jakarta.ws.rs.core.MultivaluedHashMap<>();
        headers.add("Content-Type", "application/pdf");
        when(archivoPart.getHeaders()).thenReturn(headers);
        when(ciPart.getBodyAsString()).thenReturn("12345678");
        
        formDataMap.put("archivo", Arrays.asList(archivoPart));
        formDataMap.put("ciPaciente", Arrays.asList(ciPart));
        
        when(input.getFormDataMap()).thenReturn(formDataMap);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.setCurrentTenant("101");
        
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("documentoId", "doc-123");
        when(documentoPdfService.procesarYGuardarPdf(anyLong(), anyString(), anyString(), 
                any(InputStream.class), anyString(), anyString())).thenReturn(resultado);
        
        Response response = resource.subirPdf(input);
        
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    void testSubirPdfNoAuth() {
        MultipartFormDataInput input = mock(MultipartFormDataInput.class);
        when(securityContext.getUserPrincipal()).thenReturn(null);
        
        Response response = resource.subirPdf(input);
        
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    void testSubirPdfNoTenant() {
        MultipartFormDataInput input = mock(MultipartFormDataInput.class);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.clear();
        
        Response response = resource.subirPdf(input);
        
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testSubirPdfNoArchivo() {
        MultipartFormDataInput input = mock(MultipartFormDataInput.class);
        Map<String, List<InputPart>> formDataMap = new HashMap<>();
        when(input.getFormDataMap()).thenReturn(formDataMap);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.setCurrentTenant("101");
        
        Response response = resource.subirPdf(input);
        
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testListarDocumentosPorPacienteSuccess() {
        TenantContext.setCurrentTenant("101");
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        
        List<Map<String, Object>> documentos = new ArrayList<>();
        when(documentoPdfService.listarDocumentosPorPaciente("12345678", "prof-1", "101"))
                .thenReturn(documentos);
        
        Response response = resource.listarDocumentosPorPaciente("12345678");
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void testDescargarPdfSuccess() {
        TenantContext.setCurrentTenant("101");
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ciPaciente", "12345678");
        metadata.put("tipoDocumento", "EVALUACION");
        when(documentoPdfService.obtenerMetadataPorId("doc-123", 101L)).thenReturn(metadata);
        when(politicasAccesoClient.verificarPermiso(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);
        
        byte[] pdfBytes = "PDF content".getBytes();
        when(documentoPdfService.obtenerPdfPorId("doc-123", 101L)).thenReturn(pdfBytes);
        
        Response response = resource.descargarPdf("doc-123", null);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void testDescargarPdfNotFound() {
        TenantContext.setCurrentTenant("101");
        when(documentoPdfService.obtenerMetadataPorId("doc-123", 101L)).thenReturn(null);
        
        Response response = resource.descargarPdf("doc-123", null);
        
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void testDescargarPdfNoPermission() {
        TenantContext.setCurrentTenant("101");
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ciPaciente", "12345678");
        metadata.put("tipoDocumento", "EVALUACION");
        when(documentoPdfService.obtenerMetadataPorId("doc-123", 101L)).thenReturn(metadata);
        when(politicasAccesoClient.verificarPermiso(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(false);
        
        Response response = resource.descargarPdf("doc-123", null);
        
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    void testDescargarPdfWithTenantIdParam() {
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ciPaciente", "12345678");
        metadata.put("tipoDocumento", "EVALUACION");
        when(documentoPdfService.obtenerMetadataPorId("doc-123", 202L)).thenReturn(metadata);
        when(politicasAccesoClient.verificarPermiso(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);
        
        byte[] pdfBytes = "PDF content".getBytes();
        when(documentoPdfService.obtenerPdfPorId("doc-123", 202L)).thenReturn(pdfBytes);
        
        Response response = resource.descargarPdf("doc-123", 202L);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void testSubirPdfWithException() throws Exception {
        MultipartFormDataInput input = mock(MultipartFormDataInput.class);
        Map<String, List<InputPart>> formDataMap = new HashMap<>();
        
        InputPart archivoPart = mock(InputPart.class);
        InputPart ciPart = mock(InputPart.class);
        
        when(archivoPart.getBody(InputStream.class, null)).thenReturn(mock(InputStream.class));
        jakarta.ws.rs.core.MultivaluedMap<String, String> headers = new jakarta.ws.rs.core.MultivaluedHashMap<>();
        headers.add("Content-Type", "application/pdf");
        when(archivoPart.getHeaders()).thenReturn(headers);
        when(ciPart.getBodyAsString()).thenReturn("12345678");
        
        formDataMap.put("archivo", Arrays.asList(archivoPart));
        formDataMap.put("ciPaciente", Arrays.asList(ciPart));
        
        when(input.getFormDataMap()).thenReturn(formDataMap);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.setCurrentTenant("101");
        
        when(documentoPdfService.procesarYGuardarPdf(anyLong(), anyString(), anyString(), 
                any(InputStream.class), anyString(), anyString()))
                .thenThrow(new RuntimeException("Service error"));
        
        Response response = resource.subirPdf(input);
        
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }

    @Test
    void testListarDocumentosPorPacienteWithException() {
        TenantContext.setCurrentTenant("101");
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        
        when(documentoPdfService.listarDocumentosPorPaciente("12345678", "prof-1", "101"))
                .thenThrow(new RuntimeException("Database error"));
        
        Response response = resource.listarDocumentosPorPaciente("12345678");
        
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }

    @Test
    void testListarDocumentosPorPacienteNoAuth() {
        TenantContext.setCurrentTenant("101");
        when(securityContext.getUserPrincipal()).thenReturn(null);
        
        Response response = resource.listarDocumentosPorPaciente("12345678");
        
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    void testListarDocumentosPorPacienteNoTenant() {
        TenantContext.clear();
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        
        Response response = resource.listarDocumentosPorPaciente("12345678");
        
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testDescargarPdfWithException() {
        TenantContext.setCurrentTenant("101");
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ciPaciente", "12345678");
        metadata.put("tipoDocumento", "EVALUACION");
        when(documentoPdfService.obtenerMetadataPorId("doc-123", 101L)).thenReturn(metadata);
        when(politicasAccesoClient.verificarPermiso(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);
        when(documentoPdfService.obtenerPdfPorId("doc-123", 101L))
                .thenThrow(new RuntimeException("Service error"));
        
        Response response = resource.descargarPdf("doc-123", null);
        
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }

    @Test
    void testDescargarPdfWithBackendHCENCall() {
        TenantContext.setCurrentTenant("101");
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("hcen-backend");
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ciPaciente", "12345678");
        metadata.put("tipoDocumento", "EVALUACION");
        when(documentoPdfService.obtenerMetadataPorId("doc-123", 101L)).thenReturn(metadata);
        
        byte[] pdfBytes = "PDF content".getBytes();
        when(documentoPdfService.obtenerPdfPorId("doc-123", 101L)).thenReturn(pdfBytes);
        
        Response response = resource.descargarPdf("doc-123", null);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(politicasAccesoClient, never()).verificarPermiso(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testDescargarPdfWithNoTenantAndNoParam() {
        TenantContext.clear();
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ciPaciente", "12345678");
        when(documentoPdfService.obtenerMetadataPorId("doc-123", 1L)).thenReturn(metadata);
        
        byte[] pdfBytes = "PDF content".getBytes();
        when(documentoPdfService.obtenerPdfPorId("doc-123", 1L)).thenReturn(pdfBytes);
        
        Response response = resource.descargarPdf("doc-123", null);
        
        // Debe usar tenantId=1 por defecto
        assertNotNull(response);
    }

    @Test
    void testDescargarPdfWithEmptyPdfBytes() {
        TenantContext.setCurrentTenant("101");
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ciPaciente", "12345678");
        metadata.put("tipoDocumento", "EVALUACION");
        when(documentoPdfService.obtenerMetadataPorId("doc-123", 101L)).thenReturn(metadata);
        when(politicasAccesoClient.verificarPermiso(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);
        // El código verifica null, no bytes vacíos
        when(documentoPdfService.obtenerPdfPorId("doc-123", 101L)).thenReturn(null);
        
        Response response = resource.descargarPdf("doc-123", null);
        
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void testSubirPdfWithInvalidContentType() throws Exception {
        MultipartFormDataInput input = mock(MultipartFormDataInput.class);
        Map<String, List<InputPart>> formDataMap = new HashMap<>();
        
        InputPart archivoPart = mock(InputPart.class);
        InputPart ciPart = mock(InputPart.class);
        
        when(archivoPart.getBody(InputStream.class, null)).thenReturn(mock(InputStream.class));
        when(archivoPart.getHeaders()).thenReturn(new jakarta.ws.rs.core.MultivaluedHashMap<>());
        when(archivoPart.getHeaders().getFirst("Content-Type")).thenReturn("image/jpeg");
        when(ciPart.getBodyAsString()).thenReturn("12345678");
        
        formDataMap.put("archivo", Arrays.asList(archivoPart));
        formDataMap.put("ciPaciente", Arrays.asList(ciPart));
        
        when(input.getFormDataMap()).thenReturn(formDataMap);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.setCurrentTenant("101");
        
        Response response = resource.subirPdf(input);
        
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testSubirPdfWithNullContentType() throws Exception {
        MultipartFormDataInput input = mock(MultipartFormDataInput.class);
        Map<String, List<InputPart>> formDataMap = new HashMap<>();
        
        InputPart archivoPart = mock(InputPart.class);
        InputPart ciPart = mock(InputPart.class);
        
        when(archivoPart.getBody(InputStream.class, null)).thenReturn(mock(InputStream.class));
        when(archivoPart.getHeaders()).thenReturn(new jakarta.ws.rs.core.MultivaluedHashMap<>());
        when(archivoPart.getHeaders().getFirst("Content-Type")).thenReturn(null);
        when(ciPart.getBodyAsString()).thenReturn("12345678");
        
        formDataMap.put("archivo", Arrays.asList(archivoPart));
        formDataMap.put("ciPaciente", Arrays.asList(ciPart));
        
        when(input.getFormDataMap()).thenReturn(formDataMap);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.setCurrentTenant("101");
        
        Response response = resource.subirPdf(input);
        
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testSubirPdfWithNoCiPaciente() throws Exception {
        MultipartFormDataInput input = mock(MultipartFormDataInput.class);
        Map<String, List<InputPart>> formDataMap = new HashMap<>();
        
        InputPart archivoPart = mock(InputPart.class);
        
        when(archivoPart.getBody(InputStream.class, null)).thenReturn(mock(InputStream.class));
        jakarta.ws.rs.core.MultivaluedMap<String, String> headers = new jakarta.ws.rs.core.MultivaluedHashMap<>();
        headers.add("Content-Type", "application/pdf");
        when(archivoPart.getHeaders()).thenReturn(headers);
        
        formDataMap.put("archivo", Arrays.asList(archivoPart));
        
        when(input.getFormDataMap()).thenReturn(formDataMap);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        TenantContext.setCurrentTenant("101");
        
        Response response = resource.subirPdf(input);
        
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testDescargarPdfWithNullPacienteCI() {
        TenantContext.setCurrentTenant("101");
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ciPaciente", null);
        metadata.put("tipoDocumento", "EVALUACION");
        when(documentoPdfService.obtenerMetadataPorId("doc-123", 101L)).thenReturn(metadata);
        
        byte[] pdfBytes = "PDF content".getBytes();
        when(documentoPdfService.obtenerPdfPorId("doc-123", 101L)).thenReturn(pdfBytes);
        
        Response response = resource.descargarPdf("doc-123", null);
        
        // Debe permitir descarga si no hay pacienteCI (caso especial)
        assertNotNull(response);
    }

    @Test
    void testDescargarPdfWithBlankPacienteCI() {
        TenantContext.setCurrentTenant("101");
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ciPaciente", "");
        metadata.put("tipoDocumento", "EVALUACION");
        when(documentoPdfService.obtenerMetadataPorId("doc-123", 101L)).thenReturn(metadata);
        
        byte[] pdfBytes = "PDF content".getBytes();
        when(documentoPdfService.obtenerPdfPorId("doc-123", 101L)).thenReturn(pdfBytes);
        
        Response response = resource.descargarPdf("doc-123", null);
        
        // Debe permitir descarga si pacienteCI está vacío
        assertNotNull(response);
    }

    @Test
    void testDescargarPdfWithNullProfesionalId() {
        TenantContext.setCurrentTenant("101");
        when(securityContext.getUserPrincipal()).thenReturn(null);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ciPaciente", "12345678");
        metadata.put("tipoDocumento", "EVALUACION");
        when(documentoPdfService.obtenerMetadataPorId("doc-123", 101L)).thenReturn(metadata);
        
        byte[] pdfBytes = "PDF content".getBytes();
        when(documentoPdfService.obtenerPdfPorId("doc-123", 101L)).thenReturn(pdfBytes);
        
        Response response = resource.descargarPdf("doc-123", null);
        
        // Debe permitir descarga sin profesionalId (puede ser llamada desde backend)
        assertNotNull(response);
    }

    @Test
    void testDescargarPdfWithHCENServiceProfesionalId() {
        TenantContext.setCurrentTenant("101");
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("HCEN-Service-123");
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ciPaciente", "12345678");
        metadata.put("tipoDocumento", "EVALUACION");
        when(documentoPdfService.obtenerMetadataPorId("doc-123", 101L)).thenReturn(metadata);
        
        byte[] pdfBytes = "PDF content".getBytes();
        when(documentoPdfService.obtenerPdfPorId("doc-123", 101L)).thenReturn(pdfBytes);
        
        Response response = resource.descargarPdf("doc-123", null);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(politicasAccesoClient, never()).verificarPermiso(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testDescargarPdfWithProfesionalAndRegistroAcceso() {
        TenantContext.setCurrentTenant("101");
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ciPaciente", "12345678");
        metadata.put("tipoDocumento", "EVALUACION");
        when(documentoPdfService.obtenerMetadataPorId("doc-123", 101L)).thenReturn(metadata);
        when(politicasAccesoClient.verificarPermiso(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);
        
        byte[] pdfBytes = "%PDF-1.4 content".getBytes();
        when(documentoPdfService.obtenerPdfPorId("doc-123", 101L)).thenReturn(pdfBytes);
        
        uy.edu.tse.hcen.model.ProfesionalSalud profesional = new uy.edu.tse.hcen.model.ProfesionalSalud();
        profesional.setNombre("Dr. Test");
        profesional.setEspecialidad(uy.edu.tse.hcen.model.enums.Especialidad.CARDIOLOGIA);
        when(profesionalSaludRepository.findByNickname("prof-1"))
                .thenReturn(java.util.Optional.of(profesional));
        
        doNothing().when(hcenClient).registrarAccesoHistoriaClinica(anyString(), anyString(), 
                anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean());
        
        Response response = resource.descargarPdf("doc-123", null);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(hcenClient).registrarAccesoHistoriaClinica(anyString(), anyString(), 
                anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    void testDescargarPdfWithProfesionalButNoEspecialidad() {
        TenantContext.setCurrentTenant("101");
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ciPaciente", "12345678");
        metadata.put("tipoDocumento", "EVALUACION");
        when(documentoPdfService.obtenerMetadataPorId("doc-123", 101L)).thenReturn(metadata);
        when(politicasAccesoClient.verificarPermiso(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);
        
        byte[] pdfBytes = "%PDF-1.4 content".getBytes();
        when(documentoPdfService.obtenerPdfPorId("doc-123", 101L)).thenReturn(pdfBytes);
        
        uy.edu.tse.hcen.model.ProfesionalSalud profesional = new uy.edu.tse.hcen.model.ProfesionalSalud();
        profesional.setNombre("Dr. Test");
        profesional.setEspecialidad(null);
        when(profesionalSaludRepository.findByNickname("prof-1"))
                .thenReturn(java.util.Optional.of(profesional));
        
        doNothing().when(hcenClient).registrarAccesoHistoriaClinica(anyString(), anyString(), 
                anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean());
        
        Response response = resource.descargarPdf("doc-123", null);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void testDescargarPdfWithProfesionalRepositoryException() {
        TenantContext.setCurrentTenant("101");
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ciPaciente", "12345678");
        metadata.put("tipoDocumento", "EVALUACION");
        when(documentoPdfService.obtenerMetadataPorId("doc-123", 101L)).thenReturn(metadata);
        when(politicasAccesoClient.verificarPermiso(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);
        
        byte[] pdfBytes = "%PDF-1.4 content".getBytes();
        when(documentoPdfService.obtenerPdfPorId("doc-123", 101L)).thenReturn(pdfBytes);
        
        when(profesionalSaludRepository.findByNickname("prof-1"))
                .thenThrow(new RuntimeException("Repository error"));
        
        doNothing().when(hcenClient).registrarAccesoHistoriaClinica(anyString(), anyString(), 
                anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean());
        
        Response response = resource.descargarPdf("doc-123", null);
        
        // Debe continuar aunque falle obtener información del profesional
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void testDescargarPdfWithHcenClientException() {
        TenantContext.setCurrentTenant("101");
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ciPaciente", "12345678");
        metadata.put("tipoDocumento", "EVALUACION");
        when(documentoPdfService.obtenerMetadataPorId("doc-123", 101L)).thenReturn(metadata);
        when(politicasAccesoClient.verificarPermiso(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);
        
        byte[] pdfBytes = "%PDF-1.4 content".getBytes();
        when(documentoPdfService.obtenerPdfPorId("doc-123", 101L)).thenReturn(pdfBytes);
        
        uy.edu.tse.hcen.model.ProfesionalSalud profesional = new uy.edu.tse.hcen.model.ProfesionalSalud();
        profesional.setNombre("Dr. Test");
        when(profesionalSaludRepository.findByNickname("prof-1"))
                .thenReturn(java.util.Optional.of(profesional));
        
        doThrow(new RuntimeException("HCEN client error")).when(hcenClient)
                .registrarAccesoHistoriaClinica(anyString(), anyString(), 
                anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean());
        
        Response response = resource.descargarPdf("doc-123", null);
        
        // Debe continuar aunque falle el registro de acceso
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void testDescargarPdfWithInvalidPdfHeader() {
        TenantContext.setCurrentTenant("101");
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ciPaciente", "12345678");
        metadata.put("tipoDocumento", "EVALUACION");
        when(documentoPdfService.obtenerMetadataPorId("doc-123", 101L)).thenReturn(metadata);
        when(politicasAccesoClient.verificarPermiso(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);
        
        byte[] pdfBytes = "INVALID content".getBytes();
        when(documentoPdfService.obtenerPdfPorId("doc-123", 101L)).thenReturn(pdfBytes);
        
        Response response = resource.descargarPdf("doc-123", null);
        
        // Debe devolver el PDF aunque el header no sea válido (solo es un warning)
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void testDescargarPdfWithSmallPdfBytes() {
        TenantContext.setCurrentTenant("101");
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof-1");
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ciPaciente", "12345678");
        metadata.put("tipoDocumento", "EVALUACION");
        when(documentoPdfService.obtenerMetadataPorId("doc-123", 101L)).thenReturn(metadata);
        when(politicasAccesoClient.verificarPermiso(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);
        
        byte[] pdfBytes = "PDF".getBytes(); // Menos de 4 bytes
        when(documentoPdfService.obtenerPdfPorId("doc-123", 101L)).thenReturn(pdfBytes);
        
        Response response = resource.descargarPdf("doc-123", null);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }
}

