package uy.edu.tse.hcen.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.tse.hcen.model.UsuarioSalud;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UsuarioSaludRepository.
 * Tests native SQL queries, mapping, and error handling.
 * 
 * @author Senior Test Engineer
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioSaludRepository Tests")
class UsuarioSaludRepositoryTest {

    @Mock
    private EntityManager em;

    @Mock
    private Query query;

    @InjectMocks
    private UsuarioSaludRepository repository;

    private static final Long TENANT_ID = 101L;
    private static final String CI = "12345678";
    private static final String SCHEMA = "schema_clinica_101";

    private Object[] createMockRow() {
        return new Object[]{
                1L,                                      // id
                CI,                                       // ci
                "Juan",                                   // nombre
                "Pérez",                                  // apellido
                Date.valueOf(LocalDate.of(1990, 1, 15)), // fecha_nacimiento
                "Av. Italia 2025",                       // direccion
                "099123456",                              // telefono
                "juan@email.com",                         // email
                "Montevideo",                             // departamento
                "Montevideo",                             // localidad
                1000L,                                    // hcen_user_id
                TENANT_ID,                                // tenant_id
                Timestamp.valueOf(LocalDateTime.now()),   // fecha_alta
                null                                      // fecha_actualizacion
        };
    }

    // ==================== FIND BY CI TESTS ====================

    @Nested
    @DisplayName("FindByCiAndTenant Tests")
    class FindByCiTests {

        @Test
        @DisplayName("Buscar por CI existente debe retornar usuario")
        void findByCi_existing_shouldReturn() {
            // Arrange
            when(em.createNativeQuery(anyString())).thenReturn(query);
            when(query.getSingleResult()).thenReturn(createMockRow());

            // Act
            UsuarioSalud result = repository.findByCiAndTenant(CI, TENANT_ID);

            // Assert
            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals(CI, result.getCi());
            assertEquals("Juan", result.getNombre());
            assertEquals("Pérez", result.getApellido());
            assertEquals(TENANT_ID, result.getTenantId());
            
            verify(em).createNativeQuery(contains(SCHEMA));
            verify(query).setParameter("ci", CI);
            verify(query).setParameter("tenantId", TENANT_ID);
        }

        @Test
        @DisplayName("Buscar por CI inexistente debe retornar null")
        void findByCi_notFound_shouldReturnNull() {
            // Arrange
            when(em.createNativeQuery(anyString())).thenReturn(query);
            when(query.getSingleResult()).thenThrow(new NoResultException());

            // Act
            UsuarioSalud result = repository.findByCiAndTenant("99999999", TENANT_ID);

            // Assert
            assertNull(result);
        }

        @Test
        @DisplayName("Buscar con error SQL debe retornar null")
        void findByCi_sqlError_shouldReturnNull() {
            // Arrange
            when(em.createNativeQuery(anyString())).thenThrow(new RuntimeException("SQL error"));

            // Act
            UsuarioSalud result = repository.findByCiAndTenant(CI, TENANT_ID);

            // Assert
            assertNull(result);
        }
    }

    // ==================== FIND BY TENANT TESTS ====================

    @Nested
    @DisplayName("FindByTenant Tests")
    class FindByTenantTests {

        @Test
        @DisplayName("Buscar por tenant con usuarios debe retornar lista")
        void findByTenant_withUsers_shouldReturnList() {
            // Arrange
            when(em.createNativeQuery(anyString())).thenReturn(query);
            when(query.getResultList()).thenReturn(List.of(createMockRow(), createMockRow()));

            // Act
            List<UsuarioSalud> result = repository.findByTenant(TENANT_ID);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            
            verify(em).createNativeQuery(contains(SCHEMA));
            verify(query).setParameter("tenantId", TENANT_ID);
        }

