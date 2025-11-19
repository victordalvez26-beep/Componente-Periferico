package uy.edu.tse.hcen.exceptions;

/**
 * Excepción que indica un fallo al consultar los metadatos en el RNDC/HCEN central.
 */
public class MetadatosQueryException extends RuntimeException {

    public MetadatosQueryException(String message) {
        super(message);
    }

    public MetadatosQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}


