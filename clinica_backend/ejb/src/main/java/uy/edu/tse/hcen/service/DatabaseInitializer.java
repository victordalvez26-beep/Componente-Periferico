package uy.edu.tse.hcen.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import uy.edu.tse.hcen.exceptions.DatabaseInitializationException;

import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Singleton EJB que se ejecuta al inicio del despliegue para crear las tablas maestras
 * del sistema multi-tenant en el schema public.
 */
@Singleton
@Startup
public class DatabaseInitializer {

    private static final Logger LOG = Logger.getLogger(DatabaseInitializer.class);

    @Resource(lookup = "java:/jdbc/MyMainDataSource")
    private DataSource dataSource;

    @PostConstruct
    public void init() {
        LOG.info(" DatabaseInitializer - Checking master tables in public schema...");
        try {
            createMasterTables();
            LOG.info("✅ All master tables initialized successfully");
        } catch (Exception e) {
            LOG.error("❌ Failed to initialize master tables: " + e.getMessage(), e);
        }
    }

    private void createMasterTables() {
        try (Connection c = dataSource.getConnection()) {
            
            // 1. Tabla de nodos periféricos (clínicas registradas en este servidor)
            String createNodoPeriferico = 
                "CREATE TABLE IF NOT EXISTS public.nodoperiferico (" +
                "  id BIGINT PRIMARY KEY, " +
                "  nombre VARCHAR(255), " +
                "  rut VARCHAR(255), " +
                "  schema_name VARCHAR(255), " +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
            
            // 2. Tabla global de usuarios (datos básicos) - ID manual (para compatibilidad con init-db.sql)
            String createUsuario = 
                "CREATE TABLE IF NOT EXISTS public.usuario (" +
                "  id BIGINT PRIMARY KEY, " +
                "  nombre VARCHAR(255) NOT NULL, " +
                "  email VARCHAR(255) NOT NULL" +
                ")";
            
            // 3. Tabla global de usuarios periféricos (autenticación y multi-tenancy)
            String createUsuarioPeriDef = 
                "CREATE TABLE IF NOT EXISTS public.usuarioperiferico (" +
                "  id BIGINT PRIMARY KEY, " +
                "  nickname VARCHAR(255) UNIQUE NOT NULL, " +
                "  password_hash VARCHAR(255), " +
                "  dtype VARCHAR(31) NOT NULL, " +
                "  tenant_id VARCHAR(255), " +
                "  role VARCHAR(50)" +
                ")";
            
            // 4. Tabla de administradores de clínica
            String createAdminClinica = 
                "CREATE TABLE IF NOT EXISTS public.administradorclinica (" +
                "  id BIGINT PRIMARY KEY, " +
                "  nodo_periferico_id BIGINT" +
                ")";
            
            try (PreparedStatement ps1 = c.prepareStatement(createNodoPeriferico)) {
                ps1.execute();
                LOG.info("  ✅ Table public.nodoperiferico ready");
            }
            
            try (PreparedStatement ps2 = c.prepareStatement(createUsuario)) {
                ps2.execute();
                LOG.info("  ✅ Table public.usuario ready");
            }
            
            try (PreparedStatement ps3 = c.prepareStatement(createUsuarioPeriDef)) {
                ps3.execute();
                LOG.info("  ✅ Table public.usuarioperiferico ready");
            }
            
            try (PreparedStatement ps4 = c.prepareStatement(createAdminClinica)) {
                ps4.execute();
                LOG.info("  ✅ Table public.administradorclinica ready");
            }
            
            // Insertar usuario de prueba admin_c1 si no existe
            insertTestUserIfNotExists(c);
            
        } catch (SQLException e) {
            LOG.error("Error creating public master tables: " + e.getMessage(), e);
            throw new DatabaseInitializationException("Failed to initialize master tables", e);
        }
    }
    
    private void insertTestUserIfNotExists(Connection c) throws SQLException {
        // Verificar si el usuario ya existe
        String checkUser = "SELECT COUNT(*) FROM public.usuarioperiferico WHERE nickname = 'admin_c1'";
        try (PreparedStatement ps = c.prepareStatement(checkUser);
             java.sql.ResultSet rs = ps.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) {
                LOG.info("  ✅ Test user admin_c1 already exists");
                return;
            }
        }
        
        // Insertar usuario de prueba usando MERGE (compatible con H2 y PostgreSQL)
        // Hash BCrypt para "password123": $2b$12$i4KLHFvjqcWCJ5kiIapVHuLPiXWftj/ZXIlDStUCRwzkS3bi0mfOO
        
        // Insertar en public.usuario (usar MERGE para H2, o verificar antes)
        String checkUsuario = "SELECT COUNT(*) FROM public.usuario WHERE id = 5001";
        boolean usuarioExists = false;
        try (PreparedStatement ps = c.prepareStatement(checkUsuario);
             java.sql.ResultSet rs = ps.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) {
                usuarioExists = true;
            }
        }
        
        if (!usuarioExists) {
            String insertUsuario = "INSERT INTO public.usuario (id, nombre, email) VALUES (5001, 'Admin Global C1', 'admin.c1@global')";
            try (PreparedStatement ps1 = c.prepareStatement(insertUsuario)) {
                ps1.execute();
            }
        }
        
        // Insertar en public.usuarioperiferico
        String insertUsuarioPeriferico = "INSERT INTO public.usuarioperiferico (id, nickname, password_hash, dtype, tenant_id, role) VALUES (5001, 'admin_c1', '$2b$12$i4KLHFvjqcWCJ5kiIapVHuLPiXWftj/ZXIlDStUCRwzkS3bi0mfOO', 'AdministradorClinica', '101', 'ADMINISTRADOR')";
        try (PreparedStatement ps2 = c.prepareStatement(insertUsuarioPeriferico)) {
            ps2.execute();
            LOG.info("  ✅ Test user admin_c1 created (password: password123)");
        } catch (SQLException e) {
            // Si falla por duplicado, está bien, el usuario ya existe
            if (e.getMessage() != null && (e.getMessage().contains("already exists") || e.getMessage().contains("duplicate"))) {
                LOG.info("  ✅ Test user admin_c1 already exists (duplicate key)");
            } else {
                throw e;
            }
        }
        
        // Insertar en public.administradorclinica
        String checkAdmin = "SELECT COUNT(*) FROM public.administradorclinica WHERE id = 5001";
        boolean adminExists = false;
        try (PreparedStatement ps = c.prepareStatement(checkAdmin);
             java.sql.ResultSet rs = ps.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) {
                adminExists = true;
            }
        }
        
        if (!adminExists) {
            String insertAdminClinica = "INSERT INTO public.administradorclinica (id, nodo_periferico_id) VALUES (5001, 101)";
            try (PreparedStatement ps3 = c.prepareStatement(insertAdminClinica)) {
                ps3.execute();
            } catch (SQLException e) {
                // Si falla por duplicado, está bien
                if (e.getMessage() != null && (e.getMessage().contains("already exists") || e.getMessage().contains("duplicate"))) {
                    // Ignorar
                } else {
                    throw e;
                }
            }
        }
    }
}

