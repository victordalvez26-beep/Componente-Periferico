package uy.edu.tse.hcen.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfiguracionPortalDTOTest {

    private ConfiguracionPortalDTO dto;

    @BeforeEach
    void setUp() {
        dto = new ConfiguracionPortalDTO();
    }

    @Test
    void testConstructor() {
        ConfiguracionPortalDTO dto = new ConfiguracionPortalDTO();
        assertNotNull(dto);
        assertNull(dto.getColorPrimario());
        assertNull(dto.getColorSecundario());
        assertNull(dto.getLogoUrl());
        assertNull(dto.getNombrePortal());
    }

    @Test
    void testColorPrimario() {
        String color = "#007bff";
        dto.setColorPrimario(color);
        assertEquals(color, dto.getColorPrimario());
    }

    @Test
    void testColorPrimarioNull() {
        dto.setColorPrimario(null);
        assertNull(dto.getColorPrimario());
    }

    @Test
    void testColorPrimarioEmpty() {
        dto.setColorPrimario("");
        assertEquals("", dto.getColorPrimario());
    }

    @Test
    void testColorPrimarioHexFormat() {
        String color = "#FF5733";
        dto.setColorPrimario(color);
        assertEquals(color, dto.getColorPrimario());
    }

    @Test
    void testColorPrimarioRgbFormat() {
        String color = "rgb(255, 87, 51)";
        dto.setColorPrimario(color);
        assertEquals(color, dto.getColorPrimario());
    }

    @Test
    void testColorSecundario() {
        String color = "#6c757d";
        dto.setColorSecundario(color);
        assertEquals(color, dto.getColorSecundario());
    }

    @Test
    void testColorSecundarioNull() {
        dto.setColorSecundario(null);
        assertNull(dto.getColorSecundario());
    }

    @Test
    void testColorSecundarioEmpty() {
        dto.setColorSecundario("");
        assertEquals("", dto.getColorSecundario());
    }

    @Test
    void testLogoUrl() {
        String url = "https://example.com/logo.png";
        dto.setLogoUrl(url);
        assertEquals(url, dto.getLogoUrl());
    }

    @Test
    void testLogoUrlNull() {
        dto.setLogoUrl(null);
        assertNull(dto.getLogoUrl());
    }

    @Test
    void testLogoUrlEmpty() {
        dto.setLogoUrl("");
        assertEquals("", dto.getLogoUrl());
    }

    @Test
    void testLogoUrlRelativePath() {
        String url = "/images/logo.png";
        dto.setLogoUrl(url);
        assertEquals(url, dto.getLogoUrl());
    }

    @Test
    void testLogoUrlLongPath() {
        String url = "https://cdn.example.com/images/clinics/tenant-123/logo-large.png";
        dto.setLogoUrl(url);
        assertEquals(url, dto.getLogoUrl());
    }

    @Test
    void testNombrePortal() {
        String nombre = "Clínica San José";
        dto.setNombrePortal(nombre);
        assertEquals(nombre, dto.getNombrePortal());
    }

    @Test
    void testNombrePortalNull() {
        dto.setNombrePortal(null);
        assertNull(dto.getNombrePortal());
    }

    @Test
    void testNombrePortalEmpty() {
        dto.setNombrePortal("");
        assertEquals("", dto.getNombrePortal());
    }

    @Test
    void testAllFields() {
        String colorPrimario = "#007bff";
        String colorSecundario = "#6c757d";
        String logoUrl = "https://example.com/logo.png";
        String nombrePortal = "Clínica San José";
        
        dto.setColorPrimario(colorPrimario);
        dto.setColorSecundario(colorSecundario);
        dto.setLogoUrl(logoUrl);
        dto.setNombrePortal(nombrePortal);
        
        assertEquals(colorPrimario, dto.getColorPrimario());
        assertEquals(colorSecundario, dto.getColorSecundario());
        assertEquals(logoUrl, dto.getLogoUrl());
        assertEquals(nombrePortal, dto.getNombrePortal());
    }

    @Test
    void testPartialFields() {
        dto.setColorPrimario("#FF0000");
        dto.setNombrePortal("Test Clinic");
        
        assertEquals("#FF0000", dto.getColorPrimario());
        assertEquals("Test Clinic", dto.getNombrePortal());
        assertNull(dto.getColorSecundario());
        assertNull(dto.getLogoUrl());
    }

    @Test
    void testLongStrings() {
        String longNombre = "a".repeat(500);
        String longUrl = "https://example.com/" + "a".repeat(1000) + ".png";
        
        dto.setNombrePortal(longNombre);
        dto.setLogoUrl(longUrl);
        
        assertEquals(longNombre, dto.getNombrePortal());
        assertEquals(longUrl, dto.getLogoUrl());
    }

    @Test
    void testSpecialCharactersInNombre() {
        String nombre = "Clínica San José & Asociados";
        dto.setNombrePortal(nombre);
        assertEquals(nombre, dto.getNombrePortal());
    }

    @Test
    void testColorValues() {
        String[] colors = {"#000000", "#FFFFFF", "#FF0000", "#00FF00", "#0000FF", "#FFFF00"};
        
        for (String color : colors) {
            dto.setColorPrimario(color);
            assertEquals(color, dto.getColorPrimario());
        }
    }
}

