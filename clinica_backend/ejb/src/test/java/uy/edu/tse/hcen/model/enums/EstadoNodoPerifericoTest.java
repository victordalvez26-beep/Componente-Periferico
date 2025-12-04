package uy.edu.tse.hcen.model.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EstadoNodoPerifericoTest {

    @Test
    void testAllEnumValues() {
        EstadoNodoPeriferico[] estados = EstadoNodoPeriferico.values();
        assertTrue(estados.length > 0);
    }
    
    @Test
    void testValueOf() {
        EstadoNodoPeriferico activo = EstadoNodoPeriferico.valueOf("ACTIVO");
        assertNotNull(activo);
        assertEquals("ACTIVO", activo.name());
    }
    
    @Test
    void testEnumContainsExpectedValues() {
        assertDoesNotThrow(() -> EstadoNodoPeriferico.valueOf("ACTIVO"));
        assertDoesNotThrow(() -> EstadoNodoPeriferico.valueOf("INACTIVO"));
    }
}

