package uy.edu.tse.hcen.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EspecialidadTest {

    @Test
    void testEnumValues() {
        Especialidad[] values = Especialidad.values();
        
        assertNotNull(values);
        assertTrue(values.length > 0);
    }

    @Test
    void testValueOf() {
        Especialidad esp = Especialidad.valueOf("MEDICINA_GENERAL");
        assertEquals(Especialidad.MEDICINA_GENERAL, esp);
    }

    @Test
    void testValueOfInvalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            Especialidad.valueOf("INVALID_ESPECIALIDAD");
        });
    }

    @Test
    void testValueOfCaseSensitive() {
        assertThrows(IllegalArgumentException.class, () -> {
            Especialidad.valueOf("medicina_general");
        });
    }

    @Test
    void testAllEspecialidadesExist() {
        // Verificar que todas las especialidades esperadas existen
        assertNotNull(Especialidad.MEDICINA_GENERAL);
        assertNotNull(Especialidad.CARDIOLOGIA);
        assertNotNull(Especialidad.PEDIATRIA);
        assertNotNull(Especialidad.PSIQUIATRIA);
        assertNotNull(Especialidad.ODONTOLOGIA);
    }

    @Test
    void testEnumEquality() {
        Especialidad esp1 = Especialidad.MEDICINA_GENERAL;
        Especialidad esp2 = Especialidad.MEDICINA_GENERAL;
        Especialidad esp3 = Especialidad.CARDIOLOGIA;
        
        assertEquals(esp1, esp2);
        assertNotEquals(esp1, esp3);
    }

    @Test
    void testEnumName() {
        assertEquals("MEDICINA_GENERAL", Especialidad.MEDICINA_GENERAL.name());
        assertEquals("CARDIOLOGIA", Especialidad.CARDIOLOGIA.name());
        assertEquals("PEDIATRIA", Especialidad.PEDIATRIA.name());
    }

    @Test
    void testEnumOrdinal() {
        Especialidad[] values = Especialidad.values();
        
        for (int i = 0; i < values.length; i++) {
            assertEquals(i, values[i].ordinal());
        }
    }

    @Test
    void testEnumToString() {
        assertEquals("MEDICINA_GENERAL", Especialidad.MEDICINA_GENERAL.toString());
        assertEquals("CARDIOLOGIA", Especialidad.CARDIOLOGIA.toString());
    }

    @Test
    void testEnumCount() {
        Especialidad[] values = Especialidad.values();
        // Verificar que hay un número razonable de especialidades
        assertTrue(values.length >= 10);
    }

    @Test
    void testSpecificEspecialidades() {
        // Verificar algunas especialidades específicas
        assertNotNull(Especialidad.MEDICINA_INTERNA);
        assertNotNull(Especialidad.MEDICINA_FAMILIAR);
        assertNotNull(Especialidad.GINECOLOGIA);
        assertNotNull(Especialidad.OBSTETRICIA);
        assertNotNull(Especialidad.DERMATOLOGIA);
        assertNotNull(Especialidad.OFTALMOLOGIA);
        assertNotNull(Especialidad.OTORRINOLARINGOLOGIA);
        assertNotNull(Especialidad.UROLOGIA);
        assertNotNull(Especialidad.ORTOPEDIA);
        assertNotNull(Especialidad.CIRUGIA_GENERAL);
    }
}

