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
import uy.edu.tse.hcen.repository.DocumentoPdfRepository;
import uy.edu.tse.hcen.repository.ProfesionalSaludRepository;
import uy.edu.tse.hcen.repository.UsuarioSaludRepository;
import uy.edu.tse.hcen.model.ProfesionalSalud;
import uy.edu.tse.hcen.model.UsuarioSalud;
import uy.edu.tse.hcen.model.enums.Especialidad;
import uy.edu.tse.hcen.util.DocumentoPdfFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentoPdfServiceTest {

    @Mock
    private DocumentoPdfRepository documentoPdfRepository;

    @Mock
    private DocumentoClinicoRepository documentoClinicoRepository;

    @Mock
    private UsuarioSaludRepository usuarioSaludRepository;

    @Mock
    private ProfesionalSaludRepository profesionalSaludRepository;

    @Mock
    private HcenClient hcenClient;

    @InjectMocks
    private DocumentoPdfService documentoPdfService;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
    }

    @Test
    void testProcesarYGuardarPdf() throws Exception {
        Long tenantId = 1L;
        String profesionalId = "prof-1";
        String ciPaciente = "12345678";
        InputStream pdfStream = new ByteArrayInputStream("PDF content".getBytes());
        String tipoDocumento = "EVALUACION";
        String descripcion = "Descripción";

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
        when(documentoPdfRepository.guardarPdf(anyString(), any(byte[].class), eq(ciPaciente), eq(tenantId),
                eq(tipoDocumento), eq(descripcion), eq(profesionalId))).thenReturn(new ObjectId().toHexString());
        doNothing().when(hcenClient).registrarMetadatos(any(DTMetadatos.class));

        Map<String, Object> result = documentoPdfService.procesarYGuardarPdf(
                tenantId, profesionalId, ciPaciente, pdfStream, tipoDocumento, descripcion);

        assertNotNull(result);
        assertEquals(ciPaciente, result.get("ciPaciente"));
        assertEquals(tipoDocumento, result.get("tipoDocumento"));
        assertTrue((Boolean) result.get("sincronizado"));
        verify(hcenClient).registrarMetadatos(any(DTMetadatos.class));
    }

    @Test
    void testProcesarYGuardarPdfVacio() {
        InputStream pdfStream = new ByteArrayInputStream(new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> {
            documentoPdfService.procesarYGuardarPdf(1L, "prof-1", "12345678", pdfStream, "EVALUACION", null);
        });
    }

    @Test
    void testProcesarYGuardarPdfPacienteNoEncontrado() {
        InputStream pdfStream = new ByteArrayInputStream("PDF content".getBytes());
        when(usuarioSaludRepository.findByCiAndTenant("12345678", 1L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> {
            documentoPdfService.procesarYGuardarPdf(1L, "prof-1", "12345678", pdfStream, "EVALUACION", null);
        });
    }

    @Test
    void testObtenerMetadataPorId() {
        String mongoId = new ObjectId().toHexString();
        Long tenantId = 1L;
        Document doc = new Document("ciPaciente", "12345678");
        doc.append("tipoDocumento", "EVALUACION");
        doc.append("profesionalId", "prof-1");

        when(documentoPdfRepository.buscarPorId(mongoId, tenantId)).thenReturn(doc);

        Map<String, Object> result = documentoPdfService.obtenerMetadataPorId(mongoId, tenantId);

        assertNotNull(result);
        assertEquals("12345678", result.get("ciPaciente"));
        assertEquals("EVALUACION", result.get("tipoDocumento"));
        assertEquals(tenantId, result.get("tenantId"));
    }

    @Test
    void testObtenerMetadataPorIdEnDocumentoClinico() {
        String mongoId = new ObjectId().toHexString();
        Long tenantId = 1L;
        Document doc = new Document("ciPaciente", "12345678");
        doc.append("tipoDocumento", "EVALUACION");
        doc.append("profesionalId", "prof-1");

        when(documentoPdfRepository.buscarPorId(mongoId, tenantId)).thenReturn(null);
        when(documentoClinicoRepository.buscarPorId(mongoId, tenantId)).thenReturn(doc);

        Map<String, Object> result = documentoPdfService.obtenerMetadataPorId(mongoId, tenantId);

        assertNotNull(result);
        assertEquals("12345678", result.get("ciPaciente"));
    }

    @Test
    void testObtenerMetadataPorIdNoExiste() {
        String mongoId = new ObjectId().toHexString();
        Long tenantId = 1L;

        when(documentoPdfRepository.buscarPorId(mongoId, tenantId)).thenReturn(null);
        when(documentoClinicoRepository.buscarPorId(mongoId, tenantId)).thenReturn(null);

        Map<String, Object> result = documentoPdfService.obtenerMetadataPorId(mongoId, tenantId);

        assertNull(result);
    }

    @Test
    void testObtenerPdfPorId() {
        String mongoId = new ObjectId().toHexString();
        Long tenantId = 1L;
        byte[] pdfBytes = "%PDF content".getBytes();
        Document doc = new Document("pdfBytes", new Binary(pdfBytes));

        when(documentoPdfRepository.buscarPorId(mongoId, tenantId)).thenReturn(doc);

        byte[] result = documentoPdfService.obtenerPdfPorId(mongoId, tenantId);

        assertNotNull(result);
        assertArrayEquals(pdfBytes, result);
    }

    @Test
    void testObtenerPdfPorIdGenerarOnDemand() throws IOException {
        String mongoId = new ObjectId().toHexString();
        Long tenantId = 1L;
        Document doc = new Document("contenido", "Contenido del documento");
        doc.append("titulo", "Título");
        doc.append("autor", "Autor");
        doc.append("ciPaciente", "12345678");
        byte[] pdfGenerado = "%PDF generado".getBytes();

        when(documentoPdfRepository.buscarPorId(mongoId, tenantId)).thenReturn(null);
        when(documentoClinicoRepository.buscarPorId(mongoId, tenantId)).thenReturn(doc);
        try (var mockedStatic = mockStatic(DocumentoPdfFactory.class)) {
            mockedStatic.when(() -> DocumentoPdfFactory.generarDesdeDocumento(doc)).thenReturn(pdfGenerado);

            byte[] result = documentoPdfService.obtenerPdfPorId(mongoId, tenantId);

            assertNotNull(result);
            assertArrayEquals(pdfGenerado, result);
        }
    }

    @Test
    void testObtenerPdfPorIdNoExiste() {
        String mongoId = new ObjectId().toHexString();
        Long tenantId = 1L;

        when(documentoPdfRepository.buscarPorId(mongoId, tenantId)).thenReturn(null);
        when(documentoClinicoRepository.buscarPorId(mongoId, tenantId)).thenReturn(null);

        byte[] result = documentoPdfService.obtenerPdfPorId(mongoId, tenantId);

        assertNull(result);
    }

    @Test
    void testListarDocumentosPorPaciente() throws HcenUnavailableException {
        String ciPaciente = "12345678";
        String profesionalId = "prof-1";
        String tenantIdProfesional = "1";

        ProfesionalSalud profesional = new ProfesionalSalud();
        profesional.setNickname(profesionalId);
        profesional.setNombre("Dr. Test");
        profesional.setEspecialidad(Especialidad.MEDICINA_GENERAL);

        Map<String, Object> metadata1 = new java.util.HashMap<>();
        metadata1.put("id", "doc1");
        metadata1.put("documentoId", "doc-uuid-1");
        metadata1.put("tenantId", "1");
        metadata1.put("fechaCreacion", "2024-01-01");
        metadata1.put("tipoDocumento", "EVALUACION");
        metadata1.put("descripcion", "Descripción");
        metadata1.put("profesionalSalud", "prof-1");
        metadata1.put("nombrePaciente", "Juan");
        metadata1.put("apellidoPaciente", "Pérez");
        metadata1.put("uriDocumento", "http://example.com/doc1");

        List<Map<String, Object>> metadatos = new ArrayList<>();
        metadatos.add(metadata1);

        when(profesionalSaludRepository.findByNickname(profesionalId)).thenReturn(Optional.of(profesional));
        when(hcenClient.obtenerMetadatosDocumentosPorCI(eq(ciPaciente), eq(profesionalId), eq(tenantIdProfesional),
                eq("MEDICINA_GENERAL"), eq("Dr. Test"))).thenReturn(metadatos);

        List<Map<String, Object>> result = documentoPdfService.listarDocumentosPorPaciente(
                ciPaciente, profesionalId, tenantIdProfesional);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("doc1", result.get(0).get("id"));
        assertEquals(ciPaciente, result.get(0).get("ciPaciente"));
    }

    @Test
    void testListarDocumentosPorPacienteHcenUnavailable() throws HcenUnavailableException {
        String ciPaciente = "12345678";
        String profesionalId = "prof-1";
        String tenantIdProfesional = "1";

        when(profesionalSaludRepository.findByNickname(profesionalId)).thenReturn(Optional.empty());
        when(hcenClient.obtenerMetadatosDocumentosPorCI(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new HcenUnavailableException("HCEN no disponible"));

        List<Map<String, Object>> result = documentoPdfService.listarDocumentosPorPaciente(
                ciPaciente, profesionalId, tenantIdProfesional);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testProcesarYGuardarPdfWithHcenUnavailable() throws Exception {
        Long tenantId = 1L;
        String profesionalId = "prof-1";
        String ciPaciente = "12345678";
        InputStream pdfStream = new ByteArrayInputStream("PDF content".getBytes());

        UsuarioSalud paciente = new UsuarioSalud();
        paciente.setCi(ciPaciente);
        paciente.setTenantId(tenantId);

        when(usuarioSaludRepository.findByCiAndTenant(ciPaciente, tenantId)).thenReturn(paciente);
        when(profesionalSaludRepository.findByNickname(profesionalId)).thenReturn(Optional.empty());
        when(documentoPdfRepository.guardarPdf(anyString(), any(byte[].class), eq(ciPaciente), eq(tenantId),
                anyString(), anyString(), eq(profesionalId))).thenReturn(new ObjectId().toHexString());
        doThrow(new HcenUnavailableException("HCEN no disponible")).when(hcenClient).registrarMetadatos(any(DTMetadatos.class));

        Map<String, Object> result = documentoPdfService.procesarYGuardarPdf(
                tenantId, profesionalId, ciPaciente, pdfStream, "EVALUACION", null);

        assertNotNull(result);
        assertFalse((Boolean) result.get("sincronizado"));
    }

    @Test
    void testProcesarYGuardarPdfWithProfesional() throws Exception {
        Long tenantId = 1L;
        String profesionalId = "prof-1";
        String ciPaciente = "12345678";
        InputStream pdfStream = new ByteArrayInputStream("PDF content".getBytes());

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
        when(documentoPdfRepository.guardarPdf(anyString(), any(byte[].class), eq(ciPaciente), eq(tenantId),
                anyString(), anyString(), eq(profesionalId))).thenReturn(new ObjectId().toHexString());
        doNothing().when(hcenClient).registrarMetadatos(any(DTMetadatos.class));

        Map<String, Object> result = documentoPdfService.procesarYGuardarPdf(
                tenantId, profesionalId, ciPaciente, pdfStream, "EVALUACION", null);

        assertNotNull(result);
        verify(hcenClient).registrarMetadatos(any(DTMetadatos.class));
    }

    @Test
    void testObtenerMetadataPorIdWithNullDocument() {
        String mongoId = new ObjectId().toHexString();
        Long tenantId = 1L;

        when(documentoPdfRepository.buscarPorId(mongoId, tenantId)).thenReturn(null);
        when(documentoClinicoRepository.buscarPorId(mongoId, tenantId)).thenReturn(null);

        Map<String, Object> result = documentoPdfService.obtenerMetadataPorId(mongoId, tenantId);

        assertNull(result);
    }

    @Test
    void testObtenerPdfPorIdWithNullPdfBytes() {
        String mongoId = new ObjectId().toHexString();
        Long tenantId = 1L;
        Document doc = new Document(); // Sin pdfBytes

        when(documentoPdfRepository.buscarPorId(mongoId, tenantId)).thenReturn(doc);

        byte[] result = documentoPdfService.obtenerPdfPorId(mongoId, tenantId);

        assertNull(result);
    }

    @Test
    void testListarDocumentosPorPacienteWithProfesional() throws HcenUnavailableException {
        String ciPaciente = "12345678";
        String profesionalId = "prof-1";
        String tenantIdProfesional = "1";

        ProfesionalSalud profesional = new ProfesionalSalud();
        profesional.setNickname(profesionalId);
        profesional.setNombre("Dr. Test");
        profesional.setEspecialidad(Especialidad.CARDIOLOGIA);

        List<Map<String, Object>> metadatos = new ArrayList<>();

        when(profesionalSaludRepository.findByNickname(profesionalId)).thenReturn(Optional.of(profesional));
        when(hcenClient.obtenerMetadatosDocumentosPorCI(eq(ciPaciente), eq(profesionalId), eq(tenantIdProfesional),
                eq("CARDIOLOGIA"), eq("Dr. Test"))).thenReturn(metadatos);

        List<Map<String, Object>> result = documentoPdfService.listarDocumentosPorPaciente(
                ciPaciente, profesionalId, tenantIdProfesional);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testListarDocumentosPorPacienteWithNullProfesional() throws HcenUnavailableException {
        String ciPaciente = "12345678";
        String profesionalId = "prof-1";
        String tenantIdProfesional = "1";

        when(profesionalSaludRepository.findByNickname(profesionalId)).thenReturn(Optional.empty());
        when(hcenClient.obtenerMetadatosDocumentosPorCI(eq(ciPaciente), eq(profesionalId), eq(tenantIdProfesional),
                isNull(), isNull())).thenReturn(new ArrayList<>());

        List<Map<String, Object>> result = documentoPdfService.listarDocumentosPorPaciente(
                ciPaciente, profesionalId, tenantIdProfesional);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}

