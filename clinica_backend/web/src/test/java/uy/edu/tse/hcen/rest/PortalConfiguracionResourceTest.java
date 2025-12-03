package uy.edu.tse.hcen.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.tse.hcen.dto.ConfiguracionPortalDTO;
import uy.edu.tse.hcen.model.PortalConfiguracion;
import uy.edu.tse.hcen.service.PortalConfiguracionService;

import jakarta.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortalConfiguracionResourceTest {

    @Mock
    private PortalConfiguracionService configService;

    @InjectMocks
    private PortalConfiguracionResource resource;

    private PortalConfiguracion config;
    private ConfiguracionPortalDTO dto;

    @BeforeEach
    void setUp() {
        config = new PortalConfiguracion();
        config.setId(1L);
        config.setColorPrimario("#007bff");
        config.setColorSecundario("#6c757d");
        config.setLogoUrl("http://example.com/logo.png");
        config.setNombrePortal("Clínica Test");

        dto = new ConfiguracionPortalDTO();
        dto.setColorPrimario("#FF0000");
        dto.setColorSecundario("#00FF00");
        dto.setLogoUrl("http://example.com/new-logo.png");
        dto.setNombrePortal("Nueva Clínica");
    }

    @Test
    void testGetPublicConfiguracion() {
        // Arrange
        when(configService.getConfiguracion()).thenReturn(config);

        // Act
        ConfiguracionPortalDTO result = resource.getPublicConfiguracion();

        // Assert
        assertNotNull(result);
        assertEquals(config.getColorPrimario(), result.getColorPrimario());
        assertEquals(config.getColorSecundario(), result.getColorSecundario());
        assertEquals(config.getLogoUrl(), result.getLogoUrl());
        assertEquals(config.getNombrePortal(), result.getNombrePortal());
        verify(configService).getConfiguracion();
    }

    @Test
    void testUpdateConfiguracion() {
        // Arrange
        when(configService.updateConfiguracion(any(ConfiguracionPortalDTO.class))).thenReturn(config);

        // Act
        Response response = resource.updateConfiguracion(dto);

        // Assert
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());
        verify(configService).updateConfiguracion(any(ConfiguracionPortalDTO.class));
    }

    @Test
    void testUpdateConfiguracionWithNullDto() {
        // Arrange
        when(configService.updateConfiguracion(any())).thenReturn(config);

        // Act
        Response response = resource.updateConfiguracion(null);

        // Assert
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void testGetPublicConfiguracionWithNullConfig() {
        // Arrange
        when(configService.getConfiguracion()).thenReturn(null);

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            resource.getPublicConfiguracion();
        });
    }

    @Test
    void testUpdateConfiguracionWithException() {
        // Arrange
        when(configService.updateConfiguracion(any(ConfiguracionPortalDTO.class)))
            .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            resource.updateConfiguracion(dto);
        });
    }
}

