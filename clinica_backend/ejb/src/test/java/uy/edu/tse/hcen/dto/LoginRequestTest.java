package uy.edu.tse.hcen.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginRequestTest {

    private LoginRequest request;

    @BeforeEach
    void setUp() {
        request = new LoginRequest();
    }

    @Test
    void testConstructor() {
        LoginRequest request = new LoginRequest();
        assertNotNull(request);
        assertNull(request.getNickname());
        assertNull(request.getPassword());
        assertNull(request.getTenantId());
    }

    @Test
    void testConstructorWithNicknameAndPassword() {
        String nickname = "jperez";
        String password = "password123";
        
        LoginRequest request = new LoginRequest(nickname, password);
        
        assertEquals(nickname, request.getNickname());
        assertEquals(password, request.getPassword());
        assertNull(request.getTenantId());
    }

    @Test
    void testConstructorWithAllParameters() {
        String nickname = "jperez";
        String password = "password123";
        String tenantId = "tenant-123";
        
        LoginRequest request = new LoginRequest(nickname, password, tenantId);
        
        assertEquals(nickname, request.getNickname());
        assertEquals(password, request.getPassword());
        assertEquals(tenantId, request.getTenantId());
    }

    @Test
    void testNickname() {
        String nickname = "jperez";
        request.setNickname(nickname);
        assertEquals(nickname, request.getNickname());
    }

    @Test
    void testNicknameNull() {
        request.setNickname(null);
        assertNull(request.getNickname());
    }

    @Test
    void testNicknameEmpty() {
        request.setNickname("");
        assertEquals("", request.getNickname());
    }

    @Test
    void testPassword() {
        String password = "password123";
        request.setPassword(password);
        assertEquals(password, request.getPassword());
    }

    @Test
    void testPasswordNull() {
        request.setPassword(null);
        assertNull(request.getPassword());
    }

    @Test
    void testPasswordEmpty() {
        request.setPassword("");
        assertEquals("", request.getPassword());
    }

    @Test
    void testTenantId() {
        String tenantId = "tenant-123";
        request.setTenantId(tenantId);
        assertEquals(tenantId, request.getTenantId());
    }

    @Test
    void testTenantIdNull() {
        request.setTenantId(null);
        assertNull(request.getTenantId());
    }

    @Test
    void testTenantIdEmpty() {
        request.setTenantId("");
        assertEquals("", request.getTenantId());
    }

    @Test
    void testAllFields() {
        String nickname = "mgarcia";
        String password = "securePassword123";
        String tenantId = "tenant-456";
        
        request.setNickname(nickname);
        request.setPassword(password);
        request.setTenantId(tenantId);
        
        assertEquals(nickname, request.getNickname());
        assertEquals(password, request.getPassword());
        assertEquals(tenantId, request.getTenantId());
    }

    @Test
    void testSpecialCharactersInNickname() {
        String nickname = "user_123-test";
        request.setNickname(nickname);
        assertEquals(nickname, request.getNickname());
    }

    @Test
    void testLongPassword() {
        String longPassword = "a".repeat(200);
        request.setPassword(longPassword);
        assertEquals(longPassword, request.getPassword());
    }

    @Test
    void testNumericTenantId() {
        String tenantId = "123";
        request.setTenantId(tenantId);
        assertEquals(tenantId, request.getTenantId());
    }

    @Test
    void testTenantIdWithSpecialCharacters() {
        String tenantId = "tenant-123_abc";
        request.setTenantId(tenantId);
        assertEquals(tenantId, request.getTenantId());
    }

    @Test
    void testConstructorWithNullValues() {
        LoginRequest request = new LoginRequest(null, null);
        
        assertNull(request.getNickname());
        assertNull(request.getPassword());
    }

    @Test
    void testConstructorWithNullTenantId() {
        LoginRequest request = new LoginRequest("user", "pass", null);
        
        assertEquals("user", request.getNickname());
        assertEquals("pass", request.getPassword());
        assertNull(request.getTenantId());
    }
}

