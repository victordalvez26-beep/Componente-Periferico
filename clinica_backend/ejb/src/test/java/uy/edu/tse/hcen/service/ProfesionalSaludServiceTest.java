package uy.edu.tse.hcen.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.tse.hcen.dto.ProfesionalDTO;
import uy.edu.tse.hcen.model.ProfesionalSalud;
import uy.edu.tse.hcen.model.enums.Especialidad;
import uy.edu.tse.hcen.context.TenantContext;
import uy.edu.tse.hcen.repository.ProfesionalSaludRepository;
import jakarta.ws.rs.WebApplicationException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for ProfesionalSaludService.
 * 
 * Tests cover:
 * - CRUD operations
 * - Security validations (admin-only)
 * - Uniqueness constraints
 * - Tenant context handling
 * - Edge cases
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProfesionalSaludService - CRUD & Business Logic")
class ProfesionalSaludServiceTest {

    @Mock
    private ProfesionalSaludRepository profesionalRepository;

    @Mock
    private TenantContext tenantContext;

    @Mock
    private ProfesionalPersistenceHelper persistenceHelper;

    @InjectMocks
    private ProfesionalSaludService service;

    private static final String TENANT_ID = "101";
    private static final String SCHEMA_NAME = "schema_clinica_" + TENANT_ID;

    private void setupAsAdmin() {
        when(tenantContext.getRole()).thenReturn("ADMINISTRADOR");
        when(tenantContext.getTenantId()).thenReturn(TENANT_ID);
    }

    // ==================== CREATE TESTS ====================

    @Nested
    @DisplayName("Crear Profesional")
    class CreateTests {

        @Test
        @DisplayName("Admin puede crear profesional exitosamente")
        void create_asAdmin_validData_shouldSucceed() throws Exception {
            // Arrange
            setupAsAdmin();
            ProfesionalDTO dto = createValidDTO();
            
            doAnswer(invocation -> {
                ProfesionalSalud prof = invocation.getArgument(0);
                prof.setId(1L);
                return null;
            }).when(persistenceHelper).persistWithManualTransaction(any(ProfesionalSalud.class), eq(SCHEMA_NAME));

            // Act
            ProfesionalSalud result = service.create(dto);

            // Assert
            assertNotNull(result);
            assertEquals(dto.getNombre(), result.getNombre());
            assertEquals(dto.getEmail(), result.getEmail());
            assertEquals(dto.getNickname(), result.getNickname());
            assertEquals(dto.getEspecialidad(), result.getEspecialidad());
            
            verify(persistenceHelper).persistWithManualTransaction(any(ProfesionalSalud.class), eq(SCHEMA_NAME));
        }

        @Test
        @DisplayName("No-admin debe lanzar SecurityException")
        void create_asNonAdmin_shouldThrowSecurityException() throws Exception {
            // Arrange
            when(tenantContext.getRole()).thenReturn("PROFESIONAL");
            ProfesionalDTO dto = createValidDTO();

            // Act & Assert
            SecurityException ex = assertThrows(SecurityException.class,
                () -> service.create(dto));
            
            assertEquals("Solo los administradores pueden crear profesionales.", ex.getMessage());
            verify(persistenceHelper, never()).persistWithManualTransaction(any(), anyString());
        }

        @Test
        @DisplayName("Role null debe lanzar SecurityException")
        void create_nullRole_shouldThrowSecurityException() {
            // Arrange
            when(tenantContext.getRole()).thenReturn(null);
            ProfesionalDTO dto = createValidDTO();

            // Act & Assert
            assertThrows(SecurityException.class, () -> service.create(dto));
        }

