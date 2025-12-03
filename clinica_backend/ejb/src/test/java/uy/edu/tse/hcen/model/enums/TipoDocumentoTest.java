package uy.edu.tse.hcen.model.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TipoDocumentoTest {

    @Test
    void testAllEnumValues() {
        TipoDocumento[] tipos = TipoDocumento.values();
        assertTrue(tipos.length > 0);
    }
    
    @Test
    void testValueOf() {
        TipoDocumento resumen = TipoDocumento.valueOf("RESUMEN_ALTA");
        assertNotNull(resumen);
        assertEquals("RESUMEN_ALTA", resumen.name());
        assertEquals("Resumen de Alta", resumen.getDescripcion());
    }
    
    @Test
    void testEnumContainsExpectedValues() {
        assertDoesNotThrow(() -> TipoDocumento.valueOf("RESUMEN_ALTA"));
        assertDoesNotThrow(() -> TipoDocumento.valueOf("INFORME_LABORATORIO"));
        assertDoesNotThrow(() -> TipoDocumento.valueOf("RECETA_MEDICA"));
    }
    
    @Test
    void testDescripcion() {
        assertEquals("Consulta Médica", TipoDocumento.CONSULTA_MEDICA.getDescripcion());
        assertEquals("Vacunación", TipoDocumento.VACUNACION.getDescripcion());
    }
}
