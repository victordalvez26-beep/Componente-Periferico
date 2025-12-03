package uy.edu.tse.hcen.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.tse.hcen.model.PortalConfiguracion;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortalConfiguracionRepositoryTest {

    @Mock
    private EntityManager em;

    @Mock
    private TypedQuery<PortalConfiguracion> query;

    @InjectMocks
    private PortalConfiguracionRepository repository;

    private PortalConfiguracion config;

    @BeforeEach
    void setUp() throws Exception {
        config = new PortalConfiguracion();
        config.setId(1L);
        config.setColorPrimario("#007bff");
        config.setColorSecundario("#6c757d");
        config.setLogoUrl("http://example.com/logo.png");
        config.setNombrePortal("Clínica Test");
        
        // Inyectar EntityManager mock usando reflection
        java.lang.reflect.Field emField = PortalConfiguracionRepository.class.getDeclaredField("em");
        emField.setAccessible(true);
        emField.set(repository, em);
    }

    @Test
    void testFindCurrentConfig() {
        // Arrange
        when(em.createQuery(eq("SELECT c FROM PortalConfiguracion c"), eq(PortalConfiguracion.class))).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
        when(query.getSingleResult()).thenReturn(config);

        // Act
        Optional<PortalConfiguracion> result = repository.findCurrentConfig();

        // Assert
        assertTrue(result.isPresent());
        assertEquals(config.getId(), result.get().getId());
        verify(em, times(1)).createQuery(eq("SELECT c FROM PortalConfiguracion c"), eq(PortalConfiguracion.class));
        verify(query, times(1)).setMaxResults(1);
        verify(query, times(1)).getSingleResult();
    }

    @Test
    void testFindCurrentConfigNotFound() {
        // Arrange
        when(em.createQuery(eq("SELECT c FROM PortalConfiguracion c"), eq(PortalConfiguracion.class))).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
        when(query.getSingleResult()).thenThrow(new NoResultException());

        // Act
        Optional<PortalConfiguracion> result = repository.findCurrentConfig();

        // Assert
        assertFalse(result.isPresent());
        verify(em, times(1)).createQuery(eq("SELECT c FROM PortalConfiguracion c"), eq(PortalConfiguracion.class));
        verify(query, times(1)).setMaxResults(1);
        verify(query, times(1)).getSingleResult();
    }

    @Test
    void testSaveNew() {
        // Arrange
        PortalConfiguracion newConfig = new PortalConfiguracion();
        newConfig.setId(null); // New entity
        doNothing().when(em).persist(any(PortalConfiguracion.class));

        // Act
        PortalConfiguracion result = repository.save(newConfig);

        // Assert
        assertNotNull(result);
        verify(em).persist(newConfig);
        verify(em, never()).merge(any(PortalConfiguracion.class));
    }

    @Test
    void testFindCurrentConfigWithEmptyResult() {
        // Arrange
        when(em.createQuery(eq("SELECT c FROM PortalConfiguracion c"), eq(PortalConfiguracion.class))).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
        when(query.getSingleResult()).thenThrow(new NoResultException());

        // Act
        Optional<PortalConfiguracion> result = repository.findCurrentConfig();

        // Assert
        assertFalse(result.isPresent());
        assertTrue(result.isEmpty());
        verify(em, times(1)).createQuery(eq("SELECT c FROM PortalConfiguracion c"), eq(PortalConfiguracion.class));
        verify(query, times(1)).setMaxResults(1);
        verify(query, times(1)).getSingleResult();
    }

    @Test
    void testSaveExisting() {
        // Arrange
        config.setId(1L); // Existing entity
        when(em.merge(config)).thenReturn(config);

        // Act
        PortalConfiguracion result = repository.save(config);

        // Assert
        assertNotNull(result);
        verify(em, never()).persist(any(PortalConfiguracion.class));
        verify(em).merge(config);
    }

    @Test
    void testSaveWithZeroId() {
        // Arrange
        PortalConfiguracion newConfig = new PortalConfiguracion();
        newConfig.setId(0L);
        // Cuando ID es 0 (no null), el código usa merge, no persist
        when(em.merge(newConfig)).thenReturn(newConfig);

        // Act
        PortalConfiguracion result = repository.save(newConfig);

        // Assert
        assertNotNull(result);
        verify(em, never()).persist(any(PortalConfiguracion.class));
        verify(em, times(1)).merge(newConfig);
    }
}

