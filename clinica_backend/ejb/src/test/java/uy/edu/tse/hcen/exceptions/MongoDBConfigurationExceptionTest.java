package uy.edu.tse.hcen.exceptions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MongoDBConfigurationExceptionTest {

    @Test
    void testConstructorWithMessage() {
        String message = "MongoDB is not configured";
        MongoDBConfigurationException exception = new MongoDBConfigurationException(message);
        
        assertEquals(message, exception.getMessage());
    }
    
    @Test
    void testExceptionIsRuntimeException() {
        MongoDBConfigurationException exception = new MongoDBConfigurationException("Test");
        assertTrue(exception instanceof RuntimeException);
    }
    
    @Test
    void testExceptionCanBeThrown() {
        assertThrows(MongoDBConfigurationException.class, () -> {
            throw new MongoDBConfigurationException("Database not available");
        });
    }
}

