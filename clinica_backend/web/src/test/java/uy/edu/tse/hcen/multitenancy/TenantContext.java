package uy.edu.tse.hcen.multitenancy;

/**
 * Test shim for TenantContext to make web unit tests independent from the EJB runtime.
 * This mirrors the API of the real TenantContext used in the EJB module.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setCurrentTenant(String tenantId) {
        CURRENT.set(tenantId);
    }

    public static String getCurrentTenant() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
