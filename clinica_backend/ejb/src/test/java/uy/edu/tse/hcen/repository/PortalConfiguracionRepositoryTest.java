package uy.edu.tse.hcen.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.tse.hcen.model.PortalConfiguracion;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PortalConfiguracionRepository.
 * 
 * @author Senior Test Engineer
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PortalConfiguracionRepository Tests")
class PortalConfiguracionRepositoryTest {

    @Mock
    private EntityManager em;

    @Mock
    private TypedQuery<PortalConfiguracion> query;

    @InjectMocks
    private PortalConfiguracionRepository repository;

    @Test
    @DisplayName("FindCurrentConfig existente debe retornar Optional con config")
    void findCurrentConfig_exists_shouldReturn() {
        // Arrange
        PortalConfiguracion config = new PortalConfiguracion();
        config.setId(1L);
        config.setColorPrimario("#007bff");
        config.setNombrePortal("Portal Test");
        
        when(em.createQuery(anyString(), eq(PortalConfiguracion.class))).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
        when(query.getSingleResult()).thenReturn(config);

        // Act
        Optional<PortalConfiguracion> result = repository.findCurrentConfig();

        // Assert
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        assertEquals("#007bff", result.get().getColorPrimario());
        
        verify(em).createQuery(anyString(), eq(PortalConfiguracion.class));
        verify(query).setMaxResults(1);
    }

    @Test
    @DisplayName("FindCurrentConfig sin config debe retornar vacío")
    void findCurrentConfig_notFound_shouldReturnEmpty() {
        // Arrange
        when(em.createQuery(anyString(), eq(PortalConfiguracion.class))).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
        when(query.getSingleResult()).thenThrow(new NoResultException());

        // Act
        Optional<PortalConfiguracion> result = repository.findCurrentConfig();

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Save con ID null debe hacer persist")
    void save_newEntity_shouldPersist() {
        // Arrange
        PortalConfiguracion config = new PortalConfiguracion();
        config.setId(null);
        config.setColorPrimario("#ff0000");
        
        doNothing().when(em).persist(config);

        // Act
        PortalConfiguracion result = repository.save(config);

        // Assert
        assertEquals(config, result);
        verify(em).persist(config);
        verify(em, never()).merge(any());
    }

    @Test
    @DisplayName("Save con ID existente debe hacer merge")
    void save_existingEntity_shouldMerge() {
        // Arrange
        PortalConfiguracion config = new PortalConfiguracion();
        config.setId(1L);
        config.setColorPrimario("#00ff00");
        
        when(em.merge(config)).thenReturn(config);

        // Act
        PortalConfiguracion result = repository.save(config);

        // Assert
        assertEquals(config, result);
        verify(em).merge(config);
        verify(em, never()).persist(any());
    }

    @Test
    @DisplayName("Save debe retornar la entidad actualizada del merge")
    void save_shouldReturnMergedEntity() {
        // Arrange
        PortalConfiguracion config = new PortalConfiguracion();
        config.setId(1L);
        
        PortalConfiguracion merged = new PortalConfiguracion();
        merged.setId(1L);
        merged.setColorPrimario("#updated");
        
        when(em.merge(config)).thenReturn(merged);

        // Act
        PortalConfiguracion result = repository.save(config);

        // Assert
        assertEquals(merged, result);
        assertEquals("#updated", result.getColorPrimario());
    }
}

