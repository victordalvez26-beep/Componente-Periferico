package uy.edu.tse.hcen.client;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PoliticasAccesoClientTest {

    @InjectMocks
    private PoliticasAccesoClient client;

    private String originalEnvUrl;
    private String originalSysPropUrl;

    @BeforeEach
    void setUp() {
        originalEnvUrl = System.getenv("POLITICAS_SERVICE_URL");
        originalSysPropUrl = System.getProperty("POLITICAS_SERVICE_URL");
        System.clearProperty("POLITICAS_SERVICE_URL");
    }

    @AfterEach
    void tearDown() {
        if (originalEnvUrl != null) {
            // No podemos restaurar variables de entorno fácilmente
        }
        if (originalSysPropUrl != null) {
            System.setProperty("POLITICAS_SERVICE_URL", originalSysPropUrl);
        } else {
            System.clearProperty("POLITICAS_SERVICE_URL");
        }
    }

    @Test
    void testVerificarPermisoWithNullProfesionalId() {
        boolean result = client.verificarPermiso(null, "12345678", null, "101");
        assertFalse(result);
    }

    @Test
    void testVerificarPermisoWithBlankProfesionalId() {
        boolean result = client.verificarPermiso("", "12345678", null, "101");
        assertFalse(result);
    }

    @Test
    void testVerificarPermisoWithNullPacienteCI() {
        boolean result = client.verificarPermiso("prof1", null, null, "101");
        assertFalse(result);
    }

    @Test
    void testVerificarPermisoWithBlankPacienteCI() {
        boolean result = client.verificarPermiso("prof1", "", null, "101");
        assertFalse(result);
    }

    @Test
    void testVerificarPermisoSuccess() {
        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class)) {
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(mockClient);
            when(mockClient.target(anyString())).thenReturn(mockTarget);
            when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
            when(mockBuilder.get()).thenReturn(mockResponse);
            when(mockResponse.getStatus()).thenReturn(200);

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("tienePermiso", true);
            when(mockResponse.readEntity(Map.class)).thenReturn(responseMap);

            boolean result = client.verificarPermiso("prof1", "12345678", null, "101");

            assertTrue(result);
            verify(mockClient).close();
        }
    }

    @Test
    void testVerificarPermisoDenied() {
        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class)) {
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(mockClient);
            when(mockClient.target(anyString())).thenReturn(mockTarget);
            when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
            when(mockBuilder.get()).thenReturn(mockResponse);
            when(mockResponse.getStatus()).thenReturn(200);

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("tienePermiso", false);
            when(mockResponse.readEntity(Map.class)).thenReturn(responseMap);

            boolean result = client.verificarPermiso("prof1", "12345678", null, "101");

            assertFalse(result);
            verify(mockClient).close();
        }
    }

    @Test
    void testVerificarPermisoWithTipoDocumento() {
        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class)) {
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(mockClient);
            when(mockClient.target(contains("tipoDoc"))).thenReturn(mockTarget);
            when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
            when(mockBuilder.get()).thenReturn(mockResponse);
            when(mockResponse.getStatus()).thenReturn(200);

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("tienePermiso", true);
            when(mockResponse.readEntity(Map.class)).thenReturn(responseMap);

            boolean result = client.verificarPermiso("prof1", "12345678", "PDF", "101");

            assertTrue(result);
            verify(mockClient).close();
        }
    }

    @Test
    void testVerificarPermisoWithTenantId() {
        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class)) {
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(mockClient);
            when(mockClient.target(contains("tenantId"))).thenReturn(mockTarget);
            when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
            when(mockBuilder.get()).thenReturn(mockResponse);
            when(mockResponse.getStatus()).thenReturn(200);

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("tienePermiso", true);
            when(mockResponse.readEntity(Map.class)).thenReturn(responseMap);

            boolean result = client.verificarPermiso("prof1", "12345678", null, "101");

            assertTrue(result);
            verify(mockClient).close();
        }
    }

    @Test
    void testVerificarPermisoWithNon200Status() {
        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class)) {
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(mockClient);
            when(mockClient.target(anyString())).thenReturn(mockTarget);
            when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
            when(mockBuilder.get()).thenReturn(mockResponse);
            when(mockResponse.getStatus()).thenReturn(500);
            when(mockResponse.readEntity(String.class)).thenReturn("Internal Server Error");

            boolean result = client.verificarPermiso("prof1", "12345678", null, "101");

            assertFalse(result);
            verify(mockClient).close();
        }
    }

    @Test
    void testVerificarPermisoWithException() {
        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class)) {
            clientBuilderMock.when(ClientBuilder::newClient).thenThrow(new RuntimeException("Connection error"));

            boolean result = client.verificarPermiso("prof1", "12345678", null, "101");

            assertFalse(result);
        }
    }

    @Test
    void testVerificarPermisoWithNullTienePermiso() {
        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class)) {
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(mockClient);
            when(mockClient.target(anyString())).thenReturn(mockTarget);
            when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
            when(mockBuilder.get()).thenReturn(mockResponse);
            when(mockResponse.getStatus()).thenReturn(200);

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("tienePermiso", null);
            when(mockResponse.readEntity(Map.class)).thenReturn(responseMap);

            boolean result = client.verificarPermiso("prof1", "12345678", null, "101");

            assertFalse(result);
            verify(mockClient).close();
        }
    }

    @Test
    void testVerificarPermisoWithSpecialCharacters() {
        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class)) {
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(mockClient);
            when(mockClient.target(anyString())).thenReturn(mockTarget);
            when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
            when(mockBuilder.get()).thenReturn(mockResponse);
            when(mockResponse.getStatus()).thenReturn(200);

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("tienePermiso", true);
            when(mockResponse.readEntity(Map.class)).thenReturn(responseMap);

            boolean result = client.verificarPermiso("prof@test", "12345-678", "PDF/DOC", "tenant-101");

            assertTrue(result);
            verify(mockClient).close();
        }
    }
}

