package uy.edu.tse.hcen.exceptions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HcenUnavailableExceptionTest {

    @Test
    void testConstructorWithMessage() {
        String message = "HCEN service is unavailable";
        HcenUnavailableException exception = new HcenUnavailableException(message);
        
        assertEquals(message, exception.getMessage());
    }
    
    @Test
    void testConstructorWithMessageAndCause() {
        String message = "Connection failed";
        Throwable cause = new RuntimeException("Network error");
        HcenUnavailableException exception = new HcenUnavailableException(message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
    
    @Test
    void testExceptionCanBeThrown() {
        assertThrows(HcenUnavailableException.class, () -> {
            throw new HcenUnavailableException("Test exception");
        });
    }
}

