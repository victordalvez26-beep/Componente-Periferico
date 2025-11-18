package uy.edu.tse.hcen.exceptions;

/**
 * Excepción lanzada cuando el backend HCEN central no está disponible
 * o no responde correctamente.
 */
public class HcenUnavailableException extends Exception {

    private static final long serialVersionUID = 1L;

    public HcenUnavailableException(String message) {
        super(message);
    }

    public HcenUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
