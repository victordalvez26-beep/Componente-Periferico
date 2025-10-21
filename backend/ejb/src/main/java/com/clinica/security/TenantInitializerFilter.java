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

/**
 * Filtro que se ejecuta ANTES de que se procese la solicitud.
 * Extrae el tenantId del JWT y lo establece en el TenantContext.
 */
@Provider
@Priority(Priorities.AUTHENTICATION) // Se ejecuta después de la autenticación JWT
public class TenantInitializerFilter implements ContainerRequestFilter {

    @Inject
    private Instance<JsonWebToken> jwtInstance; // Optional MP-JWT token provider

    @Inject
    private TenantContext tenantContext;

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
            // Ignorar problemas al leer el JWT en entornos de prueba
        }

        if (tenantId != null && !tenantId.isEmpty()) {
            tenantContext.setCurrentTenantId(tenantId);
            System.out.println("Tenant ID establecido: " + tenantId);
        } else {
            // Fallback para pruebas locales: establecer un tenant por defecto
            String fallback = "test-tenant";
            tenantContext.setCurrentTenantId(fallback);
            System.out.println("Tenant ID fallback establecido: " + fallback);
        }
    }
}
