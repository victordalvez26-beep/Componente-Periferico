package uy.edu.tse.hcen.utils;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TokenUtilsTest {

    @BeforeAll
    static void setUp() {
        // Set JWT secret for tests
        System.setProperty("hcen.jwt.secret.base64", "bXlzdXBlcnNlY3JldGtleWZvcmhjZW5qd3R0b2tlbnNzaG91bGRiZWxvbmdlcg==");
    }

    @Test
    void testGenerateToken() {
        String token = TokenUtils.generateToken("testuser", "ADMINISTRADOR", "10");
        
        assertNotNull(token);
        assertTrue(token.length() > 50); // JWT tokens are long
        assertEquals(3, token.split("\\.").length); // header.payload.signature
    }
    
    @Test
    void testParseTokenAndExtractClaims() {
        String username = "testuser";
        String tenantId = "15";
        String rol = "PROFESIONAL";
        
        String token = TokenUtils.generateToken(username, rol, tenantId);
        Claims claims = TokenUtils.parseToken(token);
        
        assertNotNull(claims);
        assertEquals(username, claims.getSubject());
        assertEquals(tenantId, claims.get("tenantId", String.class));
        assertEquals(rol, claims.get("role", String.class));
    }
    
    @Test
    void testParseInvalidToken() {
        String invalidToken = "invalid.token.here";
        assertThrows(Exception.class, () -> TokenUtils.parseToken(invalidToken));
    }
    
    @Test
    void testGetTenantIdFromToken() {
        String tenantId = "25";
        String token = TokenUtils.generateToken("user", "PROFESIONAL", tenantId);
        
        String extractedTenantId = TokenUtils.getTenantIdFromToken(token);
        assertEquals(tenantId, extractedTenantId);
    }
    
    @Test
    void testGenerateTokenWithNullValues() {
        String token = TokenUtils.generateToken(null, null, null);
        assertNotNull(token);
        
        Claims claims = TokenUtils.parseToken(token);
        assertNull(claims.getSubject());
    }
}

