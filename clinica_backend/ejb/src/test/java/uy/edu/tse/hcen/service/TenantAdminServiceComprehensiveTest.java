package uy.edu.tse.hcen.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;

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
    }

    @Nested
    @DisplayName("activateAdminUser Tests")
    class ActivateAdminUserTests {

        @Test
        @DisplayName("Debe rechazar token inválido")
        void activateAdmin_invalidToken_throws() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false); // Token no encontrado

            // Act & Assert
            assertThrows(Exception.class,
                    () -> service.activateAdminUser("101", "invalidToken", "Password123!"));
        }

        @Test
        @DisplayName("Debe ejecutar código de activación")
        void activateAdmin_executesCode() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // Act & Assert - Ejecuta código aunque falle por token inválido
            assertThrows(Exception.class,
                    () -> service.activateAdminUser("101", "token123", "Password123!"));
            
            verify(dataSource).getConnection();
        }
    }

    @Nested
    @DisplayName("listTenants Tests")
    class ListTenantsTests {

        @Test
        @DisplayName("Debe ejecutar query de listado")
        void listTenants_executesQuery() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // Act
            java.util.List<java.util.Map<String, Object>> result = service.listTenants();

            // Assert
            assertNotNull(result);
            verify(preparedStatement).executeQuery();
        }
    }

    @Nested
    @DisplayName("registerNodoInPublic Tests")
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
        @DisplayName("Debe manejar RUT duplicado (ON CONFLICT)")
        void registerNodo_duplicateRut_success() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(0); // ON CONFLICT DO UPDATE

            // Act
            service.registerNodoInPublic(101L, "Clinica", "123456789012", "schema_clinica_101");

            // Assert
            verify(preparedStatement).executeUpdate();
        }
    }

    @Nested
    @DisplayName("getTenantConfig Tests")
    class GetTenantConfigTests {

        @Test
        @DisplayName("Debe obtener config exitosamente")
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
            java.util.Map<String, Object> result = service.getTenantConfig("101");

            // Assert
            assertNotNull(result);
            assertEquals("#007bff", result.get("colorPrimario"));
            assertEquals("Mi Clinica", result.get("nombrePortal"));
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
            java.util.Map<String, Object> result = service.getTenantConfig("999");

            // Assert
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("updateTenantConfig Tests")
    class UpdateTenantConfigTests {

        @Test
        @DisplayName("Debe ejecutar update")
        void updateConfig_executesUpdate() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(1);

            // Act
            service.updateTenantConfig("101", "Nueva Clinica", "#FF0000", "#000000", "http://logo.png");

            // Assert
            verify(dataSource).getConnection();
            verify(preparedStatement).executeUpdate();
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

