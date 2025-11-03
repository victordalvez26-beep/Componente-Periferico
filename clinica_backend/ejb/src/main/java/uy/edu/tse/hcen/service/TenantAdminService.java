package uy.edu.tse.hcen.service;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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

        String createUsuario = String.format(
            "CREATE TABLE IF NOT EXISTS %s.usuario (id BIGINT PRIMARY KEY, nombre VARCHAR(255) NOT NULL, email VARCHAR(255) NOT NULL);",
            tenantSchema);

        String createUsuPer = String.format(
            "CREATE TABLE IF NOT EXISTS %s.usuarioperiferico (id BIGINT PRIMARY KEY, nickname VARCHAR(255) UNIQUE NOT NULL, password_hash VARCHAR(255) NOT NULL, dtype VARCHAR(31) NOT NULL);",
            tenantSchema);

        String createNodo = String.format(
            "CREATE TABLE IF NOT EXISTS %s.nodoperiferico (id BIGINT PRIMARY KEY, nombre VARCHAR(255), rut VARCHAR(255));",
            tenantSchema);

        try (Connection c = dataSource.getConnection()) {
            try (PreparedStatement s1 = c.prepareStatement(createSchema)) {
                s1.execute();
            }

            try (PreparedStatement s2 = c.prepareStatement(createPortal)) {
                s2.execute();
            }

            try (PreparedStatement s3 = c.prepareStatement(insertPortal)) {
                s3.execute();
            }

            try (PreparedStatement s4 = c.prepareStatement(createUsuario)) { s4.execute(); }
            try (PreparedStatement s5 = c.prepareStatement(createUsuPer)) { s5.execute(); }
            try (PreparedStatement s6 = c.prepareStatement(createNodo)) { s6.execute(); }

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
    public void registerNodoInPublic(Long id, String nombre, String rut) throws SQLException {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        if (rut == null || rut.isBlank()) {
            throw new IllegalArgumentException("rut is required");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("nombre is required");
        }

        String sql = "INSERT INTO public.nodoperiferico (id, nombre, rut) VALUES (?, ?, ?) " +
                     "ON CONFLICT (id) DO UPDATE SET nombre = EXCLUDED.nombre, rut = EXCLUDED.rut";
        
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setString(2, nombre);
            ps.setString(3, rut);
            int rowsAffected = ps.executeUpdate();
            
            LOG.infof("Registered nodo in public.nodoperiferico: id=%s, nombre=%s, rut=%s (rows affected: %d)", 
                      id, nombre, rut, rowsAffected);
        } catch (SQLException ex) {
            LOG.errorf(ex, "Error registering nodo in public schema: id=%s, rut=%s", id, rut);
            throw ex;
        }
    }
}
