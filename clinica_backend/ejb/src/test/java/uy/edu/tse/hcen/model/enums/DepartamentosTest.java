package uy.edu.tse.hcen.model.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DepartamentosTest {

    @Test
    void testAllEnumValues() {
        Departamentos[] departamentos = Departamentos.values();
        assertTrue(departamentos.length > 0);
    }
    
    @Test
    void testValueOf() {
        Departamentos montevideo = Departamentos.valueOf("MONTEVIDEO");
        assertNotNull(montevideo);
        assertEquals("MONTEVIDEO", montevideo.name());
    }
    
    @Test
    void testEnumContainsCommonDepartments() {
        assertDoesNotThrow(() -> Departamentos.valueOf("MONTEVIDEO"));
        assertDoesNotThrow(() -> Departamentos.valueOf("CANELONES"));
        assertDoesNotThrow(() -> Departamentos.valueOf("MALDONADO"));
    }
}

