package uy.edu.tse.hcen.exceptions;

/**
 * Excepción que indica que los metadatos no pudieron sincronizarse con el HCEN central.
 */
public class MetadatosSyncException extends RuntimeException {

    public MetadatosSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}


