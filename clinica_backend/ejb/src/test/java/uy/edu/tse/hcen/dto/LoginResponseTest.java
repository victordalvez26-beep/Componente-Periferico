package uy.edu.tse.hcen.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginResponseTest {

    private LoginResponse response;

    @BeforeEach
    void setUp() {
        response = new LoginResponse();
    }

    @Test
    void testConstructor() {
        LoginResponse response = new LoginResponse();
        assertNotNull(response);
        assertNull(response.getToken());
        assertNull(response.getRole());
        assertNull(response.getTenant_id());
    }

    @Test
    void testConstructorWithTokenAndRole() {
        String token = "jwt-token-123";
        String role = "PROFESIONAL";
        
        LoginResponse response = new LoginResponse(token, role);
        
        assertEquals(token, response.getToken());
        assertEquals(role, response.getRole());
        assertNull(response.getTenant_id());
    }

    @Test
    void testConstructorWithAllParameters() {
        String token = "jwt-token-123";
        String role = "ADMINISTRADOR";
        String tenantId = "tenant-123";
        
        LoginResponse response = new LoginResponse(token, role, tenantId);
        
        assertEquals(token, response.getToken());
        assertEquals(role, response.getRole());
        assertEquals(tenantId, response.getTenant_id());
    }

    @Test
    void testToken() {
        String token = "jwt-token-123";
        response.setToken(token);
        assertEquals(token, response.getToken());
    }

    @Test
    void testTokenNull() {
        response.setToken(null);
        assertNull(response.getToken());
    }

    @Test
    void testTokenEmpty() {
        response.setToken("");
        assertEquals("", response.getToken());
    }

    @Test
    void testRole() {
        String role = "PROFESIONAL";
        response.setRole(role);
        assertEquals(role, response.getRole());
    }

    @Test
    void testRoleNull() {
        response.setRole(null);
        assertNull(response.getRole());
    }

    @Test
    void testRoleEmpty() {
        response.setRole("");
        assertEquals("", response.getRole());
    }

    @Test
    void testTenantId() {
        String tenantId = "tenant-123";
        response.setTenant_id(tenantId);
        assertEquals(tenantId, response.getTenant_id());
    }

    @Test
    void testTenantIdNull() {
        response.setTenant_id(null);
        assertNull(response.getTenant_id());
    }

    @Test
    void testTenantIdEmpty() {
        response.setTenant_id("");
        assertEquals("", response.getTenant_id());
    }

    @Test
    void testAllFields() {
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
        String role = "ADMINISTRADOR";
        String tenantId = "tenant-456";
        
        response.setToken(token);
        response.setRole(role);
        response.setTenant_id(tenantId);
        
        assertEquals(token, response.getToken());
        assertEquals(role, response.getRole());
        assertEquals(tenantId, response.getTenant_id());
    }

    @Test
    void testDifferentRoles() {
        String[] roles = {"PROFESIONAL", "ADMINISTRADOR", "USUARIO"};
        
        for (String role : roles) {
            response.setRole(role);
            assertEquals(role, response.getRole());
        }
    }

    @Test
    void testLongToken() {
        String longToken = "a".repeat(1000);
        response.setToken(longToken);
        assertEquals(longToken, response.getToken());
    }

    @Test
    void testNumericTenantId() {
        String tenantId = "123";
        response.setTenant_id(tenantId);
        assertEquals(tenantId, response.getTenant_id());
    }

    @Test
    void testConstructorWithNullValues() {
        LoginResponse response = new LoginResponse(null, null);
        
        assertNull(response.getToken());
        assertNull(response.getRole());
    }

    @Test
    void testConstructorWithNullTenantId() {
        LoginResponse response = new LoginResponse("token", "role", null);
        
        assertEquals("token", response.getToken());
        assertEquals("role", response.getRole());
        assertNull(response.getTenant_id());
    }

    @Test
    void testJwtTokenFormat() {
        String jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        response.setToken(jwtToken);
        assertEquals(jwtToken, response.getToken());
    }
}

