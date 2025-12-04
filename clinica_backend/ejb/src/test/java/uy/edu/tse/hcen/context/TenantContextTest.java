package uy.edu.tse.hcen.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TenantContextTest {

    private TenantContext tenantContext;
    
    @BeforeEach
    void setUp() {
        tenantContext = new TenantContext();
    }
    
    @Test
    void testSetAndGetTenantId() {
        tenantContext.setTenantId("10");
        assertEquals("10", tenantContext.getTenantId());
    }
    
    @Test
    void testSetAndGetNickname() {
        tenantContext.setNickname("testuser");
        assertEquals("testuser", tenantContext.getNickname());
    }
    
    @Test
    void testSetAndGetRole() {
        tenantContext.setRole("ADMINISTRADOR");
        assertEquals("ADMINISTRADOR", tenantContext.getRole());
    }
    
    @Test
    void testIsAuthenticated() {
        assertFalse(tenantContext.isAuthenticated());
        
        tenantContext.setTenantId("15");
        tenantContext.setNickname("user");
        
        assertTrue(tenantContext.isAuthenticated());
    }
    
    @Test
    void testIsAuthenticatedWithOnlyTenantId() {
        tenantContext.setTenantId("20");
        assertFalse(tenantContext.isAuthenticated());
    }
    
    @Test
    void testIsAuthenticatedWithOnlyNickname() {
        tenantContext.setNickname("user");
        assertFalse(tenantContext.isAuthenticated());
    }
    
    @Test
    void testDefaultValues() {
        assertNull(tenantContext.getTenantId());
        assertNull(tenantContext.getNickname());
        assertNull(tenantContext.getRole());
    }
}

