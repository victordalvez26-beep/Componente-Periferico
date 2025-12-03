package uy.edu.tse.hcen.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ActorSaludTest {

    @Test
    void testEnumValues() {
        ActorSalud[] values = ActorSalud.values();
        
        assertNotNull(values);
        assertEquals(2, values.length);
    }

    @Test
    void testValueOf() {
        ActorSalud actor = ActorSalud.valueOf("CLINICA");
        assertEquals(ActorSalud.CLINICA, actor);
    }

    @Test
    void testValueOfInvalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            ActorSalud.valueOf("INVALID_ACTOR");
        });
    }

    @Test
    void testValueOfCaseSensitive() {
        assertThrows(IllegalArgumentException.class, () -> {
            ActorSalud.valueOf("clinica");
        });
    }

    @Test
    void testAllActoresExist() {
        assertNotNull(ActorSalud.CLINICA);
        assertNotNull(ActorSalud.LABORATORIO);
    }

    @Test
    void testEnumEquality() {
        ActorSalud actor1 = ActorSalud.CLINICA;
        ActorSalud actor2 = ActorSalud.CLINICA;
        ActorSalud actor3 = ActorSalud.LABORATORIO;
        
        assertEquals(actor1, actor2);
        assertNotEquals(actor1, actor3);
    }

    @Test
    void testEnumName() {
        assertEquals("CLINICA", ActorSalud.CLINICA.name());
        assertEquals("LABORATORIO", ActorSalud.LABORATORIO.name());
    }

    @Test
    void testEnumOrdinal() {
        assertEquals(0, ActorSalud.CLINICA.ordinal());
        assertEquals(1, ActorSalud.LABORATORIO.ordinal());
    }

    @Test
    void testEnumToString() {
        assertEquals("CLINICA", ActorSalud.CLINICA.toString());
        assertEquals("LABORATORIO", ActorSalud.LABORATORIO.toString());
    }
}

