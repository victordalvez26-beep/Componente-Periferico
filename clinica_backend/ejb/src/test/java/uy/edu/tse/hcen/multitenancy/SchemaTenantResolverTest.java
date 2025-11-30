package uy.edu.tse.hcen.multitenancy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SchemaTenantResolverTest {

    private SchemaTenantResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new SchemaTenantResolver();
        TenantContext.clear();
    }

    @Test
    void testResolveCurrentTenantIdentifier() {
        String tenantId = "123";
        TenantContext.setCurrentTenant(tenantId);
        
        Object result = resolver.resolveCurrentTenantIdentifier();
        
        assertEquals("schema_clinica_123", result);
    }

    @Test
    void testResolveCurrentTenantIdentifierDefault() {
        Object result = resolver.resolveCurrentTenantIdentifier();
        
        assertEquals("public", result);
    }

    @Test
    void testResolveCurrentTenantIdentifierBlank() {
        TenantContext.setCurrentTenant("");
        
        Object result = resolver.resolveCurrentTenantIdentifier();
        
        assertEquals("public", result);
    }

    @Test
    void testValidateExistingCurrentSessions() {
        assertFalse(resolver.validateExistingCurrentSessions());
    }
}

