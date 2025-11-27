package uy.edu.tse.hcen.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.tse.hcen.dto.LoginRequest;
import uy.edu.tse.hcen.dto.LoginResponse;
import uy.edu.tse.hcen.service.LoginService;

import jakarta.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthResourceTest {

    @Mock
    private LoginService loginService;

    @InjectMocks
    private AuthResource authResource;

    private LoginRequest loginRequest;
    private LoginResponse loginResponse;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest();
        loginRequest.setNickname("testuser");
        loginRequest.setPassword("password123");
        loginRequest.setTenantId("101");

        loginResponse = new LoginResponse();
        loginResponse.setToken("jwt-token-123");
        loginResponse.setRole("PROFESIONAL");
        loginResponse.setTenant_id("101");
    }

    @Test
    void testLoginSuccess() throws SecurityException {
        // Arrange
        when(loginService.authenticateAndGenerateToken(
            eq("testuser"), eq("password123"), eq("101")
        )).thenReturn(loginResponse);

        // Act
        Response response = authResource.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());
        verify(loginService).authenticateAndGenerateToken("testuser", "password123", "101");
    }

    @Test
    void testLoginWithInvalidCredentials() throws SecurityException {
        // Arrange
        when(loginService.authenticateAndGenerateToken(
            anyString(), anyString(), anyString()
        )).thenThrow(new SecurityException("Credenciales inválidas"));

        // Act
        Response response = authResource.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());
    }

    @Test
    void testLoginWithNullRequest() throws SecurityException {
        // Arrange
        when(loginService.authenticateAndGenerateToken(
            isNull(), isNull(), isNull()
        )).thenThrow(new SecurityException("Credenciales inválidas"));

        // Act
        Response response = authResource.login(null);

        // Assert
        assertNotNull(response);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    void testLoginWithNullNickname() throws SecurityException {
        // Arrange
        loginRequest.setNickname(null);
        when(loginService.authenticateAndGenerateToken(
            isNull(), anyString(), anyString()
        )).thenThrow(new SecurityException("Credenciales inválidas"));

        // Act
        Response response = authResource.login(loginRequest);

        // Assert
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    void testLogout() {
        // Act
        Response response = authResource.logout();

        // Assert
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());
    }

    @Test
    void testLoginWithEmptyPassword() throws SecurityException {
        // Arrange
        loginRequest.setPassword("");
        when(loginService.authenticateAndGenerateToken(
            anyString(), eq(""), anyString()
        )).thenThrow(new SecurityException("Credenciales inválidas"));

        // Act
        Response response = authResource.login(loginRequest);

        // Assert
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    void testLoginWithNullTenantId() throws SecurityException {
        // Arrange
        loginRequest.setTenantId(null);
        when(loginService.authenticateAndGenerateToken(
            anyString(), anyString(), isNull()
        )).thenReturn(loginResponse);

        // Act
        Response response = authResource.login(loginRequest);

        // Assert
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }
}