        @Test
        @DisplayName("DTO con teléfono y CI debe establecerlos correctamente")
        void create_withTelefonoAndCi_shouldSetFields() throws Exception {
            // Arrange
            setupAsAdmin();
            ProfesionalDTO dto = createValidDTO();
            dto.setTelefono("099123456");
            dto.setCi("12345678");

            doAnswer(invocation -> {
                try {
                    ProfesionalSalud prof = invocation.getArgument(0);
                    assertEquals("099123456", prof.getTelefono());
                    assertEquals("12345678", prof.getCi());
                    prof.setId(1L);
                    return null;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).when(persistenceHelper).persistWithManualTransaction(any(ProfesionalSalud.class), anyString());

            // Act
            ProfesionalSalud result = service.create(dto);

            // Assert
            assertNotNull(result);
            verify(persistenceHelper).persistWithManualTransaction(any(ProfesionalSalud.class), eq(SCHEMA_NAME));
        }

        @Test
        @DisplayName("Error en persistencia debe lanzar EJBException con mensaje contextual")
        void create_persistenceError_shouldThrowEJBException() throws Exception {
            // Arrange
            setupAsAdmin();
            ProfesionalDTO dto = createValidDTO();
            
            doThrow(new RuntimeException("DB error")).when(persistenceHelper)
                    .persistWithManualTransaction(any(), anyString());

            // Act & Assert
            jakarta.ejb.EJBException ex = assertThrows(jakarta.ejb.EJBException.class,
                () -> service.create(dto));
            
            assertTrue(ex.getMessage().contains(dto.getNickname()));
            assertTrue(ex.getMessage().contains("Failed to create professional"));
        }

        @Test
        @DisplayName("IllegalArgumentException en persistencia debe re-lanzarse con contexto")
        void create_validationError_shouldRethrowWithContext() throws Exception {
            // Arrange
            setupAsAdmin();
            ProfesionalDTO dto = createValidDTO();
            
            doThrow(new IllegalArgumentException("Nickname duplicado"))
                    .when(persistenceHelper).persistWithManualTransaction(any(), anyString());

            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(dto));
            
            assertTrue(ex.getMessage().contains(dto.getNickname()));
            assertTrue(ex.getMessage().contains("Error de validación"));
        }
    }

    // ==================== UPDATE TESTS ====================

    @Nested
    @DisplayName("Actualizar Profesional")
    class UpdateTests {

