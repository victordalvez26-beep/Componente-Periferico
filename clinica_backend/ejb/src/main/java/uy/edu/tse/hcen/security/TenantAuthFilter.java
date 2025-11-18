package uy.edu.tse.hcen.security;

import uy.edu.tse.hcen.multitenancy.SchemaTenantResolver;
import uy.edu.tse.hcen.utils.TokenUtils;
import uy.edu.tse.hcen.context.TenantContext; // Clase de utilidad para almacenar el ID (CDI bean)
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
        String method = requestContext.getMethod();
        
        // Log para debugging
        java.util.logging.Logger.getLogger(TenantAuthFilter.class.getName())
            .info("TenantAuthFilter - Path: " + path + ", Method: " + method);
        
        // Permitir acceso SIN token a endpoints públicos:
        // - /auth/login : Login de usuarios
        // - /config/* : Endpoints llamados por HCEN central (init, update, delete, activate, health)
        // - /api/documentos-pdf/{id} : Endpoint de descarga de PDFs (permite tokens de servicio o acceso sin autenticación)
        if (path.contains("/auth/login") || path.contains("config/") || path.equals("config/health")) {
            // Establecer un SecurityContext básico para permitir acceso sin autenticación
            final SecurityContext previous = requestContext.getSecurityContext();
            SecurityContext sc = new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return () -> "internal-service";
                }
                @Override
                public boolean isUserInRole(String role) {
                    return "PROFESIONAL".equalsIgnoreCase(role);
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
            requestContext.setSecurityContext(sc);
            java.util.logging.Logger.getLogger(TenantAuthFilter.class.getName())
                .info("TenantAuthFilter - Acceso permitido sin token para: " + path);
            return; 
        }
        
        // Para documentos-pdf/{id} GET (descarga individual), permitir acceso siempre
        // El backend HCEN ya valida la autenticación del usuario del frontend
        // Puede venir con token de servicio o sin token
        // NO incluir /paciente/{ci} que requiere autenticación y tenant
        // El path puede venir como /documentos-pdf/{id} o /hcen-web/api/documentos-pdf/{id}
        if (path.contains("documentos-pdf/") && "GET".equals(method) && !path.contains("/paciente/") && !path.contains("documentos-pdf/paciente/")) {
            // Establecer un SecurityContext básico para permitir acceso
            final SecurityContext previous = requestContext.getSecurityContext();
            SecurityContext sc = new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return () -> "hcen-backend";
                }
                @Override
                public boolean isUserInRole(String role) {
                    return true; // Permitir todos los roles para acceso desde backend HCEN
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
            requestContext.setSecurityContext(sc);
            java.util.logging.Logger.getLogger(TenantAuthFilter.class.getName())
                .info("TenantAuthFilter - Acceso permitido para documentos-pdf/{id} (con o sin token): " + path);
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
            // 2. Validar como token de usuario normal
            Claims claims = TokenUtils.parseToken(token);
            String tenantId = claims.get("tenantId", String.class);
            String role = claims.get("role", String.class);
            String nickname = claims.getSubject();
            
            // Si el token no tiene tenantId, puede ser un token de servicio
            // Los tokens de servicio tienen "serviceName" o "iss": "HCEN-Service"
            if (tenantId == null || tenantId.isEmpty()) {
                String serviceName = claims.get("serviceName", String.class);
                String iss = claims.getIssuer();
                if (serviceName != null || "HCEN-Service".equals(iss)) {
                    // Es un token de servicio, permitir acceso pero sin establecer tenant
                    java.util.logging.Logger.getLogger(TenantAuthFilter.class.getName())
                        .info("TenantAuthFilter - Token de servicio detectado, permitiendo acceso sin tenant");
                    final SecurityContext previous = requestContext.getSecurityContext();
                    SecurityContext sc = new SecurityContext() {
                        @Override
                        public Principal getUserPrincipal() {
                            return () -> "hcen-backend";
                        }
                        @Override
                        public boolean isUserInRole(String role) {
                            return true;
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
                    requestContext.setSecurityContext(sc);
                    return; // Permitir acceso sin establecer tenant
                } else {
                    // No es token de servicio ni de usuario válido
                    abortRequest(requestContext, "Token inválido: Tenant ID ausente.");
                    return;
                }
            }

            // 3. Establecer el contexto Multi-Tenant de Hibernate
            tenantResolver.setTenantIdentifier(tenantId);
            
            // Establecer tenant en el CDI bean
            tenantContext.setTenantId(tenantId);
            tenantContext.setRole(role);
            tenantContext.setNickname(nickname);
            
            // Establecer tenant en el thread-local (para Hibernate y TenantContext.getCurrentTenant())
            uy.edu.tse.hcen.multitenancy.TenantContext.setCurrentTenant(tenantId);

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
