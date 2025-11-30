package uy.edu.tse.hcen.multitenancy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MultiTenantResolverTest {

    private MultiTenantResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new SchemaTenantResolver();
        TenantContext.clear();
    }

    @Test
    void testSetTenantIdentifier() {
        String tenantId = "123";
        resolver.setTenantIdentifier(tenantId);
        
        assertEquals(tenantId, TenantContext.getCurrentTenant());
    }

    @Test
    void testValidateExistingCurrentSessions() {
        assertFalse(resolver.validateExistingCurrentSessions());
    }
}

