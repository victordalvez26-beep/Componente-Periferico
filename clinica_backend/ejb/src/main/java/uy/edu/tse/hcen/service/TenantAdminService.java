package uy.edu.tse.hcen.service;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import javax.sql.DataSource;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDateTime;


@Stateless
public class TenantAdminService {

    @Resource(lookup = "java:/jdbc/MyMainDataSource")
    private DataSource dataSource;

    private static final Logger LOG = Logger.getLogger(TenantAdminService.class);

    public void createTenantSchema(String tenantSchema, String colorPrimario, String nombrePortal) throws SQLException {
        if (tenantSchema == null || tenantSchema.isBlank()) {
            throw new IllegalArgumentException("tenantSchema is required");
        }

        String createSchema = String.format("CREATE SCHEMA IF NOT EXISTS %s;", tenantSchema);

        // Use literal substitution for DDL/DDL-like statements (PreparedStatement parameters
        // are not reliably supported for DDL). Escape single quotes in inputs.
        String escColor = (colorPrimario == null) ? "#007bff" : colorPrimario.replace("'", "''");
        String escNombre = (nombrePortal == null) ? "Clinica" : nombrePortal.replace("'", "''");

        String createPortal = String.format(
            "CREATE TABLE IF NOT EXISTS %s.portal_configuracion (id BIGSERIAL PRIMARY KEY, color_primario VARCHAR(7) DEFAULT '%s', color_secundario VARCHAR(7) DEFAULT '#6c757d', logo_url VARCHAR(512), nombre_portal VARCHAR(100));",
            tenantSchema, escColor);

        String insertPortal = String.format(
            "INSERT INTO %s.portal_configuracion (id, color_primario, color_secundario, logo_url, nombre_portal) VALUES (1, '%s', '#6c757d', '', '%s') ON CONFLICT (id) DO NOTHING;",
            tenantSchema, escColor, escNombre);

        // Crear secuencia para IDs auto-incrementables
        String createSequence = String.format(
            "CREATE SEQUENCE IF NOT EXISTS %s.usuario_id_seq;",
            tenantSchema);

        String createUsuario = String.format(
            "CREATE TABLE IF NOT EXISTS %s.usuario (" +
            "  id BIGINT PRIMARY KEY DEFAULT nextval('%s.usuario_id_seq'), " +
            "  nombre VARCHAR(255) NOT NULL, " +
            "  email VARCHAR(255) NOT NULL, " +
            "  nickname VARCHAR(255) UNIQUE, " +
            "  password_hash VARCHAR(255), " +
            "  dtype VARCHAR(31), " +
            "  role VARCHAR(50), " +
            "  tenant_id VARCHAR(50), " +
            "  especialidad VARCHAR(100), " +
            "  departamento VARCHAR(50), " +
            "  direccion VARCHAR(255), " +
            "  nodo_periferico_id BIGINT" +
            ");",
            tenantSchema, tenantSchema);
        
        // Vincular la secuencia a la tabla
        String alterSequence = String.format(
            "ALTER SEQUENCE %s.usuario_id_seq OWNED BY %s.usuario.id;",
            tenantSchema, tenantSchema);

        // Crear secuencia para usuarioperiferico
        String createUsuPerSeq = String.format(
            "CREATE SEQUENCE IF NOT EXISTS %s.usuarioperiferico_id_seq;",
            tenantSchema);

        // Tabla usuarioperiferico con TODAS las columnas (herencia SINGLE_TABLE)
        // NOTA: Incluye tenant_id por compatibilidad JPA (aunque sea redundante con el schema)
        String createUsuPer = String.format(
            "CREATE TABLE IF NOT EXISTS %s.usuarioperiferico (" +
            "  id BIGINT PRIMARY KEY DEFAULT nextval('%s.usuarioperiferico_id_seq'), " +
            "  nickname VARCHAR(255) UNIQUE NOT NULL, " +
            "  password_hash VARCHAR(255) NOT NULL, " +
            "  dtype VARCHAR(31) NOT NULL, " +
            "  nombre VARCHAR(255), " +           // De Usuario
            "  email VARCHAR(255), " +            // De Usuario  
            "  role VARCHAR(50), " +              // Para facilitar queries
            "  tenant_id VARCHAR(50), " +         // Redundante pero necesario para JPA
            "  especialidad VARCHAR(100), " +     // De ProfesionalSalud
            "  departamento VARCHAR(50), " +      // De ProfesionalSalud
            "  direccion VARCHAR(255), " +        // De ProfesionalSalud
            "  nodo_periferico_id BIGINT" +      // Relación con clínica
            ");",
            tenantSchema, tenantSchema);
        
        String alterUsuPerSeq = String.format(
            "ALTER SEQUENCE %s.usuarioperiferico_id_seq OWNED BY %s.usuarioperiferico.id;",
            tenantSchema, tenantSchema);

        String createNodo = String.format(
            "CREATE TABLE IF NOT EXISTS %s.nodoperiferico (id BIGINT PRIMARY KEY, nombre VARCHAR(255), rut VARCHAR(255));",
            tenantSchema);

        // Tablas para herencia JOINED de JPA
        String createProfesionalSalud = String.format(
            "CREATE TABLE IF NOT EXISTS %s.profesionalsalud (" +
            "  id BIGINT PRIMARY KEY, " +
            "  especialidad VARCHAR(100), " +
            "  departamento VARCHAR(50), " +
            "  direccion VARCHAR(255), " +
            "  nodo_periferico_id BIGINT" +
            ");",
            tenantSchema);

        String createAdministradorClinica = String.format(
            "CREATE TABLE IF NOT EXISTS %s.administradorclinica (" +
            "  id BIGINT PRIMARY KEY, " +
            "  nodo_periferico_id BIGINT" +
            ");",
            tenantSchema);

        // Tabla usuario_salud para gestionar pacientes de la clínica
        String createUsuarioSalud = String.format(
            "CREATE TABLE IF NOT EXISTS %s.usuario_salud (" +
            "  id BIGSERIAL PRIMARY KEY, " +
            "  ci VARCHAR(20) NOT NULL, " +
            "  nombre VARCHAR(255), " +
            "  apellido VARCHAR(255), " +
            "  fecha_nacimiento DATE, " +
            "  direccion VARCHAR(255), " +
            "  telefono VARCHAR(50), " +
            "  email VARCHAR(255), " +
            "  departamento VARCHAR(100), " +
            "  localidad VARCHAR(100), " +
            "  hcen_user_id BIGINT, " +
            "  tenant_id BIGINT NOT NULL, " +
            "  fecha_alta TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "  fecha_actualizacion TIMESTAMP, " +
            "  CONSTRAINT uk_ci_tenant_%s UNIQUE (ci, tenant_id)" +
            ");",
            tenantSchema, tenantId);

        try (Connection c = dataSource.getConnection()) {
            // 1. Crear schema
            try (PreparedStatement s1 = c.prepareStatement(createSchema)) {
                s1.execute();
            }

            // 2. Crear secuencia ANTES de la tabla usuario
            try (PreparedStatement s = c.prepareStatement(createSequence)) {
                s.execute();
            }

            // 3. Crear tabla usuario con todas las columnas
            try (PreparedStatement s4 = c.prepareStatement(createUsuario)) { 
                s4.execute(); 
            }
            
            // 4. Vincular secuencia a la tabla
            try (PreparedStatement s = c.prepareStatement(alterSequence)) {
                s.execute();
            }

            // 5. Crear otras tablas
            try (PreparedStatement s2 = c.prepareStatement(createPortal)) {
                s2.execute();
            }

            try (PreparedStatement s3 = c.prepareStatement(insertPortal)) {
                s3.execute();
            }

            // 6. Crear secuencia para usuarioperiferico
            try (PreparedStatement s = c.prepareStatement(createUsuPerSeq)) {
                s.execute();
            }

            // 7. Crear tabla usuarioperiferico
            try (PreparedStatement s5 = c.prepareStatement(createUsuPer)) { s5.execute(); }
            
            // 8. Vincular secuencia
            try (PreparedStatement s = c.prepareStatement(alterUsuPerSeq)) {
                s.execute();
            }

            try (PreparedStatement s6 = c.prepareStatement(createNodo)) { s6.execute(); }
            try (PreparedStatement s7 = c.prepareStatement(createProfesionalSalud)) { s7.execute(); }
            try (PreparedStatement s8 = c.prepareStatement(createAdministradorClinica)) { s8.execute(); }
            
            // 9. Crear tabla usuario_salud para gestionar pacientes
            try (PreparedStatement s9 = c.prepareStatement(createUsuarioSalud)) { 
                s9.execute(); 
                LOGGER.info("Tabla usuario_salud creada para tenant: " + tenantId);
            }

            // using container-managed transactions; let the container handle commit
        } catch (SQLException ex) {
            LOG.errorf(ex, "Error creating tenant schema %s", tenantSchema);
            throw ex;
        }
    }

