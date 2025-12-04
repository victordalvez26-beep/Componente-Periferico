package uy.edu.tse.hcen.util;

import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DocumentoPdfFactory Tests")
class DocumentoPdfFactoryTest {

    @Test
    @DisplayName("Generar PDF desde documento con datos completos")
    void generarPdf_completeDocument_shouldGeneratePdf() throws IOException {
        Document doc = new Document();
        doc.append("contenido", "Contenido del documento médico");
        doc.append("ciPaciente", "12345678");
        doc.append("tipoDocumento", "EVALUACION");
        doc.append("profesionalId", "doctor1");
        
        byte[] result = DocumentoPdfFactory.generarDesdeDocumento(doc);
        
        assertNotNull(result);
        assertTrue(result.length > 0);
        assertTrue(new String(result).startsWith("%PDF"));
    }

    @Test
    @DisplayName("Generar PDF sin contenido debe lanzar excepción")
    void generarPdf_noContent_shouldThrow() {
        Document doc = new Document();
        
        assertThrows(IllegalArgumentException.class, () -> 
                DocumentoPdfFactory.generarDesdeDocumento(doc));
    }

    @Test
    @DisplayName("Generar PDF con documento null debe lanzar excepción")
    void generarPdf_nullDocument_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> 
                DocumentoPdfFactory.generarDesdeDocumento(null));
    }
}

