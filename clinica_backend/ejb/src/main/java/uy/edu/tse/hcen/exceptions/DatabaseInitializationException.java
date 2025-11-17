package uy.edu.tse.hcen.exceptions;

/**
 * Excepción específica para fallos durante la inicialización de tablas.
 */
public class DatabaseInitializationException extends RuntimeException {

    public DatabaseInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}

