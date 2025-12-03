package uy.edu.tse.hcen.service;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpenAIServiceTest {

    private OpenAIService openAIService;
    private String originalApiKey;

    @BeforeEach
    void setUp() {
        openAIService = new OpenAIService();
        // Guardar el valor original de la variable de entorno
        originalApiKey = System.getProperty("OPENROUTER_API_KEY");
        if (originalApiKey == null) {
            originalApiKey = System.getenv("OPENROUTER_API_KEY");
        }
    }

    @Test
    void testGenerarResumenHistoriaClinicaNullInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            openAIService.generarResumenHistoriaClinica(null);
        });
    }

    @Test
    void testGenerarResumenHistoriaClinicaEmptyInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            openAIService.generarResumenHistoriaClinica("");
        });
    }

    @Test
    void testGenerarResumenHistoriaClinicaBlankInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            openAIService.generarResumenHistoriaClinica("   ");
        });
    }

    @Test
    void testGenerarResumenHistoriaClinicaNoApiKey() {
        // Limpiar la variable de entorno
        System.clearProperty("OPENROUTER_API_KEY");
        
        // Crear un nuevo servicio para que lea el entorno actualizado
        OpenAIService service = new OpenAIService();
        
        assertThrows(IllegalStateException.class, () -> {
            service.generarResumenHistoriaClinica("Historia clínica de prueba");
        });
    }

    @Test
    void testGenerarResumenHistoriaClinicaSuccess() {
        String testApiKey = "test-api-key-12345";
        String historiaClinica = "Paciente con síntomas de gripe. Tratamiento con paracetamol.";
        String resumenEsperado = "Resumen médico: Paciente con gripe tratado con paracetamol.";

        // Configurar variable de entorno
        System.setProperty("OPENROUTER_API_KEY", testApiKey);

        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            WebTarget webTarget = mock(WebTarget.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);
            Response response = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);
            when(client.target(anyString())).thenReturn(webTarget);
            when(webTarget.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.header(eq("Authorization"), anyString())).thenReturn(builder);
            when(builder.post(any(Entity.class))).thenReturn(response);
            when(response.getStatus()).thenReturn(200);

            // Construir respuesta mock
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> message = new HashMap<>();
            message.put("content", resumenEsperado);
            Map<String, Object> choice = new HashMap<>();
            choice.put("message", message);
            result.put("choices", List.of(choice));

            when(response.readEntity(Map.class)).thenReturn(result);

            // Ejecutar
            String resultado = openAIService.generarResumenHistoriaClinica(historiaClinica);

            // Verificar
            assertNotNull(resultado);
            assertEquals(resumenEsperado, resultado);
        } finally {
            // Restaurar variable de entorno
            if (originalApiKey != null) {
                System.setProperty("OPENROUTER_API_KEY", originalApiKey);
            } else {
                System.clearProperty("OPENROUTER_API_KEY");
            }
        }
    }

    @Test
    void testGenerarResumenHistoriaClinicaHttpError() {
        String testApiKey = "test-api-key-12345";
        String historiaClinica = "Historia clínica de prueba";

        System.setProperty("OPENROUTER_API_KEY", testApiKey);

        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            WebTarget webTarget = mock(WebTarget.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);
            Response response = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);
            when(client.target(anyString())).thenReturn(webTarget);
            when(webTarget.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.header(eq("Authorization"), anyString())).thenReturn(builder);
            when(builder.post(any(Entity.class))).thenReturn(response);
            when(response.getStatus()).thenReturn(500);
            when(response.hasEntity()).thenReturn(true);
            when(response.readEntity(String.class)).thenReturn("Internal Server Error");

            // Ejecutar y verificar
            assertThrows(RuntimeException.class, () -> {
                openAIService.generarResumenHistoriaClinica(historiaClinica);
            });
        } finally {
            if (originalApiKey != null) {
                System.setProperty("OPENROUTER_API_KEY", originalApiKey);
            } else {
                System.clearProperty("OPENROUTER_API_KEY");
            }
        }
    }

    @Test
    void testGenerarResumenHistoriaClinicaProcessingException() {
        String testApiKey = "test-api-key-12345";
        String historiaClinica = "Historia clínica de prueba";

        System.setProperty("OPENROUTER_API_KEY", testApiKey);

        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            WebTarget webTarget = mock(WebTarget.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);
            when(client.target(anyString())).thenReturn(webTarget);
            when(webTarget.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.header(eq("Authorization"), anyString())).thenReturn(builder);
            when(builder.post(any(Entity.class))).thenThrow(new ProcessingException("Connection error"));

            // Ejecutar y verificar
            assertThrows(RuntimeException.class, () -> {
                openAIService.generarResumenHistoriaClinica(historiaClinica);
            });
        } finally {
            if (originalApiKey != null) {
                System.setProperty("OPENROUTER_API_KEY", originalApiKey);
            } else {
                System.clearProperty("OPENROUTER_API_KEY");
            }
        }
    }

    @Test
    void testGenerarResumenHistoriaClinicaEmptyResponse() {
        String testApiKey = "test-api-key-12345";
        String historiaClinica = "Historia clínica de prueba";

        System.setProperty("OPENROUTER_API_KEY", testApiKey);

        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            WebTarget webTarget = mock(WebTarget.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);
            Response response = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);
            when(client.target(anyString())).thenReturn(webTarget);
            when(webTarget.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.header(eq("Authorization"), anyString())).thenReturn(builder);
            when(builder.post(any(Entity.class))).thenReturn(response);
            when(response.getStatus()).thenReturn(200);

            // Respuesta sin choices
            Map<String, Object> result = new HashMap<>();
            result.put("choices", List.of());

            when(response.readEntity(Map.class)).thenReturn(result);

            // Ejecutar y verificar
            assertThrows(RuntimeException.class, () -> {
                openAIService.generarResumenHistoriaClinica(historiaClinica);
            });
        } finally {
            if (originalApiKey != null) {
                System.setProperty("OPENROUTER_API_KEY", originalApiKey);
            } else {
                System.clearProperty("OPENROUTER_API_KEY");
            }
        }
    }

}

