package uy.edu.tse.hcen.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uy.edu.tse.hcen.model.enums.Departamentos;
import uy.edu.tse.hcen.model.enums.EstadoNodoPeriferico;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PrestadorSalud model.
 * 
 * @author Senior Test Engineer
 */
@DisplayName("PrestadorSalud Model Tests")
class PrestadorSaludTest {

    @Test
    @DisplayName("Constructor con parámetros debe setear valores")
    void constructor_shouldSetValues() {
        // Act
        PrestadorSalud prestador = new PrestadorSalud(
                "Hospital Central",
                "123456780011",
                Departamentos.MONTEVIDEO,
                "Montevideo",
                "21 de Setiembre 2900",
                "2487 0000",
                EstadoNodoPeriferico.ACTIVO
        );

        // Assert
        assertEquals("Hospital Central", prestador.getNombre());
        assertEquals("123456780011", prestador.getRut());
        assertEquals(Departamentos.MONTEVIDEO, prestador.getDepartamento());
        assertEquals("Montevideo", prestador.getLocalidad());
        assertEquals("21 de Setiembre 2900", prestador.getDireccion());
        assertEquals("2487 0000", prestador.getContacto());
        assertEquals(EstadoNodoPeriferico.ACTIVO, prestador.getEstado());
    }

    @Test
    @DisplayName("Hereda todas las propiedades de NodoPeriferico")
    void shouldInheritFromNodoPeriferico() {
        // Arrange
        PrestadorSalud prestador = new PrestadorSalud(
                "Clínica Privada",
                "999",
                Departamentos.CANELONES,
                "Las Piedras",
                "Dir",
                "099",
                EstadoNodoPeriferico.INACTIVO
        );

        // Act
        prestador.setId(1L);
        prestador.setNombre("Nuevo Nombre");

        // Assert
        assertEquals(1L, prestador.getId());
        assertEquals("Nuevo Nombre", prestador.getNombre());
    }

    @Test
    @DisplayName("setEstado debe actualizar estado")
    void setEstado_shouldUpdate() {
        // Arrange
        PrestadorSalud prestador = new PrestadorSalud(
                "Prestador", "123", Departamentos.MONTEVIDEO, 
                "MVD", "Dir", "999", EstadoNodoPeriferico.INACTIVO
        );

        // Act
        prestador.setEstado(EstadoNodoPeriferico.ACTIVO);

        // Assert
        assertEquals(EstadoNodoPeriferico.ACTIVO, prestador.getEstado());
    }
}

