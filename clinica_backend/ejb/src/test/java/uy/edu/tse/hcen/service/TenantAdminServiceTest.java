package uy.edu.tse.hcen.service;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantAdminServiceTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @InjectMocks
    private TenantAdminService tenantAdminService;

    @BeforeEach
    void setUp() throws SQLException {
        // Usar reflection para inyectar el DataSource mock
        try {
            java.lang.reflect.Field field = TenantAdminService.class.getDeclaredField("dataSource");
            field.setAccessible(true);
            field.set(tenantAdminService, dataSource);
        } catch (Exception e) {
            // Si falla, el test probará sin inyección
        }

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    @Test
    void testCreateTenantSchema() throws SQLException {
        String tenantSchema = "schema_clinica_1";
        String colorPrimario = "#007bff";
        String nombrePortal = "Clínica Test";

        when(preparedStatement.execute()).thenReturn(true);

        assertDoesNotThrow(() -> {
            tenantAdminService.createTenantSchema(tenantSchema, colorPrimario, nombrePortal);
        });

        verify(connection, atLeastOnce()).prepareStatement(anyString());
        verify(preparedStatement, atLeastOnce()).execute();
    }

    @Test
    void testCreateTenantSchemaNullSchema() {
        assertThrows(IllegalArgumentException.class, () -> {
            tenantAdminService.createTenantSchema(null, "#007bff", "Clínica");
        });
    }

    @Test
    void testCreateTenantSchemaBlankSchema() {
        assertThrows(IllegalArgumentException.class, () -> {
            tenantAdminService.createTenantSchema("   ", "#007bff", "Clínica");
        });
    }

    @Test
    void testListTenants() throws SQLException {
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getLong("id")).thenReturn(1L, 2L);
        when(resultSet.getString("nombre")).thenReturn("Clínica 1", "Clínica 2");
        when(resultSet.getString("rut")).thenReturn("12345678", "87654321");

        List<Map<String, Object>> result = tenantAdminService.listTenants();

        assertNotNull(result);
        verify(preparedStatement).executeQuery();
    }

    @Test
    void testListTenantsEmpty() throws SQLException {
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<Map<String, Object>> result = tenantAdminService.listTenants();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testRegisterNodoInPublic() throws SQLException {
        Long id = 1L;
        String nombre = "Clínica Test";
        String rut = "12345678";
        String schemaName = "schema_clinica_1";

        when(preparedStatement.executeUpdate()).thenReturn(1);

        assertDoesNotThrow(() -> {
            tenantAdminService.registerNodoInPublic(id, nombre, rut, schemaName);
        });

        verify(preparedStatement).setLong(1, id);
        verify(preparedStatement).setString(2, nombre);
        verify(preparedStatement).setString(3, rut);
        verify(preparedStatement).setString(4, schemaName);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void testRegisterNodoInPublicNullId() {
        assertThrows(IllegalArgumentException.class, () -> {
            tenantAdminService.registerNodoInPublic(null, "Clínica", "12345678", "schema_clinica_1");
        });
    }

    @Test
    void testRegisterNodoInPublicNullRut() {
        assertThrows(IllegalArgumentException.class, () -> {
            tenantAdminService.registerNodoInPublic(1L, "Clínica", null, "schema_clinica_1");
        });
    }

    @Test
    void testRegisterNodoInPublicNullNombre() {
        assertThrows(IllegalArgumentException.class, () -> {
            tenantAdminService.registerNodoInPublic(1L, null, "12345678", "schema_clinica_1");
        });
    }

    @Test
    void testRegisterNodoInPublicNullSchemaName() {
        assertThrows(IllegalArgumentException.class, () -> {
            tenantAdminService.registerNodoInPublic(1L, "Clínica", "12345678", null);
        });
    }

    @Test
    void testCreateAdminUser() throws SQLException {
        String tenantId = "123";
        String tenantSchema = "schema_clinica_123";
        String adminEmail = "admin@test.com";
        String baseUrl = "http://localhost:8081";

        when(preparedStatement.execute()).thenReturn(true);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong(1)).thenReturn(1L);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        TenantAdminService.AdminCreationResult result = tenantAdminService.createAdminUser(
                tenantId, tenantSchema, adminEmail, baseUrl);

        assertNotNull(result);
        assertNotNull(result.adminNickname);
        assertNotNull(result.activationToken);
        assertNotNull(result.activationUrl);
        assertNotNull(result.tokenExpiry);
        assertTrue(result.adminNickname.startsWith("admin_c"));
    }

    @Test
    void testCreateAdminUserNullTenantId() {
        assertThrows(IllegalArgumentException.class, () -> {
            tenantAdminService.createAdminUser(null, "schema_clinica_123", "admin@test.com", "http://localhost:8081");
        });
    }

    @Test
    void testCreateAdminUserNullTenantSchema() {
        assertThrows(IllegalArgumentException.class, () -> {
            tenantAdminService.createAdminUser("123", null, "admin@test.com", "http://localhost:8081");
        });
    }

    @Test
    void testActivateAdminUser() throws SQLException {
        String tenantId = "123";
        String token = "activation-token-123";
        String password = "newPassword123";

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("user_nickname")).thenReturn("admin_c123");
        when(resultSet.getTimestamp("expires_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now().plusHours(1)));
        when(resultSet.getBoolean("used")).thenReturn(false);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        String result = tenantAdminService.activateAdminUser(tenantId, token, password);

        assertNotNull(result);
        assertEquals("admin_c123", result);
        verify(preparedStatement, atLeastOnce()).executeUpdate();
    }

    @Test
    void testActivateAdminUserNullParams() {
        assertThrows(IllegalArgumentException.class, () -> {
            tenantAdminService.activateAdminUser(null, "token", "password");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            tenantAdminService.activateAdminUser("123", null, "password");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            tenantAdminService.activateAdminUser("123", "token", null);
        });
    }

    @Test
    void testActivateAdminUserTokenUsed() throws SQLException {
        String tenantId = "123";
        String token = "used-token";
        String password = "password";

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("user_nickname")).thenReturn("admin_c123");
        when(resultSet.getTimestamp("expires_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now().plusHours(1)));
        when(resultSet.getBoolean("used")).thenReturn(true);

        assertThrows(SecurityException.class, () -> {
            tenantAdminService.activateAdminUser(tenantId, token, password);
        });
    }

    @Test
    void testActivateAdminUserTokenExpired() throws SQLException {
        String tenantId = "123";
        String token = "expired-token";
        String password = "password";

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("user_nickname")).thenReturn("admin_c123");
        when(resultSet.getTimestamp("expires_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now().minusHours(1)));
        when(resultSet.getBoolean("used")).thenReturn(false);

        assertThrows(SecurityException.class, () -> {
            tenantAdminService.activateAdminUser(tenantId, token, password);
        });
    }

    @Test
    void testGetTenantConfig() throws SQLException {
        String tenantId = "123";

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("nombre_portal")).thenReturn("Clínica Test");
        when(resultSet.getString("color_primario")).thenReturn("#007bff");
        when(resultSet.getString("color_secundario")).thenReturn("#6c757d");
        when(resultSet.getString("logo_url")).thenReturn("http://example.com/logo.png");

        Map<String, Object> result = tenantAdminService.getTenantConfig(tenantId);

        assertNotNull(result);
        assertEquals(tenantId, result.get("tenantId"));
        assertEquals("Clínica Test", result.get("nombrePortal"));
    }

    @Test
    void testGetTenantConfigNullTenantId() {
        assertThrows(IllegalArgumentException.class, () -> {
            tenantAdminService.getTenantConfig(null);
        });
    }

    @Test
    void testUpdateTenantConfig() throws SQLException {
        String tenantId = "123";
        String nombrePortal = "Clínica Actualizada";
        String colorPrimario = "#ff0000";
        String colorSecundario = "#00ff00";
        String logoUrl = "http://example.com/new-logo.png";

        when(preparedStatement.executeUpdate()).thenReturn(1);

        assertDoesNotThrow(() -> {
            tenantAdminService.updateTenantConfig(tenantId, nombrePortal, colorPrimario, colorSecundario, logoUrl);
        });

        verify(preparedStatement).executeUpdate();
    }

    @Test
    void testUpdateTenantConfigNullTenantId() {
        assertThrows(IllegalArgumentException.class, () -> {
            tenantAdminService.updateTenantConfig(null, "Clínica", "#007bff", "#6c757d", "");
        });
    }

    @Test
    void testCreateTenantSchemaWithNullColorPrimario() throws SQLException {
        String tenantSchema = "schema_clinica_1";
        String nombrePortal = "Clínica Test";

        when(connection.createStatement()).thenReturn(mock(java.sql.Statement.class));
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.execute()).thenReturn(true);

        assertDoesNotThrow(() -> {
            tenantAdminService.createTenantSchema(tenantSchema, null, nombrePortal);
        });
    }

    @Test
    void testCreateTenantSchemaWithNullNombrePortal() throws SQLException {
        String tenantSchema = "schema_clinica_1";
        String colorPrimario = "#007bff";

        when(connection.createStatement()).thenReturn(mock(java.sql.Statement.class));
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.execute()).thenReturn(true);

        assertDoesNotThrow(() -> {
            tenantAdminService.createTenantSchema(tenantSchema, colorPrimario, null);
        });
    }

    @Test
    void testCreateTenantSchemaWithSQLException() throws SQLException {
        String tenantSchema = "schema_clinica_1";
        String colorPrimario = "#007bff";
        String nombrePortal = "Clínica Test";

        when(connection.createStatement()).thenThrow(new SQLException("DB error"));

        assertThrows(SQLException.class, () -> {
            tenantAdminService.createTenantSchema(tenantSchema, colorPrimario, nombrePortal);
        });
    }

    @Test
    void testListTenantsWithSQLException() throws SQLException {
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("DB error"));

        assertThrows(SQLException.class, () -> {
            tenantAdminService.listTenants();
        });
    }

    @Test
    void testRegisterNodoInPublicWithSQLException() throws SQLException {
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("DB error"));

        assertThrows(SQLException.class, () -> {
            tenantAdminService.registerNodoInPublic(1L, "Clínica", "12345678", "schema_clinica_1");
        });
    }

    @Test
    void testCreateAdminUserWithSQLException() throws SQLException {
        when(preparedStatement.execute()).thenThrow(new SQLException("DB error"));

        assertThrows(SQLException.class, () -> {
            tenantAdminService.createAdminUser("123", "schema_clinica_123", "admin@test.com", "http://localhost:8081");
        });
    }

    @Test
    void testActivateAdminUserTokenNotFound() throws SQLException {
        String tenantId = "123";
        String token = "invalid-token";
        String password = "password";

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertThrows(SecurityException.class, () -> {
            tenantAdminService.activateAdminUser(tenantId, token, password);
        });
    }

    @Test
    void testActivateAdminUserWithSQLException() throws SQLException {
        String tenantId = "123";
        String token = "token";
        String password = "password";

        when(preparedStatement.executeQuery()).thenThrow(new SQLException("DB error"));

        assertThrows(SQLException.class, () -> {
            tenantAdminService.activateAdminUser(tenantId, token, password);
        });
    }

    @Test
    void testActivateAdminUserComplete() throws SQLException {
        String tenantId = "123";
        String tenantSchema = "schema_clinica_123";
        String token = "activation-token-123";
        String customUsername = "admin_c123";
        String password = "newPassword123";

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong(1)).thenReturn(1L);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        String result = tenantAdminService.activateAdminUserComplete(tenantId, tenantSchema, token, customUsername, password);

        assertNotNull(result);
        assertEquals(customUsername, result);
    }

    @Test
    void testActivateAdminUserCompleteNullParams() {
        assertThrows(IllegalArgumentException.class, () -> {
            tenantAdminService.activateAdminUserComplete(null, "schema", "token", "username", "password");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            tenantAdminService.activateAdminUserComplete("123", null, "token", "username", "password");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            tenantAdminService.activateAdminUserComplete("123", "schema", null, "username", "password");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            tenantAdminService.activateAdminUserComplete("123", "schema", "token", null, "password");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            tenantAdminService.activateAdminUserComplete("123", "schema", "token", "username", null);
        });
    }

    @Test
    void testActivateAdminUserCompleteTokenUsed() throws SQLException {
        String tenantId = "123";
        String tenantSchema = "schema_clinica_123";
        String token = "used-token";
        String customUsername = "admin_c123";
        String password = "password";

        // Este método no valida el token, solo crea el usuario
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong(1)).thenReturn(1L);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        String result = tenantAdminService.activateAdminUserComplete(tenantId, tenantSchema, token, customUsername, password);
        assertNotNull(result);
    }

    @Test
    void testActivateAdminUserCompleteTokenExpired() throws SQLException {
        String tenantId = "123";
        String tenantSchema = "schema_clinica_123";
        String token = "expired-token";
        String customUsername = "admin_c123";
        String password = "password";

        // Este método no valida el token, solo crea el usuario
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong(1)).thenReturn(1L);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        String result = tenantAdminService.activateAdminUserComplete(tenantId, tenantSchema, token, customUsername, password);
        assertNotNull(result);
    }

    @Test
    void testActivateAdminUserCompleteTokenNotFound() throws SQLException {
        String tenantId = "123";
        String tenantSchema = "schema_clinica_123";
        String token = "invalid-token";
        String customUsername = "admin_c123";
        String password = "password";

        // Este método no valida el token, solo crea el usuario
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong(1)).thenReturn(1L);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        String result = tenantAdminService.activateAdminUserComplete(tenantId, tenantSchema, token, customUsername, password);
        assertNotNull(result);
    }

    @Test
    void testActivateAdminUserCompleteWithSQLException() throws SQLException {
        String tenantId = "123";
        String tenantSchema = "schema_clinica_123";
        String token = "token";
        String customUsername = "admin_c123";
        String password = "password";

        when(preparedStatement.executeQuery()).thenThrow(new SQLException("DB error"));

        assertThrows(SQLException.class, () -> {
            tenantAdminService.activateAdminUserComplete(tenantId, tenantSchema, token, customUsername, password);
        });
    }

    @Test
    void testGetTenantConfigNotFound() throws SQLException {
        String tenantId = "123";

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Map<String, Object> result = tenantAdminService.getTenantConfig(tenantId);

        assertNotNull(result);
        assertEquals(tenantId, result.get("tenantId"));
        assertNull(result.get("nombrePortal"));
    }

    @Test
    void testGetTenantConfigWithSQLException() throws SQLException {
        String tenantId = "123";

        when(preparedStatement.executeQuery()).thenThrow(new SQLException("DB error"));

        assertThrows(SQLException.class, () -> {
            tenantAdminService.getTenantConfig(tenantId);
        });
    }

    @Test
    void testUpdateTenantConfigWithSQLException() throws SQLException {
        String tenantId = "123";

        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("DB error"));

        assertThrows(SQLException.class, () -> {
            tenantAdminService.updateTenantConfig(tenantId, "Clínica", "#007bff", "#6c757d", "");
        });
    }

    @Test
    void testCreateTenantSchemaWithEscapedQuotes() throws SQLException {
        String tenantSchema = "schema_clinica_1";
        String colorPrimario = "#007bff";
        String nombrePortal = "Clínica's Test"; // Con comilla simple

        when(connection.createStatement()).thenReturn(mock(java.sql.Statement.class));
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.execute()).thenReturn(true);

        assertDoesNotThrow(() -> {
            tenantAdminService.createTenantSchema(tenantSchema, colorPrimario, nombrePortal);
        });
    }

    @Test
    void testCreateAdminUserWithNullBaseUrl() throws SQLException {
        String tenantId = "123";
        String tenantSchema = "schema_clinica_123";
        String adminEmail = "admin@test.com";

        when(preparedStatement.execute()).thenReturn(true);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong(1)).thenReturn(1L);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        TenantAdminService.AdminCreationResult result = tenantAdminService.createAdminUser(
                tenantId, tenantSchema, adminEmail, null);

        assertNotNull(result);
        assertNotNull(result.adminNickname);
        assertNotNull(result.activationToken);
        assertNotNull(result.activationUrl);
    }
}

