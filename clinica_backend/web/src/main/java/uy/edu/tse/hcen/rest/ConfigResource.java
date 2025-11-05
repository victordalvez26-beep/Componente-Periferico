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
        public String tenantId; // Alternativa a 'id' para compatibilidad
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
        public String colorPrimario; // Para configuración de portal
        public String nombrePortal; // Para configuración de portal
        public String adminEmail; // Email del administrador
        
        // Getters y setters para JSON-B
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
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
            // Validar datos requeridos - aceptar 'id' o 'tenantId'
            Long tenantIdLong = null;
            if (req.id != null) {
                tenantIdLong = req.id;
            } else if (req.tenantId != null && !req.tenantId.isBlank()) {
                try {
                    tenantIdLong = Long.parseLong(req.tenantId);
                } catch (NumberFormatException e) {
                    LOG.error("Invalid tenantId format: " + req.tenantId);
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", "Field 'tenantId' must be a valid number"))
                            .build();
                }
            } else {
                LOG.error("Missing required field: id or tenantId");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Field 'id' or 'tenantId' is required"))
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
            String tenantId = String.valueOf(tenantIdLong);
            String schemaName = "schema_clinica_" + tenantId;
            
            LOG.infof("Creating tenant schema: %s", schemaName);
            
            // Usar color primario del request o por defecto
            String colorPrimario = (req.colorPrimario != null && !req.colorPrimario.isBlank()) 
                    ? req.colorPrimario : "#007bff"; // Azul por defecto
            
            // Usar nombre del portal del request o el nombre de la clínica
            String nombrePortal = (req.nombrePortal != null && !req.nombrePortal.isBlank()) 
                    ? req.nombrePortal : req.nombre;
            
            tenantAdminService.createTenantSchema(schemaName, colorPrimario, nombrePortal);
            
            // 2. Registrar nodo en tabla maestra public.nodoperiferico
            LOG.infof("Registering nodo in public schema: id=%s, nombre=%s, rut=%s, schema=%s", 
                      tenantIdLong, req.nombre, req.rut, schemaName);
            tenantAdminService.registerNodoInPublic(tenantIdLong, req.nombre, req.rut, schemaName);
            
            // 3. Crear usuario administrador inicial de la clínica
            LOG.infof("Creating admin user for tenant %s", tenantId);
            
            // Usar adminEmail del request, o extraer del contacto, o usar por defecto
            String adminEmail = (req.adminEmail != null && !req.adminEmail.isBlank()) 
                    ? req.adminEmail 
                    : (req.contacto != null ? extractEmail(req.contacto) : null);
            
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
            
            // TODO: Implementar actualización de datos del nodo si es necesario
            // El nodo ya está registrado desde /init, no necesita re-registro
            
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
     * DTO para recibir datos completos de activación/registro de cuenta.
     * Incluye tanto credenciales de usuario como datos de la clínica.
     */
    public static class ActivationRequest {
        public String tenantId;
        public String token;
        // Credenciales de usuario
        public String username;
        public String password;
        // Datos de la clínica
        public String rut;
        public String departamento;
        public String localidad;
        public String direccion;
        public String telefono;
        
        // Getters y Setters
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getRut() { return rut; }
        public void setRut(String rut) { this.rut = rut; }
        public String getDepartamento() { return departamento; }
        public void setDepartamento(String departamento) { this.departamento = departamento; }
        public String getLocalidad() { return localidad; }
        public void setLocalidad(String localidad) { this.localidad = localidad; }
        public String getDireccion() { return direccion; }
        public void setDireccion(String direccion) { this.direccion = direccion; }
        public String getTelefono() { return telefono; }
        public void setTelefono(String telefono) { this.telefono = telefono; }
    }

    /**
     * Endpoint simple para activar un usuario admin creado por /init.
     * Solo requiere tenantId, token y password.
     * 
     * @param body Map con tenantId, token y password
     * @return 200 OK con nickname del usuario activado
     */
    @POST
    @Path("/activate-simple")
    public Response activateSimple(Map<String, String> body) {
        if (body == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "Request body required")).build();
        }
        
        String tenantId = body.get("tenantId");
        String token = body.get("token");
        String password = body.get("password");
        
        if (tenantId == null || tenantId.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "tenantId is required")).build();
        }
        if (token == null || token.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "token is required")).build();
        }
        if (password == null || password.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "password is required")).build();
        }
        
        try {
            String nickname = tenantAdminService.activateAdminUser(tenantId, token, password);
            return Response.ok(Map.of("nickname", nickname, "message", "Usuario activado exitosamente")).build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", e.getMessage())).build();
        } catch (Exception e) {
            LOG.errorf(e, "Error activating user for tenant %s", tenantId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", "Error activando usuario: " + e.getMessage())).build();
        }
    }

    /**
     * Endpoint para activar/registrar completamente una clínica.
     * El administrador completa el formulario con datos de la clínica y sus credenciales.
     * Este endpoint crea el tenant, el usuario, y notifica a HCEN del registro completado.
     * 
     * @param req Datos completos de activación (tenant, token, username, password, RUT, dirección, etc.)
     * @return 200 OK con mensaje de éxito y datos de login
     */
    @POST
    @Path("/activate")
    public Response activate(ActivationRequest req) {
        LOG.infof("Received complete registration request for tenant: %s", req.tenantId);
        
        try {
            // Validar datos requeridos básicos
            if (req.tenantId == null || req.tenantId.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "tenantId is required")).build();
            }
            if (req.token == null || req.token.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "token is required")).build();
            }
            if (req.username == null || req.username.length() < 3) {
                return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "username must be at least 3 characters")).build();
            }
            if (req.password == null || req.password.length() < 8) {
                return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "password must be at least 8 characters")).build();
            }
            // Validar datos de la clínica
            if (req.rut == null || req.rut.length() < 12) {
                return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "RUT must be 12 digits")).build();
            }
            if (req.departamento == null || req.departamento.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "departamento is required")).build();
            }
            if (req.direccion == null || req.direccion.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "direccion is required")).build();
            }
            if (req.telefono == null || req.telefono.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "telefono is required")).build();
            }
            
            String schemaName = "schema_clinica_" + req.tenantId;
            String colorPrimario = "#007bff";
            
            // PASO 1: Crear schema y tablas del tenant
            LOG.infof("Creating tenant schema: %s", schemaName);
            tenantAdminService.createTenantSchema(schemaName, colorPrimario, "Clínica " + req.tenantId);
            
            // PASO 2: Registrar clínica en public.nodoperiferico con los datos completos
            LOG.infof("Registering clinic in public schema with RUT: %s", req.rut);
            tenantAdminService.registerNodoInPublic(Long.parseLong(req.tenantId), "Clínica " + req.tenantId, req.rut, schemaName);
            
            // PASO 3: Activar el usuario con username personalizado y contraseña
            String userNickname = tenantAdminService.activateAdminUserComplete(
                req.tenantId,
                schemaName,
                req.token,
                req.username,
                req.password
            );
            
            LOG.infof("✅ Clinic fully registered: tenant=%s, username=%s, RUT=%s", req.tenantId, userNickname, req.rut);
            
            // PASO 4: Notificar a HCEN que el registro se completó
            try {
                String hcenUrl = "http://hcen-backend:8080/api/nodos/" + req.tenantId + "/complete-registration";
                LOG.infof("Notifying HCEN about completed registration: %s", hcenUrl);
                
                java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
                String jsonPayload = String.format(
                    "{\"rut\":\"%s\",\"departamento\":\"%s\",\"localidad\":\"%s\",\"direccion\":\"%s\",\"adminNickname\":\"%s\"}",
                    req.rut, req.departamento, 
                    req.localidad != null ? req.localidad : "", 
                    req.direccion, userNickname
                );
                
                java.net.http.HttpRequest hcenRequest = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(hcenUrl))
                        .header("Content-Type", "application/json")
                        .timeout(java.time.Duration.ofSeconds(10))
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();
                
                java.net.http.HttpResponse<String> hcenResponse = httpClient.send(
                    hcenRequest, 
                    java.net.http.HttpResponse.BodyHandlers.ofString()
                );
                
                if (hcenResponse.statusCode() >= 200 && hcenResponse.statusCode() < 300) {
                    LOG.info("✅ HCEN notified successfully about clinic " + req.tenantId);
                } else {
                    LOG.warn("⚠️ HCEN notification failed. Status: " + hcenResponse.statusCode());
                }
            } catch (Exception e) {
                LOG.error("Error notifying HCEN (clinic still functional): " + e.getMessage(), e);
                // No fallar el registro si HCEN no responde - la clínica ya está creada
            }
            
            return Response.ok()
                    .entity(Map.of(
                        "message", "Clinic registered and account activated successfully",
                        "username", userNickname,
                        "loginUrl", "/portal/clinica/" + req.tenantId + "/login",
                        "clinicData", Map.of(
                            "rut", req.rut,
                            "departamento", req.departamento,
                            "direccion", req.direccion
                        )
                    ))
                    .build();
                    
        } catch (SecurityException se) {
            LOG.warn("Activation failed - security error: " + se.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", se.getMessage()))
                    .build();
                    
        } catch (Exception ex) {
            LOG.error("Error during clinic registration/activation", ex);
            return Response.serverError()
                    .entity(Map.of(
                        "error", "Failed to complete clinic registration",
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

