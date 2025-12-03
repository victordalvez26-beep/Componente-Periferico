package uy.edu.tse.hcen.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LoginRequestTest {

    @Test
    void testConstructorAndGetters() {
        LoginRequest request = new LoginRequest();
        request.setNickname("testuser");
        request.setPassword("testpass123");
        request.setTenantId("10");
        
        assertEquals("testuser", request.getNickname());
        assertEquals("testpass123", request.getPassword());
        assertEquals("10", request.getTenantId());
    }
    
    @Test
    void testParameterizedConstructorTwoParams() {
        LoginRequest request = new LoginRequest("user1", "pass1");
        assertEquals("user1", request.getNickname());
        assertEquals("pass1", request.getPassword());
        assertNull(request.getTenantId());
    }
    
    @Test
    void testParameterizedConstructorThreeParams() {
        LoginRequest request = new LoginRequest("user2", "pass2", "15");
        assertEquals("user2", request.getNickname());
        assertEquals("pass2", request.getPassword());
        assertEquals("15", request.getTenantId());
    }
    
    @Test
    void testDefaultConstructor() {
        LoginRequest request = new LoginRequest();
        assertNull(request.getNickname());
        assertNull(request.getPassword());
        assertNull(request.getTenantId());
    }
    
    @Test
    void testWithEmptyStrings() {
        LoginRequest request = new LoginRequest();
        request.setNickname("");
        request.setPassword("");
        request.setTenantId("");
        
        assertEquals("", request.getNickname());
        assertEquals("", request.getPassword());
        assertEquals("", request.getTenantId());
    }
}

