package uy.edu.tse.hcen.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PdfGenerator Tests")
class PdfGeneratorTest {

    @Test
    @DisplayName("textoAPdf con parámetros completos debe funcionar")
    void textoAPdf_complete_shouldWork() throws Exception {
        byte[] result = PdfGenerator.textoAPdf(
                "Contenido de prueba",
                "Título",
                "Doctor",
                "12345678"
        );
        
        assertNotNull(result);
        assertTrue(result.length > 0);
        assertTrue(new String(result).startsWith("%PDF"));
    }

    @Test
    @DisplayName("textoAPdf con contenido null debe lanzar excepción")
    void textoAPdf_nullContent_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> 
                PdfGenerator.textoAPdf(null, "Título", "Autor", "CI"));
    }

    @Test
    @DisplayName("textoAPdf con contenido vacío debe lanzar excepción")
    void textoAPdf_emptyContent_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> 
                PdfGenerator.textoAPdf("", "Título", "Autor", "CI"));
    }

    @Test
    @DisplayName("textoAPdf sin parámetros opcionales debe funcionar")
    void textoAPdf_minimal_shouldWork() throws Exception {
        byte[] result = PdfGenerator.textoAPdf("Contenido");
        
        assertNotNull(result);
        assertTrue(result.length > 0);
    }
}

