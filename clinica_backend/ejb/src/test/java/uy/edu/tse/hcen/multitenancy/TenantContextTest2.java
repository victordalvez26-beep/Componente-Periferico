package uy.edu.tse.hcen.multitenancy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TenantContext Additional Tests")
class TenantContextTest2 {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void setCurrentTenant_multipleTimes_shouldOverwrite() {
        TenantContext.setCurrentTenant("101");
        TenantContext.setCurrentTenant("102");
        TenantContext.setCurrentTenant("103");
        
        assertEquals("103", TenantContext.getCurrentTenant());
    }

    @Test
    void clear_afterSet_shouldRemoveTenant() {
        TenantContext.setCurrentTenant("101");
        TenantContext.clear();
        
        assertNull(TenantContext.getCurrentTenant());
    }

    @Test
    void setCurrentTenant_withNull_shouldHandle() {
        TenantContext.setCurrentTenant(null);
        assertNull(TenantContext.getCurrentTenant());
    }

    @Test
    void setCurrentTenant_withEmpty_shouldSet() {
        TenantContext.setCurrentTenant("");
        assertEquals("", TenantContext.getCurrentTenant());
    }
}

