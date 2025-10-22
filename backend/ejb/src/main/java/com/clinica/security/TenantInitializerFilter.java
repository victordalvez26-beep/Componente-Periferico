package com.clinica.security;

import com.clinica.multitenant.TenantContext;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.jwt.JsonWebToken;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Filtro que se ejecuta ANTES de que se procese la solicitud.
 * Extrae el tenantId del JWT y lo establece en el TenantContext.
 */
@Provider 
@Priority(Priorities.AUTHENTICATION) // Se ejecuta después de la autenticación JWT
public class TenantInitializerFilter implements ContainerRequestFilter {
    private static final Logger LOGGER = Logger.getLogger(TenantInitializerFilter.class.getName());

    // Fields are not final so a no-arg constructor can initialize the provider
    private Instance<JsonWebToken> jwtInstance; // Optional MP-JWT token provider

    private TenantContext tenantContext;

    // Public no-arg constructor required by some CDI/Resteasy instantiation flows
    public TenantInitializerFilter() {
        this.jwtInstance = null;
        this.tenantContext = null;
    }

    @Inject
    public TenantInitializerFilter(Instance<JsonWebToken> jwtInstance, TenantContext tenantContext) {
        this.jwtInstance = jwtInstance;
        this.tenantContext = tenantContext;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // Intentamos extraer la claim 'tenantId' del JWT validado
        String tenantId = null;
        try {
            if (jwtInstance != null && jwtInstance.isResolvable()) {
                JsonWebToken jwt = jwtInstance.get();
                Object claim = jwt.getClaim("tenantId");
                if (claim != null) {
                    tenantId = claim.toString();
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error extracting tenantId from JWT: " + e.getMessage());
        }
        if (tenantId != null && !tenantId.isEmpty()) {
            tenantContext.setCurrentTenantId(tenantId);
            LOGGER.log(Level.INFO, "Tenant ID establecido: {0}", tenantId);
        } else {
            // Fallback para pruebas locales: establecer un tenant por defecto
            String fallback = "test-tenant";
            tenantContext.setCurrentTenantId(fallback);
            LOGGER.log(Level.INFO, "Tenant ID fallback establecido: {0}", fallback);
        }
    }
}
