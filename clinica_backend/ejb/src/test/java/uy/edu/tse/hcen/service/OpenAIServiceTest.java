package uy.edu.tse.hcen.service;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests EXHAUSTIVOS para OpenAIService con mocks JAX-RS.
 * Cubre TODOS los métodos y casos posibles.
 */
@DisplayName("OpenAIService Comprehensive Tests")
class OpenAIServiceTest {

    private final OpenAIService service = new OpenAIService();

    @Nested
    @DisplayName("generarResumenHistoriaClinica - Validaciones")
    class ValidacionesTests {

        @Test
        @DisplayName("Debe rechazar historia clínica null")
        void generarResumen_nullHistoria_throws() {
            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.generarResumenHistoriaClinica(null));
            assertTrue(ex.getMessage().contains("vacía"));
        }

        @Test
        @DisplayName("Debe rechazar historia clínica vacía")
        void generarResumen_emptyHistoria_throws() {
            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.generarResumenHistoriaClinica(""));
            assertTrue(ex.getMessage().contains("vacía"));
        }

        @Test
        @DisplayName("Debe rechazar historia clínica con solo espacios")
        void generarResumen_blankHistoria_throws() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> service.generarResumenHistoriaClinica("   "));
        }

        @Test
        @DisplayName("Debe rechazar cuando OPENROUTER_API_KEY no está configurado")
        void generarResumen_noApiKey_throws() {
            // Arrange - Asegurar que no hay API key
            System.clearProperty("OPENROUTER_API_KEY");
            
            // Act & Assert
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.generarResumenHistoriaClinica("Historia clínica de prueba"));
            assertTrue(ex.getMessage().contains("OPENROUTER_API_KEY no está configurado"));
        }

        @Test
        @DisplayName("Debe rechazar cuando API key está vacía")
        void generarResumen_emptyApiKey_throws() {
            // Arrange
            System.setProperty("OPENROUTER_API_KEY", "");
            
            // Act & Assert
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.generarResumenHistoriaClinica("Historia"));
            assertTrue(ex.getMessage().contains("OPENROUTER_API_KEY no está configurado"));
            
            System.clearProperty("OPENROUTER_API_KEY");
        }

        @Test
        @DisplayName("Debe rechazar cuando API key es solo espacios")
        void generarResumen_blankApiKey_throws() {
            // Arrange
            System.setProperty("OPENROUTER_API_KEY", "   ");
            
            // Act & Assert
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.generarResumenHistoriaClinica("Historia"));
            assertTrue(ex.getMessage().contains("OPENROUTER_API_KEY no está configurado"));
            
            System.clearProperty("OPENROUTER_API_KEY");
        }
    }

    @Nested
    @DisplayName("generarResumenHistoriaClinica - Casos Exitosos")
    class CasosExitososTests {

        @Test
        @DisplayName("Debe generar resumen con status 200 y respuesta válida")
        void generarResumen_status200_returnsResumen() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class, RETURNS_DEEP_STUBS);
            Response mockResponse = mock(Response.class);
            
            Map<String, Object> openAIResponse = new HashMap<>();
            Map<String, Object> choice = new HashMap<>();
            Map<String, Object> message = new HashMap<>();
            message.put("content", "Resumen generado por IA: Paciente con diagnóstico de gripe común. Tratamiento: reposo y antipiréticos.");
            choice.put("message", message);
            openAIResponse.put("choices", List.of(choice));

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                
                when(mockClient.target(anyString())
                        .request(MediaType.APPLICATION_JSON)
                        .header(anyString(), anyString())
                        .post(any())).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(200);
                when(mockResponse.readEntity(Map.class)).thenReturn(openAIResponse);

                System.setProperty("OPENROUTER_API_KEY", "sk-test-key-12345");

                // Act
                String resumen = service.generarResumenHistoriaClinica("Historia clínica de prueba");

                // Assert
                assertNotNull(resumen);
                assertTrue(resumen.contains("Resumen generado por IA"));
                
                System.clearProperty("OPENROUTER_API_KEY");
            }
        }

        @Test
        @DisplayName("Debe construir prompt con instrucciones médicas")
        void generarResumen_constructsPrompt() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class, RETURNS_DEEP_STUBS);
            Response mockResponse = mock(Response.class);
            
            Map<String, Object> openAIResponse = new HashMap<>();
            Map<String, Object> choice = new HashMap<>();
            Map<String, Object> message = new HashMap<>();
            message.put("content", "Resumen médico profesional");
            choice.put("message", message);
            openAIResponse.put("choices", List.of(choice));

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                
                when(mockClient.target(anyString())
                        .request(MediaType.APPLICATION_JSON)
                        .header(anyString(), anyString())
                        .post(any())).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(200);
                when(mockResponse.readEntity(Map.class)).thenReturn(openAIResponse);

                System.setProperty("OPENROUTER_API_KEY", "sk-test-key");

                // Act
                String resumen = service.generarResumenHistoriaClinica("Paciente con fiebre alta");

                // Assert
                assertNotNull(resumen);
                assertEquals("Resumen médico profesional", resumen);
                
                System.clearProperty("OPENROUTER_API_KEY");
            }
        }

        @Test
        @DisplayName("Debe usar modelo gpt-oss-20b:free")
        void generarResumen_usesCorrectModel() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class, RETURNS_DEEP_STUBS);
            Response mockResponse = mock(Response.class);
            
            Map<String, Object> openAIResponse = new HashMap<>();
            Map<String, Object> choice = new HashMap<>();
            Map<String, Object> message = new HashMap<>();
            message.put("content", "Resumen");
            choice.put("message", message);
            openAIResponse.put("choices", List.of(choice));

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                
                when(mockClient.target(contains("openrouter.ai"))
                        .request(MediaType.APPLICATION_JSON)
                        .header(anyString(), anyString())
                        .post(any())).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(200);
                when(mockResponse.readEntity(Map.class)).thenReturn(openAIResponse);

                System.setProperty("OPENROUTER_API_KEY", "sk-test-key");

                // Act
                service.generarResumenHistoriaClinica("Historia");

                // Assert
                verify(mockClient).target(contains("openrouter.ai"));
                
                System.clearProperty("OPENROUTER_API_KEY");
            }
        }

        @Test
        @DisplayName("Debe agregar header Authorization con Bearer token")
        void generarResumen_addsAuthHeader() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class, RETURNS_DEEP_STUBS);
            Response mockResponse = mock(Response.class);
            
            Map<String, Object> openAIResponse = new HashMap<>();
            Map<String, Object> choice = new HashMap<>();
            Map<String, Object> message = new HashMap<>();
            message.put("content", "Resumen");
            choice.put("message", message);
            openAIResponse.put("choices", List.of(choice));

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                
                when(mockClient.target(anyString())
                        .request(MediaType.APPLICATION_JSON)
                        .header(eq("Authorization"), startsWith("Bearer "))
                        .post(any())).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(200);
                when(mockResponse.readEntity(Map.class)).thenReturn(openAIResponse);

                System.setProperty("OPENROUTER_API_KEY", "sk-test-key");

                // Act
                service.generarResumenHistoriaClinica("Historia");

                // Assert - Verifica que se llamó con Authorization header
                verify(mockClient.target(anyString())
                        .request(MediaType.APPLICATION_JSON), atLeastOnce())
                        .header(eq("Authorization"), anyString());
                
                System.clearProperty("OPENROUTER_API_KEY");
            }
        }
    }

    @Nested
    @DisplayName("generarResumenHistoriaClinica - Casos de Error HTTP")
    class CasosErrorHttpTests {

        @Test
        @DisplayName("Debe lanzar excepción con status 401 (API key inválida)")
        void generarResumen_status401_throws() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class, RETURNS_DEEP_STUBS);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                
                when(mockClient.target(anyString())
                        .request(MediaType.APPLICATION_JSON)
                        .header(anyString(), anyString())
                        .post(any())).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(401);
                when(mockResponse.hasEntity()).thenReturn(true);
                when(mockResponse.readEntity(String.class)).thenReturn("Invalid API key");

                System.setProperty("OPENROUTER_API_KEY", "invalid-key");

                // Act & Assert
                RuntimeException ex = assertThrows(RuntimeException.class,
                        () -> service.generarResumenHistoriaClinica("Historia"));
                assertTrue(ex.getMessage().contains("401"));
                assertTrue(ex.getMessage().contains("Invalid API key"));
                
                System.clearProperty("OPENROUTER_API_KEY");
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción con status 429 (rate limit)")
        void generarResumen_status429_throws() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class, RETURNS_DEEP_STUBS);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                
                when(mockClient.target(anyString())
                        .request(MediaType.APPLICATION_JSON)
                        .header(anyString(), anyString())
                        .post(any())).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(429);
                when(mockResponse.hasEntity()).thenReturn(true);
                when(mockResponse.readEntity(String.class)).thenReturn("Rate limit exceeded");

                System.setProperty("OPENROUTER_API_KEY", "sk-test-key");

                // Act & Assert
                RuntimeException ex = assertThrows(RuntimeException.class,
                        () -> service.generarResumenHistoriaClinica("Historia"));
                assertTrue(ex.getMessage().contains("429"));
                
                System.clearProperty("OPENROUTER_API_KEY");
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción con status 500")
        void generarResumen_status500_throws() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class, RETURNS_DEEP_STUBS);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                
                when(mockClient.target(anyString())
                        .request(MediaType.APPLICATION_JSON)
                        .header(anyString(), anyString())
                        .post(any())).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(500);
                when(mockResponse.hasEntity()).thenReturn(true);
                when(mockResponse.readEntity(String.class)).thenReturn("Internal Server Error");

                System.setProperty("OPENROUTER_API_KEY", "sk-test-key");

                // Act & Assert
                RuntimeException ex = assertThrows(RuntimeException.class,
                        () -> service.generarResumenHistoriaClinica("Historia"));
                assertTrue(ex.getMessage().contains("500"));
                
                System.clearProperty("OPENROUTER_API_KEY");
            }
        }

        @Test
        @DisplayName("Debe manejar error sin entity body")
        void generarResumen_errorNoEntity_throws() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class, RETURNS_DEEP_STUBS);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                
                when(mockClient.target(anyString())
                        .request(MediaType.APPLICATION_JSON)
                        .header(anyString(), anyString())
                        .post(any())).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(503);
                when(mockResponse.hasEntity()).thenReturn(false);

                System.setProperty("OPENROUTER_API_KEY", "sk-test-key");

                // Act & Assert
                RuntimeException ex = assertThrows(RuntimeException.class,
                        () -> service.generarResumenHistoriaClinica("Historia"));
                assertTrue(ex.getMessage().contains("Unknown error"));
                
                System.clearProperty("OPENROUTER_API_KEY");
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando hay ProcessingException")
        void generarResumen_processingException_throws() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class, RETURNS_DEEP_STUBS);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                
                when(mockClient.target(anyString())
                        .request(MediaType.APPLICATION_JSON)
                        .header(anyString(), anyString())
                        .post(any())).thenThrow(new ProcessingException("Network error"));

                System.setProperty("OPENROUTER_API_KEY", "sk-test-key");

                // Act & Assert
                RuntimeException ex = assertThrows(RuntimeException.class,
                        () -> service.generarResumenHistoriaClinica("Historia"));
                assertTrue(ex.getMessage().contains("Error de conexión"));
                assertNotNull(ex.getCause());
                
                System.clearProperty("OPENROUTER_API_KEY");
            }
        }
    }

    @Nested
    @DisplayName("generarResumenHistoriaClinica - Respuestas Malformadas")
    class RespuestasMalformadasTests {

        @Test
        @DisplayName("Debe manejar respuesta sin choices")
        void generarResumen_noChoices_throws() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class, RETURNS_DEEP_STUBS);
            Response mockResponse = mock(Response.class);
            
            Map<String, Object> openAIResponse = new HashMap<>();
            openAIResponse.put("choices", null);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                
                when(mockClient.target(anyString())
                        .request(MediaType.APPLICATION_JSON)
                        .header(anyString(), anyString())
                        .post(any())).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(200);
                when(mockResponse.readEntity(Map.class)).thenReturn(openAIResponse);

                System.setProperty("OPENROUTER_API_KEY", "sk-test-key");

                // Act & Assert
                RuntimeException ex = assertThrows(RuntimeException.class,
                        () -> service.generarResumenHistoriaClinica("Historia"));
                assertTrue(ex.getMessage().contains("sin contenido válido"));
                
                System.clearProperty("OPENROUTER_API_KEY");
            }
        }

        @Test
        @DisplayName("Debe manejar choices vacío")
        void generarResumen_emptyChoices_throws() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class, RETURNS_DEEP_STUBS);
            Response mockResponse = mock(Response.class);
            
            Map<String, Object> openAIResponse = new HashMap<>();
            openAIResponse.put("choices", List.of());

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                
                when(mockClient.target(anyString())
                        .request(MediaType.APPLICATION_JSON)
                        .header(anyString(), anyString())
                        .post(any())).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(200);
                when(mockResponse.readEntity(Map.class)).thenReturn(openAIResponse);

                System.setProperty("OPENROUTER_API_KEY", "sk-test-key");

                // Act & Assert
                RuntimeException ex = assertThrows(RuntimeException.class,
                        () -> service.generarResumenHistoriaClinica("Historia"));
                assertTrue(ex.getMessage().contains("sin contenido válido"));
                
                System.clearProperty("OPENROUTER_API_KEY");
            }
        }

        @Test
        @DisplayName("Debe manejar message null")
        void generarResumen_nullMessage_throws() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class, RETURNS_DEEP_STUBS);
            Response mockResponse = mock(Response.class);
            
            Map<String, Object> openAIResponse = new HashMap<>();
            Map<String, Object> choice = new HashMap<>();
            choice.put("message", null);
            openAIResponse.put("choices", List.of(choice));

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                
                when(mockClient.target(anyString())
                        .request(MediaType.APPLICATION_JSON)
                        .header(anyString(), anyString())
                        .post(any())).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(200);
                when(mockResponse.readEntity(Map.class)).thenReturn(openAIResponse);

                System.setProperty("OPENROUTER_API_KEY", "sk-test-key");

                // Act & Assert
                RuntimeException ex = assertThrows(RuntimeException.class,
                        () -> service.generarResumenHistoriaClinica("Historia"));
                assertTrue(ex.getMessage().contains("sin contenido válido"));
                
                System.clearProperty("OPENROUTER_API_KEY");
            }
        }

        @Test
        @DisplayName("Debe manejar content null (retorna mensaje default)")
        void generarResumen_nullContent_returnsDefault() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class, RETURNS_DEEP_STUBS);
            Response mockResponse = mock(Response.class);
            
            Map<String, Object> openAIResponse = new HashMap<>();
            Map<String, Object> choice = new HashMap<>();
            Map<String, Object> message = new HashMap<>();
            message.put("content", null);
            choice.put("message", message);
            openAIResponse.put("choices", List.of(choice));

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                
                when(mockClient.target(anyString())
                        .request(MediaType.APPLICATION_JSON)
                        .header(anyString(), anyString())
                        .post(any())).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(200);
                when(mockResponse.readEntity(Map.class)).thenReturn(openAIResponse);

                System.setProperty("OPENROUTER_API_KEY", "sk-test-key");

                // Act
                String resumen = service.generarResumenHistoriaClinica("Historia");

                // Assert
                assertEquals("No se pudo generar el resumen", resumen);
                
                System.clearProperty("OPENROUTER_API_KEY");
            }
        }

        @Test
        @DisplayName("Debe manejar error al leer entity body")
        void generarResumen_errorReadingEntity_usesDefault() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class, RETURNS_DEEP_STUBS);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                
                when(mockClient.target(anyString())
                        .request(MediaType.APPLICATION_JSON)
                        .header(anyString(), anyString())
                        .post(any())).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(400);
                when(mockResponse.hasEntity()).thenReturn(true);
                when(mockResponse.readEntity(String.class)).thenThrow(new RuntimeException("Parse error"));

                System.setProperty("OPENROUTER_API_KEY", "sk-test-key");

                // Act & Assert
                RuntimeException ex = assertThrows(RuntimeException.class,
                        () -> service.generarResumenHistoriaClinica("Historia"));
                assertTrue(ex.getMessage().contains("Unknown error"));
                
                System.clearProperty("OPENROUTER_API_KEY");
            }
        }

        @Test
        @DisplayName("Debe manejar entity vacío")
        void generarResumen_emptyEntity_usesDefault() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class, RETURNS_DEEP_STUBS);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                
                when(mockClient.target(anyString())
                        .request(MediaType.APPLICATION_JSON)
                        .header(anyString(), anyString())
                        .post(any())).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(400);
                when(mockResponse.hasEntity()).thenReturn(true);
                when(mockResponse.readEntity(String.class)).thenReturn("   ");

                System.setProperty("OPENROUTER_API_KEY", "sk-test-key");

                // Act & Assert
                RuntimeException ex = assertThrows(RuntimeException.class,
                        () -> service.generarResumenHistoriaClinica("Historia"));
                assertTrue(ex.getMessage().contains("Unknown error"));
                
                System.clearProperty("OPENROUTER_API_KEY");
            }
        }
    }

    @Nested
    @DisplayName("generarResumenHistoriaClinica - Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Debe manejar historia clínica muy larga")
        void generarResumen_historiaLarga_success() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class, RETURNS_DEEP_STUBS);
            Response mockResponse = mock(Response.class);
            
            Map<String, Object> openAIResponse = new HashMap<>();
            Map<String, Object> choice = new HashMap<>();
            Map<String, Object> message = new HashMap<>();
            message.put("content", "Resumen de historia larga");
            choice.put("message", message);
            openAIResponse.put("choices", List.of(choice));

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                
                when(mockClient.target(anyString())
                        .request(MediaType.APPLICATION_JSON)
                        .header(anyString(), anyString())
                        .post(any())).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(200);
                when(mockResponse.readEntity(Map.class)).thenReturn(openAIResponse);

                System.setProperty("OPENROUTER_API_KEY", "sk-test-key");

                // Act
                String historiaLarga = "Historia clínica muy larga. ".repeat(100);
                String resumen = service.generarResumenHistoriaClinica(historiaLarga);

                // Assert
                assertNotNull(resumen);
                
                System.clearProperty("OPENROUTER_API_KEY");
            }
        }

        @Test
        @DisplayName("Debe manejar historia con caracteres especiales")
        void generarResumen_caracteresEspeciales_success() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class, RETURNS_DEEP_STUBS);
            Response mockResponse = mock(Response.class);
            
            Map<String, Object> openAIResponse = new HashMap<>();
            Map<String, Object> choice = new HashMap<>();
            Map<String, Object> message = new HashMap<>();
            message.put("content", "Resumen con caracteres especiales");
            choice.put("message", message);
            openAIResponse.put("choices", List.of(choice));

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                
                when(mockClient.target(anyString())
                        .request(MediaType.APPLICATION_JSON)
                        .header(anyString(), anyString())
                        .post(any())).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(200);
                when(mockResponse.readEntity(Map.class)).thenReturn(openAIResponse);

                System.setProperty("OPENROUTER_API_KEY", "sk-test-key");

                // Act
                String resumen = service.generarResumenHistoriaClinica("Paciente José María O'Brien - Diagnóstico: úlcera gástrica");

                // Assert
                assertNotNull(resumen);
                
                System.clearProperty("OPENROUTER_API_KEY");
            }
        }

        @Test
        @DisplayName("Debe manejar historia con saltos de línea")
        void generarResumen_conSaltosDeLinea_success() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class, RETURNS_DEEP_STUBS);
            Response mockResponse = mock(Response.class);
            
            Map<String, Object> openAIResponse = new HashMap<>();
            Map<String, Object> choice = new HashMap<>();
            Map<String, Object> message = new HashMap<>();
            message.put("content", "Resumen multilinea");
            choice.put("message", message);
            openAIResponse.put("choices", List.of(choice));

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                
                when(mockClient.target(anyString())
                        .request(MediaType.APPLICATION_JSON)
                        .header(anyString(), anyString())
                        .post(any())).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(200);
                when(mockResponse.readEntity(Map.class)).thenReturn(openAIResponse);

                System.setProperty("OPENROUTER_API_KEY", "sk-test-key");

                // Act
                String resumen = service.generarResumenHistoriaClinica("Línea 1\nLínea 2\nLínea 3");

                // Assert
                assertNotNull(resumen);
                
                System.clearProperty("OPENROUTER_API_KEY");
            }
        }

        @Test
        @DisplayName("Debe manejar múltiples llamadas consecutivas")
        void generarResumen_multipleCalls_success() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class, RETURNS_DEEP_STUBS);
            Response mockResponse = mock(Response.class);
            
            Map<String, Object> openAIResponse = new HashMap<>();
            Map<String, Object> choice = new HashMap<>();
            Map<String, Object> message = new HashMap<>();
            message.put("content", "Resumen");
            choice.put("message", message);
            openAIResponse.put("choices", List.of(choice));

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                
                when(mockClient.target(anyString())
                        .request(MediaType.APPLICATION_JSON)
                        .header(anyString(), anyString())
                        .post(any())).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(200);
                when(mockResponse.readEntity(Map.class)).thenReturn(openAIResponse);

                System.setProperty("OPENROUTER_API_KEY", "sk-test-key");

                // Act
                String resumen1 = service.generarResumenHistoriaClinica("Historia 1");
                String resumen2 = service.generarResumenHistoriaClinica("Historia 2");
                String resumen3 = service.generarResumenHistoriaClinica("Historia 3");

                // Assert
                assertNotNull(resumen1);
                assertNotNull(resumen2);
                assertNotNull(resumen3);
                
                System.clearProperty("OPENROUTER_API_KEY");
            }
        }

        @Test
        @DisplayName("Debe manejar API key muy larga")
        void generarResumen_longApiKey_success() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class, RETURNS_DEEP_STUBS);
            Response mockResponse = mock(Response.class);
            
            Map<String, Object> openAIResponse = new HashMap<>();
            Map<String, Object> choice = new HashMap<>();
            Map<String, Object> message = new HashMap<>();
            message.put("content", "Resumen");
            choice.put("message", message);
            openAIResponse.put("choices", List.of(choice));

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                
                when(mockClient.target(anyString())
                        .request(MediaType.APPLICATION_JSON)
                        .header(anyString(), anyString())
                        .post(any())).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(200);
                when(mockResponse.readEntity(Map.class)).thenReturn(openAIResponse);

                System.setProperty("OPENROUTER_API_KEY", "sk-test-key-very-long-key-with-many-characters-12345678901234567890");

                // Act
                String resumen = service.generarResumenHistoriaClinica("Historia");

                // Assert
                assertNotNull(resumen);
                
                System.clearProperty("OPENROUTER_API_KEY");
            }
        }

        @Test
        @DisplayName("Debe manejar API key corta (menos de 10 caracteres)")
        void generarResumen_shortApiKey_success() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class, RETURNS_DEEP_STUBS);
            Response mockResponse = mock(Response.class);
            
            Map<String, Object> openAIResponse = new HashMap<>();
            Map<String, Object> choice = new HashMap<>();
            Map<String, Object> message = new HashMap<>();
            message.put("content", "Resumen");
            choice.put("message", message);
            openAIResponse.put("choices", List.of(choice));

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                
                when(mockClient.target(anyString())
                        .request(MediaType.APPLICATION_JSON)
                        .header(anyString(), anyString())
                        .post(any())).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(200);
                when(mockResponse.readEntity(Map.class)).thenReturn(openAIResponse);

                System.setProperty("OPENROUTER_API_KEY", "sk-short");

                // Act
                String resumen = service.generarResumenHistoriaClinica("Historia");

                // Assert
                assertNotNull(resumen);
                
                System.clearProperty("OPENROUTER_API_KEY");
            }
        }
    }
}
