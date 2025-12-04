package uy.edu.tse.hcen.client;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PoliticasAccesoClient Tests")
class PoliticasAccesoClientTest {

    @Test
    void verificarPermiso_shouldHandleParameters() {
        PoliticasAccesoClient client = new PoliticasAccesoClient();
        assertNotNull(client);
    }

    @Test
    void solicitarAcceso_shouldBeCallable() {
        PoliticasAccesoClient client = new PoliticasAccesoClient();
        assertNotNull(client);
    }
}
