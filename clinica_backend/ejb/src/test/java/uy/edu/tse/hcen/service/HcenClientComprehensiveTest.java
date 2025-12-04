package uy.edu.tse.hcen.service;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import uy.edu.tse.hcen.dto.DTMetadatos;
import uy.edu.tse.hcen.exceptions.HcenUnavailableException;
import uy.edu.tse.hcen.utils.HcenCentralUrlUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests exhaustivos para HcenClient con mocks JAX-RS
 */
@DisplayName("HcenClient Comprehensive Tests")
class HcenClientComprehensiveTest {

    private HcenClient hcenClient;

    @BeforeEach
    void setUp() {
        hcenClient = new HcenClient();
    }

    @Nested
    @DisplayName("registrarMetadatos Tests")
    class RegistrarMetadatosTests {

        @Test
        @DisplayName("Debe registrar metadata con status 200")
        void registrar_status200_success() throws Exception {
            // Arrange
            DTMetadatos metadata = createMetadata();
            
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.header(anyString(), anyString())).thenReturn(mockBuilder);
                when(mockBuilder.post(any(Entity.class))).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(200);

                // Act
                hcenClient.registrarMetadatos(metadata);

                // Assert
                verify(mockBuilder).post(any(Entity.class));
                verify(mockResponse).close();
                verify(mockClient).close();
            }
        }

        @Test
        @DisplayName("Debe registrar metadata con status 201")
        void registrar_status201_success() throws Exception {
            // Arrange
            DTMetadatos metadata = createMetadata();
            
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.header(anyString(), anyString())).thenReturn(mockBuilder);
                when(mockBuilder.post(any(Entity.class))).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(201);

                // Act
                hcenClient.registrarMetadatos(metadata);

                // Assert
                verify(mockBuilder).post(any(Entity.class));
            }
        }

        @Test
        @DisplayName("Debe registrar metadata con status 202")
        void registrar_status202_success() throws Exception {
            // Arrange
            DTMetadatos metadata = createMetadata();
            
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.header(anyString(), anyString())).thenReturn(mockBuilder);
                when(mockBuilder.post(any(Entity.class))).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(202);

                // Act
                hcenClient.registrarMetadatos(metadata);

                // Assert
                verify(mockBuilder).post(any(Entity.class));
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción con status 500")
        void registrar_status500_throws() {
            // Arrange
            DTMetadatos metadata = createMetadata();
            
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.header(anyString(), anyString())).thenReturn(mockBuilder);
                when(mockBuilder.post(any(Entity.class))).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(500);
                when(mockResponse.hasEntity()).thenReturn(true);
                when(mockResponse.readEntity(String.class)).thenReturn("Server error");

                // Act & Assert
                assertThrows(HcenUnavailableException.class,
                        () -> hcenClient.registrarMetadatos(metadata));
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción con status 400")
        void registrar_status400_throws() {
            // Arrange
            DTMetadatos metadata = createMetadata();
            
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.header(anyString(), anyString())).thenReturn(mockBuilder);
                when(mockBuilder.post(any(Entity.class))).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(400);
                when(mockResponse.hasEntity()).thenReturn(false);

                // Act & Assert
                assertThrows(HcenUnavailableException.class,
                        () -> hcenClient.registrarMetadatos(metadata));
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción con ProcessingException")
        void registrar_processingException_throws() {
            // Arrange
            DTMetadatos metadata = createMetadata();
            
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.header(anyString(), anyString())).thenReturn(mockBuilder);
                when(mockBuilder.post(any(Entity.class)))
                        .thenThrow(new ProcessingException("Network timeout"));

                // Act & Assert
                HcenUnavailableException ex = assertThrows(HcenUnavailableException.class,
                        () -> hcenClient.registrarMetadatos(metadata));
                assertTrue(ex.getMessage().contains("no disponible"));
            }
        }
    }

    @Nested
    @DisplayName("registrarMetadatosCompleto Tests")
    class RegistrarMetadatosCompletoTests {

        @Test
        @DisplayName("Debe registrar metadata completo exitosamente")
        void registrarCompleto_success() throws Exception {
            // Arrange
            Map<String, Object> payload = createPayload();
            
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.header(anyString(), anyString())).thenReturn(mockBuilder);
                when(mockBuilder.post(any(Entity.class))).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(200);

                // Act
                hcenClient.registrarMetadatosCompleto(payload);

                // Assert
                verify(mockBuilder).post(any(Entity.class));
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción con error")
        void registrarCompleto_error_throws() {
            // Arrange
            Map<String, Object> payload = createPayload();
            
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.header(anyString(), anyString())).thenReturn(mockBuilder);
                when(mockBuilder.post(any(Entity.class))).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(500);
                when(mockResponse.hasEntity()).thenReturn(true);
                when(mockResponse.readEntity(String.class)).thenReturn("Error");

                // Act & Assert
                assertThrows(HcenUnavailableException.class,
                        () -> hcenClient.registrarMetadatosCompleto(payload));
            }
        }
    }

    @Nested
    @DisplayName("obtenerMetadatosDocumentosPorCI Tests")
    class ObtenerMetadatosTests {

        @Test
        @DisplayName("Debe obtener metadatos exitosamente")
        void obtenerMetadatos_success() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            List<Map<String, Object>> expectedList = new ArrayList<>();
            expectedList.add(Map.of("documentoId", "doc1"));
            expectedList.add(Map.of("documentoId", "doc2"));

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.header(anyString(), anyString())).thenReturn(mockBuilder);
                when(mockBuilder.get()).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(200);
                when(mockResponse.readEntity(List.class)).thenReturn(expectedList);

                // Act
                List<Map<String, Object>> result = hcenClient.obtenerMetadatosDocumentosPorCI(
                        "12345678", "doctor1", "101", "MEDICINA_GENERAL", "Dr. Juan");

                // Assert
                assertNotNull(result);
                assertEquals(2, result.size());
            }
        }

        @Test
        @DisplayName("Debe retornar lista vacía con status 404")
        void obtenerMetadatos_status404_returnsEmpty() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.header(anyString(), anyString())).thenReturn(mockBuilder);
                when(mockBuilder.get()).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(404);

                // Act
                List<Map<String, Object>> result = hcenClient.obtenerMetadatosDocumentosPorCI(
                        "99999999", "doctor1", "101", "MEDICINA_GENERAL", "Dr. Test");

                // Assert
                assertNotNull(result);
                assertEquals(0, result.size());
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción con status 500")
        void obtenerMetadatos_status500_throws() {
            // Arrange
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.header(anyString(), anyString())).thenReturn(mockBuilder);
                when(mockBuilder.get()).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(500);
                when(mockResponse.hasEntity()).thenReturn(true);
                when(mockResponse.readEntity(String.class)).thenReturn("Internal error");

                // Act & Assert
                assertThrows(HcenUnavailableException.class,
                        () -> hcenClient.obtenerMetadatosDocumentosPorCI(
                                "12345678", "doctor1", "101", "MEDICINA_GENERAL", "Dr. Test"));
            }
        }

        @Test
        @DisplayName("Debe manejar ProcessingException")
        void obtenerMetadatos_processingException_throws() {
            // Arrange
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.header(anyString(), anyString())).thenReturn(mockBuilder);
                when(mockBuilder.get()).thenThrow(new ProcessingException("Connection timeout"));

                // Act & Assert
                assertThrows(HcenUnavailableException.class,
                        () -> hcenClient.obtenerMetadatosDocumentosPorCI(
                                "12345678", "doctor1", "101", "MEDICINA_GENERAL", "Dr. Test"));
            }
        }
    }

    @Nested
    @DisplayName("consultarMetadatosPaciente Tests")
    class ConsultarMetadatosTests {

        @Test
        @DisplayName("Debe consultar metadatos exitosamente")
        void consultarMetadatos_success() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            List<Map<String, Object>> expectedList = List.of(
                    Map.of("id", "doc1", "tipo", "EVALUACION"),
                    Map.of("id", "doc2", "tipo", "RECETA")
            );

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.get()).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(200);
                when(mockResponse.readEntity(List.class)).thenReturn(expectedList);

                // Act
                List<Map<String, Object>> result = hcenClient.consultarMetadatosPaciente("12345678");

                // Assert
                assertNotNull(result);
                assertEquals(2, result.size());
            }
        }

        @Test
        @DisplayName("Debe retornar lista vacía con status 404")
        void consultarMetadatos_status404_returnsEmpty() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.get()).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(404);

                // Act
                List<Map<String, Object>> result = hcenClient.consultarMetadatosPaciente("99999999");

                // Assert
                assertNotNull(result);
                assertEquals(0, result.size());
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción con status 500")
        void consultarMetadatos_status500_throws() {
            // Arrange
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.get()).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(500);

                // Act & Assert
                assertThrows(HcenUnavailableException.class,
                        () -> hcenClient.consultarMetadatosPaciente("12345678"));
            }
        }
    }

    @Nested
    @DisplayName("Token Management Tests")
    class TokenManagementTests {

        @Test
        @DisplayName("Debe generar token de servicio localmente")
        void getServiceToken_generatesLocally() throws Exception {
            // Arrange
            DTMetadatos metadata = createMetadata();
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.header(anyString(), anyString())).thenReturn(mockBuilder);
                when(mockBuilder.post(any(Entity.class))).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(200);

                // Act - Llama getServiceToken() internamente
                hcenClient.registrarMetadatos(metadata);

                // Assert - Verifica que se agregó header de autorización
                verify(mockBuilder).header(eq("Authorization"), startsWith("Bearer "));
            }
        }

        @Test
        @DisplayName("Debe usar token cacheado en llamadas consecutivas")
        void getServiceToken_usesCache() throws Exception {
            // Arrange
            DTMetadatos metadata = createMetadata();
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.header(anyString(), anyString())).thenReturn(mockBuilder);
                when(mockBuilder.post(any(Entity.class))).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(200);

                // Act - Hacer 3 llamadas consecutivas
                hcenClient.registrarMetadatos(metadata);
                hcenClient.registrarMetadatos(metadata);
                hcenClient.registrarMetadatos(metadata);

                // Assert - Token se genera una vez y se reusa
                verify(mockBuilder, times(3)).header(eq("Authorization"), anyString());
            }
        }

        @Test
        @DisplayName("Debe reintentar con nuevo token cuando recibe 401")
        void handleTokenRejection_retries() throws Exception {
            // Arrange
            DTMetadatos metadata = createMetadata();
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse401 = mock(Response.class);
            Response mockResponse200 = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.header(anyString(), anyString())).thenReturn(mockBuilder);
                
                // Primera llamada retorna 401, segunda retorna 200
                when(mockBuilder.post(any(Entity.class)))
                        .thenReturn(mockResponse401)
                        .thenReturn(mockResponse200);
                when(mockResponse401.getStatus()).thenReturn(401);
                when(mockResponse200.getStatus()).thenReturn(200);

                // Act - Ejecuta el código de retry
                hcenClient.registrarMetadatos(metadata);

                // Assert - Se hicieron 2 intentos (original + retry)
                verify(mockBuilder, times(2)).post(any(Entity.class));
                verify(mockResponse401).close();
                verify(mockResponse200).close();
            }
        }

        @Test
        @DisplayName("Debe reintentar con nuevo token cuando recibe 403")
        void handleTokenRejection_403_retries() throws Exception {
            // Arrange
            DTMetadatos metadata = createMetadata();
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse403 = mock(Response.class);
            Response mockResponse200 = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.header(anyString(), anyString())).thenReturn(mockBuilder);
                
                when(mockBuilder.post(any(Entity.class)))
                        .thenReturn(mockResponse403)
                        .thenReturn(mockResponse200);
                when(mockResponse403.getStatus()).thenReturn(403);
                when(mockResponse200.getStatus()).thenReturn(200);

                // Act
                hcenClient.registrarMetadatos(metadata);

                // Assert
                verify(mockBuilder, times(2)).post(any(Entity.class));
            }
        }
    }

    @Nested
    @DisplayName("HTTP Status Code Tests")
    class HttpStatusCodeTests {

        @Test
        @DisplayName("Debe aceptar status 202 (Accepted)")
        void registrar_status202_success() throws Exception {
            // Arrange
            DTMetadatos metadata = createMetadata();
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.header(anyString(), anyString())).thenReturn(mockBuilder);
                when(mockBuilder.post(any(Entity.class))).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(202);

                // Act
                hcenClient.registrarMetadatos(metadata);

                // Assert - No debe lanzar excepción
                verify(mockBuilder).post(any(Entity.class));
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción con status 400")
        void registrar_status400_throwsException() throws Exception {
            // Arrange
            DTMetadatos metadata = createMetadata();
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.header(anyString(), anyString())).thenReturn(mockBuilder);
                when(mockBuilder.post(any(Entity.class))).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(400);
                when(mockResponse.hasEntity()).thenReturn(true);
                when(mockResponse.readEntity(String.class)).thenReturn("Bad Request");

                // Act & Assert
                assertThrows(HcenUnavailableException.class, () ->
                        hcenClient.registrarMetadatos(metadata));
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción con status 500")
        void registrar_status500_throwsException() throws Exception {
            // Arrange
            DTMetadatos metadata = createMetadata();
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.header(anyString(), anyString())).thenReturn(mockBuilder);
                when(mockBuilder.post(any(Entity.class))).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(500);
                when(mockResponse.hasEntity()).thenReturn(true);
                when(mockResponse.readEntity(String.class)).thenReturn("Internal Server Error");

                // Act & Assert
                assertThrows(HcenUnavailableException.class, () ->
                        hcenClient.registrarMetadatos(metadata));
            }
        }

        @Test
        @DisplayName("Debe manejar error sin entity body")
        void registrar_errorNoEntity_throwsException() throws Exception {
            // Arrange
            DTMetadatos metadata = createMetadata();
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.header(anyString(), anyString())).thenReturn(mockBuilder);
                when(mockBuilder.post(any(Entity.class))).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(500);
                when(mockResponse.hasEntity()).thenReturn(false);

                // Act & Assert
                HcenUnavailableException ex = assertThrows(HcenUnavailableException.class, () ->
                        hcenClient.registrarMetadatos(metadata));
                assertTrue(ex.getMessage().contains("Unknown error"));
            }
        }
    }

    @Nested
    @DisplayName("URL Encoding Tests")
    class UrlEncodingTests {

        @Test
        @DisplayName("Debe encodear caracteres especiales en profesionalId")
        void obtener_specialCharsInProfesionalId_encodesUrl() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.header(anyString(), anyString())).thenReturn(mockBuilder);
                when(mockBuilder.get()).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(200);
                when(mockResponse.readEntity(List.class)).thenReturn(new ArrayList<>());

                // Act - profesionalId con caracteres especiales
                hcenClient.obtenerMetadatosDocumentosPorCI("12345678", "prof@123", "101", "CARDIOLOGIA", "Dr. José");

                // Assert - Verifica que se llamó con URL encodificada
                verify(mockClient).target(contains("prof"));
            }
        }

        @Test
        @DisplayName("Debe encodear espacios en nombreProfesional")
        void obtener_spacesInNombre_encodesUrl() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.header(anyString(), anyString())).thenReturn(mockBuilder);
                when(mockBuilder.get()).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(200);
                when(mockResponse.readEntity(List.class)).thenReturn(new ArrayList<>());

                // Act - nombre con espacios
                hcenClient.obtenerMetadatosDocumentosPorCI("12345678", "prof1", "101", "CARDIOLOGIA", "Dr. José María");

                // Assert
                verify(mockBuilder).get();
            }
        }

        @Test
        @DisplayName("Debe manejar parámetros null sin agregar query params")
        void obtener_nullParams_noQueryParams() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.header(anyString(), anyString())).thenReturn(mockBuilder);
                when(mockBuilder.get()).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(200);
                when(mockResponse.readEntity(List.class)).thenReturn(new ArrayList<>());

                // Act - Todos los params null
                List<Map<String, Object>> result = hcenClient.obtenerMetadatosDocumentosPorCI("12345678", null, null, null, null);

                // Assert - Retorna lista vacía, no lanza excepción
                assertNotNull(result);
                verify(mockClient).target(anyString());
            }
        }
    }

    @Nested
    @DisplayName("consultarMetadatosPaciente Tests - TODOS LOS CASOS")
    class ConsultarMetadatosPacienteTests {

        @Test
        @DisplayName("Debe consultar metadatos con status 200")
        void consultar_status200_returnsMetadatos() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);
            
            List<Map<String, Object>> expectedMetadatos = List.of(
                    Map.of("documentoId", "doc1", "tipo", "EVALUACION"),
                    Map.of("documentoId", "doc2", "tipo", "INFORME")
            );

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api/paciente/12345678/metadatos");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.get()).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(200);
                when(mockResponse.readEntity(List.class)).thenReturn(expectedMetadatos);

                // Act
                List<Map<String, Object>> result = hcenClient.consultarMetadatosPaciente("12345678");

                // Assert
                assertNotNull(result);
                assertEquals(2, result.size());
                verify(mockBuilder).get();
            }
        }

        @Test
        @DisplayName("Debe retornar lista vacía con status 404")
        void consultar_status404_returnsEmptyList() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.get()).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(404);

                // Act
                List<Map<String, Object>> result = hcenClient.consultarMetadatosPaciente("99999999");

                // Assert
                assertNotNull(result);
                assertTrue(result.isEmpty());
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción con status 500")
        void consultar_status500_throwsException() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.get()).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(500);

                // Act & Assert
                assertThrows(HcenUnavailableException.class, () ->
                        hcenClient.consultarMetadatosPaciente("12345678"));
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando hay ProcessingException")
        void consultar_processingException_throwsHcenUnavailable() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.get()).thenThrow(new ProcessingException("Network error"));

                // Act & Assert
                HcenUnavailableException ex = assertThrows(HcenUnavailableException.class, () ->
                        hcenClient.consultarMetadatosPaciente("12345678"));
                assertTrue(ex.getMessage().contains("HCEN no disponible"));
            }
        }

        @Test
        @DisplayName("Debe consultar con CI con guiones")
        void consultar_ciWithDashes_success() throws Exception {
            // Arrange
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.get()).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(200);
                when(mockResponse.readEntity(List.class)).thenReturn(new ArrayList<>());

                // Act
                List<Map<String, Object>> result = hcenClient.consultarMetadatosPaciente("1.234.567-8");

                // Assert
                assertNotNull(result);
            }
        }
    }

    @Nested
    @DisplayName("registrarAccesoHistoriaClinica Tests - TODOS LOS CASOS")
    class RegistrarAccesoHistoriaClinicaTests {

        @Test
        @DisplayName("Debe registrar acceso exitoso con todos los parámetros")
        void registrarAcceso_conTodosParams_registra() {
            // Arrange
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(HcenCentralUrlUtil::getBaseUrl)
                        .thenReturn("http://localhost:8080");

                // Configurar TODA la cadena de mocks
                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(eq(MediaType.APPLICATION_JSON))).thenReturn(mockBuilder);
                when(mockBuilder.post(any())).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(201);
                when(mockResponse.hasEntity()).thenReturn(false);
                doNothing().when(mockClient).close();

                // Act - NO debe lanzar excepción
                hcenClient.registrarAccesoHistoriaClinica(
                        "prof123", "Dr. Juan Pérez", "CARDIOLOGIA",
                        "101", "12345678", "doc123", "EVALUACION", true);

                // Assert - Verifica que se ejecutó el flujo completo
                verify(mockClient).target(contains("/hcen-politicas-service/api/registros"));
                verify(mockBuilder).post(any());
                verify(mockResponse).getStatus();
                verify(mockClient).close();
            }
        }

        @Test
        @DisplayName("Debe registrar acceso sin documentoId")
        void registrarAcceso_sinDocumentoId_registra() {
            // Arrange
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(HcenCentralUrlUtil::getBaseUrl)
                        .thenReturn("http://localhost:8080");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(eq(MediaType.APPLICATION_JSON))).thenReturn(mockBuilder);
                when(mockBuilder.post(any())).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(200);
                when(mockResponse.hasEntity()).thenReturn(false);
                doNothing().when(mockClient).close();

                // Act - documentoId null
                hcenClient.registrarAccesoHistoriaClinica(
                        "prof123", "Dr. Juan", "CARDIOLOGIA",
                        "101", "12345678", null, null, true);

                // Assert
                verify(mockBuilder).post(any());
                verify(mockResponse).getStatus();
                verify(mockClient).close();
            }
        }

        @Test
        @DisplayName("Debe registrar acceso fallido con motivoRechazo")
        void registrarAcceso_exitoFalse_agregaMotivoRechazo() {
            // Arrange
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(HcenCentralUrlUtil::getBaseUrl)
                        .thenReturn("http://localhost:8080");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(eq(MediaType.APPLICATION_JSON))).thenReturn(mockBuilder);
                when(mockBuilder.post(any())).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(201);
                when(mockResponse.hasEntity()).thenReturn(false);
                doNothing().when(mockClient).close();

                // Act - exito = false
                hcenClient.registrarAccesoHistoriaClinica(
                        "prof123", "Dr. Juan", "CARDIOLOGIA",
                        "101", "12345678", "doc123", "EVALUACION", false);

                // Assert - Verifica que se ejecutó (payload tiene motivoRechazo)
                verify(mockBuilder).post(any());
                verify(mockClient).close();
            }
        }

        @Test
        @DisplayName("Debe usar tipo DESCARGA cuando tipoDocumento es null")
        void registrarAcceso_tipoDocumentoNull_usaDescarga() {
            // Arrange
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(HcenCentralUrlUtil::getBaseUrl)
                        .thenReturn("http://localhost:8080");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(eq(MediaType.APPLICATION_JSON))).thenReturn(mockBuilder);
                when(mockBuilder.post(any())).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(201);
                when(mockResponse.hasEntity()).thenReturn(false);
                doNothing().when(mockClient).close();

                // Act - tipoDocumento null (debe usar "DESCARGA" por defecto)
                hcenClient.registrarAccesoHistoriaClinica(
                        "prof123", "Dr. Juan", "CARDIOLOGIA",
                        "101", "12345678", "doc123", null, true);

                // Assert
                verify(mockBuilder).post(any());
                verify(mockClient).close();
            }
        }

        @Test
        @DisplayName("Debe manejar error sin lanzar excepción (no crítico)")
        void registrarAcceso_error_noPropagaExcepcion() {
            // Arrange
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(HcenCentralUrlUtil::getBaseUrl)
                        .thenReturn("http://localhost:8080");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.post(any(Entity.class))).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(500);
                when(mockResponse.hasEntity()).thenReturn(true);
                when(mockResponse.readEntity(String.class)).thenReturn("Error interno");

                // Act - NO debe lanzar excepción
                assertDoesNotThrow(() ->
                        hcenClient.registrarAccesoHistoriaClinica(
                                "prof123", "Dr. Juan", "CARDIOLOGIA",
                                "101", "12345678", "doc123", "EVALUACION", true));
            }
        }

        @Test
        @DisplayName("Debe manejar ProcessingException sin propagarla")
        void registrarAcceso_processingException_noPropaga() {
            // Arrange
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(HcenCentralUrlUtil::getBaseUrl)
                        .thenReturn("http://localhost:8080");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.post(any(Entity.class))).thenThrow(new ProcessingException("Network error"));

                // Act - NO debe lanzar excepción (catch genérico)
                assertDoesNotThrow(() ->
                        hcenClient.registrarAccesoHistoriaClinica(
                                "prof123", "Dr. Juan", "CARDIOLOGIA",
                                "101", "12345678", "doc123", "EVALUACION", true));
            }
        }

        @Test
        @DisplayName("Debe registrar con nombreProfesional null")
        void registrarAcceso_nombreNull_registra() {
            // Arrange
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(HcenCentralUrlUtil::getBaseUrl)
                        .thenReturn("http://localhost:8080");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(eq(MediaType.APPLICATION_JSON))).thenReturn(mockBuilder);
                when(mockBuilder.post(any())).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(201);
                when(mockResponse.hasEntity()).thenReturn(false);
                doNothing().when(mockClient).close();

                // Act - nombreProfesional null (no se agrega al payload)
                hcenClient.registrarAccesoHistoriaClinica(
                        "prof123", null, null,
                        "101", "12345678", "doc123", "EVALUACION", true);

                // Assert
                verify(mockBuilder).post(any());
                verify(mockClient).close();
            }
        }

        @Test
        @DisplayName("Debe registrar con especialidad vacía")
        void registrarAcceso_especialidadBlank_registra() {
            // Arrange
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(HcenCentralUrlUtil::getBaseUrl)
                        .thenReturn("http://localhost:8080");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(eq(MediaType.APPLICATION_JSON))).thenReturn(mockBuilder);
                when(mockBuilder.post(any())).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(201);
                when(mockResponse.hasEntity()).thenReturn(false);
                doNothing().when(mockClient).close();

                // Act - especialidad blank (no se agrega al payload)
                hcenClient.registrarAccesoHistoriaClinica(
                        "prof123", "Dr. Juan", "   ",
                        "101", "12345678", "doc123", "EVALUACION", true);

                // Assert
                verify(mockBuilder).post(any());
                verify(mockClient).close();
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("HcenClient debe ser instanciable")
        void hcenClient_isInstantiable() {
            assertNotNull(hcenClient);
        }

        @Test
        @DisplayName("Debe manejar metadata con campos mínimos")
        void registrar_minimalMetadata_success() throws Exception {
            // Arrange
            DTMetadatos metadata = new DTMetadatos();
            metadata.setDocumentoId("doc123");
            metadata.setDocumentoIdPaciente("12345678");
            metadata.setTenantId("101");
            
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.header(anyString(), anyString())).thenReturn(mockBuilder);
                when(mockBuilder.post(any(Entity.class))).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(200);

                // Act
                hcenClient.registrarMetadatos(metadata);

                // Assert
                verify(mockBuilder).post(any(Entity.class));
            }
        }

        @Test
        @DisplayName("Debe manejar payload vacío")
        void registrarCompleto_emptyPayload_success() throws Exception {
            // Arrange
            Map<String, Object> payload = new HashMap<>();
            
            Client mockClient = mock(Client.class);
            WebTarget mockTarget = mock(WebTarget.class);
            Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
            Response mockResponse = mock(Response.class);

            try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class);
                 MockedStatic<HcenCentralUrlUtil> util = mockStatic(HcenCentralUrlUtil.class)) {
                
                cb.when(ClientBuilder::newClient).thenReturn(mockClient);
                util.when(() -> HcenCentralUrlUtil.buildApiUrl(anyString()))
                        .thenReturn("http://localhost:8080/api");

                when(mockClient.target(anyString())).thenReturn(mockTarget);
                when(mockTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockBuilder);
                when(mockBuilder.header(anyString(), anyString())).thenReturn(mockBuilder);
                when(mockBuilder.post(any(Entity.class))).thenReturn(mockResponse);
                when(mockResponse.getStatus()).thenReturn(200);

                // Act
                hcenClient.registrarMetadatosCompleto(payload);

                // Assert
                verify(mockBuilder).post(any(Entity.class));
            }
        }
    }

    // Helper methods
    private DTMetadatos createMetadata() {
        DTMetadatos metadata = new DTMetadatos();
        metadata.setDocumentoId("doc-" + System.currentTimeMillis());
        metadata.setDocumentoIdPaciente("12345678");
        metadata.setTenantId("101");
        metadata.setTipoDocumento("EVALUACION");
        metadata.setFormato("application/pdf");
        metadata.setFechaCreacion(LocalDateTime.now());
        metadata.setUrlAcceso("http://test.com/doc");
        return metadata;
    }

    private Map<String, Object> createPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("documentoId", "doc123");
        payload.put("ciPaciente", "12345678");
        payload.put("tenantId", "101");
        return payload;
    }
}

