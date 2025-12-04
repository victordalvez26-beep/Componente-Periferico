package uy.edu.tse.hcen.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PoliticasAccesoClient Tests")
class PoliticasAccesoClientTest {

    @InjectMocks
    private PoliticasAccesoClient client;

    @Test
    void client_shouldBeInstantiable() {
        assertNotNull(client);
    }

    @Test
    void verificarPermiso_withValidParams_shouldNotThrow() {
        assertDoesNotThrow(() -> 
            client.verificarPermiso("doctor1", "12345678", "EVALUACION", "101"));
    }

    @Test
    void verificarPermiso_withNulls_shouldNotThrow() {
        assertDoesNotThrow(() ->
            client.verificarPermiso(null, null, null, null));
    }
}

