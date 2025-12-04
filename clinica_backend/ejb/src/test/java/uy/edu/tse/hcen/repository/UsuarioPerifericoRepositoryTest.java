package uy.edu.tse.hcen.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.tse.hcen.model.UsuarioPeriferico;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioPerifericoRepository Tests")
class UsuarioPerifericoRepositoryTest {

    @Mock
    private EntityManager em;

    @Mock
    private Query query;

    @InjectMocks
    private UsuarioPerifericoRepository repository;

    @Test
    @DisplayName("FindByNicknameInTenantSchema existente debe retornar usuario")
    void findByNicknameInTenantSchema_existing_shouldReturn() {
        // Arrange
        Object[] row = new Object[]{
                1L, "doctor1", "hash123", "PROFESIONAL",
                "Dr. Juan", "doctor@email.com", "CARDIOLOGIA", "MONTEVIDEO", "ProfesionalSalud"
        };
        
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(row);

        // Act
        UsuarioPeriferico result = repository.findByNicknameInTenantSchema("doctor1", "schema_clinica_101");

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("doctor1", result.getNickname());
        assertEquals("PROFESIONAL", result.getRole());
        
        verify(query).setParameter(1, "doctor1");
    }

    @Test
    @DisplayName("FindByNicknameInTenantSchema inexistente debe retornar null")
    void findByNicknameInTenantSchema_notFound_shouldReturnNull() {
        // Arrange
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenThrow(new NoResultException());

        // Act
        UsuarioPeriferico result = repository.findByNicknameInTenantSchema("nonexistent", "schema_clinica_101");

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("FindByNicknameInTenantSchema con ID BigInteger debe funcionar")
    void findByNicknameInTenantSchema_bigIntegerId_shouldWork() {
        // Arrange
        Object[] row = new Object[]{
                BigInteger.valueOf(1L), "doctor1", "hash123", "PROFESIONAL",
                "Dr. Juan", "doctor@email.com", "CARDIOLOGIA", "MONTEVIDEO", "ProfesionalSalud"
        };
        
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(row);

        // Act
        UsuarioPeriferico result = repository.findByNicknameInTenantSchema("doctor1", "schema_clinica_101");

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("FindByNicknameForLogin existente debe retornar usuario")
    void findByNicknameForLogin_existing_shouldReturn() {
        // Arrange
        Object[] row = new Object[]{
                1L, "admin1", "hash123", "101", "ADMINISTRADOR",
                "Admin", "admin@clinic.com"
        };
        
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(row);

        // Act
        UsuarioPeriferico result = repository.findByNicknameForLogin("admin1");

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("admin1", result.getNickname());
        assertEquals("101", result.getTenantId());
        assertEquals("ADMINISTRADOR", result.getRole());
        
        verify(query).setParameter(1, "admin1");
    }

    @Test
    @DisplayName("FindByNicknameForLogin inexistente debe retornar null")
    void findByNicknameForLogin_notFound_shouldReturnNull() {
        // Arrange
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenThrow(new NoResultException());

        // Act
        UsuarioPeriferico result = repository.findByNicknameForLogin("nonexistent");

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("FindByNicknameForLogin con error debe retornar null")
    void findByNicknameForLogin_error_shouldReturnNull() {
        // Arrange
        when(em.createNativeQuery(anyString())).thenThrow(new RuntimeException("DB error"));

        // Act
        UsuarioPeriferico result = repository.findByNicknameForLogin("admin1");

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("FindByNicknameInTenantSchema con error debe retornar null")
    void findByNicknameInTenantSchema_error_shouldReturnNull() {
        // Arrange
        when(em.createNativeQuery(anyString())).thenThrow(new RuntimeException("SQL error"));

        // Act
        UsuarioPeriferico result = repository.findByNicknameInTenantSchema("doctor1", "schema_clinica_101");

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("Mapeo con role null debe deducir de dtype")
    void mapping_nullRole_shouldDeduceFromDtype() {
        // Arrange
        Object[] row = new Object[]{
                1L, "doctor1", "hash123", null, // role null
                "Dr. Juan", "doctor@email.com", "CARDIOLOGIA", "MONTEVIDEO", "ProfesionalSalud"
        };
        
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(row);

        // Act
        UsuarioPeriferico result = repository.findByNicknameInTenantSchema("doctor1", "schema_clinica_101");

        // Assert
        assertNotNull(result);
        assertEquals("PROFESIONAL", result.getRole()); // Deducido de dtype
    }
}

