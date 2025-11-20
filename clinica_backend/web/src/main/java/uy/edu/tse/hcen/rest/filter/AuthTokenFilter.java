package uy.edu.tse.hcen.rest.filter;

import uy.edu.tse.hcen.utils.TokenUtils;
import uy.edu.tse.hcen.multitenancy.TenantContext;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Map;

/**
 * Filtro JAX-RS de autenticación que valida el JWT (Bearer) y establece
 * TenantContext con el claim "tenantId" para que el provider de multi-tenant
 * seleccione el esquema adecuado en llamadas posteriores.
 */
@Provider
@Priority(Priorities.AUTHENTICATION - 100) // Prioridad más alta para manejar OPTIONS antes que otros filtros
public class AuthTokenFilter implements ContainerRequestFilter, ContainerResponseFilter {

    // Orígenes permitidos para CORS (configurable mediante variable de entorno)
    // Formato: "http://localhost:3000,http://localhost:3001,https://example.com"
    // Si no se define, se permite cualquier origen (solo para desarrollo)
    private static final String ALLOWED_ORIGINS_ENV = "CORS_ALLOWED_ORIGINS";
    private static final String ALLOW_CREDENTIALS_ENV = "CORS_ALLOW_CREDENTIALS";
    
    private static final String[] DEFAULT_ALLOWED_ORIGINS = {
        "http://localhost:3000",
        "http://localhost:3001"
    };
    
    private static final boolean DEFAULT_ALLOW_CREDENTIALS = true;
    
    // Cache de orígenes permitidos (se lee una vez al inicializar)
    private static final String[] allowedOrigins = initAllowedOrigins();
    private static final boolean allowCredentials = initAllowCredentials();
    
    private static String[] initAllowedOrigins() {
        String envValue = System.getProperty(ALLOWED_ORIGINS_ENV, 
                System.getenv().getOrDefault(ALLOWED_ORIGINS_ENV, null));
        
        if (envValue != null && !envValue.trim().isEmpty()) {
            // Si está configurado, usar los valores de la variable de entorno
            return envValue.split(",");
        }
        
        // Si no está configurado, usar valores por defecto para desarrollo
        return DEFAULT_ALLOWED_ORIGINS;
    }
    
    private static boolean initAllowCredentials() {
        String envValue = System.getProperty(ALLOW_CREDENTIALS_ENV,
                System.getenv().getOrDefault(ALLOW_CREDENTIALS_ENV, null));
        
        if (envValue != null && !envValue.trim().isEmpty()) {
            return Boolean.parseBoolean(envValue);
        }
        
        return DEFAULT_ALLOW_CREDENTIALS;
    }
    
