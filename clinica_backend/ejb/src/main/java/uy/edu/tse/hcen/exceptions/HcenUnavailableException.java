package uy.edu.tse.hcen.exceptions;

/**
 * Exception thrown when HCEN central is unavailable or returns an error.
 */
public class HcenUnavailableException extends Exception {

    public HcenUnavailableException() {
        super();
    }

    public HcenUnavailableException(String message) {
        super(message);
    }

    public HcenUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
