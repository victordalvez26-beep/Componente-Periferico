package uy.edu.tse.hcen.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class PdfGeneratorTest {

    @Test
    void testTextoAPdf() throws IOException {
        String contenido = "Este es un documento de prueba.\nSegunda línea del documento.";
        String titulo = "Documento de Prueba";
        String autor = "Dr. Test";
        String pacienteCI = "12345678";
        
        byte[] pdfBytes = PdfGenerator.textoAPdf(contenido, titulo, autor, pacienteCI);
        
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        // Verificar que es un PDF válido
        String header = new String(pdfBytes, 0, Math.min(4, pdfBytes.length));
        assertTrue(header.startsWith("%PDF"));
    }

    @Test
    void testTextoAPdfSimple() throws IOException {
        String contenido = "Contenido simple";
        
        byte[] pdfBytes = PdfGenerator.textoAPdf(contenido);
        
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testTextoAPdfSinTitulo() throws IOException {
        String contenido = "Contenido sin título";
        String autor = "Dr. Test";
        String pacienteCI = "12345678";
        
        byte[] pdfBytes = PdfGenerator.textoAPdf(contenido, null, autor, pacienteCI);
        
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testTextoAPdfSinAutor() throws IOException {
        String contenido = "Contenido sin autor";
        String titulo = "Título";
        String pacienteCI = "12345678";
        
        byte[] pdfBytes = PdfGenerator.textoAPdf(contenido, titulo, null, pacienteCI);
        
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testTextoAPdfSinPacienteCI() throws IOException {
        String contenido = "Contenido sin CI";
        String titulo = "Título";
        String autor = "Dr. Test";
        
        byte[] pdfBytes = PdfGenerator.textoAPdf(contenido, titulo, autor, null);
        
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testTextoAPdfContenidoNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            PdfGenerator.textoAPdf(null);
        });
    }

    @Test
    void testTextoAPdfContenidoVacio() {
        assertThrows(IllegalArgumentException.class, () -> {
            PdfGenerator.textoAPdf("");
        });
    }

    @Test
    void testTextoAPdfContenidoConLineas() throws IOException {
        String contenido = "Línea 1\n\nLínea 2\nLínea 3\n\nLínea 4";
        
        byte[] pdfBytes = PdfGenerator.textoAPdf(contenido);
        
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }
}

