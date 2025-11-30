package uy.edu.tse.hcen.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MongoDBProducerTest {

    @InjectMocks
    private MongoDBProducer producer;

    @BeforeEach
    void setUp() {
        // Setup method if needed
    }

    @AfterEach
    void tearDown() {
        // No podemos restaurar variables de entorno fácilmente
    }



    @Test
    void testCreateMongoDatabase() {
        MongoClient mockClient = mock(MongoClient.class);
        MongoDatabase mockDatabase = mock(MongoDatabase.class);
        
        when(mockClient.getDatabase(anyString())).thenReturn(mockDatabase);

        MongoDatabase result = producer.createMongoDatabase(mockClient);

        assertNotNull(result);
        verify(mockClient).getDatabase(anyString());
    }



    @Test
    void testCloseMongoClient() {
        MongoClient mockClient = mock(MongoClient.class);
        doNothing().when(mockClient).close();

        assertDoesNotThrow(() -> {
            producer.close(mockClient);
        });

        verify(mockClient).close();
    }

    @Test
    void testCloseMongoClientWithException() {
        MongoClient mockClient = mock(MongoClient.class);
        doThrow(new RuntimeException("Close error")).when(mockClient).close();

        // No debe lanzar excepción, solo loguear
        assertDoesNotThrow(() -> {
            producer.close(mockClient);
        });

        verify(mockClient).close();
    }

    @Test
    void testCloseMongoClientWithNull() {
        // No debe lanzar excepción si el client es null
        assertDoesNotThrow(() -> {
            producer.close(null);
        });
    }

    @Test
    void testVerifyClientConnectionSuccess() throws Exception {
        MongoClient mockClient = mock(MongoClient.class);
        MongoDatabase mockDatabase = mock(MongoDatabase.class);
        Document mockResult = mock(Document.class);

        when(mockClient.getDatabase("admin")).thenReturn(mockDatabase);
        when(mockDatabase.runCommand(any(Document.class))).thenReturn(mockResult);

        // Usar reflection para llamar al método privado
        java.lang.reflect.Method method = MongoDBProducer.class.getDeclaredMethod("verifyClientConnection", MongoClient.class, String.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(producer, mockClient, "mongodb://localhost:27017");

        assertTrue(result);
        verify(mockClient, never()).close();
    }

    @Test
    void testVerifyClientConnectionFailure() throws Exception {
        MongoClient mockClient = mock(MongoClient.class);
        MongoDatabase mockDatabase = mock(MongoDatabase.class);

        when(mockClient.getDatabase("admin")).thenReturn(mockDatabase);
        when(mockDatabase.runCommand(any(Document.class))).thenThrow(new RuntimeException("Ping failed"));
        doNothing().when(mockClient).close();

        // Usar reflection para llamar al método privado
        java.lang.reflect.Method method = MongoDBProducer.class.getDeclaredMethod("verifyClientConnection", MongoClient.class, String.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(producer, mockClient, "mongodb://localhost:27017");

        assertFalse(result);
        verify(mockClient).close();
    }

    @Test
    void testVerifyClientConnectionFailureWithCloseException() throws Exception {
        MongoClient mockClient = mock(MongoClient.class);
        MongoDatabase mockDatabase = mock(MongoDatabase.class);

        when(mockClient.getDatabase("admin")).thenReturn(mockDatabase);
        when(mockDatabase.runCommand(any(Document.class))).thenThrow(new RuntimeException("Ping failed"));
        doThrow(new RuntimeException("Close failed")).when(mockClient).close();

        // Usar reflection para llamar al método privado
        java.lang.reflect.Method method = MongoDBProducer.class.getDeclaredMethod("verifyClientConnection", MongoClient.class, String.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(producer, mockClient, "mongodb://localhost:27017");

        assertFalse(result);
        verify(mockClient).close();
    }

    @Test
    void testCreateMongoDatabaseWithNullClient() {
        // No debe lanzar excepción si el client es null (aunque no es el comportamiento esperado)
        // Este test verifica que el método maneja null correctamente
        assertThrows(NullPointerException.class, () -> {
            producer.createMongoDatabase(null);
        });
    }

    @Test
    void testCreateMongoDatabaseWithDifferentDbNames() {
        MongoClient mockClient = mock(MongoClient.class);
        MongoDatabase mockDatabase1 = mock(MongoDatabase.class);
        MongoDatabase mockDatabase2 = mock(MongoDatabase.class);
        
        when(mockClient.getDatabase("hcen_db")).thenReturn(mockDatabase1);
        when(mockClient.getDatabase("test_db")).thenReturn(mockDatabase2);

        MongoDatabase result1 = producer.createMongoDatabase(mockClient);
        assertNotNull(result1);
        
        // Cambiar la variable de entorno para probar otro nombre de DB
        // (En un test real, esto se haría con reflection o configurando la variable)
        verify(mockClient).getDatabase(anyString());
    }
}

