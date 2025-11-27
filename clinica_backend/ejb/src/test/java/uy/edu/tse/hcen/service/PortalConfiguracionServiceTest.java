package uy.edu.tse.hcen.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.tse.hcen.dto.ConfiguracionPortalDTO;
import uy.edu.tse.hcen.model.PortalConfiguracion;
import uy.edu.tse.hcen.repository.PortalConfiguracionRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortalConfiguracionServiceTest {

    @Mock
    private PortalConfiguracionRepository configRepository;

    @InjectMocks
    private PortalConfiguracionService configService;

    private ConfiguracionPortalDTO dto;
    private PortalConfiguracion config;

    @BeforeEach
    void setUp() {
        dto = new ConfiguracionPortalDTO();
        dto.setColorPrimario("#FF0000");
        dto.setColorSecundario("#00FF00");
        dto.setLogoUrl("http://example.com/logo.png");
        dto.setNombrePortal("Clínica Test");

        config = new PortalConfiguracion();
        config.setId(1L);
        config.setColorPrimario("#007bff");
        config.setColorSecundario("#6c757d");
        config.setLogoUrl("http://example.com/default.png");
        config.setNombrePortal("Clínica Default");
    }

    @Test
    void testGetConfiguracionWhenExists() {
        // Arrange
        when(configRepository.findCurrentConfig()).thenReturn(Optional.of(config));

        // Act
        PortalConfiguracion result = configService.getConfiguracion();

        // Assert
        assertNotNull(result);
        assertEquals(config.getId(), result.getId());
        verify(configRepository).findCurrentConfig();
        verify(configRepository, never()).save(any());
    }

    @Test
    void testGetConfiguracionWhenNotExists() {
        // Arrange
        when(configRepository.findCurrentConfig()).thenReturn(Optional.empty());
        when(configRepository.save(any(PortalConfiguracion.class))).thenReturn(config);

        // Act
        PortalConfiguracion result = configService.getConfiguracion();

        // Assert
        assertNotNull(result);
        verify(configRepository).findCurrentConfig();
        verify(configRepository).save(any(PortalConfiguracion.class));
    }

    @Test
    void testUpdateConfiguracionWhenExists() {
        // Arrange
        when(configRepository.findCurrentConfig()).thenReturn(Optional.of(config));
        when(configRepository.save(any(PortalConfiguracion.class))).thenReturn(config);

        // Act
        PortalConfiguracion result = configService.updateConfiguracion(dto);

        // Assert
        assertNotNull(result);
        verify(configRepository).findCurrentConfig();
        verify(configRepository).save(any(PortalConfiguracion.class));
    }

    @Test
    void testUpdateConfiguracionWhenNotExists() {
        // Arrange
        when(configRepository.findCurrentConfig()).thenReturn(Optional.empty());
        when(configRepository.save(any(PortalConfiguracion.class))).thenReturn(config);

        // Act
        PortalConfiguracion result = configService.updateConfiguracion(dto);

        // Assert
        assertNotNull(result);
        verify(configRepository).findCurrentConfig();
        verify(configRepository, times(2)).save(any(PortalConfiguracion.class)); // Una vez para crear default, otra para actualizar
    }

    @Test
    void testUpdateConfiguracionWithNullValues() {
        // Arrange
        ConfiguracionPortalDTO dtoNull = new ConfiguracionPortalDTO();
        when(configRepository.findCurrentConfig()).thenReturn(Optional.of(config));
        when(configRepository.save(any(PortalConfiguracion.class))).thenReturn(config);

        // Act
        PortalConfiguracion result = configService.updateConfiguracion(dtoNull);

        // Assert
        assertNotNull(result);
        verify(configRepository).save(any(PortalConfiguracion.class));
    }

    @Test
    void testUpdateConfiguracionWithPartialValues() {
        // Arrange
        ConfiguracionPortalDTO dtoPartial = new ConfiguracionPortalDTO();
        dtoPartial.setColorPrimario("#FF5733");
        dtoPartial.setNombrePortal("Nuevo Nombre");
        // colorSecundario y logoUrl son null
        
        when(configRepository.findCurrentConfig()).thenReturn(Optional.of(config));
        when(configRepository.save(any(PortalConfiguracion.class))).thenReturn(config);

        // Act
        PortalConfiguracion result = configService.updateConfiguracion(dtoPartial);

        // Assert
        assertNotNull(result);
        verify(configRepository).save(any(PortalConfiguracion.class));
    }
}