        @Test
        @DisplayName("Actualizar datos básicos debe funcionar")
        void update_basicFields_shouldSucceed() {
            // Arrange
            ProfesionalSalud existing = createProfesional(1L, "doctor1", "doctor@clinic.com");
            ProfesionalDTO dto = new ProfesionalDTO();
            dto.setNombre("Dr. Juan Pérez");
            dto.setDireccion("Calle 123");
            dto.setTelefono("099888777");

            when(profesionalRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(profesionalRepository.save(existing)).thenReturn(existing);

            // Act
            ProfesionalSalud result = service.update(1L, dto);

            // Assert
            assertNotNull(result);
            assertEquals("Dr. Juan Pérez", result.getNombre());
            assertEquals("Calle 123", result.getDireccion());
            assertEquals("099888777", result.getTelefono());
            verify(profesionalRepository).save(existing);
        }

        @Test
        @DisplayName("Actualizar nickname duplicado debe lanzar excepción")
        void update_duplicateNickname_shouldThrowConflict() {
            // Arrange
            ProfesionalSalud existing = createProfesional(1L, "doctor1", "email1@clinic.com");
            ProfesionalSalud other = createProfesional(2L, "doctor2", "email2@clinic.com");
            
            ProfesionalDTO dto = new ProfesionalDTO();
            dto.setNickname("doctor2"); // Already taken by 'other'

            when(profesionalRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(profesionalRepository.findByNickname("doctor2")).thenReturn(Optional.of(other));

            // Act & Assert - WebApplicationException wrapped by Mockito
            assertThrows(RuntimeException.class,
                () -> service.update(1L, dto));
            
            verify(profesionalRepository, never()).save(any());
        }

        @Test
        @DisplayName("Actualizar email duplicado debe lanzar excepción")
        void update_duplicateEmail_shouldThrowConflict() {
            // Arrange
            ProfesionalSalud existing = createProfesional(1L, "doctor1", "email1@clinic.com");
            ProfesionalSalud other = createProfesional(2L, "doctor2", "email2@clinic.com");
            
            ProfesionalDTO dto = new ProfesionalDTO();
            dto.setEmail("email2@clinic.com"); // Already taken

            when(profesionalRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(profesionalRepository.findByEmail("email2@clinic.com")).thenReturn(Optional.of(other));

            // Act & Assert - WebApplicationException wrapped by Mockito
            assertThrows(RuntimeException.class,
                () -> service.update(1L, dto));
        }

        @Test
        @DisplayName("Actualizar con mismo nickname debe funcionar")
        void update_sameNickname_shouldSucceed() {
            // Arrange
            ProfesionalSalud existing = createProfesional(1L, "doctor1", "email1@clinic.com");
            ProfesionalDTO dto = new ProfesionalDTO();
            dto.setNickname("doctor1"); // Same nickname
            dto.setNombre("Nuevo nombre");

            when(profesionalRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(profesionalRepository.save(existing)).thenReturn(existing);

            // Act
            ProfesionalSalud result = service.update(1L, dto);

            // Assert
            assertNotNull(result);
            verify(profesionalRepository, never()).findByNickname(anyString());
            verify(profesionalRepository).save(existing);
        }

        @Test
        @DisplayName("Actualizar ID inexistente debe lanzar IllegalArgumentException")
        void update_nonExistentId_shouldThrow() {
            // Arrange
            when(profesionalRepository.findById(999L)).thenReturn(Optional.empty());
            ProfesionalDTO dto = new ProfesionalDTO();

            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.update(999L, dto));
            
            assertTrue(ex.getMessage().contains("no encontrado"));
        }

        @Test
        @DisplayName("Actualizar password debe hashear correctamente")
        void update_withPassword_shouldHashPassword() {
            // Arrange
            ProfesionalSalud existing = createProfesional(1L, "doctor1", "email@clinic.com");
            ProfesionalDTO dto = new ProfesionalDTO();
            dto.setPassword("NewSecurePassword123!");

            when(profesionalRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(profesionalRepository.save(existing)).thenAnswer(inv -> {
                ProfesionalSalud saved = inv.getArgument(0);
                assertNotNull(saved.getPasswordHash());
                assertNotEquals("NewSecurePassword123!", saved.getPasswordHash());
                return saved;
            });

            // Act
            service.update(1L, dto);

            // Assert
            verify(profesionalRepository).save(existing);
        }
    }

    // ==================== READ TESTS ====================

    @Nested
    @DisplayName("Consultar Profesionales")
    class ReadTests {

        @Test
        @DisplayName("findAll debe retornar lista de profesionales del tenant")
        void findAllInCurrentTenant_shouldReturnList() {
            // Arrange
            List<ProfesionalSalud> profesionales = Arrays.asList(
                    createProfesional(1L, "doc1", "doc1@clinic.com"),
                    createProfesional(2L, "doc2", "doc2@clinic.com")
            );
            when(profesionalRepository.findAll()).thenReturn(profesionales);

            // Act
            List<ProfesionalSalud> result = service.findAllInCurrentTenant();

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            verify(profesionalRepository).findAll();
        }

        @Test
        @DisplayName("findById con ID existente debe retornar profesional")
        void findById_existingId_shouldReturnProfesional() {
            // Arrange
            ProfesionalSalud profesional = createProfesional(1L, "doctor1", "doc@clinic.com");
            when(profesionalRepository.findById(1L)).thenReturn(Optional.of(profesional));

            // Act
            Optional<ProfesionalSalud> result = service.findById(1L);

            // Assert
            assertTrue(result.isPresent());
            assertEquals("doctor1", result.get().getNickname());
        }

        @Test
        @DisplayName("findById con ID inexistente debe retornar Optional vacío")
        void findById_nonExistentId_shouldReturnEmpty() {
            // Arrange
            when(profesionalRepository.findById(999L)).thenReturn(Optional.empty());

            // Act
            Optional<ProfesionalSalud> result = service.findById(999L);

            // Assert
            assertFalse(result.isPresent());
        }
    }

    // ==================== DELETE TESTS ====================

    @Nested
    @DisplayName("Eliminar Profesional")
    class DeleteTests {

        @Test
        @DisplayName("Delete con ID existente debe eliminar")
        void delete_existingId_shouldDelete() {
            // Arrange
            ProfesionalSalud profesional = createProfesional(1L, "doctor1", "doc@clinic.com");
            when(profesionalRepository.findById(1L)).thenReturn(Optional.of(profesional));

            // Act
            service.delete(1L);

            // Assert
            verify(profesionalRepository).findById(1L);
            verify(profesionalRepository).delete(profesional);
        }

        @Test
        @DisplayName("Delete con ID inexistente debe lanzar IllegalArgumentException")
        void delete_nonExistentId_shouldThrow() {
            // Arrange
            when(profesionalRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.delete(999L));
            
            assertTrue(ex.getMessage().contains("no encontrado"));
            verify(profesionalRepository, never()).delete(any());
        }
    }

    // ==================== TENANT CONTEXT TESTS ====================

    @Nested
    @DisplayName("Manejo de Tenant Context")
    class TenantContextTests {

        @Test
        @DisplayName("Tenant ID null debe usar schema 'public'")
        void create_nullTenantId_shouldUsePublicSchema() throws Exception {
            // Arrange
            when(tenantContext.getRole()).thenReturn("ADMINISTRADOR");
            when(tenantContext.getTenantId()).thenReturn(null);
            ProfesionalDTO dto = createValidDTO();

            // Act
            service.create(dto);

            // Assert
            verify(persistenceHelper).persistWithManualTransaction(any(), eq("public"));
        }

        @Test
        @DisplayName("Tenant ID vacío debe usar schema 'public'")
        void create_emptyTenantId_shouldUsePublicSchema() throws Exception {
            // Arrange
            when(tenantContext.getRole()).thenReturn("ADMINISTRADOR");
            when(tenantContext.getTenantId()).thenReturn("");
            ProfesionalDTO dto = createValidDTO();

            // Act
            service.create(dto);

            // Assert
            verify(persistenceHelper).persistWithManualTransaction(any(), eq("public"));
        }

        @Test
        @DisplayName("Tenant ID inválido debe logear warning y continuar")
        void create_invalidTenantIdFormat_shouldLogWarningAndContinue() throws Exception {
            // Arrange
            when(tenantContext.getRole()).thenReturn("ADMINISTRADOR");
            when(tenantContext.getTenantId()).thenReturn("invalid_id");
            ProfesionalDTO dto = createValidDTO();

            // Act
            service.create(dto);

            // Assert - Should still try to persist
            verify(persistenceHelper).persistWithManualTransaction(any(), eq("schema_clinica_invalid_id"));
        }
    }

    // ==================== EDGE CASES ====================

    @Nested
    @DisplayName("Casos Borde")
    class EdgeCaseTests {

        @Test
        @DisplayName("DTO sin teléfono ni CI debe funcionar")
        void create_withoutOptionalFields_shouldSucceed() throws Exception {
            // Arrange
            setupAsAdmin();
            ProfesionalDTO dto = createValidDTO();
            dto.setTelefono(null);
            dto.setCi(null);

            // Act
            ProfesionalSalud result = service.create(dto);

            // Assert
            assertNotNull(result);
            assertNull(result.getTelefono());
            assertNull(result.getCi());
        }

        @Test
        @DisplayName("Update con DTO vacío (todos null) debe retornar sin cambios")
        void update_emptyDTO_shouldReturnUnchanged() {
            // Arrange
            ProfesionalSalud existing = createProfesional(1L, "doctor1", "doc@clinic.com");
            existing.setNombre("Original Name");
            
            ProfesionalDTO dto = new ProfesionalDTO(); // All fields null

            when(profesionalRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(profesionalRepository.save(existing)).thenReturn(existing);

            // Act
            ProfesionalSalud result = service.update(1L, dto);

            // Assert
            assertEquals("Original Name", result.getNombre());
            assertEquals("doctor1", result.getNickname());
            verify(profesionalRepository).save(existing);
        }

        @Test
        @DisplayName("Update con password vacío no debe cambiar password")
        void update_emptyPassword_shouldNotChangePassword() {
            // Arrange
            ProfesionalSalud existing = createProfesional(1L, "doctor1", "doc@clinic.com");
            String originalHash = existing.getPasswordHash();
            
            ProfesionalDTO dto = new ProfesionalDTO();
            dto.setPassword(""); // Empty password

            when(profesionalRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(profesionalRepository.save(existing)).thenReturn(existing);

            // Act
            service.update(1L, dto);

            // Assert
            assertEquals(originalHash, existing.getPasswordHash(), "Password should not change");
        }

        @Test
        @DisplayName("Update con password solo espacios no debe cambiar password")
        void update_blankPassword_shouldNotChangePassword() {
            // Arrange
            ProfesionalSalud existing = createProfesional(1L, "doctor1", "doc@clinic.com");
            String originalHash = existing.getPasswordHash();
            
            ProfesionalDTO dto = new ProfesionalDTO();
            dto.setPassword("   "); // Blank password

            when(profesionalRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(profesionalRepository.save(existing)).thenReturn(existing);

            // Act
            service.update(1L, dto);

            // Assert
            assertEquals(originalHash, existing.getPasswordHash());
        }
    }

    // ==================== HELPER METHODS ====================

    private ProfesionalDTO createValidDTO() {
        ProfesionalDTO dto = new ProfesionalDTO();
        dto.setNombre("Dr. John Smith");
        dto.setEmail("john.smith@clinic.com");
        dto.setNickname("dr_smith");
        dto.setPassword("SecurePass123!");
        dto.setEspecialidad(Especialidad.MEDICINA_GENERAL);
        dto.setDireccion("Av. Principal 456");
        return dto;
    }

    private ProfesionalSalud createProfesional(Long id, String nickname, String email) {
        ProfesionalSalud prof = new ProfesionalSalud();
        prof.setId(id);
        prof.setNickname(nickname);
        prof.setEmail(email);
        prof.setNombre("Dr. Test");
        prof.setPassword("TestPass123!");
        prof.setEspecialidad(Especialidad.MEDICINA_GENERAL);
        return prof;
    }
}

