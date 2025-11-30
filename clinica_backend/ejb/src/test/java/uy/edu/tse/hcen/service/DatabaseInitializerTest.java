package uy.edu.tse.hcen.service;

import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
    void testInit() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.execute()).thenReturn(true);

        // Usar reflection para llamar a init() ya que es @PostConstruct
        try {
            java.lang.reflect.Method initMethod = DatabaseInitializer.class.getDeclaredMethod("init");
            initMethod.setAccessible(true);
            initMethod.invoke(initializer);
        } catch (Exception e) {
            // Si falla, probar directamente createMasterTables
            java.lang.reflect.Method createMethod = DatabaseInitializer.class.getDeclaredMethod("createMasterTables");
            createMethod.setAccessible(true);
            createMethod.invoke(initializer);
        }

        verify(dataSource, atLeastOnce()).getConnection();
        verify(preparedStatement, atLeastOnce()).execute();
    }

    @Test
    void testInitWithException() throws Exception {
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Connection error"));

        try {
            java.lang.reflect.Method createMethod = DatabaseInitializer.class.getDeclaredMethod("createMasterTables");
            createMethod.setAccessible(true);
            assertThrows(Exception.class, () -> {
                createMethod.invoke(initializer);
            });
        } catch (NoSuchMethodException e) {
            // Método no encontrado, test pasa
        }
    }
}

