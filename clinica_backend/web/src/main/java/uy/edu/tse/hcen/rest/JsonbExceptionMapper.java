package uy.edu.tse.hcen.rest;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * Manejador de excepciones para errores de deserialización JSON.
 * Convierte errores de ProcessingException (incluyendo JsonbException) en respuestas HTTP apropiadas.
 */
@Provider
public class JsonbExceptionMapper implements ExceptionMapper<ProcessingException> {
    
    private static final Logger LOGGER = Logger.getLogger(JsonbExceptionMapper.class);
    private static final String ERROR_FECHA_NACIMIENTO = "El campo 'fechaNacimiento' no puede estar vacío. Debe ser una fecha válida (formato: YYYY-MM-DD) o null.";
    
    @Override
    public Response toResponse(ProcessingException exception) {
        String message = exception.getMessage();
        
        // Buscar mensajes relacionados con fechaNacimiento en la cadena de excepciones
        if (message != null && message.contains("fechaNacimiento") 
            && (message.contains("could not be parsed") || message.contains("Text ''") 
                || message.contains("Unable to deserialize property"))) {
            LOGGER.warnf("Error de deserialización: fechaNacimiento vacía o inválida. Mensaje: %s", message);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(ERROR_FECHA_NACIMIENTO))
                .build();
        }
        
        // Buscar en la causa también
        String rootMessage = getRootCauseMessage(exception);
        if (rootMessage != null && rootMessage.contains("fechaNacimiento") 
            && (rootMessage.contains("could not be parsed") || rootMessage.contains("Text ''") 
                || rootMessage.contains("Unable to deserialize property"))) {
            LOGGER.warnf("Error de deserialización: fechaNacimiento vacía o inválida. Mensaje raíz: %s", rootMessage);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(ERROR_FECHA_NACIMIENTO))
                .build();
        }
        
        // Para otros tipos de ProcessingException relacionados con JSON
        if (message != null && (message.contains("JSON Binding deserialization") 
            || message.contains("JsonbException") || message.contains("deserialization error"))) {
            LOGGER.warnf("Error de deserialización JSON: %s", message);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("Error al procesar los datos JSON: " + 
                    (rootMessage != null ? rootMessage : message)))
                .build();
        }
        
        // Para otros tipos de ProcessingException
        LOGGER.errorf(exception, "Error de procesamiento: %s", message);
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(new ErrorResponse("Error al procesar la solicitud: " + message))
            .build();
    }
    
    private String getRootCauseMessage(Throwable ex) {
        Throwable cause = ex.getCause();
        if (cause != null && cause != ex) {
            String causeMessage = getRootCauseMessage(cause);
            return causeMessage != null ? causeMessage : cause.getMessage();
        }
        return ex.getMessage();
    }
    
    /**
     * Clase para respuestas de error.
     */
    public static class ErrorResponse {
        private String error;
        
        public ErrorResponse(String error) {
            this.error = error;
        }
        
        public String getError() {
            return error;
        }
        
        public void setError(String error) {
            this.error = error;
        }
    }
}

