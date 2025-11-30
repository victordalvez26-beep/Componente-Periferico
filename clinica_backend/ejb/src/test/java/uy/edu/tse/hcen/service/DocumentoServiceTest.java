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

import java.io.IOException;
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

        assertThrows(IllegalArgumentException.class, () -> {
            documentoService.crearDocumentoCompleto(1L, "prof-1", "12345678", "Contenido", "EVALUACION", null, null, null);
        });
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
    void testObtenerPdfGenerarOnDemand() throws IOException {
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
        assertThrows(IllegalArgumentException.class, () -> {
            documentoService.crearDocumentoCompletoConArchivo(
                    1L, "prof-1", "12345678", "", "EVALUACION", null, null, null,
                    "archivo".getBytes(), "archivo.pdf", "application/pdf");
        });
    }

    @Test
    void testObtenerPdfWithNullDocument() {
        String mongoId = new ObjectId().toHexString();
        Long tenantId = 1L;

        when(documentoRepository.buscarPorId(mongoId, tenantId)).thenReturn(null);

        byte[] result = documentoService.obtenerPdf(mongoId, tenantId);

        assertNull(result);
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
}

