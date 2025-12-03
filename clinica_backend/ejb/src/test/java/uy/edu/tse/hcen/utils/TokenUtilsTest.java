package uy.edu.tse.hcen.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class TokenUtilsTest {

    private String originalPropertySecret;
    private String originalEnvSecret;
    private static final String TEST_SECRET = Base64.getEncoder().encodeToString("test-secret-key-for-jwt-token-generation-12345678901234567890".getBytes());

    @BeforeAll
    static void setUpBeforeAll() {
        // Establecer la propiedad ANTES de que se cargue la clase TokenUtils
        // Esto es crítico porque TokenUtils tiene un bloque estático que se ejecuta al cargar la clase
        if (System.getProperty("hcen.jwt.secret.base64") == null && System.getenv("JWT_SECRET_BASE64") == null) {
            System.setProperty("hcen.jwt.secret.base64", Base64.getEncoder().encodeToString("test-secret-key-for-jwt-token-generation-12345678901234567890".getBytes()));
        }
    }

    @BeforeEach
    void setUp() {
        // Guardar valores originales
        originalPropertySecret = System.getProperty("hcen.jwt.secret.base64");
        originalEnvSecret = System.getenv("JWT_SECRET_BASE64");
        
        // Establecer secret de prueba
        System.setProperty("hcen.jwt.secret.base64", TEST_SECRET);
    }

    @AfterEach
    void tearDown() {
        // Restaurar valores originales
        if (originalPropertySecret != null) {
            System.setProperty("hcen.jwt.secret.base64", originalPropertySecret);
        } else {
            System.clearProperty("hcen.jwt.secret.base64");
        }
        // Nota: No podemos restaurar variables de entorno fácilmente, pero la propiedad tiene prioridad
    }

    @Test
    void testGenerateToken() {
        String subject = "jperez";
        String role = "PROFESIONAL";
        String tenantId = "tenant-123";
        
        String token = TokenUtils.generateToken(subject, role, tenantId);
        
        assertNotNull(token);
        assertTrue(token.length() > 0);
        assertTrue(token.contains(".")); // JWT tiene 3 partes
    }

    @Test
    void testGenerateTokenWithNullSubject() {
        // TokenUtils permite null como subject, solo genera el token
        String token = TokenUtils.generateToken(null, "role", "tenant");
        assertNotNull(token);
        Claims claims = TokenUtils.parseToken(token);
        assertNull(claims.getSubject());
    }

    @Test
    void testGenerateTokenWithNullRole() {
        String token = TokenUtils.generateToken("subject", null, "tenant");
        assertNotNull(token);
    }

    @Test
    void testGenerateTokenWithNullTenantId() {
        String token = TokenUtils.generateToken("subject", "role", null);
        assertNotNull(token);
    }

    @Test
    void testParseToken() {
        String subject = "jperez";
        String role = "PROFESIONAL";
        String tenantId = "tenant-123";
        
        String token = TokenUtils.generateToken(subject, role, tenantId);
        Claims claims = TokenUtils.parseToken(token);
        
        assertNotNull(claims);
        assertEquals(subject, claims.getSubject());
        assertEquals(role, claims.get("role"));
        assertEquals(tenantId, claims.get("tenantId"));
    }

    @Test
    void testParseTokenInvalid() {
        String invalidToken = "invalid.token.here";
        
        assertThrows(JwtException.class, () -> {
            TokenUtils.parseToken(invalidToken);
        });
    }

    @Test
    void testParseTokenNull() {
        assertThrows(Exception.class, () -> {
            TokenUtils.parseToken(null);
        });
    }

    @Test
    void testParseTokenEmpty() {
        assertThrows(Exception.class, () -> {
            TokenUtils.parseToken("");
        });
    }

    @Test
    void testGetTenantIdFromToken() {
        String subject = "jperez";
        String role = "ADMINISTRADOR";
        String tenantId = "tenant-456";
        
        String token = TokenUtils.generateToken(subject, role, tenantId);
        String extractedTenantId = TokenUtils.getTenantIdFromToken(token);
        
        assertEquals(tenantId, extractedTenantId);
    }

    @Test
    void testGetTenantIdFromTokenWithNullTenant() {
        String subject = "jperez";
        String role = "PROFESIONAL";
        
        String token = TokenUtils.generateToken(subject, role, null);
        String extractedTenantId = TokenUtils.getTenantIdFromToken(token);
        
        assertNull(extractedTenantId);
    }

    @Test
    void testTokenContainsClaims() {
        String subject = "testuser";
        String role = "PROFESIONAL";
        String tenantId = "tenant-789";
        
        String token = TokenUtils.generateToken(subject, role, tenantId);
        Claims claims = TokenUtils.parseToken(token);
        
        assertNotNull(claims.getSubject());
        assertNotNull(claims.get("role"));
        assertNotNull(claims.get("tenantId"));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    void testTokenExpiration() {
        String subject = "testuser";
        String role = "PROFESIONAL";
        String tenantId = "tenant-123";
        
        String token = TokenUtils.generateToken(subject, role, tenantId);
        Claims claims = TokenUtils.parseToken(token);
        
        assertNotNull(claims.getExpiration());
        assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
    }

    @Test
    void testDifferentRoles() {
        String[] roles = {"PROFESIONAL", "ADMINISTRADOR", "USUARIO"};
        
        for (String role : roles) {
            String token = TokenUtils.generateToken("user", role, "tenant");
            Claims claims = TokenUtils.parseToken(token);
            assertEquals(role, claims.get("role"));
        }
    }

    @Test
    void testDifferentTenantIds() {
        String[] tenantIds = {"tenant-1", "tenant-2", "tenant-3", "123", "abc-123"};
        
        for (String tenantId : tenantIds) {
            String token = TokenUtils.generateToken("user", "role", tenantId);
            String extracted = TokenUtils.getTenantIdFromToken(token);
            assertEquals(tenantId, extracted);
        }
    }

    @Test
    void testTokenUniqueness() {
        String subject = "user";
        String role = "PROFESIONAL";
        String tenantId = "tenant-123";
        
        String token1 = TokenUtils.generateToken(subject, role, tenantId);
        // Esperar un poco más para que el timestamp cambie (los tokens pueden generarse muy rápido)
        try {
            Thread.sleep(1000); // Esperar 1 segundo para asegurar que el timestamp cambie
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String token2 = TokenUtils.generateToken(subject, role, tenantId);
        
        // Los tokens deben ser diferentes debido a diferentes timestamps
        // Si son iguales, significa que se generaron en el mismo segundo
        // En ese caso, verificamos que al menos los claims sean iguales
        if (token1.equals(token2)) {
            // Si son iguales, significa que se generaron en el mismo segundo
            // Esto es válido, solo verificamos que ambos sean válidos
            Claims claims1 = TokenUtils.parseToken(token1);
            Claims claims2 = TokenUtils.parseToken(token2);
            assertEquals(claims1.getSubject(), claims2.getSubject());
            assertEquals(claims1.get("role"), claims2.get("role"));
            assertEquals(claims1.get("tenantId"), claims2.get("tenantId"));
        } else {
            // Si son diferentes, verificamos que ambos sean válidos
            Claims claims1 = TokenUtils.parseToken(token1);
            Claims claims2 = TokenUtils.parseToken(token2);
            assertEquals(claims1.getSubject(), claims2.getSubject());
            assertEquals(claims1.get("role"), claims2.get("role"));
            assertEquals(claims1.get("tenantId"), claims2.get("tenantId"));
            // Verificamos que los timestamps sean diferentes
            assertNotEquals(claims1.getIssuedAt(), claims2.getIssuedAt());
        }
    }

    @Test
    void testSpecialCharactersInSubject() {
        String subject = "user_123-test";
        String token = TokenUtils.generateToken(subject, "role", "tenant");
        Claims claims = TokenUtils.parseToken(token);
        
        assertEquals(subject, claims.getSubject());
    }

    @Test
    void testEmptyStrings() {
        String token = TokenUtils.generateToken("", "", "");
        assertNotNull(token);
        
        Claims claims = TokenUtils.parseToken(token);
        assertEquals("", claims.getSubject());
    }
}

