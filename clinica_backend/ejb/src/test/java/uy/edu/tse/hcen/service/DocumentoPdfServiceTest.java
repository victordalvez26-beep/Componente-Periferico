package uy.edu.tse.hcen.service;

import org.bson.Document;
import org.bson.types.Binary;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uy.edu.tse.hcen.dto.DTMetadatos;
import uy.edu.tse.hcen.exceptions.HcenUnavailableException;
import uy.edu.tse.hcen.model.ProfesionalSalud;
import uy.edu.tse.hcen.model.UsuarioSalud;
import uy.edu.tse.hcen.repository.DocumentoClinicoRepository;
import uy.edu.tse.hcen.repository.DocumentoPdfRepository;
import uy.edu.tse.hcen.repository.ProfesionalSaludRepository;
import uy.edu.tse.hcen.repository.UsuarioSaludRepository;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DocumentoPdfService.
 * Tests PDF processing, MongoDB storage, metadata generation, and HCEN synchronization.
 * 
 * @author Senior Test Engineer
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentoPdfService Tests")
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
    private DocumentoPdfService service;

    private static final Long TENANT_ID = 101L;
    private static final String PROFESIONAL_ID = "doctor1";
    private static final String CI_PACIENTE = "12345678";
    private static final String TIPO_DOC = "EVALUACION";
    private static final byte[] VALID_PDF_BYTES = "%PDF-1.4\n test content here".getBytes();

    private UsuarioSalud createTestPaciente() {
        UsuarioSalud paciente = new UsuarioSalud();
        paciente.setId(1L);
        paciente.setCi(CI_PACIENTE);
        paciente.setNombre("Juan");
        paciente.setApellido("Pérez");
        paciente.setFechaNacimiento(LocalDate.of(1990, 1, 15));
        paciente.setTenantId(TENANT_ID);
        return paciente;
    }

    private ProfesionalSalud createTestProfesional() {
        ProfesionalSalud prof = new ProfesionalSalud();
        prof.setId(1L);
        prof.setNickname(PROFESIONAL_ID);
        prof.setNombre("Dr. Juan Pérez");
        return prof;
    }

    // ==================== PROCESAR Y GUARDAR PDF TESTS ====================

    @Nested
    @DisplayName("Procesar Y Guardar PDF Tests")
    @MockitoSettings(strictness = Strictness.LENIENT)
    class ProcesarYGuardarTests {

        @Test
        @DisplayName("Procesar PDF válido debe guardar y sincronizar exitosamente")
        void procesarPdf_validPdf_shouldSaveAndSync() throws Exception {
            // Arrange
            UsuarioSalud paciente = createTestPaciente();
            ProfesionalSalud profesional = createTestProfesional();
            
            when(usuarioSaludRepository.findByCiAndTenant(CI_PACIENTE, TENANT_ID))
                    .thenReturn(paciente);
            when(profesionalSaludRepository.findByNickname(PROFESIONAL_ID))
                    .thenReturn(Optional.of(profesional));
            when(documentoPdfRepository.guardarPdf(anyString(), any(byte[].class), eq(CI_PACIENTE),
                    eq(TENANT_ID), eq(TIPO_DOC), any(), eq(PROFESIONAL_ID)))
                    .thenReturn("mongo123");
            doNothing().when(hcenClient).registrarMetadatos(any(DTMetadatos.class));
            
            InputStream pdfStream = new ByteArrayInputStream(VALID_PDF_BYTES);

            // Act
            Map<String, Object> result = service.procesarYGuardarPdf(
                    TENANT_ID, PROFESIONAL_ID, CI_PACIENTE, pdfStream, TIPO_DOC, "Test desc");

            // Assert
            assertNotNull(result);
            assertTrue(result.containsKey("documentoId"));
            assertTrue(result.containsKey("mongoId"));
            assertEquals("mongo123", result.get("mongoId"));
            assertEquals(CI_PACIENTE, result.get("ciPaciente"));
            assertTrue(result.containsKey("urlAcceso"));
            
            verify(usuarioSaludRepository).findByCiAndTenant(CI_PACIENTE, TENANT_ID);
            verify(profesionalSaludRepository).findByNickname(PROFESIONAL_ID);
            verify(documentoPdfRepository).guardarPdf(anyString(), any(byte[].class), eq(CI_PACIENTE),
                    eq(TENANT_ID), eq(TIPO_DOC), anyString(), eq(PROFESIONAL_ID));
            verify(hcenClient).registrarMetadatos(any(DTMetadatos.class));
        }

        @Test
        @DisplayName("Procesar PDF vacío debe lanzar IllegalArgumentException")
        void procesarPdf_emptyPdf_shouldThrowException() {
            // Arrange
            InputStream emptyStream = new ByteArrayInputStream(new byte[0]);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.procesarYGuardarPdf(
                            TENANT_ID, PROFESIONAL_ID, CI_PACIENTE, emptyStream, TIPO_DOC, null)
            );
            
            assertTrue(exception.getMessage().contains("vacío"));
            
            verify(documentoPdfRepository, never()).guardarPdf(anyString(), any(), anyString(),
                    any(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Procesar PDF con paciente inexistente debe lanzar excepción")
        void procesarPdf_patientNotFound_shouldThrowException() {
            // Arrange
            when(usuarioSaludRepository.findByCiAndTenant(CI_PACIENTE, TENANT_ID))
                    .thenReturn(null);
            
            InputStream pdfStream = new ByteArrayInputStream(VALID_PDF_BYTES);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.procesarYGuardarPdf(
                            TENANT_ID, PROFESIONAL_ID, CI_PACIENTE, pdfStream, TIPO_DOC, null)
            );
            
            assertTrue(exception.getMessage().contains("Paciente no encontrado"));
            assertTrue(exception.getMessage().contains(CI_PACIENTE));
            
            verify(documentoPdfRepository, never()).guardarPdf(anyString(), any(), anyString(),
                    any(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Procesar PDF cuando HCEN falla debe continuar y guardar localmente")
        void procesarPdf_hcenFails_shouldContinue() throws Exception {
            // Arrange
            UsuarioSalud paciente = createTestPaciente();
            ProfesionalSalud profesional = createTestProfesional();
            
            when(usuarioSaludRepository.findByCiAndTenant(CI_PACIENTE, TENANT_ID))
                    .thenReturn(paciente);
            when(profesionalSaludRepository.findByNickname(PROFESIONAL_ID))
                    .thenReturn(Optional.of(profesional));
            when(documentoPdfRepository.guardarPdf(anyString(), any(byte[].class), anyString(),
                    any(), anyString(), anyString(), anyString()))
                    .thenReturn("mongo123");
            
            doThrow(new HcenUnavailableException("HCEN timeout"))
                    .when(hcenClient).registrarMetadatos(any(DTMetadatos.class));
            
            InputStream pdfStream = new ByteArrayInputStream(VALID_PDF_BYTES);

            // Act - No debe lanzar excepción
            Map<String, Object> result = service.procesarYGuardarPdf(
                    TENANT_ID, PROFESIONAL_ID, CI_PACIENTE, pdfStream, TIPO_DOC, "Desc");

            // Assert - Documento guardado aunque HCEN falle
            assertNotNull(result);
            assertEquals("mongo123", result.get("mongoId"));
            
            verify(documentoPdfRepository).guardarPdf(anyString(), any(byte[].class), anyString(),
                    any(), anyString(), anyString(), anyString());
            verify(hcenClient).registrarMetadatos(any(DTMetadatos.class));
        }

        @Test
        @DisplayName("Procesar PDF sin profesional debe usar profesionalId como nombre")
        void procesarPdf_noProfessional_shouldUseProfesionalId() throws Exception {
            // Arrange
            UsuarioSalud paciente = createTestPaciente();
            
            when(usuarioSaludRepository.findByCiAndTenant(CI_PACIENTE, TENANT_ID))
                    .thenReturn(paciente);
            when(profesionalSaludRepository.findByNickname(PROFESIONAL_ID))
                    .thenReturn(Optional.empty());
            when(documentoPdfRepository.guardarPdf(anyString(), any(byte[].class), anyString(),
                    any(), anyString(), anyString(), anyString()))
                    .thenReturn("mongo123");
            
            InputStream pdfStream = new ByteArrayInputStream(VALID_PDF_BYTES);

            // Act
            Map<String, Object> result = service.procesarYGuardarPdf(
                    TENANT_ID, PROFESIONAL_ID, CI_PACIENTE, pdfStream, TIPO_DOC, null);

            // Assert
            assertNotNull(result);
            verify(documentoPdfRepository).guardarPdf(anyString(), any(byte[].class), eq(CI_PACIENTE),
                    eq(TENANT_ID), eq(TIPO_DOC), isNull(), eq(PROFESIONAL_ID));
        }

        @Test
        @DisplayName("Procesar PDF con stream null debe lanzar excepción")
        void procesarPdf_nullStream_shouldThrowException() {
            // Act & Assert
            assertThrows(Exception.class, () ->
                    service.procesarYGuardarPdf(
                            TENANT_ID, PROFESIONAL_ID, CI_PACIENTE, null, TIPO_DOC, null)
            );
        }

        @Test
        @DisplayName("Procesar PDF debe generar metadata con campos correctos")
        void procesarPdf_shouldGenerateCorrectMetadata() throws Exception {
            // Arrange
            UsuarioSalud paciente = createTestPaciente();
            ProfesionalSalud profesional = createTestProfesional();
            
            when(usuarioSaludRepository.findByCiAndTenant(CI_PACIENTE, TENANT_ID))
                    .thenReturn(paciente);
            when(profesionalSaludRepository.findByNickname(PROFESIONAL_ID))
                    .thenReturn(Optional.of(profesional));
            when(documentoPdfRepository.guardarPdf(anyString(), any(byte[].class), anyString(),
                    any(), anyString(), anyString(), anyString()))
                    .thenReturn("mongo123");
            
            InputStream pdfStream = new ByteArrayInputStream(VALID_PDF_BYTES);

            // Act
            Map<String, Object> result = service.procesarYGuardarPdf(
                    TENANT_ID, PROFESIONAL_ID, CI_PACIENTE, pdfStream, TIPO_DOC, "Descripción test");

            // Assert
            assertNotNull(result);
            assertTrue(result.containsKey("documentoId"));
            assertTrue(result.containsKey("tipoDocumento"));
            assertEquals(TIPO_DOC, result.get("tipoDocumento"));
            assertTrue(result.containsKey("urlAcceso"));
            
            // Verificar que se llamó a registrarMetadatos con DTMetadatos correctos
            verify(hcenClient).registrarMetadatos(argThat(metadata ->
                    metadata.getDocumentoIdPaciente().equals(CI_PACIENTE) &&
                    metadata.getTenantId().equals(String.valueOf(TENANT_ID)) &&
                    metadata.getTipoDocumento().equals(TIPO_DOC)
            ));
        }
    }

    // ==================== OBTENER METADATA POR ID TESTS ====================

    @Nested
    @DisplayName("Obtener Metadata Por ID Tests")
    class ObtenerMetadataTests {

        @Test
        @DisplayName("Obtener metadata existente en DocumentoPdfRepository")
        void getMetadata_existsInPdfRepo_shouldReturn() {
            // Arrange
            String mongoId = "507f1f77bcf86cd799439011";
            Document doc = new Document();
            doc.append("ciPaciente", CI_PACIENTE);
            doc.append("tipoDocumento", TIPO_DOC);
            doc.append("profesionalId", PROFESIONAL_ID);
            doc.append("tenantId", TENANT_ID);
            
            when(documentoPdfRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(doc);

            // Act
            Map<String, Object> result = service.obtenerMetadataPorId(mongoId, TENANT_ID);

            // Assert
            assertNotNull(result);
            assertEquals(mongoId, result.get("id"));
            assertEquals(CI_PACIENTE, result.get("ciPaciente"));
            assertEquals(TIPO_DOC, result.get("tipoDocumento"));
            assertEquals(TENANT_ID, result.get("tenantId"));
            
            verify(documentoPdfRepository).buscarPorId(mongoId, TENANT_ID);
            verify(documentoClinicoRepository, never()).buscarPorId(anyString(), any());
        }

        @Test
        @DisplayName("Obtener metadata no encontrada debe retornar null")
        void getMetadata_notFound_shouldReturnNull() {
            // Arrange
            String mongoId = "507f1f77bcf86cd799439011";
            
            when(documentoPdfRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(null);
            when(documentoClinicoRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(null);
            when(documentoPdfRepository.buscarPorId(mongoId, null)).thenReturn(null);
            when(documentoClinicoRepository.buscarPorId(mongoId, null)).thenReturn(null);

            // Act
            Map<String, Object> result = service.obtenerMetadataPorId(mongoId, TENANT_ID);

            // Assert
            assertNull(result);
        }

        @Test
        @DisplayName("Obtener metadata debe buscar en DocumentoClinico si no está en Pdf")
        void getMetadata_notInPdfRepo_shouldSearchInClinico() {
            // Arrange
            String mongoId = "507f1f77bcf86cd799439011";
            Document doc = new Document();
            doc.append("ciPaciente", CI_PACIENTE);
            doc.append("tipoDocumento", TIPO_DOC);
            doc.append("tenantId", TENANT_ID);
            
            when(documentoPdfRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(null);
            when(documentoClinicoRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(doc);

            // Act
            Map<String, Object> result = service.obtenerMetadataPorId(mongoId, TENANT_ID);

            // Assert
            assertNotNull(result);
            assertEquals(CI_PACIENTE, result.get("ciPaciente"));
            
            verify(documentoPdfRepository).buscarPorId(mongoId, TENANT_ID);
            verify(documentoClinicoRepository).buscarPorId(mongoId, TENANT_ID);
        }

        @Test
        @DisplayName("Obtener metadata debe hacer fallback sin tenant si no encuentra")
        void getMetadata_notFoundWithTenant_shouldFallbackWithoutTenant() {
            // Arrange
            String mongoId = "507f1f77bcf86cd799439011";
            Document doc = new Document();
            doc.append("ciPaciente", CI_PACIENTE);
            doc.append("tenantId", 102L); // Otro tenant
            
            when(documentoPdfRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(null);
            when(documentoClinicoRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(null);
            when(documentoPdfRepository.buscarPorId(mongoId, null)).thenReturn(doc);

            // Act
            Map<String, Object> result = service.obtenerMetadataPorId(mongoId, TENANT_ID);

            // Assert
            assertNotNull(result);
            verify(documentoPdfRepository).buscarPorId(mongoId, null); // Fallback
        }

        @Test
        @DisplayName("Obtener metadata con mongoId null debe retornar null")
        void getMetadata_nullMongoId_shouldReturnNull() {
            // Act
            Map<String, Object> result = service.obtenerMetadataPorId(null, TENANT_ID);

            // Assert
            assertNull(result);
            verify(documentoPdfRepository, never()).buscarPorId(anyString(), any());
        }
    }

    // ==================== OBTENER PDF POR ID TESTS ====================

    @Nested
    @DisplayName("Obtener PDF Por ID Tests")
    class ObtenerPdfTests {

        @Test
        @DisplayName("Obtener PDF con bytes existentes debe retornar bytes")
        void getPdf_withBytes_shouldReturn() {
            // Arrange
            String mongoId = "507f1f77bcf86cd799439011";
            Document doc = new Document();
            doc.append("pdfBytes", new Binary(VALID_PDF_BYTES));
            
            when(documentoPdfRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(doc);

            // Act
            byte[] result = service.obtenerPdfPorId(mongoId, TENANT_ID);

            // Assert
            assertNotNull(result);
            assertTrue(result.length > 0);
            assertTrue(new String(result).startsWith("%PDF"));
            
            verify(documentoPdfRepository).buscarPorId(mongoId, TENANT_ID);
        }

        @Test
        @DisplayName("Obtener PDF inexistente debe retornar null")
        void getPdf_notFound_shouldReturnNull() {
            // Arrange
            String mongoId = "507f1f77bcf86cd799439011";
            
            when(documentoPdfRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(null);
            when(documentoClinicoRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(null);
            when(documentoPdfRepository.buscarPorId(mongoId, null)).thenReturn(null);
            when(documentoClinicoRepository.buscarPorId(mongoId, null)).thenReturn(null);

            // Act
            byte[] result = service.obtenerPdfPorId(mongoId, TENANT_ID);

            // Assert
            assertNull(result);
        }

        @Test
        @DisplayName("Obtener PDF debe buscar en DocumentoClinico si no está en Pdf")
        void getPdf_notInPdfRepo_shouldSearchInClinico() {
            // Arrange
            String mongoId = "507f1f77bcf86cd799439011";
            Document doc = new Document();
            doc.append("pdf", new Binary(VALID_PDF_BYTES)); // Campo 'pdf' en lugar de 'pdfBytes'
            
            when(documentoPdfRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(null);
            when(documentoClinicoRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(doc);

            // Act
            byte[] result = service.obtenerPdfPorId(mongoId, TENANT_ID);

            // Assert
            assertNotNull(result);
            assertTrue(result.length > 0);
            
            verify(documentoClinicoRepository).buscarPorId(mongoId, TENANT_ID);
        }

        @Test
        @DisplayName("Obtener PDF con header inválido debe retornar bytes igual")
        void getPdf_invalidHeader_shouldReturnBytes() {
            // Arrange
            String mongoId = "507f1f77bcf86cd799439011";
            byte[] invalidBytes = "NOT A PDF CONTENT".getBytes();
            Document doc = new Document();
            doc.append("pdfBytes", new Binary(invalidBytes));
            
            when(documentoPdfRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(doc);

            // Act
            byte[] result = service.obtenerPdfPorId(mongoId, TENANT_ID);

            // Assert
            assertNotNull(result);
            assertEquals("NOT A PDF CONTENT", new String(result));
        }

        @Test
        @DisplayName("Obtener PDF con mongoId null debe retornar null")
        void getPdf_nullMongoId_shouldReturnNull() {
            // Act
            byte[] result = service.obtenerPdfPorId(null, TENANT_ID);

            // Assert
            assertNull(result);
            verify(documentoPdfRepository, never()).buscarPorId(anyString(), any());
        }

        @Test
        @DisplayName("Obtener PDF debe hacer fallback sin tenant")
        void getPdf_shouldFallbackWithoutTenant() {
            // Arrange
            String mongoId = "507f1f77bcf86cd799439011";
            Document doc = new Document();
            doc.append("pdfBytes", new Binary(VALID_PDF_BYTES));
            doc.append("tenantId", 102L);
            
            when(documentoPdfRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(null);
            when(documentoClinicoRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(null);
            when(documentoPdfRepository.buscarPorId(mongoId, null)).thenReturn(doc);

            // Act
            byte[] result = service.obtenerPdfPorId(mongoId, TENANT_ID);

            // Assert
            assertNotNull(result);
            verify(documentoPdfRepository).buscarPorId(mongoId, null);
        }
    }

    // ==================== EDGE CASES ====================

    @Nested
    @DisplayName("Edge Cases Tests")
    @MockitoSettings(strictness = Strictness.LENIENT)
    class EdgeCaseTests {

        @Test
        @DisplayName("Procesar PDF con descripción null debe usar descripción por defecto")
        void procesarPdf_nullDescription_shouldUseDefault() throws Exception {
            // Arrange
            UsuarioSalud paciente = createTestPaciente();
            
            when(usuarioSaludRepository.findByCiAndTenant(CI_PACIENTE, TENANT_ID))
                    .thenReturn(paciente);
            when(profesionalSaludRepository.findByNickname(PROFESIONAL_ID))
                    .thenReturn(Optional.empty());
            when(documentoPdfRepository.guardarPdf(anyString(), any(byte[].class), anyString(),
                    any(), anyString(), anyString(), anyString()))
                    .thenReturn("mongo123");
            
            InputStream pdfStream = new ByteArrayInputStream(VALID_PDF_BYTES);

            // Act
            Map<String, Object> result = service.procesarYGuardarPdf(
                    TENANT_ID, PROFESIONAL_ID, CI_PACIENTE, pdfStream, TIPO_DOC, null);

            // Assert
            assertNotNull(result);
            verify(hcenClient).registrarMetadatos(argThat(metadata ->
                    metadata.getDescripcion() != null &&
                    metadata.getDescripcion().contains("componente periférico")
            ));
        }

        @Test
        @DisplayName("Obtener metadata con tenantId null debe buscar sin filtro")
        void getMetadata_nullTenant_shouldSearchWithoutFilter() {
            // Arrange
            String mongoId = "507f1f77bcf86cd799439011";
            Document doc = new Document();
            doc.append("ciPaciente", CI_PACIENTE);
            
            when(documentoPdfRepository.buscarPorId(mongoId, null)).thenReturn(doc);

            // Act
            Map<String, Object> result = service.obtenerMetadataPorId(mongoId, null);

            // Assert
            assertNotNull(result);
            verify(documentoPdfRepository).buscarPorId(mongoId, null);
        }

        @Test
        @DisplayName("Obtener PDF con tenantId null debe funcionar")
        void getPdf_nullTenant_shouldWork() {
            // Arrange
            String mongoId = "507f1f77bcf86cd799439011";
            
            when(documentoPdfRepository.buscarPorId(mongoId, null)).thenReturn(null);

            // Act
            byte[] result = service.obtenerPdfPorId(mongoId, null);

            // Assert
            assertNull(result);
            verify(documentoPdfRepository).buscarPorId(mongoId, null);
        }

        @Test
        @DisplayName("Procesar PDF con diferentes tipos")
        void procesarPdf_differentTypes_shouldWork() throws Exception {
            // Arrange
            UsuarioSalud paciente = createTestPaciente();
            
            when(usuarioSaludRepository.findByCiAndTenant(CI_PACIENTE, TENANT_ID))
                    .thenReturn(paciente);
            when(profesionalSaludRepository.findByNickname(PROFESIONAL_ID))
                    .thenReturn(Optional.empty());
            when(documentoPdfRepository.guardarPdf(anyString(), any(byte[].class), anyString(),
                    any(), anyString(), anyString(), anyString()))
                    .thenReturn("mongo123");

            String[] tipos = {"EVALUACION", "RECETA", "ESTUDIO"};

            // Act & Assert
            for (String tipo : tipos) {
                InputStream pdfStream = new ByteArrayInputStream(VALID_PDF_BYTES);
                Map<String, Object> result = service.procesarYGuardarPdf(
                        TENANT_ID, PROFESIONAL_ID, CI_PACIENTE, pdfStream, tipo, "Desc");
                assertNotNull(result);
            }
        }

        @Test
        @DisplayName("Procesar PDF grande")
        void procesarPdf_largePdf_shouldWork() throws Exception {
            // Arrange
            UsuarioSalud paciente = createTestPaciente();
            byte[] largePdf = new byte[1024 * 512]; // 512KB
            
            when(usuarioSaludRepository.findByCiAndTenant(CI_PACIENTE, TENANT_ID))
                    .thenReturn(paciente);
            when(profesionalSaludRepository.findByNickname(PROFESIONAL_ID))
                    .thenReturn(Optional.empty());
            when(documentoPdfRepository.guardarPdf(anyString(), any(byte[].class), anyString(),
                    any(), anyString(), anyString(), anyString()))
                    .thenReturn("mongo123");
            
            InputStream pdfStream = new ByteArrayInputStream(largePdf);

            // Act
            Map<String, Object> result = service.procesarYGuardarPdf(
                    TENANT_ID, PROFESIONAL_ID, CI_PACIENTE, pdfStream, TIPO_DOC, null);

            // Assert
            assertNotNull(result);
        }

        @Test
        @DisplayName("Debe manejar IOException al leer InputStream")
        void procesarPdf_ioException_throws() throws Exception {
            // Arrange
            InputStream badStream = mock(InputStream.class);
            when(badStream.read(any(byte[].class), anyInt(), anyInt()))
                    .thenThrow(new java.io.IOException("Read error"));

            // Act & Assert
            assertThrows(Exception.class, () ->
                    service.procesarYGuardarPdf(
                            TENANT_ID, PROFESIONAL_ID, CI_PACIENTE, badStream, TIPO_DOC, null));
        }
    }

    // ==================== CASOS ADICIONALES EXHAUSTIVOS ====================

    @Nested
    @DisplayName("ObtenerMetadata - Casos Adicionales")
    @MockitoSettings(strictness = Strictness.LENIENT)
    class ObtenerMetadataCasosAdicionalesTests {

        @Test
        @DisplayName("Debe buscar sin tenantId si no encuentra con tenantId")
        void obtenerMetadata_buscarSinTenantId() {
            // Arrange
            String mongoId = "507f1f77bcf86cd799439011";
            Document doc = new Document();
            doc.append("ciPaciente", CI_PACIENTE);
            doc.append("tenantId", 999L);
            
            when(documentoPdfRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(null);
            when(documentoClinicoRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(null);
            when(documentoPdfRepository.buscarPorId(mongoId, null)).thenReturn(doc);

            // Act
            Map<String, Object> result = service.obtenerMetadataPorId(mongoId, TENANT_ID);

            // Assert
            assertNotNull(result);
            assertEquals(999L, result.get("tenantId"));
        }

        @Test
        @DisplayName("Debe manejar excepción en DocumentoClinicoRepository")
        void obtenerMetadata_clinicoException_continua() {
            // Arrange
            String mongoId = "507f1f77bcf86cd799439011";
            
            when(documentoPdfRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(null);
            when(documentoClinicoRepository.buscarPorId(mongoId, TENANT_ID))
                    .thenThrow(new RuntimeException("MongoDB error"));
            when(documentoPdfRepository.buscarPorId(mongoId, null)).thenReturn(null);
            when(documentoClinicoRepository.buscarPorId(mongoId, null)).thenReturn(null);

            // Act
            Map<String, Object> result = service.obtenerMetadataPorId(mongoId, TENANT_ID);

            // Assert
            assertNull(result);
        }

        @Test
        @DisplayName("Debe manejar excepción al buscar sin tenantId")
        void obtenerMetadata_buscarSinTenantIdException() {
            // Arrange
            String mongoId = "507f1f77bcf86cd799439011";
            
            when(documentoPdfRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(null);
            when(documentoClinicoRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(null);
            when(documentoPdfRepository.buscarPorId(mongoId, null))
                    .thenThrow(new RuntimeException("MongoDB error"));

            // Act
            Map<String, Object> result = service.obtenerMetadataPorId(mongoId, TENANT_ID);

            // Assert
            assertNull(result);
        }

        @Test
        @DisplayName("Debe obtener tenantId del documento si está disponible")
        void obtenerMetadata_usaTenantIdDelDocumento() {
            // Arrange
            String mongoId = "507f1f77bcf86cd799439011";
            Document doc = new Document();
            doc.append("ciPaciente", CI_PACIENTE);
            doc.append("tipoDocumento", TIPO_DOC);
            doc.append("tenantId", 202L);
            
            when(documentoPdfRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(doc);

            // Act
            Map<String, Object> result = service.obtenerMetadataPorId(mongoId, TENANT_ID);

            // Assert
            assertNotNull(result);
            assertEquals(202L, result.get("tenantId"));
        }
    }

    @Nested
    @DisplayName("ObtenerPdf - Casos Adicionales EXHAUSTIVOS")
    @MockitoSettings(strictness = Strictness.LENIENT)
    class ObtenerPdfCasosAdicionalesTests {

        @Test
        @DisplayName("Debe buscar en DocumentoClinico si no está en Pdf")
        void obtenerPdf_buscaEnClinico() {
            // Arrange
            String mongoId = "507f1f77bcf86cd799439011";
            Document doc = new Document();
            doc.append("pdfBytes", new Binary(VALID_PDF_BYTES));
            
            when(documentoPdfRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(null);
            when(documentoClinicoRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(doc);

            // Act
            byte[] result = service.obtenerPdfPorId(mongoId, TENANT_ID);

            // Assert
            assertNotNull(result);
            verify(documentoClinicoRepository).buscarPorId(mongoId, TENANT_ID);
        }

        @Test
        @DisplayName("Debe buscar sin tenantId si no encuentra con tenantId")
        void obtenerPdf_buscaSinTenantId() {
            // Arrange
            String mongoId = "507f1f77bcf86cd799439011";
            Document doc = new Document();
            doc.append("pdfBytes", new Binary(VALID_PDF_BYTES));
            
            when(documentoPdfRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(null);
            when(documentoClinicoRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(null);
            when(documentoPdfRepository.buscarPorId(mongoId, null)).thenReturn(doc);

            // Act
            byte[] result = service.obtenerPdfPorId(mongoId, TENANT_ID);

            // Assert
            assertNotNull(result);
        }

        @Test
        @DisplayName("Debe intentar generar PDF si pdfBytes es null")
        void obtenerPdf_pdfBytesNull_intentaGenerar() {
            // Arrange
            String mongoId = "507f1f77bcf86cd799439011";
            Document doc = new Document();
            doc.append("ciPaciente", CI_PACIENTE);
            doc.append("tipoDocumento", TIPO_DOC);
            // Sin pdfBytes ni pdf
            
            when(documentoPdfRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(doc);

            // Act
            byte[] result = service.obtenerPdfPorId(mongoId, TENANT_ID);

            // Assert - Retorna null porque no puede generar
            assertNull(result);
        }

        @Test
        @DisplayName("Debe buscar en campo pdf si pdfBytes no existe")
        void obtenerPdf_usaCampoPdf() {
            // Arrange
            String mongoId = "507f1f77bcf86cd799439011";
            Document doc = new Document();
            doc.append("pdf", new Binary(VALID_PDF_BYTES));
            
            when(documentoPdfRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(doc);

            // Act
            byte[] result = service.obtenerPdfPorId(mongoId, TENANT_ID);

            // Assert
            assertNotNull(result);
            assertArrayEquals(VALID_PDF_BYTES, result);
        }

        @Test
        @DisplayName("Debe validar header PDF")
        void obtenerPdf_validaHeaderPdf() {
            // Arrange
            String mongoId = "507f1f77bcf86cd799439011";
            Document doc = new Document();
            doc.append("pdfBytes", new Binary(VALID_PDF_BYTES));
            
            when(documentoPdfRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(doc);

            // Act
            byte[] result = service.obtenerPdfPorId(mongoId, TENANT_ID);

            // Assert
            assertNotNull(result);
            assertTrue(new String(result).startsWith("%PDF"));
        }

        @Test
        @DisplayName("Debe manejar PDF sin header válido")
        void obtenerPdf_sinHeaderValido_retornaPdf() {
            // Arrange
            String mongoId = "507f1f77bcf86cd799439011";
            byte[] invalidPdf = "INVALID PDF DATA".getBytes();
            Document doc = new Document();
            doc.append("pdfBytes", new Binary(invalidPdf));
            
            when(documentoPdfRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(doc);

            // Act
            byte[] result = service.obtenerPdfPorId(mongoId, TENANT_ID);

            // Assert - Retorna aunque no sea PDF válido
            assertNotNull(result);
            assertArrayEquals(invalidPdf, result);
        }

        @Test
        @DisplayName("Debe manejar excepción en DocumentoClinicoRepository al obtener PDF")
        void obtenerPdf_clinicoException_continua() {
            // Arrange
            String mongoId = "507f1f77bcf86cd799439011";
            
            when(documentoPdfRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(null);
            when(documentoClinicoRepository.buscarPorId(mongoId, TENANT_ID))
                    .thenThrow(new RuntimeException("MongoDB error"));
            when(documentoPdfRepository.buscarPorId(mongoId, null)).thenReturn(null);
            when(documentoClinicoRepository.buscarPorId(mongoId, null)).thenReturn(null);

            // Act
            byte[] result = service.obtenerPdfPorId(mongoId, TENANT_ID);

            // Assert
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("ListarDocumentos - Casos Adicionales EXHAUSTIVOS")
    @MockitoSettings(strictness = Strictness.LENIENT)
    class ListarDocumentosCasosAdicionalesTests {

        @Test
        @DisplayName("Debe obtener especialidad del profesional si está disponible")
        void listarDocumentos_conEspecialidad() throws Exception {
            // Arrange
            ProfesionalSalud profesional = createTestProfesional();
            profesional.setEspecialidad(uy.edu.tse.hcen.model.enums.Especialidad.CARDIOLOGIA);
            
            when(profesionalSaludRepository.findByNickname(PROFESIONAL_ID)).thenReturn(Optional.of(profesional));
            when(hcenClient.obtenerMetadatosDocumentosPorCI(anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(new java.util.ArrayList<>());

            // Act
            service.listarDocumentosPorPaciente(CI_PACIENTE, PROFESIONAL_ID, "101");

            // Assert
            verify(hcenClient).obtenerMetadatosDocumentosPorCI(
                    eq(CI_PACIENTE),
                    eq(PROFESIONAL_ID),
                    eq("101"),
                    eq("CARDIOLOGIA"),
                    anyString());
        }

        @Test
        @DisplayName("Debe manejar profesional sin especialidad")
        void listarDocumentos_sinEspecialidad() throws Exception {
            // Arrange
            ProfesionalSalud profesional = createTestProfesional();
            profesional.setEspecialidad(null);
            
            when(profesionalSaludRepository.findByNickname(PROFESIONAL_ID)).thenReturn(Optional.of(profesional));
            when(hcenClient.obtenerMetadatosDocumentosPorCI(anyString(), anyString(), anyString(), isNull(), anyString()))
                    .thenReturn(new java.util.ArrayList<>());

            // Act
            service.listarDocumentosPorPaciente(CI_PACIENTE, PROFESIONAL_ID, "101");

            // Assert
            verify(hcenClient).obtenerMetadatosDocumentosPorCI(
                    eq(CI_PACIENTE),
                    eq(PROFESIONAL_ID),
                    eq("101"),
                    isNull(),
                    anyString());
        }

        @Test
        @DisplayName("Debe manejar profesionalId null")
        void listarDocumentos_profesionalIdNull() throws Exception {
            // Arrange
            when(hcenClient.obtenerMetadatosDocumentosPorCI(anyString(), isNull(), anyString(), isNull(), isNull()))
                    .thenReturn(new java.util.ArrayList<>());

            // Act
            java.util.List<Map<String, Object>> result = service.listarDocumentosPorPaciente(CI_PACIENTE, null, "101");

            // Assert
            assertNotNull(result);
            verify(profesionalSaludRepository, never()).findByNickname(any());
        }

        @Test
        @DisplayName("Debe manejar profesionalId blank")
        void listarDocumentos_profesionalIdBlank() throws Exception {
            // Arrange
            when(hcenClient.obtenerMetadatosDocumentosPorCI(anyString(), anyString(), anyString(), isNull(), isNull()))
                    .thenReturn(new java.util.ArrayList<>());

            // Act
            java.util.List<Map<String, Object>> result = service.listarDocumentosPorPaciente(CI_PACIENTE, "   ", "101");

            // Assert
            assertNotNull(result);
            verify(profesionalSaludRepository, never()).findByNickname(any());
        }

        @Test
        @DisplayName("Debe manejar excepción al obtener profesional")
        void listarDocumentos_profesionalException_continua() throws Exception {
            // Arrange
            when(profesionalSaludRepository.findByNickname(PROFESIONAL_ID))
                    .thenThrow(new RuntimeException("DB error"));
            when(hcenClient.obtenerMetadatosDocumentosPorCI(anyString(), anyString(), anyString(), isNull(), isNull()))
                    .thenReturn(new java.util.ArrayList<>());

            // Act
            java.util.List<Map<String, Object>> result = service.listarDocumentosPorPaciente(CI_PACIENTE, PROFESIONAL_ID, "101");

            // Assert
            assertNotNull(result);
        }

        @Test
        @DisplayName("Debe mapear metadatos correctamente")
        void listarDocumentos_mapeaMetadatos() throws Exception {
            // Arrange
            Map<String, Object> metadataHcen = new HashMap<>();
            metadataHcen.put("id", "mongo123");
            metadataHcen.put("documentoId", "doc-uuid-123");
            metadataHcen.put("tenantId", "101");
            metadataHcen.put("fechaCreacion", "2024-01-15T10:30:00");
            metadataHcen.put("tipoDocumento", "EVALUACION");
            metadataHcen.put("descripcion", "Evaluación médica");
            metadataHcen.put("profesionalSalud", "doctor1");
            metadataHcen.put("nombrePaciente", "Juan");
            metadataHcen.put("apellidoPaciente", "Pérez");
            metadataHcen.put("uriDocumento", "http://test.com/doc");
            
            when(profesionalSaludRepository.findByNickname(PROFESIONAL_ID)).thenReturn(Optional.empty());
            when(hcenClient.obtenerMetadatosDocumentosPorCI(anyString(), anyString(), anyString(), isNull(), isNull()))
                    .thenReturn(java.util.List.of(metadataHcen));

            // Act
            java.util.List<Map<String, Object>> result = service.listarDocumentosPorPaciente(CI_PACIENTE, PROFESIONAL_ID, "101");

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            Map<String, Object> doc = result.get(0);
            assertEquals("mongo123", doc.get("id"));
            assertEquals("doc-uuid-123", doc.get("documentoId"));
            assertEquals(CI_PACIENTE, doc.get("ciPaciente"));
            assertEquals("application/pdf", doc.get("contentType"));
        }

        @Test
        @DisplayName("Debe retornar lista vacía cuando HCEN no está disponible")
        void listarDocumentos_hcenUnavailable_returnsEmpty() throws Exception {
            // Arrange
            when(profesionalSaludRepository.findByNickname(PROFESIONAL_ID)).thenReturn(Optional.empty());
            when(hcenClient.obtenerMetadatosDocumentosPorCI(anyString(), anyString(), anyString(), isNull(), isNull()))
                    .thenThrow(new HcenUnavailableException("HCEN timeout"));

            // Act
            java.util.List<Map<String, Object>> result = service.listarDocumentosPorPaciente(CI_PACIENTE, PROFESIONAL_ID, "101");

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Debe mapear múltiples metadatos")
        void listarDocumentos_multipleMetadatos() throws Exception {
            // Arrange
            java.util.List<Map<String, Object>> metadatos = java.util.List.of(
                    createMetadataMap("mongo1", "doc1"),
                    createMetadataMap("mongo2", "doc2"),
                    createMetadataMap("mongo3", "doc3")
            );
            
            when(profesionalSaludRepository.findByNickname(PROFESIONAL_ID)).thenReturn(Optional.empty());
            when(hcenClient.obtenerMetadatosDocumentosPorCI(anyString(), anyString(), anyString(), isNull(), isNull()))
                    .thenReturn(metadatos);

            // Act
            java.util.List<Map<String, Object>> result = service.listarDocumentosPorPaciente(CI_PACIENTE, PROFESIONAL_ID, "101");

            // Assert
            assertNotNull(result);
            assertEquals(3, result.size());
        }

        private Map<String, Object> createMetadataMap(String id, String documentoId) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", id);
            m.put("documentoId", documentoId);
            m.put("tenantId", "101");
            m.put("tipoDocumento", "EVALUACION");
            return m;
        }
    }

    @Nested
    @DisplayName("Edge Cases Finales Tests")
    @MockitoSettings(strictness = Strictness.LENIENT)
    class EdgeCasesFinalesTests {

        @Test
        @DisplayName("Obtener PDF con ID inválido debe retornar null")
        void getPdf_invalidId_shouldReturnNull() {
            // Arrange
            String mongoId = "invalid-id";
            when(documentoPdfRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(null);
            when(documentoClinicoRepository.buscarPorId(mongoId, TENANT_ID)).thenReturn(null);

            // Act
            byte[] result = service.obtenerPdfPorId(mongoId, TENANT_ID);

            // Assert
            assertNull(result);
        }
    }
}


