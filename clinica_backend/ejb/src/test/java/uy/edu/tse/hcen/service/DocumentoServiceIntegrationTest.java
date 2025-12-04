package uy.edu.tse.hcen.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.tse.hcen.dto.DTMetadatos;
import uy.edu.tse.hcen.exceptions.HcenUnavailableException;
import uy.edu.tse.hcen.model.ProfesionalSalud;
import uy.edu.tse.hcen.model.UsuarioSalud;
import uy.edu.tse.hcen.multitenancy.TenantContext;
import uy.edu.tse.hcen.repository.DocumentoClinicoRepository;
import uy.edu.tse.hcen.repository.ProfesionalSaludRepository;
import uy.edu.tse.hcen.repository.UsuarioSaludRepository;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests exhaustivos de integración para DocumentoService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentoService Integration Tests")
class DocumentoServiceIntegrationTest {

    @Mock
    private DocumentoClinicoRepository documentoRepository;

    @Mock
    private UsuarioSaludRepository usuarioSaludRepository;

    @Mock
    private ProfesionalSaludRepository profesionalSaludRepository;

    @Mock
    private HcenClient hcenClient;

    @InjectMocks
    private DocumentoService service;

    private static final Long TENANT_ID = 101L;
    private static final String PROFESIONAL_ID = "doctor1";
    private static final String CI_PACIENTE = "12345678";
    private static final String CONTENIDO = "Contenido del documento médico.";

    private UsuarioSalud createPaciente() {
        UsuarioSalud paciente = new UsuarioSalud();
        paciente.setId(1L);
        paciente.setCi(CI_PACIENTE);
        paciente.setNombre("Juan");
        paciente.setApellido("Pérez");
        paciente.setFechaNacimiento(LocalDate.of(1990, 1, 15));
        paciente.setTenantId(TENANT_ID);
        return paciente;
    }

    @Nested
    @DisplayName("Crear Documento Completo Tests")
    class CrearDocumentoCompletoTests {

        @Test
        @DisplayName("Crear documento completo exitosamente")
        void crearDocumento_success() throws Exception {
            // Arrange
            UsuarioSalud paciente = createPaciente();
            ProfesionalSalud prof = new ProfesionalSalud();
            prof.setNombre("Dr. Test");
            
            when(usuarioSaludRepository.findByCiAndTenant(CI_PACIENTE, TENANT_ID))
                    .thenReturn(paciente);
            when(profesionalSaludRepository.findByNickname(PROFESIONAL_ID))
                    .thenReturn(Optional.of(prof));
            when(documentoRepository.guardarDocumentoCompleto(
                    anyString(), anyString(), any(), any(), any(), any(),
                    anyString(), any(), anyString(), any(), anyString(), anyString(), anyString()))
                    .thenReturn("mongo123");
            doNothing().when(hcenClient).registrarMetadatos(any(DTMetadatos.class));
            
            try (MockedStatic<TenantContext> ctx = mockStatic(TenantContext.class)) {
                ctx.when(TenantContext::getCurrentTenant).thenReturn(String.valueOf(TENANT_ID));

                // Act
                Map<String, Object> result = service.crearDocumentoCompleto(
                        TENANT_ID, PROFESIONAL_ID, CI_PACIENTE, CONTENIDO,
                        "EVALUACION", "Desc", "Titulo", "Autor");

                // Assert
                assertNotNull(result);
                assertTrue(result.containsKey("documentoId"));
                verify(documentoRepository).guardarDocumentoCompleto(
                        anyString(), eq(CONTENIDO), any(), any(), any(), any(),
                        eq(CI_PACIENTE), eq(TENANT_ID), anyString(), any(), 
                        eq(PROFESIONAL_ID), anyString(), anyString());
            }
        }

