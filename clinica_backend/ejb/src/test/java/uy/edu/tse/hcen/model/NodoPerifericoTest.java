package uy.edu.tse.hcen.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uy.edu.tse.hcen.model.enums.Departamentos;
import uy.edu.tse.hcen.model.enums.EstadoNodoPeriferico;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NodoPeriferico abstract class.
 * 
 * @author Senior Test Engineer
 */
@DisplayName("NodoPeriferico Model Tests")
class NodoPerifericoTest {

    // Concrete class for testing abstract NodoPeriferico
    static class TestNodoPeriferico extends NodoPeriferico {
        public TestNodoPeriferico() {
            super();
        }
        
        public TestNodoPeriferico(String nombre, String rut, Departamentos departamento, 
                                  String localidad, String direccion, String contacto, 
                                  EstadoNodoPeriferico estado) {
            super(nombre, rut, departamento, localidad, direccion, contacto, estado);
        }
    }

    @Test
    @DisplayName("Constructor por defecto debe crear nodo vacío")
    void defaultConstructor_shouldCreateEmptyNodo() {
        // Act
        TestNodoPeriferico nodo = new TestNodoPeriferico();

        // Assert
        assertNotNull(nodo);
        assertNull(nodo.getNombre());
        assertNull(nodo.getRut());
        assertNull(nodo.getDepartamento());
    }

    @Test
    @DisplayName("Constructor con parámetros debe setear valores correctamente")
    void parameterizedConstructor_shouldSetValues() {
        // Act
        TestNodoPeriferico nodo = new TestNodoPeriferico(
                "Clínica Test",
                "123456780011",
                Departamentos.MONTEVIDEO,
                "Montevideo",
                "Av. Italia 2025",
                "099123456",
                EstadoNodoPeriferico.ACTIVO
        );

        // Assert
        assertEquals("Clínica Test", nodo.getNombre());
        assertEquals("123456780011", nodo.getRut());
        assertEquals(Departamentos.MONTEVIDEO, nodo.getDepartamento());
        assertEquals("Montevideo", nodo.getLocalidad());
        assertEquals("Av. Italia 2025", nodo.getDireccion());
        assertEquals("099123456", nodo.getContacto());
        assertEquals(EstadoNodoPeriferico.ACTIVO, nodo.getEstado());
    }

    @Test
    @DisplayName("Setters y Getters deben funcionar correctamente")
    void settersAndGetters_shouldWork() {
        // Arrange
        TestNodoPeriferico nodo = new TestNodoPeriferico();

        // Act
        nodo.setId(1L);
        nodo.setNombre("Clínica");
        nodo.setRut("123456780011");
        nodo.setDepartamento(Departamentos.CANELONES);
        nodo.setLocalidad("Las Piedras");
        nodo.setDireccion("Calle 123");
        nodo.setContacto("099999999");
        nodo.setEstado(EstadoNodoPeriferico.INACTIVO);

        // Assert
        assertEquals(1L, nodo.getId());
        assertEquals("Clínica", nodo.getNombre());
        assertEquals("123456780011", nodo.getRut());
        assertEquals(Departamentos.CANELONES, nodo.getDepartamento());
        assertEquals("Las Piedras", nodo.getLocalidad());
        assertEquals("Calle 123", nodo.getDireccion());
        assertEquals("099999999", nodo.getContacto());
        assertEquals(EstadoNodoPeriferico.INACTIVO, nodo.getEstado());
    }

    @Test
    @DisplayName("Configuración puede ser null inicialmente")
    void configuracion_canBeNull() {
        // Arrange
        TestNodoPeriferico nodo = new TestNodoPeriferico();

        // Act & Assert
        assertNull(nodo.getConfiguracion());
    }

    @Test
    @DisplayName("Puede setear y obtener configuración")
    void configuracion_canBeSet() {
        // Arrange
        TestNodoPeriferico nodo = new TestNodoPeriferico();
        ConfiguracionClinica config = new ConfiguracionClinica();

        // Act
        nodo.setConfiguracion(config);

        // Assert
        assertNotNull(nodo.getConfiguracion());
        assertEquals(config, nodo.getConfiguracion());
    }

    @Test
    @DisplayName("Contacto vacío debe ser válido")
    void contacto_canBeEmpty() {
        // Arrange & Act
        TestNodoPeriferico nodo = new TestNodoPeriferico();
        nodo.setContacto("");

        // Assert
        assertEquals("", nodo.getContacto());
    }

    @Test
    @DisplayName("Localidad puede ser null")
    void localidad_canBeNull() {
        // Arrange & Act
        TestNodoPeriferico nodo = new TestNodoPeriferico();
        nodo.setLocalidad(null);

        // Assert
        assertNull(nodo.getLocalidad());
    }
}

