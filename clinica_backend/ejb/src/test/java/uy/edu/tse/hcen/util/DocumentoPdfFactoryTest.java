package uy.edu.tse.hcen.util;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class DocumentoPdfFactoryTest {

    @Test
    void testGenerarDesdeDocumento() throws IOException {
        Document documento = new Document();
        documento.append("contenido", "Este es el contenido del documento clínico.");
        documento.append("titulo", "Evaluación Médica");
        documento.append("autor", "Dr. Juan Pérez");
        documento.append("ciPaciente", "12345678");
        
        byte[] pdfBytes = DocumentoPdfFactory.generarDesdeDocumento(documento);
        
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        // Verificar que es un PDF válido (debe empezar con %PDF)
        String header = new String(pdfBytes, 0, Math.min(4, pdfBytes.length));
        assertTrue(header.startsWith("%PDF"));
    }

    @Test
    void testGenerarDesdeDocumentoSinTitulo() throws IOException {
        Document documento = new Document();
        documento.append("contenido", "Contenido del documento");
        documento.append("tipoDocumento", "INFORME");
        documento.append("autor", "Dr. Test");
        documento.append("ciPaciente", "12345678");
        
        byte[] pdfBytes = DocumentoPdfFactory.generarDesdeDocumento(documento);
        
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testGenerarDesdeDocumentoSinAutor() throws IOException {
        Document documento = new Document();
        documento.append("contenido", "Contenido del documento");
        documento.append("titulo", "Título");
        documento.append("profesionalId", "prof-1");
        documento.append("ciPaciente", "12345678");
        
        byte[] pdfBytes = DocumentoPdfFactory.generarDesdeDocumento(documento);
        
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testGenerarDesdeDocumentoNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            DocumentoPdfFactory.generarDesdeDocumento(null);
        });
    }

    @Test
    void testGenerarDesdeDocumentoSinContenido() {
        Document documento = new Document();
        documento.append("titulo", "Título");
        
        assertThrows(IllegalArgumentException.class, () -> {
            DocumentoPdfFactory.generarDesdeDocumento(documento);
        });
    }

    @Test
    void testGenerarDesdeDocumentoContenidoVacio() {
        Document documento = new Document();
        documento.append("contenido", "");
        
        assertThrows(IllegalArgumentException.class, () -> {
            DocumentoPdfFactory.generarDesdeDocumento(documento);
        });
    }
}

