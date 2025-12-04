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

