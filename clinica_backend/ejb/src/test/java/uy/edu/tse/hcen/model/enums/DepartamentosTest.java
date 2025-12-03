package uy.edu.tse.hcen.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DepartamentosTest {

    @Test
    void testEnumValues() {
        Departamentos[] values = Departamentos.values();
        
        assertNotNull(values);
        assertEquals(19, values.length); // Uruguay tiene 19 departamentos
    }

    @Test
    void testValueOf() {
        Departamentos dept = Departamentos.valueOf("MONTEVIDEO");
        assertEquals(Departamentos.MONTEVIDEO, dept);
    }

    @Test
    void testValueOfInvalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            Departamentos.valueOf("INVALID_DEPARTAMENTO");
        });
    }

    @Test
    void testValueOfCaseSensitive() {
        assertThrows(IllegalArgumentException.class, () -> {
            Departamentos.valueOf("montevideo");
        });
    }

    @Test
    void testAllDepartamentosExist() {
        assertNotNull(Departamentos.MONTEVIDEO);
        assertNotNull(Departamentos.CANELONES);
        assertNotNull(Departamentos.MALDONADO);
        assertNotNull(Departamentos.ROCHA);
        assertNotNull(Departamentos.TREINTA_Y_TRES);
        assertNotNull(Departamentos.CERRO_LARGO);
        assertNotNull(Departamentos.RIVERA);
        assertNotNull(Departamentos.ARTIGAS);
        assertNotNull(Departamentos.SALTO);
        assertNotNull(Departamentos.PAYSANDU);
        assertNotNull(Departamentos.RIO_NEGRO);
        assertNotNull(Departamentos.TACUAREMBO);
        assertNotNull(Departamentos.DURAZNO);
        assertNotNull(Departamentos.FLORES);
        assertNotNull(Departamentos.FLORIDA);
        assertNotNull(Departamentos.LAVALLEJA);
        assertNotNull(Departamentos.SAN_JOSE);
        assertNotNull(Departamentos.COLONIA);
        assertNotNull(Departamentos.SORIANO);
    }

    @Test
    void testEnumEquality() {
        Departamentos dept1 = Departamentos.MONTEVIDEO;
        Departamentos dept2 = Departamentos.MONTEVIDEO;
        Departamentos dept3 = Departamentos.CANELONES;
        
        assertEquals(dept1, dept2);
        assertNotEquals(dept1, dept3);
    }

    @Test
    void testEnumName() {
        assertEquals("MONTEVIDEO", Departamentos.MONTEVIDEO.name());
        assertEquals("CANELONES", Departamentos.CANELONES.name());
        assertEquals("TREINTA_Y_TRES", Departamentos.TREINTA_Y_TRES.name());
    }

    @Test
    void testEnumOrdinal() {
        Departamentos[] values = Departamentos.values();
        
        for (int i = 0; i < values.length; i++) {
            assertEquals(i, values[i].ordinal());
        }
    }

    @Test
    void testEnumToString() {
        assertEquals("MONTEVIDEO", Departamentos.MONTEVIDEO.toString());
        assertEquals("CANELONES", Departamentos.CANELONES.toString());
    }

    @Test
    void testSpecificDepartamentos() {
        // Verificar algunos departamentos específicos
        assertNotNull(Departamentos.MONTEVIDEO);
        assertNotNull(Departamentos.CANELONES);
        assertNotNull(Departamentos.MALDONADO);
        assertNotNull(Departamentos.ROCHA);
    }
}

