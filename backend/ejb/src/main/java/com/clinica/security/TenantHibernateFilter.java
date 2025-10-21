package com.clinica.security;

import com.clinica.multitenant.TenantContext;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * Habilita un filtro de Hibernate por request para aplicar aislamiento por tenant.
 * Requiere que el proveedor JPA sea Hibernate y que las entidades tengan @Filter definidas.
 */
@Provider
@Priority(Priorities.AUTHENTICATION + 10)
public class TenantHibernateFilter implements ContainerRequestFilter {

    @Inject
    private TenantContext tenantContext;

    @PersistenceContext(unitName = "nodoPerifericoPersistenceUnit")
    private EntityManager em;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null || tenantId.isEmpty()) {
            return; // No tenant set; no filter applied
        }

        try {
            // Unwrap to Hibernate Session if available
            org.hibernate.Session session = em.unwrap(org.hibernate.Session.class);
            if (session != null) {
                org.hibernate.Filter filter = session.enableFilter("tenantFilter");
                filter.setParameter("tenantId", tenantId);
            }
        } catch (Exception e) {
            // Could not enable filter (e.g., not using Hibernate). Ignore silently or log.
            // Logger omitted to keep example simple.
        }
    }
}
