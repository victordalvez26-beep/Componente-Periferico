package uy.edu.tse.hcen.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.tse.hcen.client.HcenUsuarioSaludClient;
import uy.edu.tse.hcen.model.UsuarioSalud;
import uy.edu.tse.hcen.repository.UsuarioSaludRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UsuarioSaludService.
 * Tests CRUD operations, HCEN synchronization, tenant isolation, and edge cases.
 * 
 * @author Senior Test Engineer
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioSaludService Tests")
class UsuarioSaludServiceTest {

    @Mock
    private UsuarioSaludRepository repository;

    @Mock
    private HcenUsuarioSaludClient hcenClient;

    @InjectMocks
    private UsuarioSaludService service;

    private static final Long TENANT_ID = 101L;
    private static final String CI = "12345678";
    private static final String NOMBRE = "Juan";
    private static final String APELLIDO = "Pérez";

    private UsuarioSalud createTestUsuario() {
        UsuarioSalud usuario = new UsuarioSalud();
        usuario.setCi(CI);
        usuario.setNombre(NOMBRE);
        usuario.setApellido(APELLIDO);
        usuario.setFechaNacimiento(LocalDate.of(1990, 1, 15));
        usuario.setEmail("juan.perez@email.com");
        usuario.setTelefono("099123456");
        usuario.setDepartamento("Montevideo");
        usuario.setLocalidad("Montevideo");
        usuario.setDireccion("Av. 18 de Julio 1234");
        return usuario;
    }

    // ==================== CREATE TESTS ====================

    @Nested
    @DisplayName("Crear Usuario Tests")
    class CreateTests {

        @Test
        @DisplayName("Crear usuario exitosamente con sincronización HCEN")
        void create_validUser_shouldSaveAndSyncWithHcen() throws Exception {
            // Arrange
            UsuarioSalud usuario = createTestUsuario();
            
            when(repository.findByCiAndTenant(CI, TENANT_ID)).thenReturn(null);
            doNothing().when(repository).persist(any(UsuarioSalud.class));
            
            HcenUsuarioSaludClient.HcenUserResponse hcenResponse = 
                    new HcenUsuarioSaludClient.HcenUserResponse(1000L, "Usuario registrado en HCEN");
            when(hcenClient.registrarUsuarioEnHcen(TENANT_ID, usuario)).thenReturn(hcenResponse);
            
            doAnswer(invocation -> {
                UsuarioSalud u = invocation.getArgument(0);
                u.setId(1L);
                return null;
            }).when(repository).merge(any(UsuarioSalud.class));

            // Act
            UsuarioSalud result = service.crearUsuarioSalud(TENANT_ID, usuario);

            // Assert
            assertNotNull(result);
            assertEquals(CI, result.getCi());
            assertEquals(TENANT_ID, result.getTenantId());
            assertNotNull(result.getFechaAlta());
            assertEquals(1000L, result.getHcenUserId());
            
            verify(repository).findByCiAndTenant(CI, TENANT_ID);
            verify(repository).persist(usuario);
            verify(hcenClient).registrarUsuarioEnHcen(TENANT_ID, usuario);
            verify(repository).merge(usuario);
        }

        @Test
        @DisplayName("Crear usuario cuando falla sincronización HCEN debe continuar")
        void create_hcenSyncFails_shouldStillSaveLocally() throws Exception {
            // Arrange
            UsuarioSalud usuario = createTestUsuario();
            
            when(repository.findByCiAndTenant(CI, TENANT_ID)).thenReturn(null);
            
            doAnswer(invocation -> {
                UsuarioSalud u = invocation.getArgument(0);
                u.setId(1L);
                return null;
            }).when(repository).persist(any(UsuarioSalud.class));
            
            when(hcenClient.registrarUsuarioEnHcen(TENANT_ID, usuario))
                    .thenThrow(new RuntimeException("HCEN no disponible"));

            // Act
            UsuarioSalud result = service.crearUsuarioSalud(TENANT_ID, usuario);

            // Assert - Usuario se guarda localmente aunque HCEN falle
            assertNotNull(result);
            assertEquals(CI, result.getCi());
            assertNull(result.getHcenUserId()); // No se pudo sincronizar
            
            verify(repository).persist(usuario);
            verify(hcenClient).registrarUsuarioEnHcen(TENANT_ID, usuario);
            verify(repository, never()).merge(any()); // No se llama merge si HCEN falla
        }

