package uy.edu.tse.hcen.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HcenUsuarioSaludClient.HcenUserResponse Tests")
class HcenUsuarioSaludClientTest {

    @Test
    @DisplayName("HcenUserResponse constructor debe setear valores")
    void hcenUserResponse_constructor_shouldSetValues() {
        // Act
        HcenUsuarioSaludClient.HcenUserResponse response = 
                new HcenUsuarioSaludClient.HcenUserResponse(1000L, "Usuario creado");

        // Assert
        assertEquals(1000L, response.getUserId());
        assertEquals("Usuario creado", response.getMensaje());
    }

    @Test
    @DisplayName("HcenUserResponse getters deben funcionar")
    void hcenUserResponse_getters_shouldWork() {
        // Arrange
        HcenUsuarioSaludClient.HcenUserResponse response = 
                new HcenUsuarioSaludClient.HcenUserResponse(2000L, "Mensaje test");

        // Act & Assert
        assertEquals(2000L, response.getUserId());
        assertEquals("Mensaje test", response.getMensaje());
    }

    @Test
    @DisplayName("HcenUserResponse con userId null debe funcionar")
    void hcenUserResponse_nullUserId_shouldWork() {
        // Act
        HcenUsuarioSaludClient.HcenUserResponse response = 
                new HcenUsuarioSaludClient.HcenUserResponse(null, "Error");

        // Assert
        assertNull(response.getUserId());
        assertEquals("Error", response.getMensaje());
    }

    @Test
    @DisplayName("HcenUserResponse con mensaje null debe funcionar")
    void hcenUserResponse_nullMessage_shouldWork() {
        // Act
        HcenUsuarioSaludClient.HcenUserResponse response = 
                new HcenUsuarioSaludClient.HcenUserResponse(1000L, null);

        // Assert
        assertEquals(1000L, response.getUserId());
        assertNull(response.getMensaje());
    }
}

