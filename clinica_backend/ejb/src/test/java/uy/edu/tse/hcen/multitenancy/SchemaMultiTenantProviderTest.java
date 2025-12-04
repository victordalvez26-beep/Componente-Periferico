package uy.edu.tse.hcen.multitenancy;

import org.hibernate.HibernateException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests exhaustivos para SchemaMultiTenantProvider
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SchemaMultiTenantProvider Tests")
class SchemaMultiTenantProviderTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    private SchemaMultiTenantProvider provider;

    @BeforeEach
    void setUp() throws SQLException {
        provider = new SchemaMultiTenantProvider();
        
        // Usar reflection para inyectar el mock del DataSource
        try {
            var field = SchemaMultiTenantProvider.class.getDeclaredField("dataSource");
            field.setAccessible(true);
            field.set(provider, dataSource);
        } catch (Exception e) {
            fail("Error setting up test: " + e.getMessage());
        }
    }

    @Nested
    @DisplayName("Get Any Connection Tests")
    class GetAnyConnectionTests {

        @Test
        @DisplayName("Debe obtener conexión del DataSource")
        void getAnyConnection_shouldObtainConnection() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenReturn(connection);

            // Act
            Connection result = provider.getAnyConnection();

            // Assert
            assertNotNull(result);
            assertEquals(connection, result);
            verify(dataSource).getConnection();
        }

        @Test
        @DisplayName("Debe lanzar SQLException si DataSource falla")
        void getAnyConnection_shouldThrowExceptionOnFailure() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

            // Act & Assert
            assertThrows(SQLException.class, () -> provider.getAnyConnection());
        }
    }

    @Nested
    @DisplayName("Release Any Connection Tests")
    class ReleaseAnyConnectionTests {

        @Test
        @DisplayName("Debe cerrar la conexión")
        void releaseAnyConnection_shouldCloseConnection() throws SQLException {
            // Act
            provider.releaseAnyConnection(connection);

            // Assert
            verify(connection).close();
        }

        @Test
        @DisplayName("Debe propagar SQLException al cerrar")
        void releaseAnyConnection_shouldPropagateException() throws SQLException {
            // Arrange
            doThrow(new SQLException("Close failed")).when(connection).close();

            // Act & Assert
            assertThrows(SQLException.class, () -> provider.releaseAnyConnection(connection));
        }
    }

    @Nested
    @DisplayName("Get Connection with Tenant Tests")
    class GetConnectionWithTenantTests {

        @Test
        @DisplayName("Debe establecer search_path para tenant")
        void getConnection_withTenant_shouldSetSearchPath() throws SQLException {
            // Arrange
            String tenantSchema = "schema_clinica_101";
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.execute(anyString())).thenReturn(true);

            // Act
            Connection result = provider.getConnection(tenantSchema);

            // Assert
            assertNotNull(result);
            verify(statement).execute("SET search_path TO schema_clinica_101, public");
            verify(statement).close();
        }

        @Test
        @DisplayName("No debe establecer search_path para schema public")
        void getConnection_withPublicSchema_shouldNotSetSearchPath() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenReturn(connection);

            // Act
            Connection result = provider.getConnection("public");

            // Assert
            assertNotNull(result);
            verify(connection, never()).createStatement();
        }

        @Test
        @DisplayName("Debe continuar si falla establecer search_path")
        void getConnection_shouldContinueOnSearchPathError() throws SQLException {
            // Arrange
            String tenantSchema = "schema_clinica_102";
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.execute(anyString())).thenThrow(new SQLException("Schema not found"));

            // Act
            Connection result = provider.getConnection(tenantSchema);

            // Assert
            assertNotNull(result); // Debe continuar aunque falle el SET
            assertEquals(connection, result);
        }

        @Test
        @DisplayName("Debe lanzar HibernateException si falla obtener conexión")
        void getConnection_shouldThrowHibernateExceptionOnFailure() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenThrow(new SQLException("No connections available"));

            // Act & Assert
            assertThrows(HibernateException.class, () -> provider.getConnection("schema_clinica_103"));
        }

        @Test
        @DisplayName("Debe manejar tenantIdentifier null")
        void getConnection_withNullTenant_shouldWork() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenReturn(connection);

            // Act
            Connection result = provider.getConnection(null);

            // Assert
            assertNotNull(result);
            verify(connection, never()).createStatement();
        }
    }

    @Nested
    @DisplayName("Release Connection Tests")
    class ReleaseConnectionTests {

        @Test
        @DisplayName("Debe cerrar conexión correctamente")
        void releaseConnection_shouldCloseConnection() throws SQLException {
            // Act
            provider.releaseConnection("schema_clinica_101", connection);

            // Assert
            verify(connection).close();
        }

        @Test
        @DisplayName("Debe propagar SQLException como SQLException")
        void releaseConnection_shouldPropagateException() throws SQLException {
            // Arrange
            doThrow(new SQLException("Close error")).when(connection).close();

            // Act & Assert
            assertThrows(SQLException.class, () -> 
                provider.releaseConnection("schema_clinica_101", connection));
        }

        @Test
        @DisplayName("Debe manejar excepción genérica al cerrar")
        void releaseConnection_shouldHandleGenericException() throws SQLException {
            // Arrange
            doThrow(new RuntimeException("Unexpected error")).when(connection).close();

            // Act & Assert
            assertThrows(SQLException.class, () -> 
                provider.releaseConnection("schema_clinica_101", connection));
        }
    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("supportsAggressiveRelease debe retornar false")
        void supportsAggressiveRelease_shouldReturnFalse() {
            // Act & Assert
            assertFalse(provider.supportsAggressiveRelease());
        }

        @Test
        @DisplayName("isUnwrappableAs debe retornar false")
        void isUnwrappableAs_shouldReturnFalse() {
            // Act & Assert
            assertFalse(provider.isUnwrappableAs(DataSource.class));
            assertFalse(provider.isUnwrappableAs(Connection.class));
            assertFalse(provider.isUnwrappableAs(Object.class));
        }

        @Test
        @DisplayName("unwrap debe retornar null")
        void unwrap_shouldReturnNull() {
            // Act & Assert
            assertNull(provider.unwrap(DataSource.class));
            assertNull(provider.unwrap(Connection.class));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Debe manejar múltiples llamadas a getConnection")
        void multipleGetConnectionCalls_shouldWork() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);

            // Act
            Connection conn1 = provider.getConnection("schema_clinica_101");
            Connection conn2 = provider.getConnection("schema_clinica_102");

            // Assert
            assertNotNull(conn1);
            assertNotNull(conn2);
            verify(dataSource, times(2)).getConnection();
        }

        @Test
        @DisplayName("Debe manejar tenant con caracteres especiales")
        void getConnection_withSpecialChars_shouldWork() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);

            // Act
            Connection result = provider.getConnection("schema_clinica_999");

            // Assert
            assertNotNull(result);
            verify(statement).execute("SET search_path TO schema_clinica_999, public");
        }

        @Test
        @DisplayName("Debe manejar statement que no se cierra correctamente")
        void getConnection_withStatementCloseError_shouldStillWork() throws SQLException {
            // Arrange
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.execute(anyString())).thenReturn(true);
            doThrow(new SQLException("Close failed")).when(statement).close();

            // Act & Assert - try-with-resources manejará el error
            Connection result = provider.getConnection("schema_clinica_101");
            assertNotNull(result);
        }
    }
}

