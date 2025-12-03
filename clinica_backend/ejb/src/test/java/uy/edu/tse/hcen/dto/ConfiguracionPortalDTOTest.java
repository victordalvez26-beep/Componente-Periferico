package uy.edu.tse.hcen.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfiguracionPortalDTOTest {

    @Test
    void testConstructorAndGetters() {
        ConfiguracionPortalDTO dto = new ConfiguracionPortalDTO();
        dto.setNombrePortal("Test Portal");
        dto.setColorPrimario("#FF0000");
        dto.setColorSecundario("#00FF00");
        dto.setLogoUrl("http://test.com/logo.png");
        
        assertEquals("Test Portal", dto.getNombrePortal());
        assertEquals("#FF0000", dto.getColorPrimario());
        assertEquals("#00FF00", dto.getColorSecundario());
        assertEquals("http://test.com/logo.png", dto.getLogoUrl());
    }
    
    @Test
    void testSettersWithNullValues() {
        ConfiguracionPortalDTO dto = new ConfiguracionPortalDTO();
        dto.setNombrePortal(null);
        dto.setColorPrimario(null);
        dto.setColorSecundario(null);
        dto.setLogoUrl(null);
        
        assertNull(dto.getNombrePortal());
        assertNull(dto.getColorPrimario());
        assertNull(dto.getColorSecundario());
        assertNull(dto.getLogoUrl());
    }
}

