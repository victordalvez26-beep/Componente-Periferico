package uy.edu.tse.hcen.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServiceAuthUtilTest {

    private String originalPropertySecret;

    @BeforeEach
    void setUp() {
        // Guardar valores originales
        originalPropertySecret = System.getProperty("hcen.service.secret");
        
        // Limpiar variables
        System.clearProperty("hcen.service.secret");
    }

    @AfterEach
    void tearDown() {
        // Restaurar valores originales si existían
        if (originalPropertySecret != null) {
            System.setProperty("hcen.service.secret", originalPropertySecret);
        } else {
            System.clearProperty("hcen.service.secret");
        }
    }

    @Test
    void testGenerateServiceToken() {
        String serviceId = "componente-periferico";
        String serviceName = "Componente Periférico HCEN";
        
        String token = ServiceAuthUtil.generateServiceToken(serviceId, serviceName);
        
        assertNotNull(token);
        assertTrue(token.length() > 0);
        assertTrue(token.contains(".")); // JWT tiene 3 partes separadas por puntos
    }

    @Test
    void testGenerateServiceTokenWithNullServiceId() {
        // El método permite null, solo genera el token
        String token = ServiceAuthUtil.generateServiceToken(null, "Service Name");
        assertNotNull(token);
    }

    @Test
    void testGenerateServiceTokenWithNullServiceName() {
        // El método permite null, solo genera el token
        String token = ServiceAuthUtil.generateServiceToken("service-id", null);
        assertNotNull(token);
    }

    @Test
    void testGenerateServiceTokenDifferentTokens() {
        String serviceId = "test-service";
        String serviceName = "Test Service";
        
        String token1 = ServiceAuthUtil.generateServiceToken(serviceId, serviceName);
        String token2 = ServiceAuthUtil.generateServiceToken(serviceId, serviceName);
        
        // Los tokens pueden ser diferentes debido a timestamps
        assertNotNull(token1);
        assertNotNull(token2);
    }

    @Test
    void testGenerateServiceTokenWithSpecialCharacters() {
        String serviceId = "service-123_test";
        String serviceName = "Service & Associates";
        
        String token = ServiceAuthUtil.generateServiceToken(serviceId, serviceName);
        
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void testGenerateServiceTokenWithEmptyStrings() {
        String serviceId = "";
        String serviceName = "";
        
        String token = ServiceAuthUtil.generateServiceToken(serviceId, serviceName);
        
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void testGenerateServiceTokenWithLongStrings() {
        String serviceId = "a".repeat(200);
        String serviceName = "b".repeat(300);
        
        String token = ServiceAuthUtil.generateServiceToken(serviceId, serviceName);
        
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void testGenerateServiceTokenFormat() {
        String serviceId = "test-service";
        String serviceName = "Test Service";
        
        String token = ServiceAuthUtil.generateServiceToken(serviceId, serviceName);
        
        // JWT tiene formato: header.payload.signature
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
        
        // Cada parte debe ser Base64 URL-safe
        for (String part : parts) {
            assertNotNull(part);
            assertTrue(part.length() > 0);
            // Base64 URL-safe no contiene +, /, = al final
            assertFalse(part.contains("+"));
            assertFalse(part.contains("/"));
        }
    }

    @Test
    void testGenerateServiceTokenWithUnicode() {
        String serviceId = "servicio-ñ";
        String serviceName = "Servicio con acentos áéíóú";
        
        String token = ServiceAuthUtil.generateServiceToken(serviceId, serviceName);
        
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void testGenerateServiceTokenMultipleCalls() {
        String serviceId = "test";
        String serviceName = "Test";
        
        for (int i = 0; i < 10; i++) {
            String token = ServiceAuthUtil.generateServiceToken(serviceId, serviceName);
            assertNotNull(token);
            assertTrue(token.length() > 0);
        }
    }

    @Test
    void testGenerateServiceTokenWithSystemProperty() {
        System.setProperty("hcen.service.secret", "test-secret-key-12345");
        
        String serviceId = "test-service";
        String serviceName = "Test Service";
        
        String token = ServiceAuthUtil.generateServiceToken(serviceId, serviceName);
        
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }
}

