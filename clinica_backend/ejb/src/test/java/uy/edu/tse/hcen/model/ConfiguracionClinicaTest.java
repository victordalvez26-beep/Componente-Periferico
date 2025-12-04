package uy.edu.tse.hcen.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uy.edu.tse.hcen.model.enums.Departamentos;
import uy.edu.tse.hcen.model.enums.EstadoNodoPeriferico;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConfiguracionClinica model.
 * 
 * @author Senior Test Engineer
 */
@DisplayName("ConfiguracionClinica Model Tests")
class ConfiguracionClinicaTest {

    static class TestNodoPeriferico extends NodoPeriferico {
        public TestNodoPeriferico() {
            super("Test", "123", Departamentos.MONTEVIDEO, "MVD", "Dir", "999", EstadoNodoPeriferico.ACTIVO);
        }
    }

    @Test
    @DisplayName("Constructor por defecto debe crear configuración vacía")
    void defaultConstructor_shouldCreateEmpty() {
        // Act
        ConfiguracionClinica config = new ConfiguracionClinica();

        // Assert
        assertNotNull(config);
        assertNull(config.getLogoUrl());
        assertNull(config.getColorPrincipal());
        assertFalse(config.isHabilitado());
    }

    @Test
    @DisplayName("Constructor con parámetros debe setear valores")
    void parameterizedConstructor_shouldSetValues() {
        // Arrange
        TestNodoPeriferico nodo = new TestNodoPeriferico();
        nodo.setId(101L);

        // Act
        ConfiguracionClinica config = new ConfiguracionClinica(
                nodo,
                "https://logo.com/logo.png",
                "#007bff",
                true
        );

        // Assert
        assertEquals(nodo, config.getNodoPeriferico());
        assertEquals("https://logo.com/logo.png", config.getLogoUrl());
        assertEquals("#007bff", config.getColorPrincipal());
        assertTrue(config.isHabilitado());
    }

    @Test
    @DisplayName("Setters y Getters deben funcionar correctamente")
    void settersAndGetters_shouldWork() {
        // Arrange
        ConfiguracionClinica config = new ConfiguracionClinica();
        TestNodoPeriferico nodo = new TestNodoPeriferico();
        nodo.setId(102L);

        // Act
        config.setId(102L);
        config.setNodoPeriferico(nodo);
        config.setLogoUrl("https://newlogo.png");
        config.setColorPrincipal("#ff5733");
        config.setHabilitado(true);

        // Assert
        assertEquals(102L, config.getId());
        assertEquals(nodo, config.getNodoPeriferico());
        assertEquals("https://newlogo.png", config.getLogoUrl());
        assertEquals("#ff5733", config.getColorPrincipal());
        assertTrue(config.isHabilitado());
    }

    @Test
    @DisplayName("isHabilitado por defecto debe ser false")
    void habilitado_defaultShouldBeFalse() {
        // Act
        ConfiguracionClinica config = new ConfiguracionClinica();

        // Assert
        assertFalse(config.isHabilitado());
    }

    @Test
    @DisplayName("Logo URL puede ser null")
    void logoUrl_canBeNull() {
        // Arrange & Act
        ConfiguracionClinica config = new ConfiguracionClinica();
        config.setLogoUrl(null);

        // Assert
        assertNull(config.getLogoUrl());
    }

    @Test
    @DisplayName("Color principal puede ser null")
    void colorPrincipal_canBeNull() {
        // Arrange & Act
        ConfiguracionClinica config = new ConfiguracionClinica();
        config.setColorPrincipal(null);

        // Assert
        assertNull(config.getColorPrincipal());
    }

    @Test
    @DisplayName("Nodo periférico puede ser null")
    void nodoPeriferico_canBeNull() {
        // Arrange & Act
        ConfiguracionClinica config = new ConfiguracionClinica(null, "", "", false);

        // Assert
        assertNull(config.getNodoPeriferico());
    }
}

