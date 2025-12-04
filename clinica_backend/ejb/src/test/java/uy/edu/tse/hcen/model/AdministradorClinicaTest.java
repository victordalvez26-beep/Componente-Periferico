package uy.edu.tse.hcen.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uy.edu.tse.hcen.model.enums.Departamentos;
import uy.edu.tse.hcen.model.enums.EstadoNodoPeriferico;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AdministradorClinica model.
 * 
 * @author Senior Test Engineer
 */
@DisplayName("AdministradorClinica Model Tests")
class AdministradorClinicaTest {

    // Concrete test class extending NodoPeriferico for testing
    static class TestNodoPeriferico extends NodoPeriferico {
        public TestNodoPeriferico() {
            super("Test", "123", Departamentos.MONTEVIDEO, "MVD", "Dir", "999", EstadoNodoPeriferico.ACTIVO);
        }
    }

    @Test
    @DisplayName("Constructor con nodo debe setear nodo correctamente")
    void constructor_withNodo_shouldSetNodo() {
        // Arrange
        TestNodoPeriferico nodo = new TestNodoPeriferico();

        // Act
        AdministradorClinica admin = new AdministradorClinica(nodo);

        // Assert
        assertNotNull(admin);
        assertEquals(nodo, admin.getAdministra());
    }

    @Test
    @DisplayName("Constructor con null debe permitir nodo null")
    void constructor_withNull_shouldAllowNull() {
        // Act
        AdministradorClinica admin = new AdministradorClinica(null);

        // Assert
        assertNotNull(admin);
        assertNull(admin.getAdministra());
    }

    @Test
    @DisplayName("getTenantNodeId debe retornar ID del nodo")
    void getTenantNodeId_withNodo_shouldReturnId() {
        // Arrange
        TestNodoPeriferico nodo = new TestNodoPeriferico();
        nodo.setId(101L);
        AdministradorClinica admin = new AdministradorClinica(nodo);

        // Act
        Long tenantNodeId = admin.getTenantNodeId();

        // Assert
        assertEquals(101L, tenantNodeId);
    }

    @Test
    @DisplayName("getTenantNodeId sin nodo debe retornar null")
    void getTenantNodeId_withoutNodo_shouldReturnNull() {
        // Arrange
        AdministradorClinica admin = new AdministradorClinica(null);

        // Act
        Long tenantNodeId = admin.getTenantNodeId();

        // Assert
        assertNull(tenantNodeId);
    }

    @Test
    @DisplayName("setAdministra debe actualizar nodo")
    void setAdministra_shouldUpdateNodo() {
        // Arrange
        AdministradorClinica admin = new AdministradorClinica(null);
        TestNodoPeriferico nodo = new TestNodoPeriferico();
        nodo.setId(102L);

        // Act
        admin.setAdministra(nodo);

        // Assert
        assertEquals(nodo, admin.getAdministra());
        assertEquals(102L, admin.getTenantNodeId());
    }

    @Test
    @DisplayName("Hereda propiedades de UsuarioPeriferico")
    void shouldInheritFromUsuarioPeriferico() {
        // Arrange
        TestNodoPeriferico nodo = new TestNodoPeriferico();
        AdministradorClinica admin = new AdministradorClinica(nodo);

        // Act - Usando métodos heredados
        admin.setNickname("admin1");
        admin.setNombre("Admin Test");
        admin.setEmail("admin@test.com");
        admin.setRole("ADMINISTRADOR");
        admin.setTenantId("101");

        // Assert
        assertEquals("admin1", admin.getNickname());
        assertEquals("Admin Test", admin.getNombre());
        assertEquals("admin@test.com", admin.getEmail());
        assertEquals("ADMINISTRADOR", admin.getRole());
        assertEquals("101", admin.getTenantId());
    }
}

