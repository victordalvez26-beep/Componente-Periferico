package uy.edu.tse.hcen.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;

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
        LOG.info("🚀 DatabaseInitializer - Checking master tables in public schema...");
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
            
            // 2. Tabla global de usuarios (datos básicos) - ID auto-generada
            String createUsuario = 
                "CREATE TABLE IF NOT EXISTS public.usuario (" +
                "  id BIGSERIAL PRIMARY KEY, " +
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
            
        } catch (Exception e) {
            LOG.error("Error creating public master tables: " + e.getMessage(), e);
            throw new RuntimeException("Failed to initialize master tables", e);
        }
    }
}

