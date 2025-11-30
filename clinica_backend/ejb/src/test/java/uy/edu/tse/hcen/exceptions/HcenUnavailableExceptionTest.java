package uy.edu.tse.hcen.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HcenUnavailableExceptionTest {

    @Test
    void testConstructorWithMessage() {
        String message = "HCEN backend no está disponible";
        HcenUnavailableException exception = new HcenUnavailableException(message);
        
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        String message = "Error de conexión";
        Throwable cause = new RuntimeException("Connection timeout");
        HcenUnavailableException exception = new HcenUnavailableException(message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testConstructorWithNullMessage() {
        HcenUnavailableException exception = new HcenUnavailableException(null);
        
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithEmptyMessage() {
        String message = "";
        HcenUnavailableException exception = new HcenUnavailableException(message);
        
        assertEquals(message, exception.getMessage());
    }

    @Test
    void testConstructorWithNullCause() {
        String message = "Error";
        HcenUnavailableException exception = new HcenUnavailableException(message, null);
        
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testIsInstanceOfException() {
        HcenUnavailableException exception = new HcenUnavailableException("Test");
        
        assertTrue(exception instanceof Exception);
    }

    @Test
    void testExceptionChaining() {
        RuntimeException rootCause = new RuntimeException("Root cause");
        IllegalStateException intermediateCause = new IllegalStateException("Intermediate", rootCause);
        HcenUnavailableException exception = new HcenUnavailableException("Top level", intermediateCause);
        
        assertEquals("Top level", exception.getMessage());
        assertEquals(intermediateCause, exception.getCause());
        assertEquals(rootCause, exception.getCause().getCause());
    }

    @Test
    void testLongMessage() {
        String longMessage = "a".repeat(1000);
        HcenUnavailableException exception = new HcenUnavailableException(longMessage);
        
        assertEquals(longMessage, exception.getMessage());
    }

    @Test
    void testSpecialCharactersInMessage() {
        String message = "Error: HCEN no está disponible - código: 500 & estado: ERROR";
        HcenUnavailableException exception = new HcenUnavailableException(message);
        
        assertEquals(message, exception.getMessage());
    }

    @Test
    void testUnicodeCharactersInMessage() {
        String message = "Error: HCEN no está disponible - código: 500";
        HcenUnavailableException exception = new HcenUnavailableException(message);
        
        assertEquals(message, exception.getMessage());
    }
}

