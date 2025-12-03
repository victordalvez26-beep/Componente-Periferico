package uy.edu.tse.hcen.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextTest {

    private TenantContext context;

    @BeforeEach
    void setUp() {
        context = new TenantContext();
    }

    @Test
    void testConstructor() {
        TenantContext context = new TenantContext();
        assertNotNull(context);
        assertNull(context.getTenantId());
        assertNull(context.getNickname());
        assertNull(context.getRole());
    }

    @Test
    void testTenantId() {
        String tenantId = "tenant-123";
        context.setTenantId(tenantId);
        assertEquals(tenantId, context.getTenantId());
    }

    @Test
    void testTenantIdNull() {
        context.setTenantId(null);
        assertNull(context.getTenantId());
    }

    @Test
    void testTenantIdEmpty() {
        context.setTenantId("");
        assertEquals("", context.getTenantId());
    }

    @Test
    void testNickname() {
        String nickname = "jperez";
        context.setNickname(nickname);
        assertEquals(nickname, context.getNickname());
    }

    @Test
    void testNicknameNull() {
        context.setNickname(null);
        assertNull(context.getNickname());
    }

    @Test
    void testNicknameEmpty() {
        context.setNickname("");
        assertEquals("", context.getNickname());
    }

    @Test
    void testRole() {
        String role = "PROFESIONAL";
        context.setRole(role);
        assertEquals(role, context.getRole());
    }

    @Test
    void testRoleNull() {
        context.setRole(null);
        assertNull(context.getRole());
    }

    @Test
    void testRoleEmpty() {
        context.setRole("");
        assertEquals("", context.getRole());
    }

    @Test
    void testIsAuthenticated() {
        assertFalse(context.isAuthenticated());
        
        context.setTenantId("tenant-123");
        assertFalse(context.isAuthenticated()); // Falta nickname
        
        context.setNickname("jperez");
        assertTrue(context.isAuthenticated());
    }

    @Test
    void testIsAuthenticatedWithNullTenantId() {
        context.setNickname("jperez");
        assertFalse(context.isAuthenticated());
    }

    @Test
    void testIsAuthenticatedWithNullNickname() {
        context.setTenantId("tenant-123");
        assertFalse(context.isAuthenticated());
    }

    @Test
    void testIsAuthenticatedWithBothNull() {
        assertFalse(context.isAuthenticated());
    }

    @Test
    void testIsAuthenticatedWithEmptyStrings() {
        context.setTenantId("");
        context.setNickname("");
        // El método isAuthenticated() solo verifica si no son null, no si están vacíos
        // Por lo tanto, strings vacíos retornarán true
        assertTrue(context.isAuthenticated());
    }

    @Test
    void testAllFields() {
        String tenantId = "tenant-456";
        String nickname = "mgarcia";
        String role = "ADMINISTRADOR";
        
        context.setTenantId(tenantId);
        context.setNickname(nickname);
        context.setRole(role);
        
        assertEquals(tenantId, context.getTenantId());
        assertEquals(nickname, context.getNickname());
        assertEquals(role, context.getRole());
        assertTrue(context.isAuthenticated());
    }

    @Test
    void testDifferentRoles() {
        String[] roles = {"PROFESIONAL", "ADMINISTRADOR", "USUARIO"};
        
        for (String role : roles) {
            context.setRole(role);
            assertEquals(role, context.getRole());
        }
    }

    @Test
    void testSpecialCharacters() {
        context.setTenantId("tenant-123_abc");
        context.setNickname("user_123-test");
        context.setRole("ROLE_TEST");
        
        assertEquals("tenant-123_abc", context.getTenantId());
        assertEquals("user_123-test", context.getNickname());
        assertEquals("ROLE_TEST", context.getRole());
    }

    @Test
    void testLongStrings() {
        String longTenantId = "a".repeat(200);
        String longNickname = "b".repeat(300);
        
        context.setTenantId(longTenantId);
        context.setNickname(longNickname);
        
        assertEquals(longTenantId, context.getTenantId());
        assertEquals(longNickname, context.getNickname());
    }
}

