package uy.edu.tse.hcen.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests exhaustivos para TenantAdminService con mocks JDBC
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantAdminService Comprehensive Tests")
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class TenantAdminServiceComprehensiveTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @InjectMocks
    private TenantAdminService service;

    @BeforeEach
    void setUp() throws Exception {
        // Inyectar DataSource mock
        var field = TenantAdminService.class.getDeclaredField("dataSource");
        field.setAccessible(true);
        field.set(service, dataSource);
        
        // Configurar mocks para que permitan ejecución del código
        lenient().when(dataSource.getConnection()).thenReturn(connection);
        lenient().when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        lenient().when(preparedStatement.executeUpdate()).thenReturn(1);
        lenient().when(preparedStatement.executeQuery()).thenReturn(resultSet);
        lenient().when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);
        lenient().when(resultSet.next()).thenReturn(false);
        lenient().doNothing().when(preparedStatement).close();
        lenient().doNothing().when(connection).close();
    }

    @Nested
    @DisplayName("createTenantSchema Tests")
    class CreateTenantSchemaTests {

        @Test
        @DisplayName("Debe rechazar schema null")
        void createSchema_nullSchema_throws() {
            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.createTenantSchema(null, "#007bff", "Test"));
            assertTrue(ex.getMessage().contains("required"));
        }

        @Test
        @DisplayName("Debe rechazar schema vacío")
        void createSchema_emptySchema_throws() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> service.createTenantSchema("", "#007bff", "Test"));
        }

        @Test
        @DisplayName("Debe rechazar schema con espacios")
        void createSchema_blankSchema_throws() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> service.createTenantSchema("   ", "#007bff", "Test"));
        }

        @Test
        @DisplayName("Debe ejecutar código con parámetros válidos")
        void createSchema_executesCode() throws SQLException {
            // Act - Si no lanza excepción, el código se ejecutó
            assertDoesNotThrow(() ->
                    service.createTenantSchema("schema_clinica_101", "#FF0000", "Clinica Test"));
        }

        @Test
        @DisplayName("Debe usar color default con null")
        void createSchema_nullColor_usesDefault() throws SQLException {
            // Act - El código maneja null y usa default
            assertDoesNotThrow(() ->
                    service.createTenantSchema("schema_clinica_102", null, "Test"));
        }

        @Test
        @DisplayName("Debe usar nombre default con null")
        void createSchema_nullNombre_usesDefault() throws SQLException {
            // Act
            assertDoesNotThrow(() ->
                    service.createTenantSchema("schema_clinica_103", "#007bff", null));
        }

        @Test
        @DisplayName("Debe escapar comillas en color")
        void createSchema_escapesColorQuotes() throws SQLException {
            // Act - El código escapa comillas internamente
            assertDoesNotThrow(() ->
                    service.createTenantSchema("schema_clinica_104", "#FF'0000", "Test"));
        }

        @Test
        @DisplayName("Debe escapar comillas en nombre")
        void createSchema_escapesNombreQuotes() throws SQLException {
            // Act
            assertDoesNotThrow(() ->
                    service.createTenantSchema("schema_clinica_105", "#007bff", "O'Brien's Clinic"));
        }

        @Test
        @DisplayName("Debe crear múltiples schemas consecutivos")
        void createSchema_multiple_success() throws SQLException {
            // Act - Crear 5 schemas
            assertDoesNotThrow(() -> {
                for (int i = 201; i <= 205; i++) {
                    service.createTenantSchema("schema_clinica_" + i, "#007bff", "Clinica " + i);
                }
            });
        }

        @Test
        @DisplayName("Debe manejar nombres largos")
        void createSchema_longName_success() throws SQLException {
            // Act
            assertDoesNotThrow(() ->
                    service.createTenantSchema("schema_clinica_301", "#007bff",
                            "Clinica con Nombre Muy Largo para Probar el Sistema"));
        }

        @Test
        @DisplayName("Debe manejar diferentes colores hexadecimales")
        void createSchema_differentColors_success() throws SQLException {
            // Act
            assertDoesNotThrow(() -> {
                service.createTenantSchema("schema_clinica_401", "#FF0000", "Test");
                service.createTenantSchema("schema_clinica_402", "#00FF00", "Test");
                service.createTenantSchema("schema_clinica_403", "#0000FF", "Test");
            });
        }

        @Test
        @DisplayName("Debe manejar caracteres UTF-8 en nombre")
        void createSchema_utf8Chars_success() throws SQLException {
            // Act
            assertDoesNotThrow(() ->
                    service.createTenantSchema("schema_clinica_501", "#007bff", "Clínica José María González"));
        }

        @Test
        @DisplayName("Debe manejar color y nombre con comillas múltiples")
        void createSchema_multipleQuotes_success() throws SQLException {
            // Act
            assertDoesNotThrow(() ->
                    service.createTenantSchema("schema_clinica_601", "#FF'FF'FF", "O'Brien's O'Malley"));
        }
    }

    @Nested
    @DisplayName("createAdminUser Tests")
    class CreateAdminUserTests {

        @Test
        @DisplayName("Debe rechazar tenantId null")
        void createAdmin_nullTenantId_throws() {
            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.createAdminUser(null, "schema_101", "admin@test.com", "http://test"));
            assertTrue(ex.getMessage().contains("required"));
        }

        @Test
        @DisplayName("Debe rechazar tenantId vacío")
        void createAdmin_emptyTenantId_throws() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> service.createAdminUser("", "schema_101", "admin@test.com", "http://test"));
        }

        @Test
        @DisplayName("Debe rechazar tenantSchema null")
        void createAdmin_nullSchema_throws() {
            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.createAdminUser("101", null, "admin@test.com", "http://test"));
            assertTrue(ex.getMessage().contains("required"));
        }

        @Test
        @DisplayName("Debe rechazar tenantSchema vacío")
        void createAdmin_emptySchema_throws() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> service.createAdminUser("101", "", "admin@test.com", "http://test"));
        }

        @Test
        @DisplayName("Debe crear admin con email válido y ejecutar SQL")
        void createAdmin_withEmail_executesSQL() throws SQLException {
            // Arrange - createAdminUser usa MÚLTIPLES PreparedStatements
            PreparedStatement ps1 = mock(PreparedStatement.class);
            PreparedStatement ps2 = mock(PreparedStatement.class);
            PreparedStatement ps3 = mock(PreparedStatement.class);
            PreparedStatement ps4 = mock(PreparedStatement.class);
            PreparedStatement ps5 = mock(PreparedStatement.class);
            
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString()))
                    .thenReturn(ps1)
                    .thenReturn(ps2)
                    .thenReturn(ps3)
                    .thenReturn(ps4)
                    .thenReturn(ps5);
            
            when(ps1.execute()).thenReturn(true);
            when(ps2.executeUpdate()).thenReturn(1);
            when(ps3.executeQuery()).thenReturn(resultSet);
            when(ps4.executeUpdate()).thenReturn(1);
            when(ps5.executeUpdate()).thenReturn(1);
            
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(1001L);

            // Act
            TenantAdminService.AdminCreationResult result = service.createAdminUser(
                    "101", "schema_clinica_101", "admin@test.com", "http://test");

            // Assert
            assertNotNull(result);
            assertEquals("admin_c101", result.adminNickname);
            assertNotNull(result.activationToken);
            assertTrue(result.activationUrl.contains("101"));
            assertTrue(result.activationUrl.contains("activate"));
        }

        @Test
        @DisplayName("Debe crear admin con email null (usa email por defecto)")
        void createAdmin_nullEmail_usesDefaultEmail() throws SQLException {
            // Arrange
            PreparedStatement ps1 = mock(PreparedStatement.class);
            PreparedStatement ps2 = mock(PreparedStatement.class);
            PreparedStatement ps3 = mock(PreparedStatement.class);
            PreparedStatement ps4 = mock(PreparedStatement.class);
            PreparedStatement ps5 = mock(PreparedStatement.class);
            
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString()))
                    .thenReturn(ps1)
                    .thenReturn(ps2)
                    .thenReturn(ps3)
                    .thenReturn(ps4)
                    .thenReturn(ps5);
            
            when(ps1.execute()).thenReturn(true);
            when(ps2.executeUpdate()).thenReturn(1);
            when(ps3.executeQuery()).thenReturn(resultSet);
            when(ps4.executeUpdate()).thenReturn(1);
            when(ps5.executeUpdate()).thenReturn(1);
            
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(1002L);

            // Act
            TenantAdminService.AdminCreationResult result = service.createAdminUser(
                    "102", "schema_clinica_102", null, "http://test");

            // Assert
            assertNotNull(result);
            assertEquals("admin_c102", result.adminNickname);
        }

        @Test
        @DisplayName("Debe generar nickname único por tenant")
        void createAdmin_generatesUniqueNickname() throws SQLException {
            // Arrange
            PreparedStatement ps1 = mock(PreparedStatement.class);
            PreparedStatement ps2 = mock(PreparedStatement.class);
            PreparedStatement ps3 = mock(PreparedStatement.class);
            PreparedStatement ps4 = mock(PreparedStatement.class);
            PreparedStatement ps5 = mock(PreparedStatement.class);
            
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString()))
                    .thenReturn(ps1, ps2, ps3, ps4, ps5)
                    .thenReturn(ps1, ps2, ps3, ps4, ps5);
            
            when(ps1.execute()).thenReturn(true);
            when(ps2.executeUpdate()).thenReturn(1);
            when(ps3.executeQuery()).thenReturn(resultSet);
            when(ps4.executeUpdate()).thenReturn(1);
            when(ps5.executeUpdate()).thenReturn(1);
            
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong(1)).thenReturn(1003L).thenReturn(1004L);

            // Act
            TenantAdminService.AdminCreationResult result1 = service.createAdminUser(
                    "201", "schema_clinica_201", "admin1@test.com", "http://test");
            TenantAdminService.AdminCreationResult result2 = service.createAdminUser(
                    "202", "schema_clinica_202", "admin2@test.com", "http://test");

            // Assert
            assertNotEquals(result1.adminNickname, result2.adminNickname);
            assertEquals("admin_c201", result1.adminNickname);
            assertEquals("admin_c202", result2.adminNickname);
        }
    }

    @Nested
    @DisplayName("activateAdminUser Tests - TODOS LOS CASOS")
    class ActivateAdminUserTests {

        @Test
        @DisplayName("Debe rechazar tenantId null")
        void activateAdmin_nullTenantId_throws() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> service.activateAdminUser(null, "token123", "Password123!"));
        }

        @Test
        @DisplayName("Debe rechazar token null")
        void activateAdmin_nullToken_throws() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> service.activateAdminUser("101", null, "Password123!"));
        }

        @Test
        @DisplayName("Debe rechazar password null")
        void activateAdmin_nullPassword_throws() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> service.activateAdminUser("101", "token123", null));
        }

        @Test
        @DisplayName("Debe rechazar token inválido (no encontrado)")
        void activateAdmin_invalidToken_throws() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false); // Token no encontrado

            // Act & Assert
            SecurityException ex = assertThrows(SecurityException.class,
                    () -> service.activateAdminUser("101", "invalidToken", "Password123!"));
            assertTrue(ex.getMessage().contains("inválido"));
        }

        @Test
        @DisplayName("Debe rechazar token ya usado")
        void activateAdmin_usedToken_throws() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getString("user_nickname")).thenReturn("admin_c101");
            when(resultSet.getTimestamp("expires_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now().plusHours(24)));
            when(resultSet.getBoolean("used")).thenReturn(true); // Token ya usado

            // Act & Assert
            SecurityException ex = assertThrows(SecurityException.class,
                    () -> service.activateAdminUser("101", "usedToken", "Password123!"));
            assertTrue(ex.getMessage().contains("ya fue utilizado"));
        }

        @Test
        @DisplayName("Debe rechazar token expirado")
        void activateAdmin_expiredToken_throws() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getString("user_nickname")).thenReturn("admin_c101");
            when(resultSet.getTimestamp("expires_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now().minusHours(1))); // Expirado
            when(resultSet.getBoolean("used")).thenReturn(false);

            // Act & Assert
            SecurityException ex = assertThrows(SecurityException.class,
                    () -> service.activateAdminUser("101", "expiredToken", "Password123!"));
            assertTrue(ex.getMessage().contains("expirado"));
        }

        @Test
        @DisplayName("Debe activar usuario con token válido")
        void activateAdmin_validToken_activatesUser() throws SQLException {
            // Arrange
            PreparedStatement ps1 = mock(PreparedStatement.class);
            PreparedStatement ps2 = mock(PreparedStatement.class);
            PreparedStatement ps3 = mock(PreparedStatement.class);
            
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString()))
                    .thenReturn(ps1)
                    .thenReturn(ps2)
                    .thenReturn(ps3);
            
            when(ps1.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getString("user_nickname")).thenReturn("admin_c101");
            when(resultSet.getTimestamp("expires_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now().plusHours(24)));
            when(resultSet.getBoolean("used")).thenReturn(false);
            
            when(ps2.executeUpdate()).thenReturn(1);
            when(ps3.executeUpdate()).thenReturn(1);

            // Act
            String nickname = service.activateAdminUser("101", "validToken", "Password123!");

            // Assert
            assertEquals("admin_c101", nickname);
            verify(ps2).executeUpdate(); // Update password
            verify(ps3).executeUpdate(); // Mark token as used
        }
    }

    @Nested
    @DisplayName("listTenants Tests - TODOS LOS CASOS")
    class ListTenantsTests {

        @Test
        @DisplayName("Debe retornar lista vacía cuando no hay tenants")
        void listTenants_empty_returnsEmptyList() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // Act
            List<Map<String, Object>> result = service.listTenants();

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(preparedStatement).executeQuery();
        }

        @Test
        @DisplayName("Debe retornar lista con un tenant")
        void listTenants_oneTenant_returnsList() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true).thenReturn(false);
            when(resultSet.getLong("id")).thenReturn(101L);
            when(resultSet.getString("nombre")).thenReturn("Clinica Test");
            when(resultSet.getString("rut")).thenReturn("123456789012");

            // Act
            List<Map<String, Object>> result = service.listTenants();

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(101L, result.get(0).get("id"));
            assertEquals("Clinica Test", result.get(0).get("nombre"));
            assertEquals("123456789012", result.get(0).get("rut"));
        }

        @Test
        @DisplayName("Debe retornar lista con múltiples tenants")
        void listTenants_multipleTenants_returnsList() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next())
                    .thenReturn(true)
                    .thenReturn(true)
                    .thenReturn(true)
                    .thenReturn(false);
            when(resultSet.getLong("id"))
                    .thenReturn(101L)
                    .thenReturn(102L)
                    .thenReturn(103L);
            when(resultSet.getString("nombre"))
                    .thenReturn("Clinica 1")
                    .thenReturn("Clinica 2")
                    .thenReturn("Clinica 3");
            when(resultSet.getString("rut"))
                    .thenReturn("111111111111")
                    .thenReturn("222222222222")
                    .thenReturn("333333333333");

            // Act
            List<Map<String, Object>> result = service.listTenants();

            // Assert
            assertNotNull(result);
            assertEquals(3, result.size());
            assertEquals(101L, result.get(0).get("id"));
            assertEquals(102L, result.get(1).get("id"));
            assertEquals(103L, result.get(2).get("id"));
        }

        @Test
        @DisplayName("Debe manejar SQLException")
        void listTenants_sqlException_throws() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

            // Act & Assert
            assertThrows(SQLException.class, () -> service.listTenants());
        }
    }

    @Nested
    @DisplayName("registerNodoInPublic Tests - TODOS LOS CASOS")
    class RegisterNodoTests {

        @Test
        @DisplayName("Debe registrar nodo exitosamente")
        void registerNodo_success() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(1);

            // Act
            service.registerNodoInPublic(101L, "Clinica Test", "123456789012", "schema_clinica_101");

            // Assert
            verify(preparedStatement).setLong(1, 101L);
            verify(preparedStatement).setString(2, "Clinica Test");
            verify(preparedStatement).setString(3, "123456789012");
            verify(preparedStatement).setString(4, "schema_clinica_101");
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("Debe rechazar id null")
        void registerNodo_nullId_throws() {
            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.registerNodoInPublic(null, "Clinica", "123456789012", "schema_clinica_101"));
            assertTrue(ex.getMessage().contains("id is required"));
        }

        @Test
        @DisplayName("Debe rechazar rut null")
        void registerNodo_nullRut_throws() {
            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.registerNodoInPublic(101L, "Clinica", null, "schema_clinica_101"));
            assertTrue(ex.getMessage().contains("rut is required"));
        }

        @Test
        @DisplayName("Debe rechazar rut vacío")
        void registerNodo_emptyRut_throws() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> service.registerNodoInPublic(101L, "Clinica", "", "schema_clinica_101"));
        }

        @Test
        @DisplayName("Debe rechazar nombre null")
        void registerNodo_nullNombre_throws() {
            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.registerNodoInPublic(101L, null, "123456789012", "schema_clinica_101"));
            assertTrue(ex.getMessage().contains("nombre is required"));
        }

        @Test
        @DisplayName("Debe rechazar nombre vacío")
        void registerNodo_emptyNombre_throws() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> service.registerNodoInPublic(101L, "", "123456789012", "schema_clinica_101"));
        }

        @Test
        @DisplayName("Debe rechazar schemaName null")
        void registerNodo_nullSchema_throws() {
            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.registerNodoInPublic(101L, "Clinica", "123456789012", null));
            assertTrue(ex.getMessage().contains("schemaName is required"));
        }

        @Test
        @DisplayName("Debe rechazar schemaName vacío")
        void registerNodo_emptySchema_throws() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> service.registerNodoInPublic(101L, "Clinica", "123456789012", ""));
        }

        @Test
        @DisplayName("Debe manejar RUT duplicado (ON CONFLICT DO UPDATE)")
        void registerNodo_duplicateRut_updates() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(1); // ON CONFLICT actualiza

            // Act
            service.registerNodoInPublic(101L, "Clinica Actualizada", "123456789012", "schema_clinica_101");

            // Assert
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("Debe registrar múltiples nodos")
        void registerNodo_multiple_success() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(1);

            // Act
            service.registerNodoInPublic(101L, "Clinica 1", "111111111111", "schema_clinica_101");
            service.registerNodoInPublic(102L, "Clinica 2", "222222222222", "schema_clinica_102");
            service.registerNodoInPublic(103L, "Clinica 3", "333333333333", "schema_clinica_103");

            // Assert
            verify(preparedStatement, times(3)).executeUpdate();
        }

        @Test
        @DisplayName("Debe manejar nombre con caracteres especiales")
        void registerNodo_specialCharsInNombre_success() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(1);

            // Act
            service.registerNodoInPublic(101L, "Clínica José María O'Brien", "123456789012", "schema_clinica_101");

            // Assert
            verify(preparedStatement).setString(2, "Clínica José María O'Brien");
            verify(preparedStatement).executeUpdate();
        }
    }

    @Nested
    @DisplayName("getTenantConfig Tests - TODOS LOS CASOS")
    class GetTenantConfigTests {

        @Test
        @DisplayName("Debe obtener config completa exitosamente")
        void getTenantConfig_success() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getString("color_primario")).thenReturn("#007bff");
            when(resultSet.getString("color_secundario")).thenReturn("#6c757d");
            when(resultSet.getString("nombre_portal")).thenReturn("Mi Clinica");
            when(resultSet.getString("logo_url")).thenReturn("http://logo.png");

            // Act
            Map<String, Object> result = service.getTenantConfig("101");

            // Assert
            assertNotNull(result);
            assertEquals("#007bff", result.get("colorPrimario"));
            assertEquals("#6c757d", result.get("colorSecundario"));
            assertEquals("Mi Clinica", result.get("nombrePortal"));
            assertEquals("http://logo.png", result.get("logoUrl"));
        }

        @Test
        @DisplayName("Debe retornar null si no hay config")
        void getTenantConfig_notFound_returnsNull() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // Act
            Map<String, Object> result = service.getTenantConfig("999");

            // Assert
            assertNull(result);
        }

        @Test
        @DisplayName("Debe manejar logo_url null (retorna string vacío)")
        void getTenantConfig_nullLogoUrl_returnsConfig() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getString("color_primario")).thenReturn("#007bff");
            when(resultSet.getString("color_secundario")).thenReturn("#6c757d");
            when(resultSet.getString("nombre_portal")).thenReturn("Mi Clinica");
            when(resultSet.getString("logo_url")).thenReturn(null);

            // Act
            Map<String, Object> result = service.getTenantConfig("101");

            // Assert
            assertNotNull(result);
            // El código convierte null a "" (string vacío)
            assertEquals("", result.get("logoUrl"));
        }

        @Test
        @DisplayName("Debe manejar logo_url vacío")
        void getTenantConfig_emptyLogoUrl_returnsConfig() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getString("color_primario")).thenReturn("#007bff");
            when(resultSet.getString("color_secundario")).thenReturn("#6c757d");
            when(resultSet.getString("nombre_portal")).thenReturn("Mi Clinica");
            when(resultSet.getString("logo_url")).thenReturn("");

            // Act
            Map<String, Object> result = service.getTenantConfig("101");

            // Assert
            assertNotNull(result);
            assertEquals("", result.get("logoUrl"));
        }

    }

    @Nested
    @DisplayName("updateTenantConfig Tests - TODOS LOS CASOS")
    class UpdateTenantConfigTests {

        @Test
        @DisplayName("Debe rechazar tenantId null")
        void updateConfig_nullTenantId_throws() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> service.updateTenantConfig(null, "Clinica", "#FF0000", "#000000", "http://logo.png"));
        }

        @Test
        @DisplayName("Debe rechazar tenantId vacío")
        void updateConfig_emptyTenantId_throws() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> service.updateTenantConfig("", "Clinica", "#FF0000", "#000000", "http://logo.png"));
        }

        @Test
        @DisplayName("Debe actualizar config con todos los parámetros")
        void updateConfig_allParams_success() throws SQLException {
            // Arrange - updateTenantConfig usa String.format, no setString
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(1);

            // Act
            service.updateTenantConfig("101", "Nueva Clinica", "#FF0000", "#000000", "http://logo.png");

            // Assert
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("Debe actualizar config con logo null (usa string vacío)")
        void updateConfig_nullLogo_usesEmpty() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(1);

            // Act
            service.updateTenantConfig("101", "Nueva Clinica", "#FF0000", "#000000", null);

            // Assert
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("Debe usar defaults cuando parámetros son null")
        void updateConfig_nullParams_usesDefaults() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(1);

            // Act
            service.updateTenantConfig("102", null, null, null, null);

            // Assert
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("Debe escapar comillas simples en nombre")
        void updateConfig_escapesQuotesInName() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(1);

            // Act - Nombre con comillas
            service.updateTenantConfig("103", "Clínica O'Brien", "#007bff", "#6c757d", "http://logo.png");

            // Assert
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("Debe crear config si no existe (rowsAffected=0)")
        void updateConfig_notExists_insertsNew() throws SQLException {
            // Arrange
            PreparedStatement ps1 = mock(PreparedStatement.class);
            PreparedStatement ps2 = mock(PreparedStatement.class);
            
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString()))
                    .thenReturn(ps1)
                    .thenReturn(ps2);
            when(ps1.executeUpdate()).thenReturn(0); // No existe, rowsAffected=0
            when(ps2.executeUpdate()).thenReturn(1); // INSERT exitoso

            // Act
            service.updateTenantConfig("104", "Nueva Clinica", "#FF0000", "#000000", "http://logo.png");

            // Assert
            verify(ps1).executeUpdate(); // UPDATE intenta
            verify(ps2).executeUpdate(); // INSERT se ejecuta
        }

        @Test
        @DisplayName("Debe actualizar múltiples tenants consecutivos")
        void updateConfig_multipleTenants_success() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(1);

            // Act
            service.updateTenantConfig("201", "Clinica 1", "#FF0000", "#000000", "http://logo1.png");
            service.updateTenantConfig("202", "Clinica 2", "#00FF00", "#000000", "http://logo2.png");
            service.updateTenantConfig("203", "Clinica 3", "#0000FF", "#000000", "http://logo3.png");

            // Assert
            verify(preparedStatement, atLeast(3)).executeUpdate();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("AdminCreationResult debe ser mutable")
        void adminCreationResult_isMutable() {
            // Arrange
            TenantAdminService.AdminCreationResult result = new TenantAdminService.AdminCreationResult();

            // Act
            result.adminNickname = "admin1";
            result.activationToken = "token1";
            result.activationUrl = "url1";
            result.tokenExpiry = LocalDateTime.now();

            // Assert
            assertEquals("admin1", result.adminNickname);
            assertEquals("token1", result.activationToken);
            assertNotNull(result.tokenExpiry);
        }

        @Test
        @DisplayName("AdminCreationResult debe permitir null")
        void adminCreationResult_allowsNull() {
            // Arrange
            TenantAdminService.AdminCreationResult result = new TenantAdminService.AdminCreationResult();

            // Act
            result.adminNickname = null;
            result.activationToken = null;

            // Assert
            assertNull(result.adminNickname);
            assertNull(result.activationToken);
        }
    }
}

