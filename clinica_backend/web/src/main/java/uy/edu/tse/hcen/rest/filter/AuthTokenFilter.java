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

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Manejar peticiones OPTIONS (CORS preflight) - siempre permitir con headers CORS
        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            String origin = requestContext.getHeaderString("Origin");
            Response.ResponseBuilder responseBuilder = Response.status(Response.Status.OK);
            
            // Agregar headers CORS al preflight
            if (origin != null && (origin.startsWith("http://localhost:3000") || origin.startsWith("http://localhost:3001"))) {
                responseBuilder.header("Access-Control-Allow-Origin", origin);
                responseBuilder.header("Access-Control-Allow-Credentials", "true");
            } else if (origin != null) {
                responseBuilder.header("Access-Control-Allow-Origin", origin);
            } else {
                responseBuilder.header("Access-Control-Allow-Origin", "*");
            }
            responseBuilder.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH");
            responseBuilder.header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Accept, Origin");
            responseBuilder.header("Access-Control-Max-Age", "3600");
            responseBuilder.header("Access-Control-Expose-Headers", "Content-Type, Authorization");
            
            requestContext.abortWith(responseBuilder.build());
            return;
        }
        
        // Excluir endpoints públicos que NO requieren autenticación JWT:
        // - /config/* : Llamados por HCEN central (init, update, delete, activate, health)
        // - /auth/login : Login de usuarios
        // - /api/documentos-pdf/{id} SIN token: Descarga desde backend HCEN (sin token)
        // - /api/documentos-pdf/{id} CON token: Requiere autenticación (procesar el token)
        // NO incluir /api/documentos-pdf/paciente/{ci} que siempre requiere autenticación
        String path = requestContext.getUriInfo().getPath();
        String auth = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        boolean hasBearerToken = auth != null && auth.trim().startsWith("Bearer ");
        
        // Log para depuración del header Authorization
        if (auth != null && !hasBearerToken) {
            org.jboss.logging.Logger logger = org.jboss.logging.Logger.getLogger(AuthTokenFilter.class);
            logger.warnf("Header Authorization presente pero no empieza con 'Bearer '. Valor: '%s' (longitud: %d)", 
                auth, auth.length());
        }
        
        
        // Verificar si la llamada viene de la red interna de Docker (servicio hcen-backend)
        // Si viene de hcen-backend, permitir llamadas a /documentos/{id}/contenido sin token
        String forwardedFor = requestContext.getHeaderString("X-Forwarded-For");
        String realIp = requestContext.getHeaderString("X-Real-IP");
        
        // Si la llamada viene de la red Docker interna y es a /documentos/{id}/contenido, permitir
        boolean isInternalServiceCall = (forwardedFor == null && realIp == null) && 
                                         (path.contains("/documentos/") && 
                                         (path.contains("/contenido") || path.contains("/archivo")));
        
        // El path puede venir con o sin prefijo, verificar ambas variantes
        boolean isPublicEndpoint = 
            path.contains("/config/") || path.startsWith("config/") || 
            path.endsWith("/auth/login") || path.equals("auth/login") ||
            (path.matches(".*documentos-pdf/[^/]+") && "GET".equals(requestContext.getMethod()) && !path.contains("/paciente/") && !hasBearerToken) ||
            (path.matches(".*documentos/[^/]+/contenido") && "GET".equals(requestContext.getMethod()) && !hasBearerToken) ||
            (path.matches(".*documentos/[^/]+/archivo") && "GET".equals(requestContext.getMethod()) && !hasBearerToken) ||
            isInternalServiceCall;
        
        if (isPublicEndpoint) {
            // Permitir acceso sin JWT a estos endpoints públicos
            return;
        }
        
        // Para el resto de endpoints, verificar JWT - es requerido
        // (auth ya fue obtenido arriba para verificar isPublicEndpoint)
        if (!hasBearerToken) {
            // Log para depuración - listar todos los headers
            org.jboss.logging.Logger logger = org.jboss.logging.Logger.getLogger(AuthTokenFilter.class);
            logger.warnf("No se encontró token Bearer en la petición. Path: %s, Method: %s, Auth header: %s", 
                path, requestContext.getMethod(), auth != null ? "presente" : "ausente");
            
            // Listar todos los headers para depuración
            java.util.List<String> headerNames = new java.util.ArrayList<>();
            requestContext.getHeaders().keySet().forEach(headerNames::add);
            logger.warnf("Headers recibidos: %s", String.join(", ", headerNames));
        }
        
        if (hasBearerToken) {
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
                Map<String, String> err = Map.of("error", "Token inválido o expirado: " + ex.getMessage());
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .entity(err)
                        .build());
                return;
            }
        } else {
            // No hay header Authorization y el endpoint no es público: rechazar con 401
            Map<String, String> err = Map.of("error", "Autenticación requerida. Por favor inicie sesión.");
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(err)
                    .build());
        }
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
