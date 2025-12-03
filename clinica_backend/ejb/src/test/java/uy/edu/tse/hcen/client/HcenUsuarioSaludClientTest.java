package uy.edu.tse.hcen.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uy.edu.tse.hcen.model.UsuarioSalud;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HcenUsuarioSaludClientTest {

    @InjectMocks
    private HcenUsuarioSaludClient client;

    private UsuarioSalud usuario;

    @BeforeEach
    void setUp() {
        usuario = new UsuarioSalud();
        usuario.setCi("12345678");
        usuario.setNombre("Juan");
        usuario.setApellido("Pérez");
        usuario.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        usuario.setDireccion("Av. 18 de Julio 1234");
        usuario.setTelefono("099123456");
        usuario.setEmail("juan@example.com");
        usuario.setDepartamento("MONTEVIDEO");
        usuario.setLocalidad("Centro");
    }

    @Test
    void testRegistrarUsuarioEnHcenSuccess() throws Exception {
        try (MockedStatic<HttpClient> httpClientMock = mockStatic(HttpClient.class);
             MockedStatic<HttpRequest> httpRequestMock = mockStatic(HttpRequest.class)) {
            
            HttpClient.Builder mockBuilder = mock(HttpClient.Builder.class);
            HttpClient mockHttpClient = mock(HttpClient.class);
            HttpRequest.Builder mockRequestBuilder = mock(HttpRequest.Builder.class);
            HttpRequest mockRequest = mock(HttpRequest.class);
            @SuppressWarnings("unchecked")
            HttpResponse<Object> mockResponse = mock(HttpResponse.class);

            httpClientMock.when(HttpClient::newBuilder).thenReturn(mockBuilder);
            when(mockBuilder.connectTimeout(any())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockHttpClient);

            httpRequestMock.when(HttpRequest::newBuilder).thenReturn(mockRequestBuilder);
            when(mockRequestBuilder.uri(any())).thenReturn(mockRequestBuilder);
            when(mockRequestBuilder.header(anyString(), anyString())).thenReturn(mockRequestBuilder);
            when(mockRequestBuilder.timeout(any())).thenReturn(mockRequestBuilder);
            when(mockRequestBuilder.POST(any())).thenReturn(mockRequestBuilder);
            when(mockRequestBuilder.build()).thenReturn(mockRequest);

            when(mockHttpClient.send(any(HttpRequest.class), any())).thenReturn((HttpResponse<Object>) mockResponse);
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn("{\"userId\":123,\"mensaje\":\"Usuario registrado\"}");

            HcenUsuarioSaludClient.HcenUserResponse result = client.registrarUsuarioEnHcen(101L, usuario);

            assertNotNull(result);
            assertEquals(123L, result.getUserId());
            assertEquals("Usuario registrado", result.getMensaje());
        }
    }

    @Test
    void testRegistrarUsuarioEnHcenWithNon200Status() throws Exception {
        try (MockedStatic<HttpClient> httpClientMock = mockStatic(HttpClient.class);
             MockedStatic<HttpRequest> httpRequestMock = mockStatic(HttpRequest.class)) {
            
            HttpClient.Builder mockBuilder = mock(HttpClient.Builder.class);
            HttpClient mockHttpClient = mock(HttpClient.class);
            HttpRequest.Builder mockRequestBuilder = mock(HttpRequest.Builder.class);
            HttpRequest mockRequest = mock(HttpRequest.class);
            @SuppressWarnings("unchecked")
            HttpResponse<Object> mockResponse = mock(HttpResponse.class);

            httpClientMock.when(HttpClient::newBuilder).thenReturn(mockBuilder);
            when(mockBuilder.connectTimeout(any())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockHttpClient);

            httpRequestMock.when(HttpRequest::newBuilder).thenReturn(mockRequestBuilder);
            when(mockRequestBuilder.uri(any())).thenReturn(mockRequestBuilder);
            when(mockRequestBuilder.header(anyString(), anyString())).thenReturn(mockRequestBuilder);
            when(mockRequestBuilder.timeout(any())).thenReturn(mockRequestBuilder);
            when(mockRequestBuilder.POST(any())).thenReturn(mockRequestBuilder);
            when(mockRequestBuilder.build()).thenReturn(mockRequest);

            when(mockHttpClient.send(any(HttpRequest.class), any())).thenReturn((HttpResponse<Object>) mockResponse);
            when(mockResponse.statusCode()).thenReturn(500);
            when(mockResponse.body()).thenReturn("Internal Server Error");

            HcenUsuarioSaludClient.HcenUserResponse result = client.registrarUsuarioEnHcen(101L, usuario);

            assertNotNull(result);
            assertNull(result.getUserId());
            assertTrue(result.getMensaje().contains("Error HTTP 500"));
        }
    }

    @Test
    void testRegistrarUsuarioEnHcenWithException() throws Exception {
        try (MockedStatic<HttpClient> httpClientMock = mockStatic(HttpClient.class);
             MockedStatic<HttpRequest> httpRequestMock = mockStatic(HttpRequest.class)) {
            
            HttpClient.Builder mockBuilder = mock(HttpClient.Builder.class);
            HttpClient mockHttpClient = mock(HttpClient.class);
            HttpRequest.Builder mockRequestBuilder = mock(HttpRequest.Builder.class);
            HttpRequest mockRequest = mock(HttpRequest.class);

            httpClientMock.when(HttpClient::newBuilder).thenReturn(mockBuilder);
            when(mockBuilder.connectTimeout(any())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockHttpClient);

            httpRequestMock.when(HttpRequest::newBuilder).thenReturn(mockRequestBuilder);
            when(mockRequestBuilder.uri(any())).thenReturn(mockRequestBuilder);
            when(mockRequestBuilder.header(anyString(), anyString())).thenReturn(mockRequestBuilder);
            when(mockRequestBuilder.timeout(any())).thenReturn(mockRequestBuilder);
            when(mockRequestBuilder.POST(any())).thenReturn(mockRequestBuilder);
            when(mockRequestBuilder.build()).thenReturn(mockRequest);

            when(mockHttpClient.send(any(HttpRequest.class), any())).thenThrow(new RuntimeException("Connection error"));

            HcenUsuarioSaludClient.HcenUserResponse result = client.registrarUsuarioEnHcen(101L, usuario);

            assertNotNull(result);
            assertNull(result.getUserId());
            assertTrue(result.getMensaje().contains("Error"));
        }
    }

    @Test
    void testRegistrarUsuarioEnHcenWithNullFields() throws Exception {
        UsuarioSalud usuarioNull = new UsuarioSalud();
        usuarioNull.setCi("12345678");
        // Todos los demás campos son null

        try (MockedStatic<HttpClient> httpClientMock = mockStatic(HttpClient.class);
             MockedStatic<HttpRequest> httpRequestMock = mockStatic(HttpRequest.class)) {
            
            HttpClient.Builder mockBuilder = mock(HttpClient.Builder.class);
            HttpClient mockHttpClient = mock(HttpClient.class);
            HttpRequest.Builder mockRequestBuilder = mock(HttpRequest.Builder.class);
            HttpRequest mockRequest = mock(HttpRequest.class);
            @SuppressWarnings("unchecked")
            HttpResponse<Object> mockResponse = mock(HttpResponse.class);

            httpClientMock.when(HttpClient::newBuilder).thenReturn(mockBuilder);
            when(mockBuilder.connectTimeout(any())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockHttpClient);

            httpRequestMock.when(HttpRequest::newBuilder).thenReturn(mockRequestBuilder);
            when(mockRequestBuilder.uri(any())).thenReturn(mockRequestBuilder);
            when(mockRequestBuilder.header(anyString(), anyString())).thenReturn(mockRequestBuilder);
            when(mockRequestBuilder.timeout(any())).thenReturn(mockRequestBuilder);
            when(mockRequestBuilder.POST(any())).thenReturn(mockRequestBuilder);
            when(mockRequestBuilder.build()).thenReturn(mockRequest);

            when(mockHttpClient.send(any(HttpRequest.class), any())).thenReturn((HttpResponse<Object>) mockResponse);
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn("{\"userId\":123,\"mensaje\":\"Usuario registrado\"}");

            HcenUsuarioSaludClient.HcenUserResponse result = client.registrarUsuarioEnHcen(101L, usuarioNull);

            assertNotNull(result);
        }
    }

    @Test
    void testHcenUserResponseConstructor() {
        HcenUsuarioSaludClient.HcenUserResponse response = 
            new HcenUsuarioSaludClient.HcenUserResponse(123L, "Test message");

        assertEquals(123L, response.getUserId());
        assertEquals("Test message", response.getMensaje());
    }

    @Test
    void testHcenUserResponseToString() {
        HcenUsuarioSaludClient.HcenUserResponse response = 
            new HcenUsuarioSaludClient.HcenUserResponse(123L, "Test message");

        String toString = response.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("123"));
        assertTrue(toString.contains("Test message"));
    }

    @Test
    void testRegistrarUsuarioEnHcenWithEmptyDepartamento() throws Exception {
        usuario.setDepartamento("");

        try (MockedStatic<HttpClient> httpClientMock = mockStatic(HttpClient.class);
             MockedStatic<HttpRequest> httpRequestMock = mockStatic(HttpRequest.class)) {
            
            HttpClient.Builder mockBuilder = mock(HttpClient.Builder.class);
            HttpClient mockHttpClient = mock(HttpClient.class);
            HttpRequest.Builder mockRequestBuilder = mock(HttpRequest.Builder.class);
            HttpRequest mockRequest = mock(HttpRequest.class);
            @SuppressWarnings("unchecked")
            HttpResponse<Object> mockResponse = mock(HttpResponse.class);

            httpClientMock.when(HttpClient::newBuilder).thenReturn(mockBuilder);
            when(mockBuilder.connectTimeout(any())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockHttpClient);

            httpRequestMock.when(HttpRequest::newBuilder).thenReturn(mockRequestBuilder);
            when(mockRequestBuilder.uri(any())).thenReturn(mockRequestBuilder);
            when(mockRequestBuilder.header(anyString(), anyString())).thenReturn(mockRequestBuilder);
            when(mockRequestBuilder.timeout(any())).thenReturn(mockRequestBuilder);
            when(mockRequestBuilder.POST(any())).thenReturn(mockRequestBuilder);
            when(mockRequestBuilder.build()).thenReturn(mockRequest);

            when(mockHttpClient.send(any(HttpRequest.class), any())).thenReturn((HttpResponse<Object>) mockResponse);
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn("{\"userId\":123,\"mensaje\":\"Usuario registrado\"}");

            HcenUsuarioSaludClient.HcenUserResponse result = client.registrarUsuarioEnHcen(101L, usuario);

            assertNotNull(result);
        }
    }
}

