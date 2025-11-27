package uy.edu.tse.hcen.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EstadoNodoPerifericoTest {

    @Test
    void testEnumValues() {
        EstadoNodoPeriferico[] values = EstadoNodoPeriferico.values();
        
        assertNotNull(values);
        assertEquals(3, values.length);
    }

    @Test
    void testValueOf() {
        EstadoNodoPeriferico estado = EstadoNodoPeriferico.valueOf("ACTIVO");
        assertEquals(EstadoNodoPeriferico.ACTIVO, estado);
    }

    @Test
    void testValueOfInvalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            EstadoNodoPeriferico.valueOf("INVALID_ESTADO");
        });
    }

    @Test
    void testValueOfCaseSensitive() {
        assertThrows(IllegalArgumentException.class, () -> {
            EstadoNodoPeriferico.valueOf("activo");
        });
    }

    @Test
    void testAllEstadosExist() {
        assertNotNull(EstadoNodoPeriferico.ACTIVO);
        assertNotNull(EstadoNodoPeriferico.INACTIVO);
        assertNotNull(EstadoNodoPeriferico.MANTENIMIENTO);
    }

    @Test
    void testEnumEquality() {
        EstadoNodoPeriferico estado1 = EstadoNodoPeriferico.ACTIVO;
        EstadoNodoPeriferico estado2 = EstadoNodoPeriferico.ACTIVO;
        EstadoNodoPeriferico estado3 = EstadoNodoPeriferico.INACTIVO;
        
        assertEquals(estado1, estado2);
        assertNotEquals(estado1, estado3);
    }

    @Test
    void testEnumName() {
        assertEquals("ACTIVO", EstadoNodoPeriferico.ACTIVO.name());
        assertEquals("INACTIVO", EstadoNodoPeriferico.INACTIVO.name());
        assertEquals("MANTENIMIENTO", EstadoNodoPeriferico.MANTENIMIENTO.name());
    }

    @Test
    void testEnumOrdinal() {
        assertEquals(0, EstadoNodoPeriferico.ACTIVO.ordinal());
        assertEquals(1, EstadoNodoPeriferico.INACTIVO.ordinal());
        assertEquals(2, EstadoNodoPeriferico.MANTENIMIENTO.ordinal());
    }

    @Test
    void testEnumToString() {
        assertEquals("ACTIVO", EstadoNodoPeriferico.ACTIVO.toString());
        assertEquals("INACTIVO", EstadoNodoPeriferico.INACTIVO.toString());
        assertEquals("MANTENIMIENTO", EstadoNodoPeriferico.MANTENIMIENTO.toString());
    }
}