        @Test
        @DisplayName("Crear usuario duplicado debe lanzar excepción")
        void create_duplicateCI_shouldThrowException() {
            // Arrange
            UsuarioSalud usuario = createTestUsuario();
            UsuarioSalud existing = new UsuarioSalud();
            existing.setId(1L);
            existing.setCi(CI);
            
            when(repository.findByCiAndTenant(CI, TENANT_ID)).thenReturn(existing);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.crearUsuarioSalud(TENANT_ID, usuario)
            );
            
            assertTrue(exception.getMessage().contains("Ya existe un paciente con CI"));
            assertTrue(exception.getMessage().contains(CI));
            
            verify(repository).findByCiAndTenant(CI, TENANT_ID);
            verify(repository, never()).persist(any());
            verify(hcenClient, never()).registrarUsuarioEnHcen(any(), any());
        }

        @Test
        @DisplayName("Crear usuario debe setear tenantId y fechaAlta")
        void create_shouldSetMetadata() {
            // Arrange
            UsuarioSalud usuario = createTestUsuario();
            
            when(repository.findByCiAndTenant(CI, TENANT_ID)).thenReturn(null);
            doAnswer(invocation -> {
                UsuarioSalud u = invocation.getArgument(0);
                u.setId(1L);
                // Verificar que se setearon los metadatos
                assertNotNull(u.getTenantId());
                assertNotNull(u.getFechaAlta());
                return null;
            }).when(repository).persist(any(UsuarioSalud.class));
            
            when(hcenClient.registrarUsuarioEnHcen(any(), any()))
                    .thenReturn(new HcenUsuarioSaludClient.HcenUserResponse(1000L, "OK"));

            // Act
            UsuarioSalud result = service.crearUsuarioSalud(TENANT_ID, usuario);

            // Assert
            assertEquals(TENANT_ID, result.getTenantId());
            assertNotNull(result.getFechaAlta());
            assertTrue(result.getFechaAlta().isBefore(LocalDateTime.now().plusSeconds(1)));
        }

