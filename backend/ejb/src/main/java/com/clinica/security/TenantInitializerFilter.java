package com.clinica.security;

import com.clinica.multitenant.TenantContext;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
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
    @Inject
    @ConfigProperty(name = "clinica.tenancy.header", defaultValue = "X-Tenant-Id")
    private String tenantHeaderName;

    @Inject
    @ConfigProperty(name = "clinica.tenancy.require", defaultValue = "false")
    private boolean requireTenant;

    // Do not inject fallback via @ConfigProperty (some environments may expose an empty value
    // which causes validation to fail at deployment). We read it at runtime via ConfigProvider.

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // Resolve tenant from JWT, header, query or configured fallback.
        // NOTE: subdomain/host-based tenant resolution has been disabled to avoid
        // issues in local and proxied environments.
        String tenantId = resolveFromJwt();
        if (isBlank(tenantId)) tenantId = resolveFromHeader(requestContext);
        if (isBlank(tenantId)) tenantId = resolveFromQuery(requestContext);
        if (isBlank(tenantId)) tenantId = resolveFromFallback();

        if (isBlank(tenantId)) {
            final String msg = "No tenantId resolved for request";
            if (requireTenant) {
                LOGGER.log(Level.WARNING, "{0}; aborting request because tenant is required", msg);
                requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).entity("Missing tenantId").build());
                return;
            } else {
                LOGGER.log(Level.FINE, "{0}; continuing without tenant set", msg);
                return;
            }
        }

        tenantContext.setCurrentTenantId(tenantId);
        LOGGER.log(Level.INFO, "Tenant ID establecido: {0}", tenantId);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String resolveFromJwt() {
        if (jwtInstance == null) return null;
        try {
            if (jwtInstance.isResolvable()) {
                JsonWebToken jwt = jwtInstance.get();
                Object claim = jwt.getClaim("tenantId");
                if (claim != null) return claim.toString();
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error extracting tenantId from JWT: {0}", e.getMessage());
        }
        return null;
    }

    private String resolveFromHeader(ContainerRequestContext ctx) {
        try {
            String hdr = ctx.getHeaderString(tenantHeaderName);
            if (isBlank(hdr)) hdr = ctx.getHeaderString("X-Clinic-Id");
            if (!isBlank(hdr)) return hdr.trim();
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error reading tenant header: {0}", e.getMessage());
        }
        return null;
    }

    private String resolveFromQuery(ContainerRequestContext ctx) {
        try {
            String q = ctx.getUriInfo().getQueryParameters().getFirst("tenantId");
            if (isBlank(q)) q = ctx.getUriInfo().getQueryParameters().getFirst("clinicId");
            if (!isBlank(q)) return q.trim();
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error reading tenant from query params: {0}", e.getMessage());
        }
        return null;
    }

    // Host/subdomain-based tenant resolution intentionally removed.

    private String resolveFromFallback() {
        try {
            Config cfg = ConfigProvider.getConfig();
            java.util.Optional<String> opt = cfg.getOptionalValue("clinica.tenancy.fallback", String.class);
            if (opt.isPresent() && !isBlank(opt.get())) return opt.get().trim();
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error reading configured fallback: {0}", e.getMessage());
        }
        String env = System.getenv("CLINICA_TENANT_FALLBACK");
        if (!isBlank(env)) return env.trim();
        String prop = System.getProperty("clinica.tenant.fallback");
        if (!isBlank(prop)) return prop.trim();
        return null;
    }

}
