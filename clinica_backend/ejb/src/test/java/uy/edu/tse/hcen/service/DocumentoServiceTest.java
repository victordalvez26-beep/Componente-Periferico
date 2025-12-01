package uy.edu.tse.hcen.service;

import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uy.edu.tse.hcen.dto.DTMetadatos;
import uy.edu.tse.hcen.exceptions.HcenUnavailableException;
import uy.edu.tse.hcen.multitenancy.TenantContext;
import uy.edu.tse.hcen.repository.DocumentoClinicoRepository;
import uy.edu.tse.hcen.repository.ProfesionalSaludRepository;
import uy.edu.tse.hcen.repository.UsuarioSaludRepository;
import uy.edu.tse.hcen.model.ProfesionalSalud;
import uy.edu.tse.hcen.model.UsuarioSalud;
import uy.edu.tse.hcen.util.DocumentoPdfFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentoServiceTest {

    @Mock
    private DocumentoClinicoRepository documentoRepository;

    @Mock
    private UsuarioSaludRepository usuarioSaludRepository;

    @Mock
    private ProfesionalSaludRepository profesionalSaludRepository;

    @Mock
    private HcenClient hcenClient;

    @InjectMocks
    private DocumentoService documentoService;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
    }

    @Test
    void testCrearDocumentoCompleto() throws Exception {
        Long tenantId = 1L;
        String profesionalId = "prof-1";
        String ciPaciente = "12345678";
        String contenido = "Contenido del documento";
        String tipoDocumento = "EVALUACION";
        String descripcion = "Descripción";
        String titulo = "Título";
        String autor = "Autor";

        UsuarioSalud paciente = new UsuarioSalud();
        paciente.setCi(ciPaciente);
        paciente.setNombre("Juan");
        paciente.setApellido("Pérez");
        paciente.setTenantId(tenantId);

        ProfesionalSalud profesional = new ProfesionalSalud();
        profesional.setNickname(profesionalId);
        profesional.setNombre("Dr. Test");

        when(usuarioSaludRepository.findByCiAndTenant(ciPaciente, tenantId)).thenReturn(paciente);
        when(profesionalSaludRepository.findByNickname(profesionalId)).thenReturn(Optional.of(profesional));
        when(documentoRepository.guardarDocumentoCompleto(anyString(), anyString(), isNull(), isNull(), isNull(), isNull(),
                eq(ciPaciente), eq(tenantId), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ObjectId().toHexString());
        doNothing().when(hcenClient).registrarMetadatos(any(DTMetadatos.class));

        Map<String, Object> result = documentoService.crearDocumentoCompleto(
                tenantId, profesionalId, ciPaciente, contenido, tipoDocumento, descripcion, titulo, autor);

        assertNotNull(result);
        assertEquals(ciPaciente, result.get("ciPaciente"));
        assertEquals(tipoDocumento, result.get("tipoDocumento"));
        assertTrue((Boolean) result.get("sincronizado"));
        verify(hcenClient).registrarMetadatos(any(DTMetadatos.class));
    }

    @Test
    void testCrearDocumentoCompletoSinContenido() {
        assertThrows(IllegalArgumentException.class, () -> {
            documentoService.crearDocumentoCompleto(1L, "prof-1", "12345678", null, "EVALUACION", null, null, null);
        });
    }

    @Test
    void testCrearDocumentoCompletoContenidoVacio() {
        assertThrows(IllegalArgumentException.class, () -> {
            documentoService.crearDocumentoCompleto(1L, "prof-1", "12345678", "   ", "EVALUACION", null, null, null);
        });
    }

    @Test
    void testCrearDocumentoCompletoPacienteNoEncontrado() {
        when(usuarioSaludRepository.findByCiAndTenant("12345678", 1L)).thenReturn(null);
        when(documentoRepository.guardarDocumentoCompleto(anyString(), anyString(), any(), any(), any(), any(), 
                anyString(), anyLong(), anyString(), any(), anyString(), anyString(), anyString()))
                .thenReturn("mongo-id-123");

        // El servicio crea un paciente por defecto si no existe, no lanza excepción
        Map<String, Object> result = documentoService.crearDocumentoCompleto(1L, "prof-1", "12345678", "Contenido", "EVALUACION", null, null, null);
        
        assertNotNull(result);
        assertEquals("12345678", result.get("ciPaciente"));
    }

    @Test
    void testCrearDocumentoCompletoHcenUnavailable() throws Exception {
        Long tenantId = 1L;
        String profesionalId = "prof-1";
        String ciPaciente = "12345678";
        String contenido = "Contenido";

        UsuarioSalud paciente = new UsuarioSalud();
        paciente.setCi(ciPaciente);
        paciente.setTenantId(tenantId);

        when(usuarioSaludRepository.findByCiAndTenant(ciPaciente, tenantId)).thenReturn(paciente);
        when(profesionalSaludRepository.findByNickname(profesionalId)).thenReturn(Optional.empty());
        when(documentoRepository.guardarDocumentoCompleto(anyString(), anyString(), isNull(), isNull(), isNull(), isNull(),
                eq(ciPaciente), eq(tenantId), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ObjectId().toHexString());
        doThrow(new HcenUnavailableException("HCEN no disponible")).when(hcenClient).registrarMetadatos(any(DTMetadatos.class));

        Map<String, Object> result = documentoService.crearDocumentoCompleto(
                tenantId, profesionalId, ciPaciente, contenido, null, null, null, null);

        assertNotNull(result);
        // El documento se guarda aunque falle la sincronización
        verify(hcenClient).registrarMetadatos(any(DTMetadatos.class));
    }

    @Test
    void testCrearDocumentoCompletoConArchivo() throws Exception {
        Long tenantId = 1L;
        String profesionalId = "prof-1";
        String ciPaciente = "12345678";
        String contenido = "Contenido";
        byte[] archivoAdjunto = "archivo".getBytes();

        UsuarioSalud paciente = new UsuarioSalud();
        paciente.setCi(ciPaciente);
        paciente.setTenantId(tenantId);

        when(usuarioSaludRepository.findByCiAndTenant(ciPaciente, tenantId)).thenReturn(paciente);
        when(profesionalSaludRepository.findByNickname(profesionalId)).thenReturn(Optional.empty());
        when(documentoRepository.guardarDocumentoCompleto(anyString(), anyString(), isNull(), eq(archivoAdjunto), anyString(), anyString(),
                eq(ciPaciente), eq(tenantId), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ObjectId().toHexString());
        doNothing().when(hcenClient).registrarMetadatos(any(DTMetadatos.class));

        Map<String, Object> result = documentoService.crearDocumentoCompletoConArchivo(
                tenantId, profesionalId, ciPaciente, contenido, "EVALUACION", null, null, null,
                archivoAdjunto, "archivo.pdf", "application/pdf");

        assertNotNull(result);
        assertTrue((Boolean) result.get("tieneArchivoAdjunto"));
    }

    @Test
    void testObtenerDocumentoPorId() {
        String mongoId = new ObjectId().toHexString();
        Long tenantId = 1L;
        Document doc = new Document("_id", new ObjectId(mongoId));

        when(documentoRepository.buscarPorId(mongoId, tenantId)).thenReturn(doc);

        Document result = documentoService.obtenerDocumentoPorId(mongoId, tenantId);

        assertNotNull(result);
        verify(documentoRepository).buscarPorId(mongoId, tenantId);
    }

    @Test
    void testObtenerContenido() {
        String mongoId = new ObjectId().toHexString();
        Long tenantId = 1L;
        Document doc = new Document("contenido", "Contenido del documento");

        when(documentoRepository.buscarPorId(mongoId, tenantId)).thenReturn(doc);

        String result = documentoService.obtenerContenido(mongoId, tenantId);

        assertEquals("Contenido del documento", result);
    }

    @Test
    void testObtenerContenidoNoExiste() {
        String mongoId = new ObjectId().toHexString();
        Long tenantId = 1L;

        when(documentoRepository.buscarPorId(mongoId, tenantId)).thenReturn(null);

        String result = documentoService.obtenerContenido(mongoId, tenantId);

        assertNull(result);
    }

    @Test
    void testObtenerPdfConPdfBytes() {
        String mongoId = new ObjectId().toHexString();
        Long tenantId = 1L;
        byte[] pdfBytes = "PDF content".getBytes();
        Document doc = new Document("pdfBytes", new Binary(pdfBytes));

        when(documentoRepository.buscarPorId(mongoId, tenantId)).thenReturn(doc);

        byte[] result = documentoService.obtenerPdf(mongoId, tenantId);

        assertNotNull(result);
        assertArrayEquals(pdfBytes, result);
    }

    @Test
    void testObtenerPdfGenerarOnDemand() {
        String mongoId = new ObjectId().toHexString();
        Long tenantId = 1L;
        Document doc = new Document("contenido", "Contenido del documento");
        doc.append("titulo", "Título");
        doc.append("autor", "Autor");
        doc.append("ciPaciente", "12345678");
        byte[] pdfGenerado = "PDF generado".getBytes();

        when(documentoRepository.buscarPorId(mongoId, tenantId)).thenReturn(doc);
        try (var mockedStatic = mockStatic(DocumentoPdfFactory.class)) {
            mockedStatic.when(() -> DocumentoPdfFactory.generarDesdeDocumento(doc)).thenReturn(pdfGenerado);

            byte[] result = documentoService.obtenerPdf(mongoId, tenantId);

            assertNotNull(result);
            assertArrayEquals(pdfGenerado, result);
        }
    }

    @Test
    void testObtenerPdfNoExiste() {
        String mongoId = new ObjectId().toHexString();
        Long tenantId = 1L;

        when(documentoRepository.buscarPorId(mongoId, tenantId)).thenReturn(null);

        byte[] result = documentoService.obtenerPdf(mongoId, tenantId);

        assertNull(result);
        verify(documentoRepository, times(1)).buscarPorId(mongoId, tenantId);
    }

    @Test
    void testObtenerArchivoAdjunto() {
        String mongoId = new ObjectId().toHexString();
        Long tenantId = 1L;
        byte[] archivoBytes = "archivo".getBytes();
        Document doc = new Document("archivoAdjunto", new Binary(archivoBytes));
        doc.append("nombreArchivoAdjunto", "archivo.pdf");
        doc.append("tipoArchivoAdjunto", "application/pdf");

        when(documentoRepository.buscarPorId(mongoId, tenantId)).thenReturn(doc);

        Map<String, Object> result = documentoService.obtenerArchivoAdjunto(mongoId, tenantId);

        assertNotNull(result);
        assertArrayEquals(archivoBytes, (byte[]) result.get("bytes"));
        assertEquals("archivo.pdf", result.get("nombre"));
        assertEquals("application/pdf", result.get("tipo"));
    }

    @Test
    void testObtenerArchivoAdjuntoNoExiste() {
        String mongoId = new ObjectId().toHexString();
        Long tenantId = 1L;
        Document doc = new Document();

        when(documentoRepository.buscarPorId(mongoId, tenantId)).thenReturn(doc);

        Map<String, Object> result = documentoService.obtenerArchivoAdjunto(mongoId, tenantId);

        assertNull(result);
    }

    @Test
    void testCrearDocumentoCompletoWithTenantContextMismatch() throws Exception {
        Long tenantId = 1L;
        TenantContext.setCurrentTenant("999"); // Diferente

        UsuarioSalud paciente = new UsuarioSalud();
        paciente.setCi("12345678");
        paciente.setTenantId(tenantId);

        when(usuarioSaludRepository.findByCiAndTenant("12345678", tenantId)).thenReturn(paciente);
        when(profesionalSaludRepository.findByNickname("prof-1")).thenReturn(Optional.empty());
        when(documentoRepository.guardarDocumentoCompleto(anyString(), anyString(), isNull(), isNull(), isNull(), isNull(),
                eq("12345678"), eq(tenantId), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ObjectId().toHexString());
        doNothing().when(hcenClient).registrarMetadatos(any(DTMetadatos.class));

        Map<String, Object> result = documentoService.crearDocumentoCompleto(
                tenantId, "prof-1", "12345678", "Contenido", "EVALUACION", null, null, null);

        assertNotNull(result);
        assertEquals("1", TenantContext.getCurrentTenant());
    }

    @Test
    void testCrearDocumentoCompletoWithProfesional() throws Exception {
        Long tenantId = 1L;
        String profesionalId = "prof-1";
        UsuarioSalud paciente = new UsuarioSalud();
        paciente.setCi("12345678");
        paciente.setNombre("Juan");
        paciente.setApellido("Pérez");
        paciente.setTenantId(tenantId);

        ProfesionalSalud profesional = new ProfesionalSalud();
        profesional.setNickname(profesionalId);
        profesional.setNombre("Dr. Test");

        when(usuarioSaludRepository.findByCiAndTenant("12345678", tenantId)).thenReturn(paciente);
        when(profesionalSaludRepository.findByNickname(profesionalId)).thenReturn(Optional.of(profesional));
        when(documentoRepository.guardarDocumentoCompleto(anyString(), anyString(), isNull(), isNull(), isNull(), isNull(),
                eq("12345678"), eq(tenantId), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ObjectId().toHexString());
        doNothing().when(hcenClient).registrarMetadatos(any(DTMetadatos.class));

        Map<String, Object> result = documentoService.crearDocumentoCompleto(
                tenantId, profesionalId, "12345678", "Contenido", "EVALUACION", null, null, null);

        assertNotNull(result);
        verify(hcenClient).registrarMetadatos(any(DTMetadatos.class));
    }

    @Test
    void testCrearDocumentoCompletoConArchivoWithNullArchivo() throws Exception {
        Long tenantId = 1L;
        UsuarioSalud paciente = new UsuarioSalud();
        paciente.setCi("12345678");
        paciente.setTenantId(tenantId);

        when(usuarioSaludRepository.findByCiAndTenant("12345678", tenantId)).thenReturn(paciente);
        when(profesionalSaludRepository.findByNickname("prof-1")).thenReturn(Optional.empty());
        when(documentoRepository.guardarDocumentoCompleto(anyString(), anyString(), isNull(), isNull(), isNull(), isNull(),
                eq("12345678"), eq(tenantId), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ObjectId().toHexString());
        doNothing().when(hcenClient).registrarMetadatos(any(DTMetadatos.class));

        Map<String, Object> result = documentoService.crearDocumentoCompletoConArchivo(
                tenantId, "prof-1", "12345678", "Contenido", "EVALUACION", null, null, null,
                null, null, null);

        assertNotNull(result);
    }

    @Test
    void testCrearDocumentoCompletoConArchivoWithBlankContenido() {
        byte[] archivoBytes = "archivo".getBytes();
        assertThrows(IllegalArgumentException.class, () -> {
            documentoService.crearDocumentoCompletoConArchivo(
                    1L, "prof-1", "12345678", "", "EVALUACION", null, null, null,
                    archivoBytes, "archivo.pdf", "application/pdf");
        });
    }

    @Test
    void testObtenerPdfWithNullDocumentReturnsNull() {
        String mongoId = new ObjectId().toHexString();
        Long tenantId = 1L;

        when(documentoRepository.buscarPorId(mongoId, tenantId)).thenReturn(null);

        byte[] result = documentoService.obtenerPdf(mongoId, tenantId);

        assertNull(result);
        verify(documentoRepository).buscarPorId(mongoId, tenantId);
    }

    @Test
    void testObtenerArchivoAdjuntoWithNullDocument() {
        String mongoId = new ObjectId().toHexString();
        Long tenantId = 1L;

        when(documentoRepository.buscarPorId(mongoId, tenantId)).thenReturn(null);

        Map<String, Object> result = documentoService.obtenerArchivoAdjunto(mongoId, tenantId);

        assertNull(result);
    }

    @Test
    void testObtenerContenidoWithNullContenido() {
        String mongoId = new ObjectId().toHexString();
        Long tenantId = 1L;
        Document doc = new Document(); // Sin campo contenido

        when(documentoRepository.buscarPorId(mongoId, tenantId)).thenReturn(doc);

        String result = documentoService.obtenerContenido(mongoId, tenantId);

        assertNull(result);
    }

    @Test
    void testObtenerContenidosPorPaciente() {
        String ciPaciente = "12345678";
        Document doc1 = new Document();
        doc1.put("contenido", "Contenido 1");
        Document doc2 = new Document();
        doc2.put("contenido", "Contenido 2");
        Document doc3 = new Document();
        doc3.put("contenido", null); // Debe ser filtrado
        Document doc4 = new Document();
        doc4.put("contenido", "   "); // Debe ser filtrado (blank)
        
        when(documentoRepository.buscarPorCiPaciente(ciPaciente, null))
                .thenReturn(Arrays.asList(doc1, doc2, doc3, doc4));
        
        List<String> contenidos = documentoService.obtenerContenidosPorPaciente(ciPaciente);
        
        assertNotNull(contenidos);
        assertEquals(2, contenidos.size());
        assertTrue(contenidos.contains("Contenido 1"));
        assertTrue(contenidos.contains("Contenido 2"));
    }

    @Test
    void testObtenerContenidosPorPacienteNull() {
        List<String> contenidos = documentoService.obtenerContenidosPorPaciente(null);
        
        assertNotNull(contenidos);
        assertTrue(contenidos.isEmpty());
    }

    @Test
    void testObtenerContenidosPorPacienteBlank() {
        List<String> contenidos = documentoService.obtenerContenidosPorPaciente("   ");
        
        assertNotNull(contenidos);
        assertTrue(contenidos.isEmpty());
    }

    @Test
    void testObtenerContenidosPorPacienteEmpty() {
        when(documentoRepository.buscarPorCiPaciente("12345678", null))
                .thenReturn(Collections.emptyList());
        
        List<String> contenidos = documentoService.obtenerContenidosPorPaciente("12345678");
        
        assertNotNull(contenidos);
        assertTrue(contenidos.isEmpty());
    }

    @Test
    void testCrearDocumentoCompletoConArchivoHcenUnavailable() throws Exception {
        Long tenantId = 1L;
        String profesionalId = "prof-1";
        String ciPaciente = "12345678";
        String contenido = "Contenido";
        byte[] archivoBytes = new byte[]{1, 2, 3};

        UsuarioSalud paciente = new UsuarioSalud();
        paciente.setCi(ciPaciente);
        paciente.setTenantId(tenantId);

        when(usuarioSaludRepository.findByCiAndTenant(ciPaciente, tenantId)).thenReturn(paciente);
        when(profesionalSaludRepository.findByNickname(profesionalId)).thenReturn(Optional.empty());
        when(documentoRepository.guardarDocumentoCompleto(anyString(), anyString(), isNull(), any(), anyString(), anyString(),
                eq(ciPaciente), eq(tenantId), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ObjectId().toHexString());
        doThrow(new HcenUnavailableException("HCEN no disponible")).when(hcenClient).registrarMetadatos(any(DTMetadatos.class));

        Map<String, Object> result = documentoService.crearDocumentoCompletoConArchivo(
                tenantId, profesionalId, ciPaciente, contenido, "EVALUACION", null, null, null,
                archivoBytes, "archivo.pdf", "application/pdf");

        assertNotNull(result);
        assertTrue((Boolean) result.get("sincronizado")); // Sigue siendo true aunque falle HCEN
    }

    @Test
    void testCrearDocumentoCompletoConArchivoWithNullTipoDocumento() throws Exception {
        Long tenantId = 1L;
        String profesionalId = "prof-1";
        String ciPaciente = "12345678";
        String contenido = "Contenido";

        UsuarioSalud paciente = new UsuarioSalud();
        paciente.setCi(ciPaciente);
        paciente.setTenantId(tenantId);

        when(usuarioSaludRepository.findByCiAndTenant(ciPaciente, tenantId)).thenReturn(paciente);
        when(profesionalSaludRepository.findByNickname(profesionalId)).thenReturn(Optional.empty());
        when(documentoRepository.guardarDocumentoCompleto(anyString(), anyString(), isNull(), isNull(), isNull(), isNull(),
                eq(ciPaciente), eq(tenantId), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ObjectId().toHexString());
        doNothing().when(hcenClient).registrarMetadatos(any(DTMetadatos.class));

        Map<String, Object> result = documentoService.crearDocumentoCompletoConArchivo(
                tenantId, profesionalId, ciPaciente, contenido, null, null, null, null,
                null, null, null);

        assertNotNull(result);
        assertEquals("EVALUACION", result.get("tipoDocumento")); // Debe usar default
    }

    @Test
    void testCrearDocumentoCompletoWithNullAutor() throws Exception {
        Long tenantId = 1L;
        String profesionalId = "prof-1";
        String ciPaciente = "12345678";
        String contenido = "Contenido";

        UsuarioSalud paciente = new UsuarioSalud();
        paciente.setCi(ciPaciente);
        paciente.setTenantId(tenantId);

        ProfesionalSalud profesional = new ProfesionalSalud();
        profesional.setNickname(profesionalId);
        profesional.setNombre("Dr. Test");

        when(usuarioSaludRepository.findByCiAndTenant(ciPaciente, tenantId)).thenReturn(paciente);
        when(profesionalSaludRepository.findByNickname(profesionalId)).thenReturn(Optional.of(profesional));
        when(documentoRepository.guardarDocumentoCompleto(anyString(), anyString(), isNull(), isNull(), isNull(), isNull(),
                eq(ciPaciente), eq(tenantId), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ObjectId().toHexString());
        doNothing().when(hcenClient).registrarMetadatos(any(DTMetadatos.class));

        Map<String, Object> result = documentoService.crearDocumentoCompleto(
                tenantId, profesionalId, ciPaciente, contenido, "EVALUACION", null, null, null);

        assertNotNull(result);
        verify(hcenClient).registrarMetadatos(any(DTMetadatos.class));
    }

    @Test
    void testCrearDocumentoCompletoWithBlankTitulo() throws Exception {
        Long tenantId = 1L;
        String profesionalId = "prof-1";
        String ciPaciente = "12345678";
        String contenido = "Contenido";

        UsuarioSalud paciente = new UsuarioSalud();
        paciente.setCi(ciPaciente);
        paciente.setTenantId(tenantId);

        when(usuarioSaludRepository.findByCiAndTenant(ciPaciente, tenantId)).thenReturn(paciente);
        when(profesionalSaludRepository.findByNickname(profesionalId)).thenReturn(Optional.empty());
        when(documentoRepository.guardarDocumentoCompleto(anyString(), anyString(), isNull(), isNull(), isNull(), isNull(),
                eq(ciPaciente), eq(tenantId), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ObjectId().toHexString());
        doNothing().when(hcenClient).registrarMetadatos(any(DTMetadatos.class));

        Map<String, Object> result = documentoService.crearDocumentoCompleto(
                tenantId, profesionalId, ciPaciente, contenido, "EVALUACION", null, "   ", null);

        assertNotNull(result);
        // Debe usar el tipoDocumento como título cuando el título está en blank
    }

    @Test
    void testObtenerPdfWithIllegalArgumentException() {
        Document doc = new Document();
        doc.put("contenido", ""); // Sin contenido suficiente (vacío)
        
        when(documentoRepository.buscarPorId("doc-123", 1L)).thenReturn(doc);
        try (var mockedStatic = mockStatic(DocumentoPdfFactory.class)) {
            mockedStatic.when(() -> DocumentoPdfFactory.generarDesdeDocumento(doc))
                    .thenThrow(new IllegalArgumentException("El documento no contiene texto para generar el PDF"));
            
            byte[] result = documentoService.obtenerPdf("doc-123", 1L);
            
            assertNull(result);
        }
    }

    @Test
    void testObtenerPdfWithIOException() {
        String mongoId = "doc-123";
        Long tenantId = 1L;
        Document doc = new Document("contenido", "Contenido del documento");
        doc.append("titulo", "Título");
        doc.append("autor", "Autor");
        
        when(documentoRepository.buscarPorId(mongoId, tenantId)).thenReturn(doc);
        try (var mockedStatic = mockStatic(DocumentoPdfFactory.class)) {
            mockedStatic.when(() -> DocumentoPdfFactory.generarDesdeDocumento(doc))
                    .thenThrow(new java.io.IOException("Error de IO"));
            
            assertThrows(jakarta.ejb.EJBException.class, () -> {
                documentoService.obtenerPdf(mongoId, tenantId);
            });
        }
    }

    @Test
    void testObtenerPdfWithEmptyBinary() {
        String mongoId = "doc-123";
        Long tenantId = 1L;
        Document doc = new Document("pdfBytes", new Binary(new byte[0]));
        doc.append("contenido", "Contenido");
        doc.append("titulo", "Título");
        doc.append("autor", "Autor");
        byte[] pdfGenerado = "PDF generado".getBytes();
        
        when(documentoRepository.buscarPorId(mongoId, tenantId)).thenReturn(doc);
        try (var mockedStatic = mockStatic(DocumentoPdfFactory.class)) {
            mockedStatic.when(() -> DocumentoPdfFactory.generarDesdeDocumento(doc)).thenReturn(pdfGenerado);
            
            byte[] result = documentoService.obtenerPdf(mongoId, tenantId);
            
            assertNotNull(result);
            assertArrayEquals(pdfGenerado, result);
        }
    }

    @Test
    void testObtenerPdfWithNullBinaryField() {
        String mongoId = "doc-123";
        Long tenantId = 1L;
        Document doc = new Document();
        doc.append("pdfBytes", null); // Campo null, no Binary(null)
        doc.append("contenido", "Contenido");
        doc.append("titulo", "Título");
        doc.append("autor", "Autor");
        byte[] pdfGenerado = "PDF generado".getBytes();
        
        when(documentoRepository.buscarPorId(mongoId, tenantId)).thenReturn(doc);
        try (var mockedStatic = mockStatic(DocumentoPdfFactory.class)) {
            mockedStatic.when(() -> DocumentoPdfFactory.generarDesdeDocumento(doc)).thenReturn(pdfGenerado);
            
            byte[] result = documentoService.obtenerPdf(mongoId, tenantId);
            
            assertNotNull(result);
            assertArrayEquals(pdfGenerado, result);
        }
    }

    @Test
    void testObtenerArchivoAdjuntoWithEmptyBinary() {
        String mongoId = "doc-123";
        Long tenantId = 1L;
        Document doc = new Document("archivoAdjunto", new Binary(new byte[0]));
        doc.append("nombreArchivoAdjunto", "archivo.pdf");
        doc.append("tipoArchivoAdjunto", "application/pdf");
        
        when(documentoRepository.buscarPorId(mongoId, tenantId)).thenReturn(doc);
        
        Map<String, Object> result = documentoService.obtenerArchivoAdjunto(mongoId, tenantId);
        
        assertNull(result);
    }

    @Test
    void testObtenerArchivoAdjuntoWithNullBinaryField() {
        String mongoId = "doc-123";
        Long tenantId = 1L;
        Document doc = new Document();
        doc.append("archivoAdjunto", null); // Campo null, no Binary(null)
        doc.append("nombreArchivoAdjunto", "archivo.pdf");
        doc.append("tipoArchivoAdjunto", "application/pdf");
        
        when(documentoRepository.buscarPorId(mongoId, tenantId)).thenReturn(doc);
        
        Map<String, Object> result = documentoService.obtenerArchivoAdjunto(mongoId, tenantId);
        
        assertNull(result);
    }

    @Test
    void testObtenerArchivoAdjuntoWithNullNombreAndTipo() {
        String mongoId = "doc-123";
        Long tenantId = 1L;
        byte[] archivoBytes = "archivo".getBytes();
        Document doc = new Document("archivoAdjunto", new Binary(archivoBytes));
        doc.append("nombreArchivoAdjunto", null);
        doc.append("tipoArchivoAdjunto", null);
        
        when(documentoRepository.buscarPorId(mongoId, tenantId)).thenReturn(doc);
        
        Map<String, Object> result = documentoService.obtenerArchivoAdjunto(mongoId, tenantId);
        
        assertNotNull(result);
        assertArrayEquals(archivoBytes, (byte[]) result.get("bytes"));
        assertNull(result.get("nombre"));
        assertNull(result.get("tipo"));
    }

    @Test
    void testObtenerContenidoWithNullDocument() {
        String mongoId = "doc-123";
        Long tenantId = 1L;
        
        when(documentoRepository.buscarPorId(mongoId, tenantId)).thenReturn(null);
        
        String result = documentoService.obtenerContenido(mongoId, tenantId);
        
        assertNull(result);
    }

    @Test
    void testObtenerDocumentoPorIdWithNullResult() {
        String mongoId = "doc-123";
        Long tenantId = 1L;
        
        when(documentoRepository.buscarPorId(mongoId, tenantId)).thenReturn(null);
        
        Document result = documentoService.obtenerDocumentoPorId(mongoId, tenantId);
        
        assertNull(result);
        verify(documentoRepository).buscarPorId(mongoId, tenantId);
    }

    @Test
    void testObtenerContenidosPorPacienteWithMixedContent() {
        String ciPaciente = "12345678";
        Document doc1 = new Document();
        doc1.put("contenido", "Contenido válido 1");
        Document doc2 = new Document();
        doc2.put("contenido", null); // Filtrado
        Document doc3 = new Document();
        doc3.put("contenido", ""); // Filtrado
        Document doc4 = new Document();
        doc4.put("contenido", "   "); // Filtrado
        Document doc5 = new Document();
        doc5.put("contenido", "Contenido válido 2");
        
        when(documentoRepository.buscarPorCiPaciente(ciPaciente, null))
                .thenReturn(Arrays.asList(doc1, doc2, doc3, doc4, doc5));
        
        List<String> contenidos = documentoService.obtenerContenidosPorPaciente(ciPaciente);
        
        assertNotNull(contenidos);
        assertEquals(2, contenidos.size());
        assertTrue(contenidos.contains("Contenido válido 1"));
        assertTrue(contenidos.contains("Contenido válido 2"));
    }
}

