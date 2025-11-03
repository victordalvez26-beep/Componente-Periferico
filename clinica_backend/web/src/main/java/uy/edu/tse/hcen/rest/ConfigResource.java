package uy.edu.tse.hcen.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import uy.edu.tse.hcen.service.TenantAdminService;

import java.util.Map;

/**
 * Resource para configuración de nodos periféricos.
 * Recibe notificaciones del componente central (HCEN) para inicializar,
 * actualizar o eliminar clínicas (tenants) en este nodo periférico.
 */
@Path("/config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConfigResource {

    private static final Logger LOG = Logger.getLogger(ConfigResource.class);

    @Inject
    private TenantAdminService tenantAdminService;

    /**
     * DTO para recibir información de inicialización de clínica desde HCEN central.
     */
    public static class InitRequest {
        public Long id;
        public String rut;
        public String nombre;
        public String departamento;
        public String localidad;
        public String direccion;
        public String nodoPerifericoUrlBase;
        public String nodoPerifericoUsuario;
        public String nodoPerifericoPassword;
        public String contacto;
        public String url;
        
        // Getters y setters para JSON-B
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getRut() { return rut; }
        public void setRut(String rut) { this.rut = rut; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getDepartamento() { return departamento; }
        public void setDepartamento(String departamento) { this.departamento = departamento; }
        public String getLocalidad() { return localidad; }
        public void setLocalidad(String localidad) { this.localidad = localidad; }
        public String getDireccion() { return direccion; }
        public void setDireccion(String direccion) { this.direccion = direccion; }
        public String getNodoPerifericoUrlBase() { return nodoPerifericoUrlBase; }
        public void setNodoPerifericoUrlBase(String nodoPerifericoUrlBase) { this.nodoPerifericoUrlBase = nodoPerifericoUrlBase; }
        public String getNodoPerifericoUsuario() { return nodoPerifericoUsuario; }
        public void setNodoPerifericoUsuario(String nodoPerifericoUsuario) { this.nodoPerifericoUsuario = nodoPerifericoUsuario; }
        public String getNodoPerifericoPassword() { return nodoPerifericoPassword; }
        public void setNodoPerifericoPassword(String nodoPerifericoPassword) { this.nodoPerifericoPassword = nodoPerifericoPassword; }
        public String getContacto() { return contacto; }
        public void setContacto(String contacto) { this.contacto = contacto; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    /**
     * Endpoint llamado por HCEN central para inicializar una nueva clínica (tenant).
     * 
     * Flujo:
     * 1. Crea el schema del tenant (ej: schema_clinica_123)
     * 2. Crea las tablas base necesarias
     * 3. Registra el nodo en la tabla maestra public.nodoperiferico
     * 4. Crea un usuario administrador inicial para la clínica (opcional)
     * 
     * @param req Datos de la clínica enviados desde HCEN
     * @return 200 OK si exitoso, 500 si hay error
     */
    @POST
    @Path("/init")
    public Response init(InitRequest req) {
        LOG.infof("Received init request for clinic: id=%s, rut=%s, nombre=%s", 
                  req.id, req.rut, req.nombre);
        
        try {
            // Validar datos requeridos
            if (req.id == null) {
                LOG.error("Missing required field: id");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Field 'id' is required"))
                        .build();
            }
            
            if (req.rut == null || req.rut.isBlank()) {
                LOG.error("Missing required field: rut");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Field 'rut' is required"))
                        .build();
            }
            
            if (req.nombre == null || req.nombre.isBlank()) {
                LOG.error("Missing required field: nombre");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Field 'nombre' is required"))
                        .build();
            }
            
            // 1. Crear schema del tenant
            String tenantId = String.valueOf(req.id);
            String schemaName = "schema_clinica_" + tenantId;
            
            LOG.infof("Creating tenant schema: %s", schemaName);
            
            // Usar color primario por defecto si no se especifica
            String colorPrimario = "#007bff"; // Azul por defecto
            
            tenantAdminService.createTenantSchema(schemaName, colorPrimario, req.nombre);
            
            // 2. Registrar nodo en tabla maestra public.nodoperiferico
            LOG.infof("Registering nodo in public schema: id=%s, nombre=%s, rut=%s", 
                      req.id, req.nombre, req.rut);
            
            tenantAdminService.registerNodoInPublic(req.id, req.nombre, req.rut);
            
            // 3. Crear usuario administrador inicial de la clínica
            LOG.infof("Creating admin user for tenant %s", tenantId);
            
            // Extraer email del contacto si está presente
            String adminEmail = extractEmail(req.contacto);
            
            // URL base del componente periférico (puede venir en la request o usar la configurada)
            String peripheralBaseUrl = req.nodoPerifericoUrlBase != null ? 
                                      req.nodoPerifericoUrlBase : "http://localhost:8081";
            
            TenantAdminService.AdminCreationResult adminResult = 
                tenantAdminService.createAdminUser(tenantId, schemaName, adminEmail, peripheralBaseUrl);
            
            LOG.infof("Successfully initialized tenant: %s (id=%s), admin user: %s", 
                      req.nombre, req.id, adminResult.adminNickname);
            
            return Response.ok()
                    .entity(Map.of(
                        "message", "Tenant initialized successfully",
                        "tenantId", tenantId,
                        "schemaName", schemaName,
                        "clinicName", req.nombre,
                        "adminNickname", adminResult.adminNickname,
                        "activationToken", adminResult.activationToken,
                        "activationUrl", adminResult.activationUrl,
                        "tokenExpiresAt", adminResult.tokenExpiry.toString()
                    ))
                    .build();
                    
        } catch (IllegalArgumentException e) {
            LOG.error("Validation error during tenant initialization", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
                    
        } catch (Exception ex) {
            LOG.error("Error initializing tenant", ex);
            return Response.serverError()
                    .entity(Map.of(
                        "error", "Failed to initialize tenant",
                        "details", ex.getMessage() != null ? ex.getMessage() : "Unknown error"
                    ))
                    .build();
        }
    }

    /**
     * Endpoint para actualizar la configuración de una clínica existente.
     * 
     * @param req Datos actualizados de la clínica
     * @return 200 OK si exitoso
     */
    @POST
    @Path("/update")
    public Response update(InitRequest req) {
        LOG.infof("Received update request for clinic: id=%s, rut=%s", req.id, req.rut);
        
        try {
            if (req.id == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Field 'id' is required"))
                        .build();
            }
            
            // TODO: Implementar actualización de datos del nodo
            // Por ahora solo actualizamos el registro en public.nodoperiferico
            if (req.nombre != null && req.rut != null) {
                tenantAdminService.registerNodoInPublic(req.id, req.nombre, req.rut);
            }
            
            LOG.infof("Successfully updated tenant: id=%s", req.id);
            
            return Response.ok()
                    .entity(Map.of(
                        "message", "Tenant configuration updated",
                        "tenantId", String.valueOf(req.id)
                    ))
                    .build();
                    
        } catch (Exception ex) {
            LOG.error("Error updating tenant", ex);
            return Response.serverError()
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }

    /**
     * Endpoint para eliminar/desactivar una clínica.
     * Realiza un soft-delete (no borra el schema, solo marca como inactivo).
     * 
     * @param req Datos de la clínica a eliminar (requiere al menos el id)
     * @return 204 No Content si exitoso
     */
    @POST
    @Path("/delete")
    public Response delete(Map<String, Object> req) {
        LOG.infof("Received delete request: %s", req);
        
        try {
            Object idObj = req.get("id");
            if (idObj == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Field 'id' is required"))
                        .build();
            }
            
            Long id = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(idObj.toString());
            
            // TODO: Implementar soft-delete
            // Por ahora solo logeamos la operación
            LOG.infof("Marking tenant as deleted: id=%s", id);
            
            return Response.noContent().build();
                    
        } catch (Exception ex) {
            LOG.error("Error deleting tenant", ex);
            return Response.serverError()
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }
    
    /**
     * DTO para recibir datos de activación de cuenta.
     */
    public static class ActivationRequest {
        public String tenantId;
        public String token;
        public String password;
        
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    /**
     * Endpoint para activar la cuenta de un administrador de clínica.
     * El administrador usa el token recibido por email para crear su contraseña.
     * 
     * @param req Datos de activación (tenantId, token, password)
     * @return 200 OK con mensaje de éxito y datos de login
     */
    @POST
    @Path("/activate")
    public Response activate(ActivationRequest req) {
        LOG.infof("Received activation request for tenant: %s", req.tenantId);
        
        try {
            // Validar datos requeridos
            if (req.tenantId == null || req.tenantId.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Field 'tenantId' is required"))
                        .build();
            }
            
            if (req.token == null || req.token.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Field 'token' is required"))
                        .build();
            }
            
            if (req.password == null || req.password.length() < 8) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Password must be at least 8 characters"))
                        .build();
            }
            
            // Activar el usuario
            String userNickname = tenantAdminService.activateAdminUser(
                req.tenantId, 
                req.token, 
                req.password
            );
            
            LOG.infof("Successfully activated user: %s for tenant: %s", userNickname, req.tenantId);
            
            return Response.ok()
                    .entity(Map.of(
                        "message", "Account activated successfully",
                        "username", userNickname,
                        "loginUrl", "/portal/clinica-" + req.tenantId + "/login",
                        "instructions", "You can now login with your credentials"
                    ))
                    .build();
                    
        } catch (SecurityException se) {
            // Token inválido, expirado o ya usado
            LOG.warn("Activation failed - security error: " + se.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", se.getMessage()))
                    .build();
                    
        } catch (Exception ex) {
            LOG.error("Error during account activation", ex);
            return Response.serverError()
                    .entity(Map.of(
                        "error", "Failed to activate account",
                        "details", ex.getMessage() != null ? ex.getMessage() : "Unknown error"
                    ))
                    .build();
        }
    }

    /**
     * Health check endpoint para verificar que el servicio de configuración está activo.
     * 
     * @return 200 OK con mensaje de estado
     */
    @GET
    @Path("/health")
    public Response health() {
        return Response.ok()
                .entity(Map.of(
                    "status", "UP",
                    "service", "config-service",
                    "message", "Configuration service is running"
                ))
                .build();
    }

    /**
     * Extrae un email del campo de contacto.
     * Busca un patrón de email en el string de contacto.
     * Si no encuentra, retorna null.
     */
    private String extractEmail(String contacto) {
        if (contacto == null || contacto.isBlank()) {
            return null;
        }
        
        // Regex simple para detectar email
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
        );
        java.util.regex.Matcher matcher = pattern.matcher(contacto);
        
        if (matcher.find()) {
            return matcher.group();
        }
        
        return null;
    }
}

