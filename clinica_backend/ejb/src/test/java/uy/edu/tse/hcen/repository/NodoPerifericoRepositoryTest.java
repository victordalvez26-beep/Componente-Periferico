package uy.edu.tse.hcen.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.tse.hcen.model.NodoPeriferico;
import uy.edu.tse.hcen.model.PrestadorSalud;
import uy.edu.tse.hcen.model.enums.Departamentos;
import uy.edu.tse.hcen.model.enums.EstadoNodoPeriferico;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NodoPerifericoRepository.
 * 
 * @author Senior Test Engineer
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NodoPerifericoRepository Tests")
class NodoPerifericoRepositoryTest {

    @Mock
    private EntityManager em;

    @InjectMocks
    private NodoPerifericoRepository repository;

    @Test
    @DisplayName("FindById existente debe retornar Optional con nodo")
    void findById_existing_shouldReturnOptional() {
        // Arrange
        PrestadorSalud nodo = new PrestadorSalud(
                "Clínica Test", "123", Departamentos.MONTEVIDEO, 
                "MVD", "Dir", "999", EstadoNodoPeriferico.ACTIVO);
        nodo.setId(101L);
        
        when(em.find(NodoPeriferico.class, 101L)).thenReturn(nodo);

        // Act
        Optional<NodoPeriferico> result = repository.findById(101L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(101L, result.get().getId());
        assertEquals("Clínica Test", result.get().getNombre());
        
        verify(em).find(NodoPeriferico.class, 101L);
    }

    @Test
    @DisplayName("FindById inexistente debe retornar Optional vacío")
    void findById_notFound_shouldReturnEmpty() {
        // Arrange
        when(em.find(NodoPeriferico.class, 999L)).thenReturn(null);
        when(em.getReference(NodoPeriferico.class, 999L)).thenThrow(new RuntimeException("Not found"));

        // Act
        Optional<NodoPeriferico> result = repository.findById(999L);

        // Assert
        assertFalse(result.isPresent());
        
        verify(em).find(NodoPeriferico.class, 999L);
    }

    @Test
    @DisplayName("FindById con null debe manejar excepción")
    void findById_null_shouldHandleException() {
        // Arrange
        when(em.find(NodoPeriferico.class, null)).thenThrow(new RuntimeException());

        // Act
        Optional<NodoPeriferico> result = repository.findById(null);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("FindById con EntityManager lanzando excepción debe retornar vacío")
    void findById_emThrows_shouldReturnEmpty() {
        // Arrange
        when(em.find(NodoPeriferico.class, 101L)).thenThrow(new RuntimeException("DB error"));

        // Act
        Optional<NodoPeriferico> result = repository.findById(101L);

        // Assert
        assertFalse(result.isPresent());
    }
}

