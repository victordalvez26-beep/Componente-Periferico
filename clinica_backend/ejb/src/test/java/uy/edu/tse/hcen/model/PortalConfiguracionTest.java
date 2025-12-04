package uy.edu.tse.hcen.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PortalConfiguracionTest {

    @Test
    void testDefaultConstructor() {
        PortalConfiguracion config = new PortalConfiguracion();
        assertNotNull(config);
        assertNull(config.getId());
        assertEquals("#007bff", config.getColorPrimario()); // Default value
        assertEquals("#6c757d", config.getColorSecundario()); // Default value
    }
    
    @Test
    void testSettersAndGetters() {
        PortalConfiguracion config = new PortalConfiguracion();
        config.setId(1L);
        config.setNombrePortal("Nuevo Portal");
        config.setColorPrimario("#123456");
        config.setColorSecundario("#654321");
        config.setLogoUrl("https://test.com/logo.jpg");
        
        assertEquals(1L, config.getId());
        assertEquals("Nuevo Portal", config.getNombrePortal());
        assertEquals("#123456", config.getColorPrimario());
        assertEquals("#654321", config.getColorSecundario());
        assertEquals("https://test.com/logo.jpg", config.getLogoUrl());
    }
    
    @Test
    void testWithNullColors() {
        PortalConfiguracion config = new PortalConfiguracion();
        config.setColorPrimario(null);
        config.setColorSecundario(null);
        
        assertNull(config.getColorPrimario());
        assertNull(config.getColorSecundario());
    }
}

