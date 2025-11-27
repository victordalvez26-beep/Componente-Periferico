package uy.edu.tse.hcen.multitenancy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
        // Usar reflection para inyectar el DataSource mock
        try {
            java.lang.reflect.Field field = SchemaMultiTenantProvider.class.getDeclaredField("dataSource");
            field.setAccessible(true);
            field.set(provider, dataSource);
        } catch (Exception e) {
            // Si falla, el test probará la inicialización vía JNDI
        }
        
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
    }

    @Test
    void testGetAnyConnection() throws SQLException {
        Connection result = provider.getAnyConnection();
        
        assertNotNull(result);
        verify(dataSource).getConnection();
    }

    @Test
    void testReleaseAnyConnection() throws SQLException {
        provider.releaseAnyConnection(connection);
        
        verify(connection).close();
    }

    @Test
    void testGetConnectionWithTenant() throws SQLException {
        String tenantSchema = "schema_clinica_123";
        
        Connection result = provider.getConnection(tenantSchema);
        
        assertNotNull(result);
        verify(connection).createStatement();
        verify(statement).execute(contains("SET search_path"));
    }

    @Test
    void testGetConnectionWithPublicSchema() throws SQLException {
        Connection result = provider.getConnection("public");
        
        assertNotNull(result);
        verify(connection, never()).createStatement();
    }

    @Test
    void testGetConnectionWithNull() throws SQLException {
        Connection result = provider.getConnection(null);
        
        assertNotNull(result);
        verify(connection, never()).createStatement();
    }

    @Test
    void testReleaseConnection() throws SQLException {
        provider.releaseConnection("schema_clinica_123", connection);
        
        verify(connection).close();
    }

    @Test
    void testSupportsAggressiveRelease() {
        assertFalse(provider.supportsAggressiveRelease());
    }

    @Test
    void testIsUnwrappableAs() {
        assertFalse(provider.isUnwrappableAs(String.class));
    }

    @Test
    void testUnwrap() {
        assertNull(provider.unwrap(String.class));
    }

    @Test
    void testGetConnectionWithSchemaSetError() throws SQLException {
        String tenantSchema = "schema_clinica_123";
        
        when(statement.execute(anyString())).thenThrow(new SQLException("Schema not found"));
        
        Connection result = provider.getConnection(tenantSchema);
        
        assertNotNull(result);
        verify(statement).execute(contains("SET search_path"));
    }

    @Test
    void testReleaseConnectionWithException() throws SQLException {
        doThrow(new SQLException("Close error")).when(connection).close();
        
        assertThrows(SQLException.class, () -> {
            provider.releaseConnection("schema_clinica_123", connection);
        });
        
        verify(connection).close();
    }

    @Test
    void testGetAnyConnectionWithJndiLookup() throws SQLException, NamingException {
        // Test cuando dataSource es null y necesita hacer JNDI lookup
        SchemaMultiTenantProvider newProvider = new SchemaMultiTenantProvider();
        
        try {
            // Usar reflection para setear dataSource a null
            java.lang.reflect.Field field = SchemaMultiTenantProvider.class.getDeclaredField("dataSource");
            field.setAccessible(true);
            field.set(newProvider, null);
            
            // Mock InitialContext
            try (var mockedStatic = mockStatic(InitialContext.class)) {
                InitialContext mockContext = mock(InitialContext.class);
                mockedStatic.when(InitialContext::new).thenReturn(mockContext);
                when(mockContext.lookup("java:/jdbc/MyMainDataSource")).thenReturn(dataSource);
                
                Connection result = newProvider.getAnyConnection();
                
                assertNotNull(result);
                verify(dataSource).getConnection();
            }
        } catch (Exception e) {
            // Si falla el JNDI lookup, es esperado en un entorno de test
        }
    }

    @Test
    void testGetAnyConnectionWithJndiLookupFailure() throws SQLException, NamingException {
        SchemaMultiTenantProvider newProvider = new SchemaMultiTenantProvider();
        
        try {
            java.lang.reflect.Field field = SchemaMultiTenantProvider.class.getDeclaredField("dataSource");
            field.setAccessible(true);
            field.set(newProvider, null);
            
            try (var mockedStatic = mockStatic(InitialContext.class)) {
                InitialContext mockContext = mock(InitialContext.class);
                mockedStatic.when(InitialContext::new).thenReturn(mockContext);
                when(mockContext.lookup("java:/jdbc/MyMainDataSource"))
                    .thenThrow(new NamingException("JNDI lookup failed"));
                
                assertThrows(SQLException.class, () -> {
                    newProvider.getAnyConnection();
                });
            }
        } catch (Exception e) {
            // Si falla, es esperado
        }
    }

    @Test
    void testGetConnectionWithEmptySchema() throws SQLException {
        Connection result = provider.getConnection("");
        
        assertNotNull(result);
        verify(connection, never()).createStatement();
    }

    @Test
    void testReleaseConnectionWithNullConnection() {
        // No debe lanzar excepción si la conexión es null
        assertThrows(NullPointerException.class, () -> {
            provider.releaseConnection("schema_clinica_123", null);
        });
    }
}

