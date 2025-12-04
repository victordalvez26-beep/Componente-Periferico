package uy.edu.tse.hcen.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import uy.edu.tse.hcen.dto.ConfiguracionPortalDTO;
import uy.edu.tse.hcen.model.PortalConfiguracion;
import uy.edu.tse.hcen.repository.PortalConfiguracionRepository;
import java.lang.reflect.Field;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PortalConfiguracionServiceTest {

    private PortalConfiguracionService service;
    private PortalConfiguracionRepository mockRepository;
    
    @BeforeEach
    void setUp() throws Exception {
        service = new PortalConfiguracionService();
        mockRepository = mock(PortalConfiguracionRepository.class);
        
        // Inject mock using reflection
        Field field = PortalConfiguracionService.class.getDeclaredField("configRepository");
        field.setAccessible(true);
        field.set(service, mockRepository);
    }
    
    @Test
    void testGetConfiguracionWhenExists() {
        PortalConfiguracion config = new PortalConfiguracion();
        config.setNombrePortal("Test Portal");
        when(mockRepository.findCurrentConfig()).thenReturn(Optional.of(config));
        
        PortalConfiguracion result = service.getConfiguracion();
        
        assertNotNull(result);
        assertEquals("Test Portal", result.getNombrePortal());
        verify(mockRepository).findCurrentConfig();
    }
    
    @Test
    void testGetConfiguracionWhenNotExists() {
        when(mockRepository.findCurrentConfig()).thenReturn(Optional.empty());
        when(mockRepository.save(any(PortalConfiguracion.class))).thenAnswer(i -> i.getArguments()[0]);
        
        PortalConfiguracion result = service.getConfiguracion();
        
        assertNotNull(result);
        verify(mockRepository).findCurrentConfig();
        verify(mockRepository).save(any(PortalConfiguracion.class));
    }
    
    @Test
    void testUpdateConfiguracion() {
        PortalConfiguracion existing = new PortalConfiguracion();
        existing.setColorPrimario("#000000");
        
        when(mockRepository.findCurrentConfig()).thenReturn(Optional.of(existing));
        when(mockRepository.save(any(PortalConfiguracion.class))).thenAnswer(i -> i.getArguments()[0]);
        
        ConfiguracionPortalDTO dto = new ConfiguracionPortalDTO();
        dto.setColorPrimario("#FF0000");
        dto.setNombrePortal("New Name");
        
        PortalConfiguracion result = service.updateConfiguracion(dto);
        
        assertNotNull(result);
        assertEquals("#FF0000", result.getColorPrimario());
        assertEquals("New Name", result.getNombrePortal());
        verify(mockRepository).save(any(PortalConfiguracion.class));
    }
}

