package uy.edu.tse.hcen.model.exceptions;

/**
 * Excepción en tiempo de ejecución lanzada cuando no se puede configurar o conectar a MongoDB.
 */
public class MongoDBConfigurationException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "Error de configuración o conexión a MongoDB";

    public MongoDBConfigurationException() {
        super(DEFAULT_MESSAGE);
    }

    public MongoDBConfigurationException(String mensaje) {
        super(mensaje == null || mensaje.isBlank() ? DEFAULT_MESSAGE : mensaje.trim());
    }

}
