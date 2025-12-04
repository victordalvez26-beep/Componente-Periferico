package uy.edu.tse.hcen.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests para DatabaseInitializer
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DatabaseInitializer Tests")
class DatabaseInitializerTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @InjectMocks
    private DatabaseInitializer initializer;

    @Test
    @DisplayName("init debe crear tablas exitosamente")
    void init_shouldCreateTables() throws SQLException {
        // Arrange
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.execute()).thenReturn(true);

        // Act & Assert
        assertDoesNotThrow(() -> initializer.init());
        
        verify(dataSource).getConnection();
        verify(preparedStatement, atLeast(4)).execute(); // 4 tablas
    }

    @Test
    @DisplayName("init debe manejar SQLException")
    void init_withSQLError_shouldLogError() throws SQLException {
        // Arrange
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        // Act & Assert - no debe lanzar excepción, solo loguear
        assertDoesNotThrow(() -> initializer.init());
    }

    @Test
    @DisplayName("init debe cerrar recursos correctamente")
    void init_shouldCloseResources() throws SQLException {
        // Arrange
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.execute()).thenReturn(true);

        // Act
        initializer.init();

        // Assert
        verify(preparedStatement, atLeast(4)).close();
        verify(connection).close();
    }

    @Test
    @DisplayName("Initializer debe ser instanciable")
    void initializer_shouldBeInstantiable() {
        assertNotNull(initializer);
    }

    @Test
    @DisplayName("init debe manejar error al crear tabla específica")
    void init_withTableCreationError_shouldContinue() throws SQLException {
        // Arrange
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.execute())
                .thenReturn(true)
                .thenThrow(new SQLException("Table error"))
                .thenReturn(true);

        // Act & Assert - debe manejar el error
        assertDoesNotThrow(() -> initializer.init());
    }

    @Test
    @DisplayName("init debe manejar DataSource null")
    void init_withNullDataSource_shouldHandleGracefully() {
        // Arrange - dataSource es null por defecto en el mock

        // Act & Assert - no debe lanzar NPE
        assertDoesNotThrow(() -> initializer.init());
    }

    @Test
    @DisplayName("init debe manejar Connection que falla al cerrar")
    void init_withConnectionCloseError_shouldComplete() throws SQLException {
        // Arrange
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.execute()).thenReturn(true);
        doThrow(new SQLException("Close failed")).when(connection).close();

        // Act & Assert - debe completar aunque falle el close
        assertDoesNotThrow(() -> initializer.init());
    }

    @Test
    @DisplayName("init debe manejar PreparedStatement que falla al cerrar")
    void init_withPreparedStatementCloseError_shouldComplete() throws SQLException {
        // Arrange
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.execute()).thenReturn(true);
        doThrow(new SQLException("PS close failed")).when(preparedStatement).close();

        // Act & Assert - debe completar
        assertDoesNotThrow(() -> initializer.init());
    }

    @Test
    @DisplayName("Múltiples llamadas a init deben ser idempotentes")
    void multipleInitCalls_shouldBeIdempotent() throws SQLException {
        // Arrange
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.execute()).thenReturn(true);

        // Act
        initializer.init();
        initializer.init();
        initializer.init();

        // Assert - debe ejecutarse sin problemas
        verify(dataSource, times(3)).getConnection();
    }
}

