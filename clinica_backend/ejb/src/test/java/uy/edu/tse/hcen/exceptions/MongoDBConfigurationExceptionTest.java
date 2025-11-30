package uy.edu.tse.hcen.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MongoDBConfigurationExceptionTest {

    @Test
    void testDefaultConstructor() {
        MongoDBConfigurationException exception = new MongoDBConfigurationException();
        
        assertNotNull(exception.getMessage());
        assertEquals("Error de configuración o conexión a MongoDB", exception.getMessage());
    }

    @Test
    void testConstructorWithMessage() {
        String message = "No se pudo conectar a MongoDB";
        MongoDBConfigurationException exception = new MongoDBConfigurationException(message);
        
        assertEquals(message, exception.getMessage());
    }

    @Test
    void testConstructorWithNullMessage() {
        MongoDBConfigurationException exception = new MongoDBConfigurationException(null);
        
        assertEquals("Error de configuración o conexión a MongoDB", exception.getMessage());
    }

    @Test
    void testConstructorWithEmptyMessage() {
        MongoDBConfigurationException exception = new MongoDBConfigurationException("");
        
        assertEquals("Error de configuración o conexión a MongoDB", exception.getMessage());
    }

    @Test
    void testConstructorWithBlankMessage() {
        MongoDBConfigurationException exception = new MongoDBConfigurationException("   ");
        
        assertEquals("Error de configuración o conexión a MongoDB", exception.getMessage());
    }

    @Test
    void testConstructorWithWhitespaceOnlyMessage() {
        MongoDBConfigurationException exception = new MongoDBConfigurationException("\t\n\r");
        
        assertEquals("Error de configuración o conexión a MongoDB", exception.getMessage());
    }

    @Test
    void testIsInstanceOfRuntimeException() {
        MongoDBConfigurationException exception = new MongoDBConfigurationException("Test");
        
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void testLongMessage() {
        String longMessage = "a".repeat(1000);
        MongoDBConfigurationException exception = new MongoDBConfigurationException(longMessage);
        
        assertEquals(longMessage, exception.getMessage());
    }

    @Test
    void testMessageWithSpecialCharacters() {
        String message = "Error: MongoDB connection failed - host: localhost:27017 & database: hcen";
        MongoDBConfigurationException exception = new MongoDBConfigurationException(message);
        
        assertEquals(message, exception.getMessage());
    }

    @Test
    void testMessageTrimming() {
        String message = "  Error message  ";
        MongoDBConfigurationException exception = new MongoDBConfigurationException(message);
        
        assertEquals("Error message", exception.getMessage());
    }

    @Test
    void testUnicodeCharactersInMessage() {
        String message = "Error: No se pudo conectar a MongoDB";
        MongoDBConfigurationException exception = new MongoDBConfigurationException(message);
        
        assertEquals(message, exception.getMessage());
    }

    @Test
    void testMultipleInstances() {
        MongoDBConfigurationException ex1 = new MongoDBConfigurationException("Error 1");
        MongoDBConfigurationException ex2 = new MongoDBConfigurationException("Error 2");
        MongoDBConfigurationException ex3 = new MongoDBConfigurationException();
        
        assertEquals("Error 1", ex1.getMessage());
        assertEquals("Error 2", ex2.getMessage());
        assertEquals("Error de configuración o conexión a MongoDB", ex3.getMessage());
    }
}

