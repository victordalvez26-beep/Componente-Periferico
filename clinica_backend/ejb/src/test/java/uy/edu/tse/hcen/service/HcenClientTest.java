package uy.edu.tse.hcen.service;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uy.edu.tse.hcen.dto.DTMetadatos;
import uy.edu.tse.hcen.exceptions.HcenUnavailableException;
import uy.edu.tse.hcen.utils.ServiceAuthUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HcenClientTest {

    @InjectMocks
    private HcenClient hcenClient;

    @BeforeEach
    void setUp() {
        // Limpiar variables de entorno para tests
        System.clearProperty("HCEN_CENTRAL_URL");
        System.clearProperty("HCEN_SERVICE_AUTH_URL");
        System.clearProperty("hcen.service.secret");
    }

    @Test
    void testRegistrarMetadatos() throws HcenUnavailableException {
        DTMetadatos dto = new DTMetadatos();
        dto.setDocumentoId("doc-123");
        dto.setDocumentoIdPaciente("12345678");

        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class);
             MockedStatic<ServiceAuthUtil> serviceAuthMock = mockStatic(ServiceAuthUtil.class)) {

            Client client = mock(Client.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);
            Response response = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);
            serviceAuthMock.when(() -> ServiceAuthUtil.generateServiceToken(anyString(), anyString()))
                    .thenReturn("service-token");

            when(client.target(anyString())).thenReturn(mock(jakarta.ws.rs.client.WebTarget.class));
            when(client.target(anyString()).request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.header(anyString(), anyString())).thenReturn(builder);
            when(builder.post(any(Entity.class))).thenReturn(response);
            when(response.getStatus()).thenReturn(200);
            when(response.hasEntity()).thenReturn(false);

            // Test que no lance excepción
            assertDoesNotThrow(() -> hcenClient.registrarMetadatos(dto));
        }
    }

    @Test
    void testRegistrarMetadatosError() {
        DTMetadatos dto = new DTMetadatos();
        dto.setDocumentoId("doc-123");

        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class);
             MockedStatic<ServiceAuthUtil> serviceAuthMock = mockStatic(ServiceAuthUtil.class)) {

            Client client = mock(Client.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);
            Response response = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);
            serviceAuthMock.when(() -> ServiceAuthUtil.generateServiceToken(anyString(), anyString()))
                    .thenReturn("service-token");

            when(client.target(anyString())).thenReturn(mock(jakarta.ws.rs.client.WebTarget.class));
            when(client.target(anyString()).request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.header(anyString(), anyString())).thenReturn(builder);
            when(builder.post(any(Entity.class))).thenReturn(response);
            when(response.getStatus()).thenReturn(500);
            when(response.hasEntity()).thenReturn(true);
            when(response.readEntity(String.class)).thenReturn("Error del servidor");

            assertThrows(HcenUnavailableException.class, () -> {
                hcenClient.registrarMetadatos(dto);
            });
        }
    }

    @Test
    void testRegistrarMetadatosCompleto() throws HcenUnavailableException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("documentoId", "doc-123");
        payload.put("ciPaciente", "12345678");

        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class);
             MockedStatic<ServiceAuthUtil> serviceAuthMock = mockStatic(ServiceAuthUtil.class)) {

            Client client = mock(Client.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);
            Response response = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);
            serviceAuthMock.when(() -> ServiceAuthUtil.generateServiceToken(anyString(), anyString()))
                    .thenReturn("service-token");

            when(client.target(anyString())).thenReturn(mock(jakarta.ws.rs.client.WebTarget.class));
            when(client.target(anyString()).request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.header(anyString(), anyString())).thenReturn(builder);
            when(builder.post(any(Entity.class))).thenReturn(response);
            when(response.getStatus()).thenReturn(201);
            when(response.hasEntity()).thenReturn(false);

            assertDoesNotThrow(() -> hcenClient.registrarMetadatosCompleto(payload));
        }
    }

    @Test
    void testObtenerMetadatosDocumentosPorCI() throws HcenUnavailableException {
        String ciPaciente = "12345678";
        String profesionalId = "prof-1";
        String tenantId = "1";
        String especialidad = "MEDICINA_GENERAL";
        String nombreProfesional = "Dr. Test";

        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class);
             MockedStatic<ServiceAuthUtil> serviceAuthMock = mockStatic(ServiceAuthUtil.class)) {

            Client client = mock(Client.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);
            Response response = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);
            serviceAuthMock.when(() -> ServiceAuthUtil.generateServiceToken(anyString(), anyString()))
                    .thenReturn("service-token");

            List<Map<String, Object>> metadatos = new ArrayList<>();
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("id", "doc-1");
            metadata.put("documentoId", "doc-uuid-1");
            metadatos.add(metadata);

            when(client.target(anyString())).thenReturn(mock(jakarta.ws.rs.client.WebTarget.class));
            when(client.target(anyString()).request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.header(anyString(), anyString())).thenReturn(builder);
            when(builder.get()).thenReturn(response);
            when(response.getStatus()).thenReturn(200);
            when(response.readEntity(List.class)).thenReturn(metadatos);

            List<Map<String, Object>> result = hcenClient.obtenerMetadatosDocumentosPorCI(
                    ciPaciente, profesionalId, tenantId, especialidad, nombreProfesional);

            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    @Test
    void testObtenerMetadatosDocumentosPorCIError() {
        String ciPaciente = "12345678";

        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class);
             MockedStatic<ServiceAuthUtil> serviceAuthMock = mockStatic(ServiceAuthUtil.class)) {

            Client client = mock(Client.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);
            Response response = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);
            serviceAuthMock.when(() -> ServiceAuthUtil.generateServiceToken(anyString(), anyString()))
                    .thenReturn("service-token");

            when(client.target(anyString())).thenReturn(mock(jakarta.ws.rs.client.WebTarget.class));
            when(client.target(anyString()).request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.header(anyString(), anyString())).thenReturn(builder);
            when(builder.get()).thenReturn(response);
            when(response.getStatus()).thenReturn(500);
            when(response.hasEntity()).thenReturn(true);
            when(response.readEntity(String.class)).thenReturn("Error");

            assertThrows(HcenUnavailableException.class, () -> {
                hcenClient.obtenerMetadatosDocumentosPorCI(ciPaciente, null, null, null, null);
            });
        }
    }

    @Test
    void testConsultarMetadatosPaciente() throws HcenUnavailableException {
        String documentoIdPaciente = "12345678";

        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            Response response = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);

            List<Map<String, Object>> metadatos = new ArrayList<>();

            when(client.target(anyString())).thenReturn(mock(jakarta.ws.rs.client.WebTarget.class));
            when(client.target(anyString()).request(MediaType.APPLICATION_JSON)).thenReturn(mock(Invocation.Builder.class));
            when(client.target(anyString()).request(MediaType.APPLICATION_JSON).get()).thenReturn(response);
            when(response.getStatus()).thenReturn(200);
            when(response.readEntity(List.class)).thenReturn(metadatos);

            List<Map<String, Object>> result = hcenClient.consultarMetadatosPaciente(documentoIdPaciente);

            assertNotNull(result);
        }
    }

    @Test
    void testRegistrarAccesoHistoriaClinica() {
        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);
            Response response = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);

            when(client.target(anyString())).thenReturn(mock(jakarta.ws.rs.client.WebTarget.class));
            when(client.target(anyString()).request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.post(any(Entity.class))).thenReturn(response);
            when(response.getStatus()).thenReturn(201);

            // No debe lanzar excepción aunque falle (es asíncrono)
            assertDoesNotThrow(() -> {
                hcenClient.registrarAccesoHistoriaClinica(
                        "prof-1", "Dr. Test", "MEDICINA_GENERAL", "1", "12345678", "doc-1", "EVALUACION", true);
            });
        }
    }

    @Test
    void testRegistrarMetadatosWithTokenRejection() throws HcenUnavailableException {
        DTMetadatos dto = new DTMetadatos();
        dto.setDocumentoId("doc-123");

        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class);
             MockedStatic<ServiceAuthUtil> serviceAuthMock = mockStatic(ServiceAuthUtil.class)) {

            Client client = mock(Client.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);
            Response response401 = mock(Response.class);
            Response response200 = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);
            serviceAuthMock.when(() -> ServiceAuthUtil.generateServiceToken(anyString(), anyString()))
                    .thenReturn("service-token");

            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            when(client.target(anyString())).thenReturn(target);
            when(target.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.header(anyString(), anyString())).thenReturn(builder);
            when(builder.post(any(Entity.class))).thenReturn(response401, response200);
            when(response401.getStatus()).thenReturn(401);
            when(response200.getStatus()).thenReturn(200);
            when(response200.hasEntity()).thenReturn(false);

            assertDoesNotThrow(() -> hcenClient.registrarMetadatos(dto));
        }
    }

    @Test
    void testRegistrarMetadatosWithProcessingException() {
        DTMetadatos dto = new DTMetadatos();
        dto.setDocumentoId("doc-123");

        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class);
             MockedStatic<ServiceAuthUtil> serviceAuthMock = mockStatic(ServiceAuthUtil.class)) {

            clientBuilderMock.when(ClientBuilder::newClient).thenThrow(new jakarta.ws.rs.ProcessingException("Connection error"));
            serviceAuthMock.when(() -> ServiceAuthUtil.generateServiceToken(anyString(), anyString()))
                    .thenReturn("service-token");

            assertThrows(HcenUnavailableException.class, () -> {
                hcenClient.registrarMetadatos(dto);
            });
        }
    }

    @Test
    void testRegistrarMetadatosCompletoWithTokenRejection() throws HcenUnavailableException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("documentoId", "doc-123");

        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class);
             MockedStatic<ServiceAuthUtil> serviceAuthMock = mockStatic(ServiceAuthUtil.class)) {

            Client client = mock(Client.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);
            Response response403 = mock(Response.class);
            Response response201 = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);
            serviceAuthMock.when(() -> ServiceAuthUtil.generateServiceToken(anyString(), anyString()))
                    .thenReturn("service-token");

            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            when(client.target(anyString())).thenReturn(target);
            when(target.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.header(anyString(), anyString())).thenReturn(builder);
            when(builder.post(any(Entity.class))).thenReturn(response403, response201);
            when(response403.getStatus()).thenReturn(403);
            when(response201.getStatus()).thenReturn(201);
            when(response201.hasEntity()).thenReturn(false);

            assertDoesNotThrow(() -> hcenClient.registrarMetadatosCompleto(payload));
        }
    }

    @Test
    void testObtenerMetadatosDocumentosPorCIWithQueryParams() throws HcenUnavailableException {
        String ciPaciente = "12345678";
        String profesionalId = "prof-1";
        String tenantId = "1";
        String especialidad = "CARDIOLOGIA";
        String nombreProfesional = "Dr. Test";

        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class);
             MockedStatic<ServiceAuthUtil> serviceAuthMock = mockStatic(ServiceAuthUtil.class)) {

            Client client = mock(Client.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);
            Response response = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);
            serviceAuthMock.when(() -> ServiceAuthUtil.generateServiceToken(anyString(), anyString()))
                    .thenReturn("service-token");

            List<Map<String, Object>> metadatos = new ArrayList<>();

            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            when(client.target(anyString())).thenReturn(target);
            when(target.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.header(anyString(), anyString())).thenReturn(builder);
            when(builder.get()).thenReturn(response);
            when(response.getStatus()).thenReturn(200);
            when(response.readEntity(List.class)).thenReturn(metadatos);

            List<Map<String, Object>> result = hcenClient.obtenerMetadatosDocumentosPorCI(
                    ciPaciente, profesionalId, tenantId, especialidad, nombreProfesional);

            assertNotNull(result);
        }
    }

    @Test
    void testConsultarMetadatosPacienteWith404() throws HcenUnavailableException {
        String documentoIdPaciente = "12345678";

        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            Response response = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);

            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);
            when(client.target(anyString())).thenReturn(target);
            when(target.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.get()).thenReturn(response);
            when(response.getStatus()).thenReturn(404);

            List<Map<String, Object>> result = hcenClient.consultarMetadatosPaciente(documentoIdPaciente);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    void testConsultarMetadatosPacienteWithError() {
        String documentoIdPaciente = "12345678";

        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            Response response = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);

            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);
            when(client.target(anyString())).thenReturn(target);
            when(target.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.get()).thenReturn(response);
            when(response.getStatus()).thenReturn(500);

            assertThrows(HcenUnavailableException.class, () -> {
                hcenClient.consultarMetadatosPaciente(documentoIdPaciente);
            });
        }
    }

    @Test
    void testRegistrarAccesoHistoriaClinicaWithException() {
        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class)) {
            clientBuilderMock.when(ClientBuilder::newClient).thenThrow(new RuntimeException("Connection error"));

            // No debe lanzar excepción aunque falle (es asíncrono)
            assertDoesNotThrow(() -> {
                hcenClient.registrarAccesoHistoriaClinica(
                        "prof-1", "Dr. Test", "MEDICINA_GENERAL", "1", "12345678", "doc-1", "EVALUACION", true);
            });
        }
    }

    @Test
    void testRegistrarAccesoHistoriaClinicaWithNullTipoDocumento() {
        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);
            Response response = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);

            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            when(client.target(anyString())).thenReturn(target);
            when(target.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.post(any(Entity.class))).thenReturn(response);
            when(response.getStatus()).thenReturn(201);

            assertDoesNotThrow(() -> {
                hcenClient.registrarAccesoHistoriaClinica(
                        "prof-1", "Dr. Test", "MEDICINA_GENERAL", "1", "12345678", "doc-1", null, true);
            });
        }
    }

    @Test
    void testRegistrarAccesoHistoriaClinicaWithFailure() {
        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);
            Response response = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);

            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            when(client.target(anyString())).thenReturn(target);
            when(target.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.post(any(Entity.class))).thenReturn(response);
            when(response.getStatus()).thenReturn(500);
            when(response.hasEntity()).thenReturn(true);
            when(response.readEntity(String.class)).thenReturn("Error");

            assertDoesNotThrow(() -> {
                hcenClient.registrarAccesoHistoriaClinica(
                        "prof-1", "Dr. Test", "MEDICINA_GENERAL", "1", "12345678", "doc-1", "EVALUACION", false);
            });
        }
    }

    @Test
    void testGetServiceTokenWithCache() {
        try (MockedStatic<ServiceAuthUtil> serviceAuthMock = mockStatic(ServiceAuthUtil.class)) {
            serviceAuthMock.when(() -> ServiceAuthUtil.generateServiceToken(anyString(), anyString()))
                    .thenReturn("cached-token");
            
            String token1 = hcenClient.getServiceToken();
            String token2 = hcenClient.getServiceToken();
            
            // El segundo llamado debería usar el cache
            assertEquals(token1, token2);
            serviceAuthMock.verify(() -> ServiceAuthUtil.generateServiceToken(anyString(), anyString()), times(1));
        }
    }

    @Test
    void testGetServiceTokenDirecto() {
        try (MockedStatic<ServiceAuthUtil> serviceAuthMock = mockStatic(ServiceAuthUtil.class)) {
            serviceAuthMock.when(() -> ServiceAuthUtil.generateServiceToken(anyString(), anyString()))
                    .thenReturn("test-token");
            
            String token = hcenClient.getServiceToken();
            
            assertNotNull(token);
            assertEquals("test-token", token);
        }
    }

    @Test
    void testGetServiceTokenWithException() {
        try (MockedStatic<ServiceAuthUtil> serviceAuthMock = mockStatic(ServiceAuthUtil.class)) {
            serviceAuthMock.when(() -> ServiceAuthUtil.generateServiceToken(anyString(), anyString()))
                    .thenThrow(new RuntimeException("Token generation failed"));
            
            System.clearProperty("hcen.service.secret");
            
            String token = hcenClient.getServiceToken();
            
            // Debe retornar null si no hay secret configurado
            assertNull(token);
        }
    }

    @Test
    void testHandleTokenRejectionDirecto() throws HcenUnavailableException {
        DTMetadatos dto = new DTMetadatos();
        dto.setDocumentoId("doc-123");
        
        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class);
             MockedStatic<ServiceAuthUtil> serviceAuthMock = mockStatic(ServiceAuthUtil.class)) {
            
            Client client = mock(Client.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);
            Response response = mock(Response.class);
            
            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);
            serviceAuthMock.when(() -> ServiceAuthUtil.generateServiceToken(anyString(), anyString()))
                    .thenReturn("new-token");
            
            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            when(client.target(anyString())).thenReturn(target);
            when(target.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.header(anyString(), anyString())).thenReturn(builder);
            when(builder.post(any(Entity.class))).thenReturn(response);
            when(response.getStatus()).thenReturn(200);
            when(response.hasEntity()).thenReturn(false);
            
            hcenClient.handleTokenRejection(client, "http://test.com", dto);
            
            verify(builder).post(any(Entity.class));
        }
    }

    @Test
    void testHandleTokenRejectionWithNullToken() throws HcenUnavailableException {
        DTMetadatos dto = new DTMetadatos();
        dto.setDocumentoId("doc-123");
        
        try (MockedStatic<ServiceAuthUtil> serviceAuthMock = mockStatic(ServiceAuthUtil.class)) {
            serviceAuthMock.when(() -> ServiceAuthUtil.generateServiceToken(anyString(), anyString()))
                    .thenReturn(null);
            
            Client client = mock(Client.class);
            
            // No debe lanzar excepción si el token es null
            assertDoesNotThrow(() -> {
                hcenClient.handleTokenRejection(client, "http://test.com", dto);
            });
        }
    }

    @Test
    void testHandleTokenRejectionWithErrorStatus() {
        DTMetadatos dto = new DTMetadatos();
        dto.setDocumentoId("doc-123");
        
        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class);
             MockedStatic<ServiceAuthUtil> serviceAuthMock = mockStatic(ServiceAuthUtil.class)) {
            
            Client client = mock(Client.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);
            Response response = mock(Response.class);
            
            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);
            serviceAuthMock.when(() -> ServiceAuthUtil.generateServiceToken(anyString(), anyString()))
                    .thenReturn("new-token");
            
            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            when(client.target(anyString())).thenReturn(target);
            when(target.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.header(anyString(), anyString())).thenReturn(builder);
            when(builder.post(any(Entity.class))).thenReturn(response);
            when(response.getStatus()).thenReturn(500);
            when(response.hasEntity()).thenReturn(true);
            when(response.readEntity(String.class)).thenReturn("Error");
            
            assertThrows(HcenUnavailableException.class, () -> {
                hcenClient.handleTokenRejection(client, "http://test.com", dto);
            });
        }
    }

    @Test
    void testGetServiceTokenWithEndpointFallback() {
        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class);
             MockedStatic<ServiceAuthUtil> serviceAuthMock = mockStatic(ServiceAuthUtil.class)) {
            
            serviceAuthMock.when(() -> ServiceAuthUtil.generateServiceToken(anyString(), anyString()))
                    .thenThrow(new RuntimeException("Token generation failed"));
            
            System.setProperty("hcen.service.secret", "test-secret");
            
            try {
                Client client = mock(Client.class);
                Invocation.Builder builder = mock(Invocation.Builder.class);
                Response response = mock(Response.class);
                
                clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);
                
                jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
                when(client.target(anyString())).thenReturn(target);
                when(target.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
                when(builder.post(any(Entity.class))).thenReturn(response);
                when(response.getStatus()).thenReturn(200);
                
                Map<String, Object> authResponse = new HashMap<>();
                authResponse.put("token", "endpoint-token");
                when(response.readEntity(Map.class)).thenReturn(authResponse);
                
                String token = hcenClient.getServiceToken();
                
                assertNotNull(token);
                assertEquals("endpoint-token", token);
            } finally {
                System.clearProperty("hcen.service.secret");
            }
        }
    }

    @Test
    void testGetServiceTokenWithEndpointFallbackNoSecret() {
        try (MockedStatic<ServiceAuthUtil> serviceAuthMock = mockStatic(ServiceAuthUtil.class)) {
            serviceAuthMock.when(() -> ServiceAuthUtil.generateServiceToken(anyString(), anyString()))
                    .thenThrow(new RuntimeException("Token generation failed"));
            
            // No configurar secret
            System.clearProperty("hcen.service.secret");
            
            String token = hcenClient.getServiceToken();
            
            // Debe retornar null si no hay secret
            assertNull(token);
        }
    }

    @Test
    void testGetServiceTokenWithEndpointFallbackError() {
        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class);
             MockedStatic<ServiceAuthUtil> serviceAuthMock = mockStatic(ServiceAuthUtil.class)) {
            
            serviceAuthMock.when(() -> ServiceAuthUtil.generateServiceToken(anyString(), anyString()))
                    .thenThrow(new RuntimeException("Token generation failed"));
            
            System.setProperty("hcen.service.secret", "test-secret");
            
            try {
                clientBuilderMock.when(ClientBuilder::newClient).thenThrow(new jakarta.ws.rs.ProcessingException("Connection error"));
                
                String token = hcenClient.getServiceToken();
                
                // Debe retornar null si falla el endpoint
                assertNull(token);
            } finally {
                System.clearProperty("hcen.service.secret");
            }
        }
    }

    @Test
    void testGetServiceTokenWithEndpointFallbackNon200() {
        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class);
             MockedStatic<ServiceAuthUtil> serviceAuthMock = mockStatic(ServiceAuthUtil.class)) {
            
            serviceAuthMock.when(() -> ServiceAuthUtil.generateServiceToken(anyString(), anyString()))
                    .thenThrow(new RuntimeException("Token generation failed"));
            
            System.setProperty("hcen.service.secret", "test-secret");
            
            try {
                Client client = mock(Client.class);
                Invocation.Builder builder = mock(Invocation.Builder.class);
                Response response = mock(Response.class);
                
                clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);
                
                jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
                when(client.target(anyString())).thenReturn(target);
                when(target.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
                when(builder.post(any(Entity.class))).thenReturn(response);
                when(response.getStatus()).thenReturn(500);
                
                String token = hcenClient.getServiceToken();
                
                // Debe retornar null si el endpoint retorna error
                assertNull(token);
            } finally {
                System.clearProperty("hcen.service.secret");
            }
        }
    }

    @Test
    void testRegistrarMetadatosWith202Status() throws HcenUnavailableException {
        DTMetadatos dto = new DTMetadatos();
        dto.setDocumentoId("doc-123");

        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class);
             MockedStatic<ServiceAuthUtil> serviceAuthMock = mockStatic(ServiceAuthUtil.class)) {

            Client client = mock(Client.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);
            Response response = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);
            serviceAuthMock.when(() -> ServiceAuthUtil.generateServiceToken(anyString(), anyString()))
                    .thenReturn("service-token");

            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            when(client.target(anyString())).thenReturn(target);
            when(target.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.header(anyString(), anyString())).thenReturn(builder);
            when(builder.post(any(Entity.class))).thenReturn(response);
            when(response.getStatus()).thenReturn(202);
            when(response.hasEntity()).thenReturn(false);

            assertDoesNotThrow(() -> hcenClient.registrarMetadatos(dto));
        }
    }

    @Test
    void testRegistrarMetadatosCompletoWith202Status() throws HcenUnavailableException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("documentoId", "doc-123");

        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class);
             MockedStatic<ServiceAuthUtil> serviceAuthMock = mockStatic(ServiceAuthUtil.class)) {

            Client client = mock(Client.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);
            Response response = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);
            serviceAuthMock.when(() -> ServiceAuthUtil.generateServiceToken(anyString(), anyString()))
                    .thenReturn("service-token");

            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            when(client.target(anyString())).thenReturn(target);
            when(target.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.header(anyString(), anyString())).thenReturn(builder);
            when(builder.post(any(Entity.class))).thenReturn(response);
            when(response.getStatus()).thenReturn(202);
            when(response.hasEntity()).thenReturn(false);

            assertDoesNotThrow(() -> hcenClient.registrarMetadatosCompleto(payload));
        }
    }

    @Test
    void testRegistrarMetadatosCompletoWithProcessingException() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("documentoId", "doc-123");

        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class);
             MockedStatic<ServiceAuthUtil> serviceAuthMock = mockStatic(ServiceAuthUtil.class)) {

            clientBuilderMock.when(ClientBuilder::newClient).thenThrow(new jakarta.ws.rs.ProcessingException("Connection error"));
            serviceAuthMock.when(() -> ServiceAuthUtil.generateServiceToken(anyString(), anyString()))
                    .thenReturn("service-token");

            assertThrows(HcenUnavailableException.class, () -> {
                hcenClient.registrarMetadatosCompleto(payload);
            });
        }
    }

    @Test
    void testRegistrarMetadatosCompletoWithErrorStatus() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("documentoId", "doc-123");

        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class);
             MockedStatic<ServiceAuthUtil> serviceAuthMock = mockStatic(ServiceAuthUtil.class)) {

            Client client = mock(Client.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);
            Response response = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);
            serviceAuthMock.when(() -> ServiceAuthUtil.generateServiceToken(anyString(), anyString()))
                    .thenReturn("service-token");

            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            when(client.target(anyString())).thenReturn(target);
            when(target.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.header(anyString(), anyString())).thenReturn(builder);
            when(builder.post(any(Entity.class))).thenReturn(response);
            when(response.getStatus()).thenReturn(500);
            when(response.hasEntity()).thenReturn(true);
            when(response.readEntity(String.class)).thenReturn("Error del servidor");

            assertThrows(HcenUnavailableException.class, () -> {
                hcenClient.registrarMetadatosCompleto(payload);
            });
        }
    }

    @Test
    void testRegistrarMetadatosWithNullDto() {
        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class);
             MockedStatic<ServiceAuthUtil> serviceAuthMock = mockStatic(ServiceAuthUtil.class)) {

            Client client = mock(Client.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);
            Response response = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);
            serviceAuthMock.when(() -> ServiceAuthUtil.generateServiceToken(anyString(), anyString()))
                    .thenReturn("service-token");

            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            when(client.target(anyString())).thenReturn(target);
            when(target.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.header(anyString(), anyString())).thenReturn(builder);
            when(builder.post(any(Entity.class))).thenReturn(response);
            when(response.getStatus()).thenReturn(200);
            when(response.hasEntity()).thenReturn(false);

            assertDoesNotThrow(() -> hcenClient.registrarMetadatos(null));
        }
    }

    @Test
    void testHandleTokenRejectionWithRetryFailure() throws HcenUnavailableException {
        DTMetadatos dto = new DTMetadatos();
        dto.setDocumentoId("doc-123");

        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class);
             MockedStatic<ServiceAuthUtil> serviceAuthMock = mockStatic(ServiceAuthUtil.class)) {

            Client client = mock(Client.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);
            Response response401 = mock(Response.class);
            Response response500 = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);
            serviceAuthMock.when(() -> ServiceAuthUtil.generateServiceToken(anyString(), anyString()))
                    .thenReturn("service-token", "new-token");

            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            when(client.target(anyString())).thenReturn(target);
            when(target.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.header(anyString(), anyString())).thenReturn(builder);
            when(builder.post(any(Entity.class))).thenReturn(response401, response500);
            when(response401.getStatus()).thenReturn(401);
            when(response500.getStatus()).thenReturn(500);
            when(response500.hasEntity()).thenReturn(true);
            when(response500.readEntity(String.class)).thenReturn("Error");

            assertThrows(HcenUnavailableException.class, () -> {
                hcenClient.registrarMetadatos(dto);
            });
        }
    }

    @Test
    void testHandleTokenRejectionWithNullNewToken() throws HcenUnavailableException {
        DTMetadatos dto = new DTMetadatos();
        dto.setDocumentoId("doc-123");

        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class);
             MockedStatic<ServiceAuthUtil> serviceAuthMock = mockStatic(ServiceAuthUtil.class)) {

            Client client = mock(Client.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);
            Response response401 = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);
            serviceAuthMock.when(() -> ServiceAuthUtil.generateServiceToken(anyString(), anyString()))
                    .thenReturn("service-token", null); // Segundo token es null

            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            when(client.target(anyString())).thenReturn(target);
            when(target.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.header(anyString(), anyString())).thenReturn(builder);
            when(builder.post(any(Entity.class))).thenReturn(response401);
            when(response401.getStatus()).thenReturn(401);

            // Debe lanzar excepción si no puede obtener nuevo token
            assertThrows(HcenUnavailableException.class, () -> {
                hcenClient.registrarMetadatos(dto);
            });
        }
    }

    @Test
    void testObtenerMetadatosDocumentosPorCIWithAllParams() throws HcenUnavailableException {
        String ciPaciente = "12345678";
        String profesionalId = "prof-1";
        String tenantId = "1";
        String especialidad = "CARDIOLOGIA";
        String nombreProfesional = "Dr. Test";

        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class);
             MockedStatic<ServiceAuthUtil> serviceAuthMock = mockStatic(ServiceAuthUtil.class)) {

            Client client = mock(Client.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);
            Response response = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);
            serviceAuthMock.when(() -> ServiceAuthUtil.generateServiceToken(anyString(), anyString()))
                    .thenReturn("service-token");

            List<Map<String, Object>> metadatos = new ArrayList<>();

            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            when(client.target(contains(ciPaciente))).thenReturn(target);
            when(target.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.header(anyString(), anyString())).thenReturn(builder);
            when(builder.get()).thenReturn(response);
            when(response.getStatus()).thenReturn(200);
            when(response.readEntity(List.class)).thenReturn(metadatos);

            List<Map<String, Object>> result = hcenClient.obtenerMetadatosDocumentosPorCI(
                    ciPaciente, profesionalId, tenantId, especialidad, nombreProfesional);

            assertNotNull(result);
        }
    }

    @Test
    void testObtenerMetadatosDocumentosPorCIWithNullParams() throws HcenUnavailableException {
        String ciPaciente = "12345678";

        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class);
             MockedStatic<ServiceAuthUtil> serviceAuthMock = mockStatic(ServiceAuthUtil.class)) {

            Client client = mock(Client.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);
            Response response = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);
            serviceAuthMock.when(() -> ServiceAuthUtil.generateServiceToken(anyString(), anyString()))
                    .thenReturn("service-token");

            List<Map<String, Object>> metadatos = new ArrayList<>();

            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            when(client.target(contains(ciPaciente))).thenReturn(target);
            when(target.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.header(anyString(), anyString())).thenReturn(builder);
            when(builder.get()).thenReturn(response);
            when(response.getStatus()).thenReturn(200);
            when(response.readEntity(List.class)).thenReturn(metadatos);

            List<Map<String, Object>> result = hcenClient.obtenerMetadatosDocumentosPorCI(
                    ciPaciente, null, null, null, null);

            assertNotNull(result);
        }
    }

    @Test
    void testObtenerMetadatosDocumentosPorCIWithProcessingException() {
        String ciPaciente = "12345678";

        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class);
             MockedStatic<ServiceAuthUtil> serviceAuthMock = mockStatic(ServiceAuthUtil.class)) {

            clientBuilderMock.when(ClientBuilder::newClient).thenThrow(new jakarta.ws.rs.ProcessingException("Connection error"));
            serviceAuthMock.when(() -> ServiceAuthUtil.generateServiceToken(anyString(), anyString()))
                    .thenReturn("service-token");

            assertThrows(HcenUnavailableException.class, () -> {
                hcenClient.obtenerMetadatosDocumentosPorCI(ciPaciente, null, null, null, null);
            });
        }
    }

    @Test
    void testConsultarMetadatosPacienteWithProcessingException() {
        String documentoIdPaciente = "12345678";

        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class)) {
            clientBuilderMock.when(ClientBuilder::newClient).thenThrow(new jakarta.ws.rs.ProcessingException("Connection error"));

            assertThrows(HcenUnavailableException.class, () -> {
                hcenClient.consultarMetadatosPaciente(documentoIdPaciente);
            });
        }
    }

    @Test
    void testConsultarMetadatosPacienteWithErrorStatus() {
        String documentoIdPaciente = "12345678";

        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            Response response = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);

            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);
            when(client.target(anyString())).thenReturn(target);
            when(target.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.get()).thenReturn(response);
            when(response.getStatus()).thenReturn(500);

            assertThrows(HcenUnavailableException.class, () -> {
                hcenClient.consultarMetadatosPaciente(documentoIdPaciente);
            });
        }
    }

    @Test
    void testRegistrarAccesoHistoriaClinicaWithNullParams() {
        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);
            Response response = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);

            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            when(client.target(anyString())).thenReturn(target);
            when(target.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.post(any(Entity.class))).thenReturn(response);
            when(response.getStatus()).thenReturn(201);

            assertDoesNotThrow(() -> {
                hcenClient.registrarAccesoHistoriaClinica(
                        null, null, null, null, "12345678", null, null, false);
            });
        }
    }

    @Test
    void testRegistrarAccesoHistoriaClinicaWithErrorStatus() {
        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);
            Response response = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);

            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            when(client.target(anyString())).thenReturn(target);
            when(target.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.post(any(Entity.class))).thenReturn(response);
            when(response.getStatus()).thenReturn(500);
            when(response.hasEntity()).thenReturn(true);
            when(response.readEntity(String.class)).thenReturn("Error");

            // No debe lanzar excepción (es asíncrono)
            assertDoesNotThrow(() -> {
                hcenClient.registrarAccesoHistoriaClinica(
                        "prof-1", "Dr. Test", "MEDICINA_GENERAL", "1", "12345678", "doc-1", "EVALUACION", true);
            });
        }
    }

    @Test
    void testRegistrarAccesoHistoriaClinicaWithBlankTipoDocumento() {
        try (MockedStatic<ClientBuilder> clientBuilderMock = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            Invocation.Builder builder = mock(Invocation.Builder.class);
            Response response = mock(Response.class);

            clientBuilderMock.when(ClientBuilder::newClient).thenReturn(client);

            jakarta.ws.rs.client.WebTarget target = mock(jakarta.ws.rs.client.WebTarget.class);
            when(client.target(anyString())).thenReturn(target);
            when(target.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
            when(builder.post(any(Entity.class))).thenReturn(response);
            when(response.getStatus()).thenReturn(201);

            assertDoesNotThrow(() -> {
                hcenClient.registrarAccesoHistoriaClinica(
                        "prof-1", "Dr. Test", "MEDICINA_GENERAL", "1", "12345678", "doc-1", "", true);
            });
        }
    }
}

