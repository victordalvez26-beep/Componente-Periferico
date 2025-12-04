package uy.edu.tse.hcen.multitenancy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TenantContext Additional Tests")
class TenantContextTest2 {

    @Test
    void setAndGetTenant_shouldWork() {
        TenantContext.setCurrentTenant("101");
        String result = TenantContext.getCurrentTenant();
        assertEquals("101", result);
        TenantContext.clear();
    }

    @Test
    void clearTenant_shouldRemove() {
        TenantContext.setCurrentTenant("102");
        TenantContext.clear();
        String result = TenantContext.getCurrentTenant();
        assertNull(result);
    }

    @Test
    void multipleCalls_shouldOverwrite() {
        TenantContext.setCurrentTenant("101");
        TenantContext.setCurrentTenant("102");
        assertEquals("102", TenantContext.getCurrentTenant());
        TenantContext.clear();
    }
}
