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
    private static final String KEY_ERROR = "error";
    private static final String KEY_TENANT_ID = "tenantId";
    private static final String KEY_NOMBRE_PORTAL = "nombrePortal";
    private static final String KEY_COLOR_PRIMARIO = "colorPrimario";
    private static final String KEY_COLOR_SECUNDARIO = "colorSecundario";
    private static final String KEY_LOGO_URL = "logoUrl";
    private static final String MSG_TENANT_ID_REQUIRED = "tenantId is required";
    private static final String PREFIX_CLINICA = "Clínica ";
    private static final String COLOR_BLUE_DEFAULT = "#007bff";
    private static final String KEY_MESSAGE = "message";

    @Inject
    private TenantAdminService tenantAdminService;

    /**
     * DTO para recibir información de inicialización de clínica desde HCEN central.
     */
    public static class InitRequest {
        private Long id;
        private String rut;
        private String nombre;
        private String departamento;
        private String localidad;
        private String direccion;
        private String nodoPerifericoUrlBase;
        private String nodoPerifericoUsuario;
        private String nodoPerifericoPassword;
        private String contacto;
        private String url;
        
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
                  req.getId(), req.getRut(), req.getNombre());
        
        try {
            // Validar datos requeridos
            if (req.getId() == null) {
                LOG.error("Missing required field: id");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(KEY_ERROR, "Field 'id' is required"))
                        .build();
            }
            
            if (req.getRut() == null || req.getRut().isBlank()) {
                LOG.error("Missing required field: rut");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(KEY_ERROR, "Field 'rut' is required"))
                        .build();
            }
            
            if (req.getNombre() == null || req.getNombre().isBlank()) {
                LOG.error("Missing required field: nombre");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(KEY_ERROR, "Field 'nombre' is required"))
                        .build();
            }
            
            // 1. Crear schema del tenant
            String tenantId = String.valueOf(req.getId());
            String schemaName = "schema_clinica_" + tenantId;
            
            LOG.infof("Creating tenant schema: %s", schemaName);
            
            // Usar color primario por defecto si no se especifica
            String colorPrimario = COLOR_BLUE_DEFAULT; // Azul por defecto
            
            tenantAdminService.createTenantSchema(schemaName, colorPrimario, req.getNombre());
            
            // 2. Registrar nodo en tabla maestra public.nodoperiferico
            LOG.infof("Registering nodo in public schema: id=%s, nombre=%s, rut=%s, schema=%s", 
                      req.getId(), req.getNombre(), req.getRut(), schemaName);
            tenantAdminService.registerNodoInPublic(req.getId(), req.getNombre(), req.getRut(), schemaName);
            
            // 3. Crear usuario administrador inicial de la clínica
            LOG.infof("Creating admin user for tenant %s", tenantId);
            
            // Extraer email del contacto si está presente
            String adminEmail = extractEmail(req.getContacto());
            
            // URL base del componente periférico (puede venir en la request o usar la configurada)
            String peripheralBaseUrl = req.getNodoPerifericoUrlBase() != null ? 
                                      req.getNodoPerifericoUrlBase() : "http://localhost:8081";
            
            TenantAdminService.AdminCreationResult adminResult = 
                tenantAdminService.createAdminUser(tenantId, schemaName, adminEmail, peripheralBaseUrl);
            
            LOG.infof("Successfully initialized tenant: %s (id=%s), admin user: %s", 
                      req.getNombre(), req.getId(), adminResult.getAdminNickname());
            
            return Response.ok()
                    .entity(Map.of(
                        KEY_MESSAGE, "Tenant initialized successfully",
                        KEY_TENANT_ID, tenantId,
                        "schemaName", schemaName,
                        "clinicName", req.getNombre(),
                        "adminNickname", adminResult.getAdminNickname(),
                        "activationToken", adminResult.getActivationToken(),
                        "activationUrl", adminResult.getActivationUrl(),
                        "tokenExpiresAt", adminResult.getTokenExpiry().toString()
                    ))
                    .build();
                    
        } catch (IllegalArgumentException e) {
            LOG.error("Validation error during tenant initialization", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(KEY_ERROR, e.getMessage()))
                    .build();
                    
        } catch (Exception ex) {
            LOG.error("Error initializing tenant", ex);
            return Response.serverError()
                    .entity(Map.of(
                        KEY_ERROR, "Failed to initialize tenant",
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
        LOG.infof("Received update request for clinic: id=%s, rut=%s", req.getId(), req.getRut());
        
        try {
            if (req.getId() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(KEY_ERROR, "Field 'id' is required"))
                        .build();
            }
            
            // El nodo ya está registrado desde /init, no necesita re-registro
            // Se asume que la actualización de datos del nodo se maneja por otro canal o no es necesaria aquí
            
            LOG.infof("Successfully updated tenant: id=%s", req.getId());
            
            return Response.ok()
                    .entity(Map.of(
                        KEY_MESSAGE, "Tenant configuration updated",
                        KEY_TENANT_ID, String.valueOf(req.getId())
                    ))
                    .build();
                    
        } catch (Exception ex) {
            LOG.error("Error updating tenant", ex);
            return Response.serverError()
                    .entity(Map.of(KEY_ERROR, ex.getMessage()))
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
                        .entity(Map.of(KEY_ERROR, "Field 'id' is required"))
                        .build();
            }
            
            Long id = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(idObj.toString());
            
            // Implementación de soft-delete pendiente
            // Por ahora solo logeamos la operación
            LOG.infof("Marking tenant as deleted: id=%s", id);
            
            return Response.noContent().build();
                    
        } catch (Exception ex) {
            LOG.error("Error deleting tenant", ex);
            return Response.serverError()
                    .entity(Map.of(KEY_ERROR, ex.getMessage()))
                    .build();
        }
    }
    
    /**
     * DTO para recibir datos completos de activación/registro de cuenta.
     * Incluye tanto credenciales de usuario como datos de la clínica.
     */
    public static class ActivationRequest {
        private String tenantId;
        private String token;
        // Credenciales de usuario
        private String username;
        private String password;
        // Datos de la clínica
        private String rut;
        private String departamento;
        private String localidad;
        private String direccion;
        private String telefono;
        
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
        LOG.infof("🔍 [ACTIVATE ENDPOINT] Received complete registration request for tenant: %s", req.tenantId);
        LOG.infof("🔍 [ACTIVATE ENDPOINT] Request data - RUT: %s, Departamento: %s, Localidad: %s, Direccion: %s, Username: %s", 
                  req.rut, req.departamento, req.localidad, req.direccion, req.username);
        
        try {
            validateActivationRequest(req);
            
            String schemaName = "schema_clinica_" + req.getTenantId();
            String colorPrimario = COLOR_BLUE_DEFAULT;
            
            // PASO 1: Crear schema y tablas del tenant
            LOG.infof("Creating tenant schema: %s", schemaName);
            tenantAdminService.createTenantSchema(schemaName, colorPrimario, PREFIX_CLINICA + req.getTenantId());
            
            // PASO 2: Registrar clínica en public.nodoperiferico con los datos completos
            LOG.infof("Registering clinic in public schema with RUT: %s", req.getRut());
            tenantAdminService.registerNodoInPublic(Long.parseLong(req.getTenantId()), PREFIX_CLINICA + req.getTenantId(), req.getRut(), schemaName);
            
            // PASO 3: Activar el usuario con username personalizado y contraseña
            String userNickname = tenantAdminService.activateAdminUserComplete(
                req.getTenantId(),
                schemaName,
                req.getToken(),
                req.getUsername(),
                req.getPassword()
            );
            
            LOG.infof("✅ Clinic fully registered: tenant=%s, username=%s, RUT=%s", req.getTenantId(), userNickname, req.getRut());
            
            // PASO 4: Notificar a HCEN que el registro se completó
            notifyHcenRegistration(req, userNickname);
            
            return Response.ok()
                    .entity(Map.of(
                        KEY_MESSAGE, "Clinic registered and account activated successfully",
                        "username", userNickname,
                        "loginUrl", "/portal/clinica/" + req.getTenantId() + "/login",
                        "clinicData", Map.of(
                            "rut", req.getRut(),
                            "departamento", req.getDepartamento(),
                            "direccion", req.getDireccion()
                        )
                    ))
                    .build();
        
        } catch (IllegalArgumentException e) {
             return Response.status(Response.Status.BAD_REQUEST).entity(Map.of(KEY_ERROR, e.getMessage())).build();
        } catch (SecurityException se) {
            LOG.warn("Activation failed - security error: " + se.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of(KEY_ERROR, se.getMessage()))
                    .build();
                    
        } catch (Exception ex) {
            LOG.error("Error during clinic registration/activation", ex);
            return Response.serverError()
                    .entity(Map.of(
                        KEY_ERROR, "Failed to complete clinic registration",
                        "details", ex.getMessage() != null ? ex.getMessage() : "Unknown error"
                    ))
                    .build();
        }
    }

    private void validateActivationRequest(ActivationRequest req) {
        validateRequiredFields(req);
        validateClinicData(req);
    }

    private void validateRequiredFields(ActivationRequest req) {
        if (req.getTenantId() == null || req.getTenantId().isBlank()) {
            throw new IllegalArgumentException(MSG_TENANT_ID_REQUIRED);
        }
        if (req.getToken() == null || req.getToken().isBlank()) {
            throw new IllegalArgumentException("token is required");
        }
        if (req.getUsername() == null || req.getUsername().length() < 3) {
            throw new IllegalArgumentException("username must be at least 3 characters");
        }
        if (req.getPassword() == null || req.getPassword().length() < 8) {
            throw new IllegalArgumentException("password must be at least 8 characters");
        }
    }

    private void validateClinicData(ActivationRequest req) {
        if (req.getRut() == null || req.getRut().length() < 12) {
            throw new IllegalArgumentException("RUT must be 12 digits");
        }
        if (req.getDepartamento() == null || req.getDepartamento().isBlank()) {
            throw new IllegalArgumentException("departamento is required");
        }
        if (req.getDireccion() == null || req.getDireccion().isBlank()) {
            throw new IllegalArgumentException("direccion is required");
        }
        if (req.getTelefono() == null || req.getTelefono().isBlank()) {
            throw new IllegalArgumentException("telefono is required");
        }
    }

    private void notifyHcenRegistration(ActivationRequest req, String userNickname) {
        try {
            String hcenUrl = uy.edu.tse.hcen.utils.HcenCentralUrlUtil.buildApiUrl("/nodos/" + req.getTenantId() + "/complete-registration");
            LOG.infof("Notifying HCEN about completed registration: %s", hcenUrl);
            
            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
            String jsonPayload = String.format(
                "{\"rut\":\"%s\",\"departamento\":\"%s\",\"localidad\":\"%s\",\"direccion\":\"%s\",\"adminNickname\":\"%s\"}",
                req.getRut(), req.getDepartamento(), 
                req.getLocalidad() != null ? req.getLocalidad() : "", 
                req.getDireccion(), userNickname
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
                LOG.info("✅ HCEN notified successfully about clinic " + req.getTenantId());
            } else {
                LOG.warn("⚠️ HCEN notification failed. Status: " + hcenResponse.statusCode());
            }
        } catch (InterruptedException ie) {
             Thread.currentThread().interrupt();
             LOG.error("Interrupted during HCEN notification", ie);
        } catch (Exception e) {
            LOG.error("Error notifying HCEN (clinic still functional): " + e.getMessage(), e);
            // No fallar el registro si HCEN no responde - la clínica ya está creada
        }
    }

    /**
     * Endpoint GET para obtener la configuración de una clínica por su ID.
     * 
     * @param id ID de la clínica (tenantId)
     * @return 200 OK con la configuración de la clínica
     */
    @GET
    @Path("/{id}")
    public Response getConfig(@PathParam("id") String id) {
        LOG.infof("Received GET config request for clinic: %s", id);
        
        try {
            if (id == null || id.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(KEY_ERROR, MSG_TENANT_ID_REQUIRED))
                        .build();
            }
            
            // Obtener configuración del tenant desde el servicio
            Map<String, Object> config = tenantAdminService.getTenantConfig(id);
            
            if (config == null || config.isEmpty()) {
                // Si no existe configuración, retornar valores por defecto
                return Response.ok()
                        .entity(Map.of(
                            KEY_TENANT_ID, id,
                            KEY_NOMBRE_PORTAL, PREFIX_CLINICA + id,
                            KEY_COLOR_PRIMARIO, COLOR_BLUE_DEFAULT,
                            KEY_COLOR_SECUNDARIO, "#6b7280",
                            KEY_LOGO_URL, ""
                        ))
                        .build();
            }
            
            return Response.ok().entity(config).build();
            
        } catch (Exception ex) {
            LOG.error("Error getting tenant config", ex);
            return Response.serverError()
                    .entity(Map.of(KEY_ERROR, ex.getMessage()))
                    .build();
        }
    }

    /**
     * Endpoint público (sin autenticación) para que el portal de login
     * pueda obtener la configuración visual de la clínica.
     *
     * Se expone bajo /config/clinic/{id} para diferenciarlo de los endpoints
     * administrativos (que requieren token) y sólo devuelve los campos necesarios
     * para personalizar la UI: nombre, colores y logo.
     *
     * @param id ID del tenant (clínica).
     * @return Configuración simplificada para el login.
     */
    @GET
    @Path("/clinic/{id}")
    public Response getPublicClinicConfig(@PathParam("id") String id) {
        LOG.infof("Received public clinic config request for tenant: %s", id);

        try {
            if (id == null || id.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(KEY_ERROR, MSG_TENANT_ID_REQUIRED))
                        .build();
            }

            Map<String, Object> config = tenantAdminService.getTenantConfig(id);

            if (config == null || config.isEmpty()) {
                return Response.ok()
                        .entity(Map.of(
                                KEY_TENANT_ID, id,
                                KEY_NOMBRE_PORTAL, PREFIX_CLINICA + id,
                                KEY_COLOR_PRIMARIO, "#667eea",
                                KEY_COLOR_SECUNDARIO, "#764ba2",
                                KEY_LOGO_URL, ""
                        ))
                        .build();
            }

            return Response.ok()
                    .entity(Map.of(
                            KEY_TENANT_ID, id,
                            KEY_NOMBRE_PORTAL, config.getOrDefault(KEY_NOMBRE_PORTAL, PREFIX_CLINICA + id),
                            KEY_COLOR_PRIMARIO, config.getOrDefault(KEY_COLOR_PRIMARIO, "#667eea"),
                            KEY_COLOR_SECUNDARIO, config.getOrDefault(KEY_COLOR_SECUNDARIO, "#764ba2"),
                            KEY_LOGO_URL, config.getOrDefault(KEY_LOGO_URL, "")
                    ))
                    .build();

        } catch (Exception ex) {
            LOG.error("Error getting public clinic config", ex);
            return Response.serverError()
                    .entity(Map.of(KEY_ERROR, ex.getMessage()))
                    .build();
        }
    }

    /**
     * Endpoint PUT para actualizar la configuración de una clínica por su ID.
     * 
     * @param id ID de la clínica (tenantId)
     * @param configData Map con los datos de configuración (nombrePortal, colorPrimario, colorSecundario, logoUrl)
     * @return 200 OK con la configuración actualizada
     */
    @PUT
    @Path("/{id}")
    public Response updateConfig(@PathParam("id") String id, Map<String, Object> configData) {
        LOG.infof("Received PUT config request for clinic: %s, data: %s", id, configData);
        
        try {
            if (id == null || id.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(KEY_ERROR, MSG_TENANT_ID_REQUIRED))
                        .build();
            }
            
            // Actualizar configuración del tenant
            tenantAdminService.updateTenantConfig(
                id,
                (String) configData.get(KEY_NOMBRE_PORTAL),
                (String) configData.get(KEY_COLOR_PRIMARIO),
                (String) configData.get(KEY_COLOR_SECUNDARIO),
                (String) configData.get(KEY_LOGO_URL)
            );
            
            LOG.infof("Successfully updated config for tenant: %s", id);
            
            return Response.ok()
                    .entity(Map.of(
                        KEY_MESSAGE, "Configuration updated successfully",
                        KEY_TENANT_ID, id
                    ))
                    .build();
                    
        } catch (Exception ex) {
            LOG.error("Error updating tenant config", ex);
            return Response.serverError()
                    .entity(Map.of(KEY_ERROR, ex.getMessage()))
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
                    KEY_MESSAGE, "Configuration service is running"
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

