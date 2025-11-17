package uy.edu.tse.hcen.multitenancy;

/**
 * Thread-local holder for the current tenant identifier.
 *
 * Hibernate creates its own instance of the CurrentTenantIdentifierResolver class
 * (via reflection) so CDI-set instance fields are not visible to Hibernate. To
 * bridge that gap we use a thread-local context that both the CDI-managed
 * components and the Hibernate resolver can consult.
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
