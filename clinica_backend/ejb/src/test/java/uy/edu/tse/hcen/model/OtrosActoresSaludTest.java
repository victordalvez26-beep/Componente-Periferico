package uy.edu.tse.hcen.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uy.edu.tse.hcen.model.enums.ActorSalud;
import uy.edu.tse.hcen.model.enums.Departamentos;
import uy.edu.tse.hcen.model.enums.EstadoNodoPeriferico;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OtrosActoresSalud model.
 * 
 * @author Senior Test Engineer
 */
@DisplayName("OtrosActoresSalud Model Tests")
class OtrosActoresSaludTest {

    @Test
    @DisplayName("Constructor con parámetros debe setear todos los valores")
    void constructor_shouldSetAllValues() {
        // Arrange
        OtrosActoresSalud.ContactInfo contactInfo = 
                new OtrosActoresSalud.ContactInfo("Montevideo", "Av. Italia 2025", "099123456");

        // Act
        OtrosActoresSalud actor = new OtrosActoresSalud(
                "Laboratorio Central",
                "123456780011",
                Departamentos.MONTEVIDEO,
                contactInfo,
                EstadoNodoPeriferico.ACTIVO,
                ActorSalud.LABORATORIO
        );

        // Assert
        assertEquals("Laboratorio Central", actor.getNombre());
        assertEquals("123456780011", actor.getRut());
        assertEquals(Departamentos.MONTEVIDEO, actor.getDepartamento());
        assertEquals("Montevideo", actor.getLocalidad());
        assertEquals("Av. Italia 2025", actor.getDireccion());
        assertEquals("099123456", actor.getContacto());
        assertEquals(EstadoNodoPeriferico.ACTIVO, actor.getEstado());
        assertEquals(ActorSalud.LABORATORIO, actor.getTipo());
    }

    @Test
    @DisplayName("setTipo y getTipo deben funcionar")
    void setTipo_shouldWork() {
        // Arrange
        OtrosActoresSalud.ContactInfo contactInfo = 
                new OtrosActoresSalud.ContactInfo("Montevideo", "Dir", "999");
        OtrosActoresSalud actor = new OtrosActoresSalud(
                "Actor Test", "123", Departamentos.MONTEVIDEO,
                contactInfo, EstadoNodoPeriferico.ACTIVO, ActorSalud.CLINICA
        );

        // Act
        actor.setTipo(ActorSalud.LABORATORIO);

        // Assert
        assertEquals(ActorSalud.LABORATORIO, actor.getTipo());
    }

    @Test
    @DisplayName("ContactInfo debe ser inmutable")
    void contactInfo_shouldBeImmutable() {
        // Act
        OtrosActoresSalud.ContactInfo contactInfo = 
                new OtrosActoresSalud.ContactInfo("Salto", "Calle 123", "092999888");

        // Assert
        assertEquals("Salto", contactInfo.getLocalidad());
        assertEquals("Calle 123", contactInfo.getDireccion());
        assertEquals("092999888", contactInfo.getContacto());
        
        // No hay setters - es inmutable
    }

    @Test
    @DisplayName("ContactInfo con valores null debe funcionar")
    void contactInfo_withNulls_shouldWork() {
        // Act
        OtrosActoresSalud.ContactInfo contactInfo = 
                new OtrosActoresSalud.ContactInfo(null, null, null);

        // Assert
        assertNull(contactInfo.getLocalidad());
        assertNull(contactInfo.getDireccion());
        assertNull(contactInfo.getContacto());
    }

    @Test
    @DisplayName("Heredar propiedades de NodoPeriferico")
    void shouldInheritFromNodoPeriferico() {
        // Arrange
        OtrosActoresSalud.ContactInfo contactInfo = 
                new OtrosActoresSalud.ContactInfo("Paysandú", "Av. España 100", "099888777");

        // Act
        OtrosActoresSalud actor = new OtrosActoresSalud(
                "Laboratorio Central",
                "999888777666",
                Departamentos.PAYSANDU,
                contactInfo,
                EstadoNodoPeriferico.INACTIVO,
                ActorSalud.LABORATORIO
        );
        
        actor.setId(50L);

        // Assert
        assertEquals(50L, actor.getId());
        assertEquals(Departamentos.PAYSANDU, actor.getDepartamento());
        assertEquals(EstadoNodoPeriferico.INACTIVO, actor.getEstado());
    }
}