        @Test
        @DisplayName("Buscar por tenant sin usuarios debe retornar lista vacía")
        void findByTenant_noUsers_shouldReturnEmpty() {
            // Arrange
            when(em.createNativeQuery(anyString())).thenReturn(query);
            when(query.getResultList()).thenReturn(Collections.emptyList());

            // Act
            List<UsuarioSalud> result = repository.findByTenant(TENANT_ID);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Buscar con error debe retornar lista vacía")
        void findByTenant_error_shouldReturnEmpty() {
            // Arrange
            when(em.createNativeQuery(anyString())).thenThrow(new RuntimeException("DB error"));

            // Act
            List<UsuarioSalud> result = repository.findByTenant(TENANT_ID);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==================== FIND BY ID TESTS ====================

    @Nested
    @DisplayName("FindById Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Buscar por ID existente debe retornar usuario")
        void findById_existing_shouldReturn() {
            // Arrange
            when(em.createNativeQuery(anyString())).thenReturn(query);
            when(query.getSingleResult()).thenReturn(createMockRow());

            // Act
            UsuarioSalud result = repository.findById(1L, TENANT_ID);

            // Assert
            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals(CI, result.getCi());
            
            verify(query).setParameter("id", 1L);
        }

        @Test
        @DisplayName("Buscar por ID inexistente debe retornar null")
        void findById_notFound_shouldReturnNull() {
            // Arrange
            when(em.createNativeQuery(anyString())).thenReturn(query);
            when(query.getSingleResult()).thenThrow(new NoResultException());

            // Act
            UsuarioSalud result = repository.findById(999L, TENANT_ID);

            // Assert
            assertNull(result);
        }
    }

    // ==================== FIND BY HCEN USER ID TESTS ====================

    @Nested
    @DisplayName("FindByHcenUserId Tests")
    class FindByHcenUserIdTests {

        @Test
        @DisplayName("Buscar por hcenUserId existente debe retornar usuario")
        void findByHcenUserId_existing_shouldReturn() {
            // Arrange
            when(em.createNativeQuery(anyString())).thenReturn(query);
            when(query.getSingleResult()).thenReturn(createMockRow());

            // Act
            UsuarioSalud result = repository.findByHcenUserId(1000L, TENANT_ID);

            // Assert
            assertNotNull(result);
            assertEquals(1000L, result.getHcenUserId());
            
            verify(query).setParameter("hcenUserId", 1000L);
            verify(query).setParameter("tenantId", TENANT_ID);
        }

        @Test
        @DisplayName("Buscar por hcenUserId inexistente debe retornar null")
        void findByHcenUserId_notFound_shouldReturnNull() {
            // Arrange
            when(em.createNativeQuery(anyString())).thenReturn(query);
            when(query.getSingleResult()).thenThrow(new NoResultException());

            // Act
            UsuarioSalud result = repository.findByHcenUserId(9999L, TENANT_ID);

            // Assert
            assertNull(result);
        }
    }

    // ==================== PERSIST TESTS ====================

    @Nested
    @DisplayName("Persist Tests")
    class PersistTests {

        @Test
        @DisplayName("Persist debe insertar usuario y setear ID")
        void persist_shouldInsertAndSetId() {
            // Arrange
            UsuarioSalud usuario = new UsuarioSalud();
            usuario.setCi(CI);
            usuario.setNombre("Juan");
            usuario.setApellido("Pérez");
            usuario.setTenantId(TENANT_ID);
            usuario.setFechaAlta(LocalDateTime.now());
            
            when(em.createNativeQuery(anyString())).thenReturn(query);
            when(query.getSingleResult()).thenReturn(5L);

            // Act
            repository.persist(usuario);

            // Assert
            assertEquals(5L, usuario.getId());
            
            verify(em).createNativeQuery(contains("INSERT INTO " + SCHEMA));
            verify(query).setParameter("ci", CI);
            verify(query).setParameter("tenantId", TENANT_ID);
        }

        @Test
        @DisplayName("Persist con valores null debe funcionar")
        void persist_withNulls_shouldWork() {
            // Arrange
            UsuarioSalud usuario = new UsuarioSalud();
            usuario.setCi(CI);
            usuario.setTenantId(TENANT_ID);
            usuario.setFechaAlta(LocalDateTime.now());
            // Muchos campos null
            
            when(em.createNativeQuery(anyString())).thenReturn(query);
            when(query.getSingleResult()).thenReturn(1L);

            // Act
            repository.persist(usuario);

            // Assert
            assertEquals(1L, usuario.getId());
        }
    }

    // ==================== MERGE TESTS ====================

    @Nested
    @DisplayName("Merge Tests")
    class MergeTests {

        @Test
        @DisplayName("Merge debe actualizar usuario")
        void merge_shouldUpdate() {
            // Arrange
            UsuarioSalud usuario = new UsuarioSalud();
            usuario.setId(1L);
            usuario.setCi(CI);
            usuario.setNombre("Juan Updated");
            usuario.setApellido("Pérez");
            usuario.setTenantId(TENANT_ID);
            usuario.setFechaActualizacion(LocalDateTime.now());
            
            when(em.createNativeQuery(anyString())).thenReturn(query);
            when(query.executeUpdate()).thenReturn(1);

            // Act
            UsuarioSalud result = repository.merge(usuario);

            // Assert
            assertNotNull(result);
            assertEquals(usuario, result);
            
            verify(em).createNativeQuery(contains("UPDATE " + SCHEMA));
            verify(query).setParameter("id", 1L);
            verify(query).setParameter("nombre", "Juan Updated");
        }
    }

    // ==================== REMOVE TESTS ====================

    @Nested
    @DisplayName("Remove Tests")
    class RemoveTests {

        @Test
        @DisplayName("Remove debe eliminar usuario")
        void remove_shouldDelete() {
            // Arrange
            UsuarioSalud usuario = new UsuarioSalud();
            usuario.setId(1L);
            usuario.setTenantId(TENANT_ID);
            
            when(em.createNativeQuery(anyString())).thenReturn(query);
            when(query.executeUpdate()).thenReturn(1);

            // Act
            repository.remove(usuario);

            // Assert
            verify(em).createNativeQuery(contains("DELETE FROM " + SCHEMA));
            verify(query).setParameter("id", 1L);
            verify(query).executeUpdate();
        }
    }

    // ==================== EDGE CASES ====================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Mapeo con fecha null debe funcionar")
        void mapping_withNullDates_shouldWork() {
            // Arrange
            Object[] row = new Object[]{
                    1L, CI, "Juan", "Pérez",
                    null, // fecha_nacimiento null
                    "Dir", "Tel", "email",
                    "Montevideo", "Montevideo",
                    null, // hcen_user_id null
                    TENANT_ID,
                    null, // fecha_alta null
                    null  // fecha_actualizacion null
            };
            
            when(em.createNativeQuery(anyString())).thenReturn(query);
            when(query.getSingleResult()).thenReturn(row);

            // Act
            UsuarioSalud result = repository.findByCiAndTenant(CI, TENANT_ID);

            // Assert
            assertNotNull(result);
            assertNull(result.getFechaNacimiento());
            assertNull(result.getHcenUserId());
            assertNull(result.getFechaAlta());
        }

        @Test
        @DisplayName("Query con CI null debe manejar correctamente")
        void findByCi_nullCi_shouldHandleGracefully() {
            // Arrange
            when(em.createNativeQuery(anyString())).thenReturn(query);
            when(query.getSingleResult()).thenThrow(new RuntimeException());

            // Act
            UsuarioSalud result = repository.findByCiAndTenant(null, TENANT_ID);

            // Assert
            assertNull(result);
        }

        @Test
        @DisplayName("Query con tenantId null debe manejar excepción")
        void findByCi_nullTenant_shouldReturnNull() {
            // Arrange
            when(em.createNativeQuery(anyString())).thenThrow(new RuntimeException());

            // Act
            UsuarioSalud result = repository.findByCiAndTenant(CI, null);

            // Assert
            assertNull(result);
        }
    }
}

