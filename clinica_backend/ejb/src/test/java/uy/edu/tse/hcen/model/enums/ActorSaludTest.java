package uy.edu.tse.hcen.model.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ActorSaludTest {

    @Test
    void testAllEnumValues() {
        ActorSalud[] actores = ActorSalud.values();
        assertEquals(2, actores.length);
    }
    
    @Test
    void testValueOf() {
        ActorSalud clinica = ActorSalud.valueOf("CLINICA");
        assertNotNull(clinica);
        assertEquals("CLINICA", clinica.name());
    }
    
    @Test
    void testEnumHasExpectedValues() {
        assertDoesNotThrow(() -> ActorSalud.valueOf("CLINICA"));
        assertDoesNotThrow(() -> ActorSalud.valueOf("LABORATORIO"));
    }
}

