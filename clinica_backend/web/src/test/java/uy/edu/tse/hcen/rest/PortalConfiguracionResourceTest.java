package uy.edu.tse.hcen.rest;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.tse.hcen.dto.ConfiguracionPortalDTO;
import uy.edu.tse.hcen.model.PortalConfiguracion;
import uy.edu.tse.hcen.service.PortalConfiguracionService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortalConfiguracionResourceTest {

    @Mock
    private PortalConfiguracionService configService;

    @InjectMocks
    private PortalConfiguracionResource resource;

    private PortalConfiguracion mockConfig;
    private ConfiguracionPortalDTO mockDTO;

    @BeforeEach
    void setUp() {
        mockConfig = new PortalConfiguracion();
        mockConfig.setColorPrimario("#3b82f6");
        mockConfig.setColorSecundario("#1e40af");
        mockConfig.setLogoUrl("https://example.com/logo.png");
        mockConfig.setNombrePortal("Mi Clínica");

        mockDTO = new ConfiguracionPortalDTO();
        mockDTO.colorPrimario = "#3b82f6";
        mockDTO.colorSecundario = "#1e40af";
        mockDTO.logoUrl = "https://example.com/logo.png";
        mockDTO.nombrePortal = "Mi Clínica";
    }

    @Test
    void testGetPublicConfiguracionSuccess() {
        when(configService.getConfiguracion()).thenReturn(mockConfig);

        ConfiguracionPortalDTO result = resource.getPublicConfiguracion();

        assertNotNull(result);
        assertEquals("#3b82f6", result.colorPrimario);
        assertEquals("#1e40af", result.colorSecundario);
        assertEquals("https://example.com/logo.png", result.logoUrl);
        assertEquals("Mi Clínica", result.nombrePortal);
        verify(configService).getConfiguracion();
    }

    @Test
    void testGetPublicConfiguracionWithNullValues() {
        PortalConfiguracion configWithNulls = new PortalConfiguracion();
        configWithNulls.setColorPrimario(null);
        configWithNulls.setColorSecundario(null);
        configWithNulls.setLogoUrl(null);
        configWithNulls.setNombrePortal(null);
        
        when(configService.getConfiguracion()).thenReturn(configWithNulls);

        ConfiguracionPortalDTO result = resource.getPublicConfiguracion();

        assertNotNull(result);
        assertNull(result.colorPrimario);
        assertNull(result.colorSecundario);
        assertNull(result.logoUrl);
        assertNull(result.nombrePortal);
    }

    @Test
    void testUpdateConfiguracionSuccess() {
        PortalConfiguracion updatedConfig = new PortalConfiguracion();
        updatedConfig.setColorPrimario("#10b981");
        updatedConfig.setColorSecundario("#059669");
        updatedConfig.setLogoUrl("https://example.com/new-logo.png");
        updatedConfig.setNombrePortal("Nueva Clínica");

        when(configService.updateConfiguracion(any(ConfiguracionPortalDTO.class))).thenReturn(updatedConfig);

        Response response = resource.updateConfiguracion(mockDTO);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());
        assertEquals(updatedConfig, response.getEntity());
        verify(configService).updateConfiguracion(any(ConfiguracionPortalDTO.class));
    }

    @Test
    void testUpdateConfiguracionWithNullDTO() {
        PortalConfiguracion updatedConfig = new PortalConfiguracion();
        when(configService.updateConfiguracion(any())).thenReturn(updatedConfig);

        Response response = resource.updateConfiguracion(null);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(configService).updateConfiguracion(any());
    }

    @Test
    void testUpdateConfiguracionWithEmptyValues() {
        ConfiguracionPortalDTO emptyDTO = new ConfiguracionPortalDTO();
        emptyDTO.colorPrimario = "";
        emptyDTO.colorSecundario = "";
        emptyDTO.logoUrl = "";
        emptyDTO.nombrePortal = "";

        PortalConfiguracion updatedConfig = new PortalConfiguracion();
        when(configService.updateConfiguracion(any(ConfiguracionPortalDTO.class))).thenReturn(updatedConfig);

        Response response = resource.updateConfiguracion(emptyDTO);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(configService).updateConfiguracion(any(ConfiguracionPortalDTO.class));
    }
}