        @Test
        @DisplayName("Crear documento con contenido vacío debe fallar")
        void crearDocumento_emptyContent_shouldFail() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> service.crearDocumentoCompleto(
                            TENANT_ID, PROFESIONAL_ID, CI_PACIENTE, "",
                            "EVALUACION", null, null, null));
        }

        @Test
        @DisplayName("Crear documento con contenido null debe fallar")
        void crearDocumento_nullContent_shouldFail() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> service.crearDocumentoCompleto(
                            TENANT_ID, PROFESIONAL_ID, CI_PACIENTE, null,
                            "EVALUACION", null, null, null));
        }

        @Test
        @DisplayName("Crear documento con paciente no encontrado debe fallar")
        void crearDocumento_patientNotFound_shouldFail() {
            // Arrange
            when(usuarioSaludRepository.findByCiAndTenant(CI_PACIENTE, TENANT_ID))
                    .thenReturn(null);

            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.crearDocumentoCompleto(
                            TENANT_ID, PROFESIONAL_ID, CI_PACIENTE, CONTENIDO,
                            "EVALUACION", null, null, null));
            
            assertTrue(ex.getMessage().contains("no está registrado"));
        }

        @Test
        @DisplayName("Crear documento cuando HCEN falla debe continuar")
        void crearDocumento_hcenFails_shouldContinue() throws Exception {
            // Arrange
            UsuarioSalud paciente = createPaciente();
            
            when(usuarioSaludRepository.findByCiAndTenant(CI_PACIENTE, TENANT_ID))
                    .thenReturn(paciente);
            when(profesionalSaludRepository.findByNickname(PROFESIONAL_ID))
                    .thenReturn(Optional.empty());
            when(documentoRepository.guardarDocumentoCompleto(
                    anyString(), anyString(), any(), any(), any(), any(),
                    anyString(), any(), anyString(), any(), anyString(), anyString(), anyString()))
                    .thenReturn("mongo123");
            doThrow(new HcenUnavailableException("Timeout"))
                    .when(hcenClient).registrarMetadatos(any(DTMetadatos.class));
            
            try (MockedStatic<TenantContext> ctx = mockStatic(TenantContext.class)) {
                ctx.when(TenantContext::getCurrentTenant).thenReturn(String.valueOf(TENANT_ID));

                // Act - No debe lanzar excepción
                Map<String, Object> result = service.crearDocumentoCompleto(
                        TENANT_ID, PROFESIONAL_ID, CI_PACIENTE, CONTENIDO,
                        "EVALUACION", null, null, null);

                // Assert
                assertNotNull(result);
            }
        }

        @Test
        @DisplayName("Crear documento con diferentes tipos")
        void crearDocumento_withDifferentTypes_shouldWork() throws Exception {
            // Arrange
            UsuarioSalud paciente = createPaciente();
            
            when(usuarioSaludRepository.findByCiAndTenant(CI_PACIENTE, TENANT_ID))
                    .thenReturn(paciente);
            when(profesionalSaludRepository.findByNickname(PROFESIONAL_ID))
                    .thenReturn(Optional.empty());
            when(documentoRepository.guardarDocumentoCompleto(
                    anyString(), anyString(), any(), any(), any(), any(),
                    anyString(), any(), anyString(), any(), anyString(), anyString(), anyString()))
                    .thenReturn("mongo123");
            
            try (MockedStatic<TenantContext> ctx = mockStatic(TenantContext.class)) {
                ctx.when(TenantContext::getCurrentTenant).thenReturn(String.valueOf(TENANT_ID));

                String[] tipos = {"EVALUACION", "RECETA", "ESTUDIO", "INFORME", "ORDEN"};
                
                for (String tipo : tipos) {
                    // Act
                    Map<String, Object> result = service.crearDocumentoCompleto(
                            TENANT_ID, PROFESIONAL_ID, CI_PACIENTE, CONTENIDO,
                            tipo, null, null, null);

                    // Assert
                    assertNotNull(result);
                }
            }
        }

        @Test
        @DisplayName("Crear documento con contenido largo")
        void crearDocumento_withLongContent_shouldWork() throws Exception {
            // Arrange
            StringBuilder longContent = new StringBuilder();
            for (int i = 0; i < 500; i++) {
                longContent.append("Línea ").append(i).append(": Información médica detallada del paciente.\n");
            }
            
            UsuarioSalud paciente = createPaciente();
            
            when(usuarioSaludRepository.findByCiAndTenant(CI_PACIENTE, TENANT_ID))
                    .thenReturn(paciente);
            when(profesionalSaludRepository.findByNickname(PROFESIONAL_ID))
                    .thenReturn(Optional.empty());
            when(documentoRepository.guardarDocumentoCompleto(
                    anyString(), anyString(), any(), any(), any(), any(),
                    anyString(), any(), anyString(), any(), anyString(), anyString(), anyString()))
                    .thenReturn("mongo123");
            
            try (MockedStatic<TenantContext> ctx = mockStatic(TenantContext.class)) {
                ctx.when(TenantContext::getCurrentTenant).thenReturn(String.valueOf(TENANT_ID));

                // Act
                Map<String, Object> result = service.crearDocumentoCompleto(
                        TENANT_ID, PROFESIONAL_ID, CI_PACIENTE, longContent.toString(),
                        "EVALUACION", null, null, null);

                // Assert
                assertNotNull(result);
            }
        }

        @Test
        @DisplayName("Crear documento con contenido con caracteres especiales")
        void crearDocumento_withSpecialChars_shouldWork() throws Exception {
            // Arrange
            String specialContent = "Paciente: José María González Pérez\n" +
                    "Diagnóstico: Hipótesis de úlcera gástrica\n" +
                    "Medicación: Ácido fólico, vitamina B12\n" +
                    "Observaciones: Ñoño, año, señal";
            
            UsuarioSalud paciente = createPaciente();
            
            when(usuarioSaludRepository.findByCiAndTenant(CI_PACIENTE, TENANT_ID))
                    .thenReturn(paciente);
            when(profesionalSaludRepository.findByNickname(PROFESIONAL_ID))
                    .thenReturn(Optional.empty());
            when(documentoRepository.guardarDocumentoCompleto(
                    anyString(), anyString(), any(), any(), any(), any(),
                    anyString(), any(), anyString(), any(), anyString(), anyString(), anyString()))
                    .thenReturn("mongo123");
            
            try (MockedStatic<TenantContext> ctx = mockStatic(TenantContext.class)) {
                ctx.when(TenantContext::getCurrentTenant).thenReturn(String.valueOf(TENANT_ID));

                // Act
                Map<String, Object> result = service.crearDocumentoCompleto(
                        TENANT_ID, PROFESIONAL_ID, CI_PACIENTE, specialContent,
                        "EVALUACION", null, null, null);

                // Assert
                assertNotNull(result);
            }
        }
    }

    @Nested
    @DisplayName("Crear Documento Con Archivo Tests")
    class CrearDocumentoConArchivoTests {

        @Test
        @DisplayName("Crear documento con archivo adjunto")
        void crearDocumentoConArchivo_success() throws Exception {
            // Arrange
            UsuarioSalud paciente = createPaciente();
            byte[] archivo = "Archivo adjunto content".getBytes();
            
            when(usuarioSaludRepository.findByCiAndTenant(CI_PACIENTE, TENANT_ID))
                    .thenReturn(paciente);
            when(profesionalSaludRepository.findByNickname(PROFESIONAL_ID))
                    .thenReturn(Optional.empty());
            when(documentoRepository.guardarDocumentoCompleto(
                    anyString(), anyString(), any(), any(), any(), any(),
                    anyString(), any(), anyString(), any(), anyString(), anyString(), anyString()))
                    .thenReturn("mongo123");
            
            try (MockedStatic<TenantContext> ctx = mockStatic(TenantContext.class)) {
                ctx.when(TenantContext::getCurrentTenant).thenReturn(String.valueOf(TENANT_ID));

                // Act
                Map<String, Object> result = service.crearDocumentoCompletoConArchivo(
                        TENANT_ID, PROFESIONAL_ID, CI_PACIENTE, CONTENIDO,
                        "EVALUACION", "Desc", "Titulo", "Autor",
                        archivo, "archivo.pdf", "application/pdf");

                // Assert
                assertNotNull(result);
                assertTrue(result.containsKey("documentoId"));
            }
        }

        @Test
        @DisplayName("Crear documento con archivo sin contenido debe fallar")
        void crearDocumentoConArchivo_emptyContent_shouldFail() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> service.crearDocumentoCompletoConArchivo(
                            TENANT_ID, PROFESIONAL_ID, CI_PACIENTE, "",
                            "EVALUACION", null, null, null,
                            new byte[0], "archivo.pdf", "application/pdf"));
        }

        @Test
        @DisplayName("Crear documento con archivo grande")
        void crearDocumentoConArchivo_largeFile_shouldWork() throws Exception {
            // Arrange
            UsuarioSalud paciente = createPaciente();
            byte[] largeFile = new byte[1024 * 1024]; // 1MB
            
            when(usuarioSaludRepository.findByCiAndTenant(CI_PACIENTE, TENANT_ID))
                    .thenReturn(paciente);
            when(profesionalSaludRepository.findByNickname(PROFESIONAL_ID))
                    .thenReturn(Optional.empty());
            when(documentoRepository.guardarDocumentoCompleto(
                    anyString(), anyString(), any(), any(), any(), any(),
                    anyString(), any(), anyString(), any(), anyString(), anyString(), anyString()))
                    .thenReturn("mongo123");
            
            try (MockedStatic<TenantContext> ctx = mockStatic(TenantContext.class)) {
                ctx.when(TenantContext::getCurrentTenant).thenReturn(String.valueOf(TENANT_ID));

                // Act
                Map<String, Object> result = service.crearDocumentoCompletoConArchivo(
                        TENANT_ID, PROFESIONAL_ID, CI_PACIENTE, CONTENIDO,
                        "ESTUDIO", null, null, null,
                        largeFile, "estudio.pdf", "application/pdf");

                // Assert
                assertNotNull(result);
            }
        }

        @Test
        @DisplayName("Crear documento con archivo null")
        void crearDocumentoConArchivo_nullFile_shouldWork() throws Exception {
            // Arrange
            UsuarioSalud paciente = createPaciente();
            
            when(usuarioSaludRepository.findByCiAndTenant(CI_PACIENTE, TENANT_ID))
                    .thenReturn(paciente);
            when(profesionalSaludRepository.findByNickname(PROFESIONAL_ID))
                    .thenReturn(Optional.empty());
            when(documentoRepository.guardarDocumentoCompleto(
                    anyString(), anyString(), any(), any(), any(), any(),
                    anyString(), any(), anyString(), any(), anyString(), anyString(), anyString()))
                    .thenReturn("mongo123");
            
            try (MockedStatic<TenantContext> ctx = mockStatic(TenantContext.class)) {
                ctx.when(TenantContext::getCurrentTenant).thenReturn(String.valueOf(TENANT_ID));

                // Act
                Map<String, Object> result = service.crearDocumentoCompletoConArchivo(
                        TENANT_ID, PROFESIONAL_ID, CI_PACIENTE, CONTENIDO,
                        "RECETA", null, null, null,
                        null, null, null);

                // Assert
                assertNotNull(result);
            }
        }

        @Test
        @DisplayName("Crear documento con diferentes tipos MIME")
        void crearDocumentoConArchivo_differentMimeTypes_shouldWork() throws Exception {
            // Arrange
            UsuarioSalud paciente = createPaciente();
            
            when(usuarioSaludRepository.findByCiAndTenant(CI_PACIENTE, TENANT_ID))
                    .thenReturn(paciente);
            when(profesionalSaludRepository.findByNickname(PROFESIONAL_ID))
                    .thenReturn(Optional.empty());
            when(documentoRepository.guardarDocumentoCompleto(
                    anyString(), anyString(), any(), any(), any(), any(),
                    anyString(), any(), anyString(), any(), anyString(), anyString(), anyString()))
                    .thenReturn("mongo123");
            
            try (MockedStatic<TenantContext> ctx = mockStatic(TenantContext.class)) {
                ctx.when(TenantContext::getCurrentTenant).thenReturn(String.valueOf(TENANT_ID));

                String[] mimeTypes = {"application/pdf", "image/jpeg", "image/png", "application/dicom"};
                
                for (String mimeType : mimeTypes) {
                    // Act
                    Map<String, Object> result = service.crearDocumentoCompletoConArchivo(
                            TENANT_ID, PROFESIONAL_ID, CI_PACIENTE, CONTENIDO,
                            "ESTUDIO", null, null, null,
                            new byte[100], "archivo", mimeType);

                    // Assert
                    assertNotNull(result);
                }
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Crear documento con descripción larga")
        void crearDocumento_withLongDescription_shouldWork() throws Exception {
            // Arrange
            UsuarioSalud paciente = createPaciente();
            String longDesc = "A".repeat(1000);
            
            when(usuarioSaludRepository.findByCiAndTenant(CI_PACIENTE, TENANT_ID))
                    .thenReturn(paciente);
            when(profesionalSaludRepository.findByNickname(PROFESIONAL_ID))
                    .thenReturn(Optional.empty());
            when(documentoRepository.guardarDocumentoCompleto(
                    anyString(), anyString(), any(), any(), any(), any(),
                    anyString(), any(), anyString(), any(), anyString(), anyString(), anyString()))
                    .thenReturn("mongo123");
            
            try (MockedStatic<TenantContext> ctx = mockStatic(TenantContext.class)) {
                ctx.when(TenantContext::getCurrentTenant).thenReturn(String.valueOf(TENANT_ID));

                // Act
                Map<String, Object> result = service.crearDocumentoCompleto(
                        TENANT_ID, PROFESIONAL_ID, CI_PACIENTE, CONTENIDO,
                        "EVALUACION", longDesc, null, null);

                // Assert
                assertNotNull(result);
            }
        }

        @Test
        @DisplayName("Crear documento con título largo")
        void crearDocumento_withLongTitle_shouldWork() throws Exception {
            // Arrange
            UsuarioSalud paciente = createPaciente();
            String longTitle = "Título Muy Largo ".repeat(20);
            
            when(usuarioSaludRepository.findByCiAndTenant(CI_PACIENTE, TENANT_ID))
                    .thenReturn(paciente);
            when(profesionalSaludRepository.findByNickname(PROFESIONAL_ID))
                    .thenReturn(Optional.empty());
            when(documentoRepository.guardarDocumentoCompleto(
                    anyString(), anyString(), any(), any(), any(), any(),
                    anyString(), any(), anyString(), any(), anyString(), anyString(), anyString()))
                    .thenReturn("mongo123");
            
            try (MockedStatic<TenantContext> ctx = mockStatic(TenantContext.class)) {
                ctx.when(TenantContext::getCurrentTenant).thenReturn(String.valueOf(TENANT_ID));

                // Act
                Map<String, Object> result = service.crearDocumentoCompleto(
                        TENANT_ID, PROFESIONAL_ID, CI_PACIENTE, CONTENIDO,
                        "EVALUACION", null, longTitle, null);

                // Assert
                assertNotNull(result);
            }
        }

        @Test
        @DisplayName("Crear documento con nombre archivo largo")
        void crearDocumentoConArchivo_withLongFilename_shouldWork() throws Exception {
            // Arrange
            UsuarioSalud paciente = createPaciente();
            String longFilename = "archivo_muy_largo_".repeat(10) + ".pdf";
            
            when(usuarioSaludRepository.findByCiAndTenant(CI_PACIENTE, TENANT_ID))
                    .thenReturn(paciente);
            when(profesionalSaludRepository.findByNickname(PROFESIONAL_ID))
                    .thenReturn(Optional.empty());
            when(documentoRepository.guardarDocumentoCompleto(
                    anyString(), anyString(), any(), any(), any(), any(),
                    anyString(), any(), anyString(), any(), anyString(), anyString(), anyString()))
                    .thenReturn("mongo123");
            
            try (MockedStatic<TenantContext> ctx = mockStatic(TenantContext.class)) {
                ctx.when(TenantContext::getCurrentTenant).thenReturn(String.valueOf(TENANT_ID));

                // Act
                Map<String, Object> result = service.crearDocumentoCompletoConArchivo(
                        TENANT_ID, PROFESIONAL_ID, CI_PACIENTE, CONTENIDO,
                        "EVALUACION", null, null, null,
                        new byte[100], longFilename, "application/pdf");

                // Assert
                assertNotNull(result);
            }
        }

        @Test
        @DisplayName("Crear múltiples documentos en secuencia")
        void crearDocumento_multiple_shouldWork() throws Exception {
            // Arrange
            UsuarioSalud paciente = createPaciente();
            
            when(usuarioSaludRepository.findByCiAndTenant(CI_PACIENTE, TENANT_ID))
                    .thenReturn(paciente);
            when(profesionalSaludRepository.findByNickname(PROFESIONAL_ID))
                    .thenReturn(Optional.empty());
            when(documentoRepository.guardarDocumentoCompleto(
                    anyString(), anyString(), any(), any(), any(), any(),
                    anyString(), any(), anyString(), any(), anyString(), anyString(), anyString()))
                    .thenReturn("mongo123", "mongo124", "mongo125");
            
            try (MockedStatic<TenantContext> ctx = mockStatic(TenantContext.class)) {
                ctx.when(TenantContext::getCurrentTenant).thenReturn(String.valueOf(TENANT_ID));

                // Act & Assert
                for (int i = 0; i < 3; i++) {
                    Map<String, Object> result = service.crearDocumentoCompleto(
                            TENANT_ID, PROFESIONAL_ID, CI_PACIENTE, CONTENIDO + i,
                            "EVALUACION", null, null, null);

                    assertNotNull(result);
                }
            }
        }
    }
}

