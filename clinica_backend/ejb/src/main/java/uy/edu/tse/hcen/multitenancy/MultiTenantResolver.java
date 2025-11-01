package uy.edu.tse.hcen.multitenancy;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

/**
 * Abstract base resolver that stores the current tenant identifier.
 * The concrete resolver must implement resolveCurrentTenantIdentifier().
 */
public abstract class MultiTenantResolver implements CurrentTenantIdentifierResolver {

    protected String tenantIdentifier;

    /**
     * Called by the request filter or authentication layer to set the tenant id
     * for the current request/context.
     */
    public void setTenantIdentifier(String tenantIdentifier) {
        this.tenantIdentifier = tenantIdentifier;
    }

    @Override
    public abstract String resolveCurrentTenantIdentifier();

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}
