package uy.edu.tse.hcen.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfiguracionClinicaTest {

    private ConfiguracionClinica config;
    private NodoPeriferico nodo;

    @BeforeEach
    void setUp() {
        config = new ConfiguracionClinica();
        nodo = new PrestadorSalud();
        nodo.setId(1L);
    }

    @Test
    void testConstructor() {
        ConfiguracionClinica config = new ConfiguracionClinica();
        assertNotNull(config);
        assertNull(config.getId());
        assertNull(config.getNodoPeriferico());
        assertNull(config.getLogoUrl());
        assertNull(config.getColorPrincipal());
        assertFalse(config.isHabilitado());
    }

    @Test
    void testConstructorWithParameters() {
        String logoUrl = "http://example.com/logo.png";
        String colorPrincipal = "#007bff";
        boolean habilitado = true;
        
        ConfiguracionClinica config = new ConfiguracionClinica(
            nodo, logoUrl, colorPrincipal, habilitado
        );
        
        assertEquals(nodo, config.getNodoPeriferico());
        assertEquals(logoUrl, config.getLogoUrl());
        assertEquals(colorPrincipal, config.getColorPrincipal());
        assertEquals(habilitado, config.isHabilitado());
    }

    @Test
    void testId() {
        Long id = 1L;
        config.setId(id);
        assertEquals(id, config.getId());
    }

    @Test
    void testIdNull() {
        config.setId(null);
        assertNull(config.getId());
    }

    @Test
    void testNodoPeriferico() {
        config.setNodoPeriferico(nodo);
        assertEquals(nodo, config.getNodoPeriferico());
    }

    @Test
    void testNodoPerifericoNull() {
        config.setNodoPeriferico(null);
        assertNull(config.getNodoPeriferico());
    }

    @Test
    void testLogoUrl() {
        String logoUrl = "http://example.com/logo.png";
        config.setLogoUrl(logoUrl);
        assertEquals(logoUrl, config.getLogoUrl());
    }

    @Test
    void testLogoUrlNull() {
        config.setLogoUrl(null);
        assertNull(config.getLogoUrl());
    }

    @Test
    void testColorPrincipal() {
        String color = "#007bff";
        config.setColorPrincipal(color);
        assertEquals(color, config.getColorPrincipal());
    }

    @Test
    void testColorPrincipalNull() {
        config.setColorPrincipal(null);
        assertNull(config.getColorPrincipal());
    }

    @Test
    void testHabilitado() {
        config.setHabilitado(true);
        assertTrue(config.isHabilitado());
        
        config.setHabilitado(false);
        assertFalse(config.isHabilitado());
    }

    @Test
    void testAllFields() {
        Long id = 1L;
        String logoUrl = "http://example.com/logo.png";
        String colorPrincipal = "#FF5733";
        boolean habilitado = true;
        
        config.setId(id);
        config.setNodoPeriferico(nodo);
        config.setLogoUrl(logoUrl);
        config.setColorPrincipal(colorPrincipal);
        config.setHabilitado(habilitado);
        
        assertEquals(id, config.getId());
        assertEquals(nodo, config.getNodoPeriferico());
        assertEquals(logoUrl, config.getLogoUrl());
        assertEquals(colorPrincipal, config.getColorPrincipal());
        assertEquals(habilitado, config.isHabilitado());
    }
}

