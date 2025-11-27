package uy.edu.tse.hcen.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PortalConfiguracionTest {

    private PortalConfiguracion config;

    @BeforeEach
    void setUp() {
        config = new PortalConfiguracion();
    }

    @Test
    void testConstructor() {
        PortalConfiguracion config = new PortalConfiguracion();
        assertNotNull(config);
        assertNull(config.getId());
        assertEquals("#007bff", config.getColorPrimario()); // Valor por defecto
        assertEquals("#6c757d", config.getColorSecundario()); // Valor por defecto
        assertNull(config.getLogoUrl());
        assertNull(config.getNombrePortal());
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
    void testColorPrimario() {
        String color = "#FF5733";
        config.setColorPrimario(color);
        assertEquals(color, config.getColorPrimario());
    }

    @Test
    void testColorPrimarioNull() {
        config.setColorPrimario(null);
        assertNull(config.getColorPrimario());
    }

    @Test
    void testColorSecundario() {
        String color = "#33FF57";
        config.setColorSecundario(color);
        assertEquals(color, config.getColorSecundario());
    }

    @Test
    void testColorSecundarioNull() {
        config.setColorSecundario(null);
        assertNull(config.getColorSecundario());
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
    void testNombrePortal() {
        String nombre = "Clínica San José";
        config.setNombrePortal(nombre);
        assertEquals(nombre, config.getNombrePortal());
    }

    @Test
    void testNombrePortalNull() {
        config.setNombrePortal(null);
        assertNull(config.getNombrePortal());
    }

    @Test
    void testAllFields() {
        Long id = 1L;
        String colorPrimario = "#FF0000";
        String colorSecundario = "#00FF00";
        String logoUrl = "http://example.com/logo.png";
        String nombrePortal = "Clínica Test";
        
        config.setId(id);
        config.setColorPrimario(colorPrimario);
        config.setColorSecundario(colorSecundario);
        config.setLogoUrl(logoUrl);
        config.setNombrePortal(nombrePortal);
        
        assertEquals(id, config.getId());
        assertEquals(colorPrimario, config.getColorPrimario());
        assertEquals(colorSecundario, config.getColorSecundario());
        assertEquals(logoUrl, config.getLogoUrl());
        assertEquals(nombrePortal, config.getNombrePortal());
    }

    @Test
    void testDefaultValues() {
        PortalConfiguracion config = new PortalConfiguracion();
        assertEquals("#007bff", config.getColorPrimario());
        assertEquals("#6c757d", config.getColorSecundario());
    }
}

