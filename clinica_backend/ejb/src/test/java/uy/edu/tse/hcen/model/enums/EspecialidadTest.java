package uy.edu.tse.hcen.model.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EspecialidadTest {

    @Test
    void testAllEnumValues() {
        Especialidad[] especialidades = Especialidad.values();
        assertTrue(especialidades.length > 0);
    }
    
    @Test
    void testValueOf() {
        Especialidad medicinaGeneral = Especialidad.valueOf("MEDICINA_GENERAL");
        assertNotNull(medicinaGeneral);
        assertEquals("MEDICINA_GENERAL", medicinaGeneral.name());
    }
    
    @Test
    void testEnumContainsExpectedValues() {
        assertDoesNotThrow(() -> Especialidad.valueOf("MEDICINA_GENERAL"));
        assertDoesNotThrow(() -> Especialidad.valueOf("CARDIOLOGIA"));
        assertDoesNotThrow(() -> Especialidad.valueOf("PEDIATRIA"));
    }
}

