package uy.edu.tse.hcen.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LoginResponseTest {

    @Test
    void testConstructorWithTwoParameters() {
        LoginResponse response = new LoginResponse("token123", "ADMINISTRADOR");
        
        assertEquals("token123", response.getToken());
        assertEquals("ADMINISTRADOR", response.getRole());
        assertNull(response.getTenant_id());
    }
    
    @Test
    void testConstructorWithThreeParameters() {
        LoginResponse response = new LoginResponse("token456", "PROFESIONAL", "15");
        
        assertEquals("token456", response.getToken());
        assertEquals("PROFESIONAL", response.getRole());
        assertEquals("15", response.getTenant_id());
    }
    
    @Test
    void testSettersAndGetters() {
        LoginResponse response = new LoginResponse();
        response.setToken("newtoken");
        response.setRole("ADMINISTRADOR");
        response.setTenant_id("25");
        
        assertEquals("newtoken", response.getToken());
        assertEquals("ADMINISTRADOR", response.getRole());
        assertEquals("25", response.getTenant_id());
    }
    
    @Test
    void testDefaultConstructor() {
        LoginResponse response = new LoginResponse();
        assertNull(response.getToken());
        assertNull(response.getRole());
        assertNull(response.getTenant_id());
    }
}