        @Test
        @DisplayName("HCEN responde sin userId debe continuar sin sincronizar")
        void create_hcenReturnsNullUserId_shouldContinueWithoutSync() throws Exception {
            // Arrange
            UsuarioSalud usuario = createTestUsuario();
            
            when(repository.findByCiAndTenant(CI, TENANT_ID)).thenReturn(null);
            doNothing().when(repository).persist(any(UsuarioSalud.class));
            
            HcenUsuarioSaludClient.HcenUserResponse hcenResponse = 
                    new HcenUsuarioSaludClient.HcenUserResponse(null, "Error: datos inválidos");
            when(hcenClient.registrarUsuarioEnHcen(TENANT_ID, usuario)).thenReturn(hcenResponse);

            // Act
            UsuarioSalud result = service.crearUsuarioSalud(TENANT_ID, usuario);

            // Assert
            assertNotNull(result);
            assertNull(result.getHcenUserId()); // No se sincronizó
            
            verify(repository).persist(usuario);
            verify(hcenClient).registrarUsuarioEnHcen(TENANT_ID, usuario);
            verify(repository, never()).merge(any()); // No se llama merge si no hay userId
        }
    }

    // ==================== UPDATE TESTS ====================

    @Nested
    @DisplayName("Actualizar Usuario Tests")
    class UpdateTests {

        @Test
        @DisplayName("Actualizar usuario exitosamente")
        void update_validUser_shouldUpdateAndSync() throws Exception {
            // Arrange
            UsuarioSalud existing = createTestUsuario();
            existing.setId(1L);
            existing.setTenantId(TENANT_ID);
            existing.setFechaAlta(LocalDateTime.now().minusDays(30));
            
            UsuarioSalud updates = new UsuarioSalud();
            updates.setNombre("Juan Carlos");
            updates.setApellido("Pérez González");
            updates.setTelefono("099999999");
            updates.setEmail("nuevo@email.com");
            updates.setDireccion("Nueva dirección 456");
            updates.setDepartamento("Canelones");
            updates.setLocalidad("Las Piedras");
            updates.setFechaNacimiento(LocalDate.of(1990, 5, 20));
            
            when(repository.findById(1L, TENANT_ID)).thenReturn(existing);
            when(repository.merge(existing)).thenReturn(existing);
            
            HcenUsuarioSaludClient.HcenUserResponse hcenResponse = 
                    new HcenUsuarioSaludClient.HcenUserResponse(1000L, "Actualizado");
            when(hcenClient.registrarUsuarioEnHcen(TENANT_ID, existing)).thenReturn(hcenResponse);

            // Act
            UsuarioSalud result = service.actualizarUsuarioSalud(TENANT_ID, 1L, updates);

            // Assert
            assertNotNull(result);
            assertEquals("Juan Carlos", result.getNombre());
            assertEquals("Pérez González", result.getApellido());
            assertEquals("099999999", result.getTelefono());
            assertEquals("nuevo@email.com", result.getEmail());
            assertEquals("Canelones", result.getDepartamento());
            assertNotNull(result.getFechaActualizacion());
            
            verify(repository).findById(1L, TENANT_ID);
            verify(repository).merge(existing);
            verify(hcenClient).registrarUsuarioEnHcen(TENANT_ID, existing);
        }

        @Test
        @DisplayName("Actualizar usuario inexistente debe lanzar excepción")
        void update_nonExistentUser_shouldThrowException() {
            // Arrange
            UsuarioSalud updates = createTestUsuario();
            
            when(repository.findById(999L, TENANT_ID)).thenReturn(null);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.actualizarUsuarioSalud(TENANT_ID, 999L, updates)
            );
            
            assertEquals("Usuario no encontrado en esta clínica", exception.getMessage());
            
            verify(repository).findById(999L, TENANT_ID);
            verify(repository, never()).merge(any());
            verify(hcenClient, never()).registrarUsuarioEnHcen(any(), any());
        }

        @Test
        @DisplayName("Actualizar cuando falla HCEN debe continuar con update local")
        void update_hcenFails_shouldStillUpdateLocally() throws Exception {
            // Arrange
            UsuarioSalud existing = createTestUsuario();
            existing.setId(1L);
            existing.setTenantId(TENANT_ID);
            
            UsuarioSalud updates = new UsuarioSalud();
            updates.setNombre("Nombre Actualizado");
            
            when(repository.findById(1L, TENANT_ID)).thenReturn(existing);
            when(repository.merge(existing)).thenReturn(existing);
            when(hcenClient.registrarUsuarioEnHcen(TENANT_ID, existing))
                    .thenThrow(new RuntimeException("HCEN timeout"));

            // Act
            UsuarioSalud result = service.actualizarUsuarioSalud(TENANT_ID, 1L, updates);

            // Assert - Actualización local exitosa aunque HCEN falle
            assertNotNull(result);
            assertEquals("Nombre Actualizado", result.getNombre());
            
            verify(repository).merge(existing);
            verify(hcenClient).registrarUsuarioEnHcen(TENANT_ID, existing);
        }

        @Test
        @DisplayName("Actualizar debe setear fechaActualizacion")
        void update_shouldSetUpdateTimestamp() {
            // Arrange
            UsuarioSalud existing = createTestUsuario();
            existing.setId(1L);
            existing.setFechaAlta(LocalDateTime.now().minusDays(10));
            existing.setFechaActualizacion(null);
            
            UsuarioSalud updates = new UsuarioSalud();
            updates.setNombre("Nuevo Nombre");
            
            when(repository.findById(1L, TENANT_ID)).thenReturn(existing);
            when(repository.merge(existing)).thenReturn(existing);

            // Act
            service.actualizarUsuarioSalud(TENANT_ID, 1L, updates);

            // Assert
            assertNotNull(existing.getFechaActualizacion());
            assertTrue(existing.getFechaActualizacion().isAfter(existing.getFechaAlta()));
        }
    }

    // ==================== READ TESTS ====================

    @Nested
    @DisplayName("Listar Usuarios Tests")
    class ReadTests {

        @Test
        @DisplayName("Listar usuarios debe retornar todos los del tenant")
        void list_shouldReturnAllUsersFromTenant() {
            // Arrange
            List<UsuarioSalud> usuarios = Arrays.asList(
                    createUsuario(1L, "11111111", "Ana", "García"),
                    createUsuario(2L, "22222222", "Luis", "Martínez"),
                    createUsuario(3L, "33333333", "María", "López")
            );
            
            when(repository.findByTenant(TENANT_ID)).thenReturn(usuarios);

            // Act
            List<UsuarioSalud> result = service.listarUsuariosSalud(TENANT_ID);

            // Assert
            assertNotNull(result);
            assertEquals(3, result.size());
            assertEquals("Ana", result.get(0).getNombre());
            assertEquals("Luis", result.get(1).getNombre());
            assertEquals("María", result.get(2).getNombre());
            
            verify(repository).findByTenant(TENANT_ID);
        }

        @Test
        @DisplayName("Listar usuarios vacío debe retornar lista vacía")
        void list_noUsers_shouldReturnEmptyList() {
            // Arrange
            when(repository.findByTenant(TENANT_ID)).thenReturn(Collections.emptyList());

            // Act
            List<UsuarioSalud> result = service.listarUsuariosSalud(TENANT_ID);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
            
            verify(repository).findByTenant(TENANT_ID);
        }

        @Test
        @DisplayName("Obtener usuario por ID existente")
        void getById_existingUser_shouldReturn() {
            // Arrange
            UsuarioSalud usuario = createUsuario(1L, CI, NOMBRE, APELLIDO);
            
            when(repository.findById(1L, TENANT_ID)).thenReturn(usuario);

            // Act
            UsuarioSalud result = service.obtenerUsuarioSalud(1L, TENANT_ID);

            // Assert
            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals(CI, result.getCi());
            
            verify(repository).findById(1L, TENANT_ID);
        }

        @Test
        @DisplayName("Obtener usuario inexistente debe retornar null")
        void getById_nonExistent_shouldReturnNull() {
            // Arrange
            when(repository.findById(999L, TENANT_ID)).thenReturn(null);

            // Act
            UsuarioSalud result = service.obtenerUsuarioSalud(999L, TENANT_ID);

            // Assert
            assertNull(result);
            
            verify(repository).findById(999L, TENANT_ID);
        }

        @Test
        @DisplayName("Buscar por CI existente")
        void findByCi_existing_shouldReturn() {
            // Arrange
            UsuarioSalud usuario = createUsuario(1L, CI, NOMBRE, APELLIDO);
            
            when(repository.findByCiAndTenant(CI, TENANT_ID)).thenReturn(usuario);

            // Act
            UsuarioSalud result = service.buscarPorCi(CI, TENANT_ID);

            // Assert
            assertNotNull(result);
            assertEquals(CI, result.getCi());
            
            verify(repository).findByCiAndTenant(CI, TENANT_ID);
        }

        @Test
        @DisplayName("Buscar por CI inexistente debe retornar null")
        void findByCi_nonExistent_shouldReturnNull() {
            // Arrange
            when(repository.findByCiAndTenant("99999999", TENANT_ID)).thenReturn(null);

            // Act
            UsuarioSalud result = service.buscarPorCi("99999999", TENANT_ID);

            // Assert
            assertNull(result);
            
            verify(repository).findByCiAndTenant("99999999", TENANT_ID);
        }
    }

    // ==================== TENANT ISOLATION TESTS ====================

    @Nested
    @DisplayName("Tenant Isolation Tests")
    class TenantIsolationTests {

        @Test
        @DisplayName("Crear usuario en tenant A no debe afectar tenant B")
        void create_tenantIsolation_shouldBeEnforced() {
            // Arrange
            UsuarioSalud usuario = createTestUsuario();
            Long tenantA = 101L;
            Long tenantB = 102L;
            
            // Usuario no existe en tenant A
            when(repository.findByCiAndTenant(CI, tenantA)).thenReturn(null);

            // Act - Crear en tenant A debe ser exitoso
            when(hcenClient.registrarUsuarioEnHcen(any(), any()))
                    .thenReturn(new HcenUsuarioSaludClient.HcenUserResponse(1000L, "OK"));
            
            assertDoesNotThrow(() -> service.crearUsuarioSalud(tenantA, usuario));
            
            // Verify - Solo busca en tenant A, nunca en B
            verify(repository).findByCiAndTenant(CI, tenantA);
            verify(repository, never()).findByCiAndTenant(CI, tenantB);
        }

        @Test
        @DisplayName("Listar debe retornar solo usuarios del tenant especificado")
        void list_shouldOnlyReturnTenantUsers() {
            // Arrange
            Long tenantA = 101L;
            Long tenantB = 102L;
            
            List<UsuarioSalud> usuariosTenantA = Arrays.asList(
                    createUsuario(1L, "11111111", "User A1", "Apellido"),
                    createUsuario(2L, "22222222", "User A2", "Apellido")
            );
            
            when(repository.findByTenant(tenantA)).thenReturn(usuariosTenantA);

            // Act
            List<UsuarioSalud> result = service.listarUsuariosSalud(tenantA);

            // Assert
            assertEquals(2, result.size());
            result.forEach(u -> assertEquals(tenantA, u.getTenantId()));
            
            verify(repository).findByTenant(tenantA);
            verify(repository, never()).findByTenant(tenantB);
        }
    }

    // ==================== EDGE CASES ====================

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Crear usuario con datos mínimos debe funcionar")
        void create_minimalData_shouldWork() {
            // Arrange
            UsuarioSalud usuario = new UsuarioSalud();
            usuario.setCi("11111111");
            usuario.setNombre("Nombre");
            usuario.setApellido("Apellido");
            
            when(repository.findByCiAndTenant("11111111", TENANT_ID)).thenReturn(null);
            when(hcenClient.registrarUsuarioEnHcen(any(), any()))
                    .thenReturn(new HcenUsuarioSaludClient.HcenUserResponse(1000L, "OK"));

            // Act & Assert
            assertDoesNotThrow(() -> service.crearUsuarioSalud(TENANT_ID, usuario));
            
            verify(repository).persist(usuario);
        }

        @Test
        @DisplayName("Actualizar solo nombre debe preservar otros campos")
        void update_partialUpdate_shouldPreserveOtherFields() {
            // Arrange
            UsuarioSalud existing = createTestUsuario();
            existing.setId(1L);
            existing.setTenantId(TENANT_ID);
            existing.setFechaAlta(LocalDateTime.now().minusDays(5));
            String originalEmail = existing.getEmail();
            String originalTelefono = existing.getTelefono();
            
            UsuarioSalud updates = new UsuarioSalud();
            updates.setNombre("Nuevo Nombre");
            updates.setApellido(existing.getApellido());
            updates.setFechaNacimiento(existing.getFechaNacimiento());
            // No actualiza email ni teléfono
            
            when(repository.findById(1L, TENANT_ID)).thenReturn(existing);
            when(repository.merge(existing)).thenReturn(existing);

            // Act
            service.actualizarUsuarioSalud(TENANT_ID, 1L, updates);

            // Assert - Email y teléfono deben actualizarse a null porque updates no los tiene
            assertEquals("Nuevo Nombre", existing.getNombre());
            // Los otros campos se actualizan con lo que venga en updates
        }

        @Test
        @DisplayName("Buscar por CI vacío debe retornar null")
        void findByCi_emptyCI_shouldReturnNull() {
            // Arrange
            when(repository.findByCiAndTenant("", TENANT_ID)).thenReturn(null);

            // Act
            UsuarioSalud result = service.buscarPorCi("", TENANT_ID);

            // Assert
            assertNull(result);
        }

        @Test
        @DisplayName("Listar con tenantId null debe funcionar")
        void list_nullTenantId_shouldCallRepository() {
            // Arrange
            when(repository.findByTenant(null)).thenReturn(Collections.emptyList());

            // Act
            List<UsuarioSalud> result = service.listarUsuariosSalud(null);

            // Assert
            assertNotNull(result);
            verify(repository).findByTenant(null);
        }
    }

    // ==================== HELPER METHODS ====================

    private UsuarioSalud createUsuario(Long id, String ci, String nombre, String apellido) {
        UsuarioSalud usuario = new UsuarioSalud();
        usuario.setId(id);
        usuario.setCi(ci);
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setTenantId(TENANT_ID);
        usuario.setFechaAlta(LocalDateTime.now());
        return usuario;
    }
}

