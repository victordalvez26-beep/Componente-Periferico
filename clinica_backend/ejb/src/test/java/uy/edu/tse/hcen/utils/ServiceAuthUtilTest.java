package uy.edu.tse.hcen.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServiceAuthUtilTest {

    @Test
    void testGenerateServiceToken() {
        String token = ServiceAuthUtil.generateServiceToken("clinica-10", "Clínica Test");
        
        assertNotNull(token);
        assertTrue(token.length() > 20);
        // JWT format: header.payload.signature
        assertEquals(3, token.split("\\.").length);
    }
    
    @Test
    void testGenerateMultipleTokensWithSameParams() {
        String token1 = ServiceAuthUtil.generateServiceToken("service1", "Test Service 1");
        String token2 = ServiceAuthUtil.generateServiceToken("service1", "Test Service 1");
        
        assertNotNull(token1);
        assertNotNull(token2);
        // Tokens may be different due to timestamp
        assertTrue(token1.length() > 20);
        assertTrue(token2.length() > 20);
    }
    
    @Test
    void testGenerateServiceTokenWithDifferentIds() {
        String token1 = ServiceAuthUtil.generateServiceToken("service-a", "Service A");
        String token2 = ServiceAuthUtil.generateServiceToken("service-b", "Service B");
        
        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
    }
    
    @Test
    void testGenerateServiceTokenWithNullValues() {
        // Token generation may succeed with null values (they'll be "null" in JSON)
        String token = ServiceAuthUtil.generateServiceToken(null, null);
        assertNotNull(token);
        assertTrue(token.length() > 20);
    }
}

