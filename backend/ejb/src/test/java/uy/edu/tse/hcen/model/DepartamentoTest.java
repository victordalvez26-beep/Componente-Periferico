package uy.edu.tse.hcen.model;

import org.junit.jupiter.api.Test;

import uy.edu.tse.hcen.model.enums.Departamento;

import static org.junit.jupiter.api.Assertions.*;

public class DepartamentoTest {

    @Test
    public void testValueOf() {
        Departamento d = Departamento.valueOf("MONTEVIDEO");
        assertNotNull(d);
        assertEquals(Departamento.MONTEVIDEO, d);
    }

    @Test
    public void testInvalidValue() {
        assertThrows(IllegalArgumentException.class, () -> Departamento.valueOf("NO_EXISTE"));
    }
}