    private static boolean isOriginAllowed(String origin) {
        if (origin == null || origin.isEmpty()) {
            return false;
        }
        
        // Si no hay orígenes configurados explícitamente, permitir todos (solo desarrollo)
        if (allowedOrigins.length == 0) {
            return true;
        }
        
        // Verificar si el origen está en la lista permitida
        for (String allowed : allowedOrigins) {
            if (origin.equals(allowed.trim()) || origin.startsWith(allowed.trim())) {
                return true;
            }
        }
        
        return false;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // NO manejar OPTIONS aquí - dejar que JAX-RS lo maneje y agregar headers en el response filter
        
        // Excluir endpoints públicos que NO requieren autenticación JWT:
        // - /config/* : Llamados por HCEN central (init, update, delete, activate, health)
        // - /auth/login : Login de usuarios
        // - /api/documentos-pdf/{id} : Descarga individual de PDFs (el backend HCEN ya valida autenticación)
        // NO incluir /api/documentos-pdf/paciente/{ci} que requiere autenticación
        String path = requestContext.getUriInfo().getPath();
        if (path.startsWith("config/") || path.equals("auth/login") || 
            (path.matches("documentos-pdf/[^/]+") && "GET".equals(requestContext.getMethod()) && !path.contains("/paciente/"))) {
            // Permitir acceso sin JWT a estos endpoints públicos
            return;
        }
        
        // Para el resto de endpoints, verificar JWT si está presente
        String auth = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring("Bearer ".length()).trim();
            try {
                Claims claims = TokenUtils.parseToken(token);
                String tenantId = claims.get("tenantId", String.class);
                String role = claims.get("role", String.class);
                String subject = claims.getSubject();

                // Si el tenantId no viene en el token, intentar leerlo del header X-Tenant-Id
                if ((tenantId == null || tenantId.isBlank()) && requestContext.getHeaderString("X-Tenant-Id") != null) {
                    tenantId = requestContext.getHeaderString("X-Tenant-Id");
                }

                if (tenantId != null && !tenantId.isBlank()) {
                    TenantContext.setCurrentTenant(tenantId);
                }

                // expose auth info to request properties and SecurityContext
                requestContext.setProperty("auth.subject", subject);
                requestContext.setProperty("auth.role", role);

                final String fSubject = subject;
                final String fRole = role;

                // set a simple SecurityContext so resource methods can call isUserInRole
                requestContext.setSecurityContext(new jakarta.ws.rs.core.SecurityContext() {
                    @Override
                    public java.security.Principal getUserPrincipal() {
                        if (fSubject == null) return null;
                        return () -> fSubject;
                    }

                    @Override
                    public boolean isUserInRole(String role) {
                        if (fRole == null) return false;
                        return fRole.equals(role);
                    }

                    @Override
                    public boolean isSecure() {
                        return "https".equalsIgnoreCase(requestContext.getUriInfo().getRequestUri().getScheme());
                    }

                    @Override
                    public String getAuthenticationScheme() {
                        return "Bearer";
                    }
                });

            } catch (Exception ex) {
                // Token inválido: abortar con 401
                Map<String, String> err = Map.of("error", "Token inválido o expirado");
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .entity(err)
                        .build());
            }
        }
        // Si no hay header Authorization, dejamos pasar la request
        // (algunos endpoints pueden ser públicos, otros pueden validar manualmente)
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        // Agregar headers CORS para permitir llamadas desde el frontend React (localhost:3000 y 3001)
        String origin = requestContext.getHeaderString("Origin");
        String method = requestContext.getMethod();
        
        // Para peticiones OPTIONS (preflight), asegurar que se devuelva 200 OK y headers completos
        if ("OPTIONS".equalsIgnoreCase(method)) {
            responseContext.setStatus(Response.Status.OK.getStatusCode());
        }
        
        // Agregar headers CORS
        if (origin != null && isOriginAllowed(origin)) {
            // Origen permitido - usar el origen específico
            responseContext.getHeaders().add("Access-Control-Allow-Origin", origin);
            if (allowCredentials) {
                responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
            }
        } else if (origin != null) {
            // Origen no permitido - en producción debería rechazarse, pero para desarrollo permitimos sin credentials
            // En producción, considera no agregar el header o devolver un error
            responseContext.getHeaders().add("Access-Control-Allow-Origin", origin);
        } else {
            // Si no hay Origin header, permitir cualquier origen (solo para desarrollo)
            // En producción, esto debería estar deshabilitado
            responseContext.getHeaders().add("Access-Control-Allow-Origin", "*");
        }
        
        responseContext.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH");
        
        // Headers permitidos - incluir los que el cliente solicita en preflight
        String accessControlRequestHeaders = requestContext.getHeaderString("Access-Control-Request-Headers");
        String allowedHeaders = "Content-Type, Authorization, X-Requested-With, Accept, Origin";
        if (accessControlRequestHeaders != null && !accessControlRequestHeaders.isEmpty()) {
            allowedHeaders = accessControlRequestHeaders + ", " + allowedHeaders;
        }
        responseContext.getHeaders().add("Access-Control-Allow-Headers", allowedHeaders);
        responseContext.getHeaders().add("Access-Control-Max-Age", "3600");
        responseContext.getHeaders().add("Access-Control-Expose-Headers", "Content-Type, Authorization");
        
        // Limpiar el TenantContext al finalizar la petición para evitar fugas entre hilos
        TenantContext.clear();
    }
}
