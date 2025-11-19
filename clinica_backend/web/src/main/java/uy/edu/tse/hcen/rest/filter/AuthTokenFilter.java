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
@Priority(Priorities.AUTHENTICATION)
public class AuthTokenFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Manejar peticiones OPTIONS (CORS preflight) - siempre permitir con headers CORS
        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            String origin = requestContext.getHeaderString("Origin");
            Response.ResponseBuilder responseBuilder = Response.ok();
            
            // Agregar headers CORS al preflight
            if (origin != null && (origin.startsWith("http://localhost:3000") || origin.startsWith("http://localhost:3001"))) {
                responseBuilder.header("Access-Control-Allow-Origin", origin);
            } else {
                responseBuilder.header("Access-Control-Allow-Origin", "*");
            }
            responseBuilder.header("Access-Control-Allow-Credentials", "true");
            responseBuilder.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH");
            responseBuilder.header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Accept");
            responseBuilder.header("Access-Control-Max-Age", "3600");
            
            requestContext.abortWith(responseBuilder.build());
            return;
        }
        
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
        if (origin != null && (origin.startsWith("http://localhost:3000") || origin.startsWith("http://localhost:3001"))) {
            responseContext.getHeaders().add("Access-Control-Allow-Origin", origin);
            responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
        } else if (origin != null) {
            // Para otros orígenes, permitir pero sin credentials
            responseContext.getHeaders().add("Access-Control-Allow-Origin", origin);
        } else {
            // Si no hay Origin header, permitir cualquier origen (solo para desarrollo)
            responseContext.getHeaders().add("Access-Control-Allow-Origin", "*");
        }
        responseContext.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH");
        responseContext.getHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Accept");
        responseContext.getHeaders().add("Access-Control-Expose-Headers", "Content-Type, Authorization");
        
        // Limpiar el TenantContext al finalizar la petición para evitar fugas entre hilos
        TenantContext.clear();
    }
}
