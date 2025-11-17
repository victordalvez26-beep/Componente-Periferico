package uy.edu.tse.hcen.multitenancy;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

/**
 * Abstract base resolver. Instead of using an instance field (which would not be
 * visible to the Hibernate-instantiated resolver) we bridge using a thread-local
 * TenantContext. CDI-managed code should call setTenantIdentifier(...) which
 * will populate the thread-local, and the resolver will read from there.
 */
public abstract class MultiTenantResolver implements CurrentTenantIdentifierResolver<Object> {

    /**
     * Called by the request filter or authentication layer to set the tenant id
     * for the current request/context. This writes into the thread-local TenantContext
     * so both CDI-managed beans and the Hibernate resolver can see the same value.
     */
    public void setTenantIdentifier(String tenantIdentifier) {
        TenantContext.setCurrentTenant(tenantIdentifier);
    }


    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}
