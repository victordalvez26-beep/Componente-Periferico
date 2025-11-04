package uy.edu.tse.hcen.security;

import uy.edu.tse.hcen.multitenancy.SchemaTenantResolver;
import uy.edu.tse.hcen.utils.TokenUtils;
import uy.edu.tse.hcen.context.TenantContext; // Clase de utilidad para almacenar el ID
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import java.security.Principal;
import java.io.IOException;

// 1. Marca la clase como un proveedor de servicios JAX-RS
@Provider
// 2. Ejecuta el filtro antes que cualquier recurso (casi al inicio del pipeline)
@Priority(jakarta.ws.rs.Priorities.AUTHENTICATION) 
public class TenantAuthFilter implements ContainerRequestFilter {

    private static final String AUTH_SCHEME = "Bearer";

    // Inyección de dependencias CDI (usar field injection para que RESTEasy pueda instanciar el provider)
    @Inject
    private SchemaTenantResolver tenantResolver;

    @Inject
    private TenantContext tenantContext;

    // Public no-arg constructor required for some container instantiation paths
    public TenantAuthFilter() {
    }
    
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        
        String path = requestContext.getUriInfo().getPath();
        
        // Permitir acceso SIN token a endpoints públicos:
        // - /auth/login : Login de usuarios
        // - /config/* : Endpoints llamados por HCEN central (init, update, delete, activate, health)
        if (path.contains("/auth/login") || path.contains("config/") || path.equals("config/health")) {
            return; 
        }

        // 1. Obtener el encabezado de autorización
        String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith(AUTH_SCHEME + " ")) {
            // No hay token o el formato es incorrecto
            abortRequest(requestContext, "Token de autorización requerido.");
            return;
        }

        // Extraer el token (eliminar "Bearer ")
        String token = authorizationHeader.substring(AUTH_SCHEME.length()).trim();

        try {
            // 2. Verificar y obtener el Tenant ID usando TokenUtils
            String tenantId = TokenUtils.getTenantIdFromToken(token);
            Claims claims = TokenUtils.parseToken(token);
            String role = claims.get("role", String.class);
            String nickname = claims.getSubject();
            

            if (tenantId == null || tenantId.isEmpty()) {
                abortRequest(requestContext, "Token inválido: Tenant ID ausente.");
                return;
            }

            // 3. Establecer el contexto Multi-Tenant de Hibernate
            tenantResolver.setTenantIdentifier(tenantId);
            
            tenantContext.setTenantId(tenantId);
            tenantContext.setRole(role);
            tenantContext.setNickname(nickname);

            // Crear un SecurityContext temporal que provee Principal y verificación de roles
            final String userRole = role != null ? role : "";
            final String userNickname = nickname != null ? nickname : "";
            final SecurityContext previous = requestContext.getSecurityContext();

            SecurityContext sc = new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return () -> userNickname;
                }

                @Override
                public boolean isUserInRole(String role) {
                    if (role == null) return false;
                    return userRole.equalsIgnoreCase(role);
                }

                @Override
                public boolean isSecure() {
                    return previous != null && previous.isSecure();
                }

                @Override
                public String getAuthenticationScheme() {
                    return AUTH_SCHEME;
                }
            };

            // Establecer el SecurityContext en la solicitud para que JAX-RS y @RolesAllowed funcionen
            requestContext.setSecurityContext(sc);

            
        } catch (JwtException e) {
            // Token expirado, firma inválida o error de parsing.
            abortRequest(requestContext, "Token inválido o expirado.");
        } catch (Exception e) {
             abortRequest(requestContext, "Error de servidor al procesar el token.");
        }
    }
    
    private void abortRequest(ContainerRequestContext requestContext, String message) {
        // Detiene el procesamiento de la solicitud y devuelve 401 Unauthorized
        requestContext.abortWith(
            Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"" + message + "\"}")
                    .type("application/json")
                    .build()
        );
    }
}