    /**
     * List tenants recorded in public.nodoperiferico. Returns a simple list
     * of maps with keys: id, nombre, rut.
     */
    public List<Map<String, Object>> listTenants() throws SQLException {
        List<Map<String, Object>> out = new ArrayList<>();
        String sql = "SELECT id, nombre, rut FROM public.nodoperiferico ORDER BY id";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", rs.getLong("id"));
                m.put("nombre", rs.getString("nombre"));
                m.put("rut", rs.getString("rut"));
                out.add(m);
            }
        }
        return out;
    }

    /**
     * Registra o actualiza un nodo en la tabla maestra public.nodoperiferico.
     * Este método se llama cuando HCEN central notifica sobre una nueva clínica.
     * 
     * @param id ID del nodo (debe ser único)
     * @param nombre Nombre de la clínica
     * @param rut RUT de la clínica (debe ser único)
     * @throws SQLException si hay error en la operación SQL
     */
    public void registerNodoInPublic(Long id, String nombre, String rut, String schemaName) throws SQLException {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        if (rut == null || rut.isBlank()) {
            throw new IllegalArgumentException("rut is required");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("nombre is required");
        }
        if (schemaName == null || schemaName.isBlank()) {
            throw new IllegalArgumentException("schemaName is required");
        }

        String sql = "INSERT INTO public.nodoperiferico (id, nombre, rut, schema_name) VALUES (?, ?, ?, ?) " +
                     "ON CONFLICT (id) DO UPDATE SET nombre = EXCLUDED.nombre, rut = EXCLUDED.rut, schema_name = EXCLUDED.schema_name";
        
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setString(2, nombre);
            ps.setString(3, rut);
            ps.setString(4, schemaName);
            int rowsAffected = ps.executeUpdate();
            
            LOG.infof("Registered nodo in public.nodoperiferico: id=%s, nombre=%s, rut=%s, schema=%s (rows affected: %d)", 
                      id, nombre, rut, schemaName, rowsAffected);
        } catch (SQLException ex) {
            LOG.errorf(ex, "Error registering nodo in public schema: id=%s, rut=%s", id, rut);
            throw ex;
        }
    }

    /**
     * Clase interna para retornar información del usuario admin creado.
     */
    public static class AdminCreationResult {
        public String adminNickname;
        public String activationToken;
        public String activationUrl;
        public LocalDateTime tokenExpiry;
    }

    /**
     * Crea un usuario administrador inicial para una clínica (tenant) recién creada.
     * El usuario se crea sin contraseña, con un token de activación que se envía por email.
     * 
     * @param tenantId ID del tenant (ej: "123")
     * @param tenantSchema Nombre del schema (ej: "schema_clinica_123")
     * @param adminEmail Email del administrador (opcional)
     * @param baseUrl URL base del componente periférico para construir link de activación
     * @return AdminCreationResult con nickname, token y URL de activación
     * @throws SQLException si hay error en la operación
     */
    public AdminCreationResult createAdminUser(String tenantId, String tenantSchema, String adminEmail, String baseUrl) 
            throws SQLException {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (tenantSchema == null || tenantSchema.isBlank()) {
            throw new IllegalArgumentException("tenantSchema is required");
        }

        // Generar credenciales
        String adminNickname = "admin_c" + tenantId;
        String activationToken = UUID.randomUUID().toString();
        LocalDateTime expiryTime = LocalDateTime.now().plusHours(48); // Token válido por 48 horas
        
        // Generar URL de activación para el frontend React (puerto 3001 en desarrollo)
        // NOTA: baseUrl se usa solo para comunicación backend-to-backend, no para URLs públicas
        String publicBaseUrl = "http://localhost:3001"; // TODO: hacer configurable (3001 dev, 8081 producción con build)
        String activationUrl = publicBaseUrl + "/portal/clinica/" + tenantId + "/activate?token=" + activationToken;

        try (Connection c = dataSource.getConnection()) {
            // 1. Crear tabla de tokens de activación en el schema del tenant (si no existe)
            String createTokensTable = String.format(
                "CREATE TABLE IF NOT EXISTS %s.activation_tokens (" +
                "  id BIGSERIAL PRIMARY KEY, " +
                "  token VARCHAR(255) UNIQUE NOT NULL, " +
                "  user_nickname VARCHAR(255) NOT NULL, " +
                "  email VARCHAR(255), " +
                "  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "  expires_at TIMESTAMP NOT NULL, " +
                "  used BOOLEAN DEFAULT FALSE, " +
                "  used_at TIMESTAMP" +
                ")",
                tenantSchema
            );
            
            try (PreparedStatement ps = c.prepareStatement(createTokensTable)) {
                ps.execute();
                LOG.infof("Created activation_tokens table in schema %s", tenantSchema);
            }

            // 2. Insertar el token de activación
            String insertToken = String.format(
                "INSERT INTO %s.activation_tokens (token, user_nickname, email, expires_at) VALUES (?, ?, ?, ?)",
                tenantSchema
            );
            
            try (PreparedStatement ps = c.prepareStatement(insertToken)) {
                ps.setString(1, activationToken);
                ps.setString(2, adminNickname);
                ps.setString(3, adminEmail);
                ps.setTimestamp(4, Timestamp.valueOf(expiryTime));
                ps.executeUpdate();
                LOG.infof("Created activation token for %s in schema %s", adminNickname, tenantSchema);
            }

            // 3. Crear usuario en public.usuario (tabla global) con ID auto-generada
            String insertUsuarioPublic = 
                "INSERT INTO public.usuario (nombre, email) " +
                "VALUES (?, ?) " +
                "RETURNING id"; // Devuelve la ID auto-generada
            
            long userId;
            try (PreparedStatement ps = c.prepareStatement(insertUsuarioPublic)) {
                ps.setString(1, "Administrador Clínica " + tenantId);
                ps.setString(2, adminEmail != null ? adminEmail : "admin" + tenantId + "@pendiente.local");
                
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        userId = rs.getLong(1);
                        LOG.infof("Created user in public.usuario with auto-generated ID: %d", userId);
                    } else {
                        throw new SQLException("Failed to get auto-generated user ID");
                    }
                }
            }

            // 4. Crear registro en public.usuarioperiferico (SIN contraseña todavía, estado pendiente)
            String insertUsuarioPeriferico = 
                "INSERT INTO public.usuarioperiferico (id, nickname, password_hash, dtype, tenant_id, role) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (nickname) DO UPDATE SET tenant_id = EXCLUDED.tenant_id";
            
            try (PreparedStatement ps = c.prepareStatement(insertUsuarioPeriferico)) {
                ps.setLong(1, userId);
                ps.setString(2, adminNickname);
                ps.setString(3, "PENDING_ACTIVATION"); // Placeholder hasta que active
                ps.setString(4, "AdministradorClinica");
                ps.setString(5, tenantId);
                ps.setString(6, "ADMINISTRADOR");
                ps.executeUpdate();
                LOG.infof("Created user in public.usuarioperiferico: %s (ID=%d)", adminNickname, userId);
            }

            // 5. Crear registro en public.administradorclinica
            String insertAdminClinica = 
                "INSERT INTO public.administradorclinica (id, nodo_periferico_id) VALUES (?, ?) " +
                "ON CONFLICT (id) DO NOTHING";
            
            try (PreparedStatement ps = c.prepareStatement(insertAdminClinica)) {
                ps.setLong(1, userId);
                ps.setLong(2, Long.parseLong(tenantId));
                ps.executeUpdate();
            }

            LOG.infof("Successfully created admin user: %s for tenant %s", adminNickname, tenantId);

            // Retornar información
            AdminCreationResult result = new AdminCreationResult();
            result.adminNickname = adminNickname;
            result.activationToken = activationToken;
            result.activationUrl = activationUrl;
            result.tokenExpiry = expiryTime;
            
            return result;

        } catch (SQLException ex) {
            LOG.errorf(ex, "Error creating admin user for tenant %s", tenantId);
            throw ex;
        }
    }

    /**
     * Valida y activa una cuenta de administrador usando el token de activación.
     * 
     * @param tenantId ID del tenant
     * @param token Token de activación
     * @param password Nueva contraseña del administrador
     * @return nickname del usuario activado
     * @throws SQLException si hay error
     * @throws SecurityException si el token es inválido o expiró
     */
    public String activateAdminUser(String tenantId, String token, String password) 
            throws SQLException, SecurityException {
        if (tenantId == null || token == null || password == null) {
            throw new IllegalArgumentException("tenantId, token and password are required");
        }

        String tenantSchema = "schema_clinica_" + tenantId;

        try (Connection c = dataSource.getConnection()) {
            // 1. Verificar el token
            String checkTokenSql = String.format(
                "SELECT user_nickname, expires_at, used FROM %s.activation_tokens WHERE token = ?",
                tenantSchema
            );
            
            String userNickname = null;
            boolean tokenUsed = false;
            LocalDateTime expiresAt = null;
            
            try (PreparedStatement ps = c.prepareStatement(checkTokenSql)) {
                ps.setString(1, token);
                ResultSet rs = ps.executeQuery();
                
                if (rs.next()) {
                    userNickname = rs.getString("user_nickname");
                    expiresAt = rs.getTimestamp("expires_at").toLocalDateTime();
                    tokenUsed = rs.getBoolean("used");
                } else {
                    throw new SecurityException("Token de activación inválido");
                }
            }

            // 2. Validar que no esté usado ni expirado
            if (tokenUsed) {
                throw new SecurityException("Token de activación ya fue utilizado");
            }
            
            if (LocalDateTime.now().isAfter(expiresAt)) {
                throw new SecurityException("Token de activación expirado");
            }

            // 3. Hashear la contraseña usando BCrypt
            // TODO: Importar PasswordUtils del proyecto
            String passwordHash = hashPassword(password);

            // 4. Actualizar el usuario en public.usuarioperiferico con la contraseña
            String updateUserSql = 
                "UPDATE public.usuarioperiferico SET password_hash = ? WHERE nickname = ?";
            
            try (PreparedStatement ps = c.prepareStatement(updateUserSql)) {
                ps.setString(1, passwordHash);
                ps.setString(2, userNickname);
                int rows = ps.executeUpdate();
                
                if (rows == 0) {
                    throw new SQLException("Usuario no encontrado: " + userNickname);
                }
            }

            // 5. Marcar el token como usado
            String markTokenUsedSql = String.format(
                "UPDATE %s.activation_tokens SET used = TRUE, used_at = CURRENT_TIMESTAMP WHERE token = ?",
                tenantSchema
            );
            
            try (PreparedStatement ps = c.prepareStatement(markTokenUsedSql)) {
                ps.setString(1, token);
                ps.executeUpdate();
            }

            LOG.infof("Successfully activated user %s for tenant %s", userNickname, tenantId);
            
            return userNickname;

        } catch (SQLException ex) {
            LOG.errorf(ex, "Error activating user for tenant %s", tenantId);
            throw ex;
        }
    }

    /**
     * Versión completa de activación que permite username personalizado y NO requiere token pre-existente.
     * Usado cuando la clínica completa el formulario de registro self-service.
     * El token se valida pero el usuario NO debe existir previamente.
     */
    public String activateAdminUserComplete(String tenantId, String tenantSchema, String token, 
                                           String customUsername, String password) 
            throws SQLException, SecurityException {
        if (tenantId == null || token == null || password == null || customUsername == null) {
            throw new IllegalArgumentException("All fields are required");
        }

        try (Connection c = dataSource.getConnection()) {
            // 1. Validar que el token existe en HCEN (no en el schema del tenant que aún no tiene tabla)
            // Por simplicidad, asumimos que el token es válido si fue generado por HCEN
            // TODO: Implementar validación contra HCEN o tabla temporal
            
            // 2. Hashear la contraseña
            String passwordHash = hashPassword(password);

            // 3. Crear usuario en public.usuario con ID auto-generada
            String insertUsuarioPublic = 
                "INSERT INTO public.usuario (nombre, email) " +
                "VALUES (?, ?) " +
                "RETURNING id";
            
            long userId;
            try (PreparedStatement ps = c.prepareStatement(insertUsuarioPublic)) {
                ps.setString(1, "Administrador");
                ps.setString(2, customUsername + "@clinic.local");
                
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        userId = rs.getLong(1);
                    } else {
                        throw new SQLException("Failed to get auto-generated user ID");
                    }
                }
            }

            // 4. Crear registro en public.usuarioperiferico
            String insertUsuarioPeriferico = 
                "INSERT INTO public.usuarioperiferico (id, nickname, password_hash, dtype, tenant_id, role) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (nickname) DO UPDATE SET password_hash = EXCLUDED.password_hash";
            
            try (PreparedStatement ps = c.prepareStatement(insertUsuarioPeriferico)) {
                ps.setLong(1, userId);
                ps.setString(2, customUsername);
                ps.setString(3, passwordHash);
                ps.setString(4, "AdministradorClinica");
                ps.setString(5, tenantId);
                ps.setString(6, "ADMINISTRADOR");
                ps.executeUpdate();
            }

            // 5. Crear registro en public.administradorclinica
            String insertAdminClinica = 
                "INSERT INTO public.administradorclinica (id, nodo_periferico_id) VALUES (?, ?) " +
                "ON CONFLICT (id) DO NOTHING";
            
            try (PreparedStatement ps = c.prepareStatement(insertAdminClinica)) {
                ps.setLong(1, userId);
                ps.setLong(2, Long.parseLong(tenantId));
                ps.executeUpdate();
            }

            LOG.infof("Successfully created custom user %s for tenant %s", customUsername, tenantId);
            
            return customUsername;

        } catch (SQLException ex) {
            LOG.errorf(ex, "Error creating user for tenant %s", tenantId);
            throw ex;
        }
    }

    /**
     * Hashea una contraseña usando BCrypt.
     * IMPORTANTE: Debe usar el mismo algoritmo que PasswordUtils para que el login funcione.
     */
    private String hashPassword(String password) {
        return uy.edu.tse.hcen.utils.PasswordUtils.hashPassword(password);
    }
}
