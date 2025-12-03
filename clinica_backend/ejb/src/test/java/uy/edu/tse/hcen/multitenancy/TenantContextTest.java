package uy.edu.tse.hcen.multitenancy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextTest {

    @BeforeEach
    void setUp() {
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @ParameterizedTest
    @CsvSource(value = {
        "tenant-123",
        "NULL",
        "''",
        "tenant-123_abc",
        "123"
    }, nullValues = "NULL")
    void testSetCurrentTenantParameterized(String tenantId) {
        TenantContext.setCurrentTenant(tenantId);
        assertEquals(tenantId, TenantContext.getCurrentTenant());
    }

    @Test
    void testGetCurrentTenantWhenNotSet() {
        assertNull(TenantContext.getCurrentTenant());
    }

    @Test
    void testClear() {
        TenantContext.setCurrentTenant("tenant-123");
        assertNotNull(TenantContext.getCurrentTenant());
        
        TenantContext.clear();
        assertNull(TenantContext.getCurrentTenant());
    }

    @Test
    void testMultipleSetAndClear() {
        TenantContext.setCurrentTenant("tenant-1");
        assertEquals("tenant-1", TenantContext.getCurrentTenant());
        
        TenantContext.setCurrentTenant("tenant-2");
        assertEquals("tenant-2", TenantContext.getCurrentTenant());
        
        TenantContext.clear();
        assertNull(TenantContext.getCurrentTenant());
        
        TenantContext.setCurrentTenant("tenant-3");
        assertEquals("tenant-3", TenantContext.getCurrentTenant());
    }

    @Test
    void testThreadLocalIsolation() throws InterruptedException {
        TenantContext.setCurrentTenant("tenant-main");
        
        Thread thread = new Thread(() -> {
            TenantContext.setCurrentTenant("tenant-thread");
            assertEquals("tenant-thread", TenantContext.getCurrentTenant());
        });
        
        thread.start();
        thread.join();
        
        // El thread principal debe mantener su valor
        assertEquals("tenant-main", TenantContext.getCurrentTenant());
    }



    @Test
    void testLongTenantId() {
        String tenantId = "a".repeat(200);
        TenantContext.setCurrentTenant(tenantId);
        assertEquals(tenantId, TenantContext.getCurrentTenant());
    }
}

