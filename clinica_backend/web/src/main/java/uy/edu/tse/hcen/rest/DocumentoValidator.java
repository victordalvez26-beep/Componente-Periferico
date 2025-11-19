package uy.edu.tse.hcen.rest;

import jakarta.ws.rs.core.Response;
import java.util.Map;

/**
 * Helper para validaciones comunes de documentos.
 */
public final class DocumentoValidator {

    private DocumentoValidator() {
        // Clase de utilidad, no instanciable
    }

    public static Response validateBody(Map<String, ?> body) {
        if (body == null) {
            return DocumentoResponseBuilder.badRequest(DocumentoConstants.ERROR_REQUEST_BODY_REQUIRED);
        }
        return null;
    }

    public static Response validateId(String id) {
        if (id == null || id.isBlank()) {
            return DocumentoResponseBuilder.badRequest(DocumentoConstants.ERROR_ID_REQUIRED);
        }
        return null;
    }

    public static Response validateDocumentoIdPaciente(String documentoIdPaciente) {
        if (documentoIdPaciente == null || documentoIdPaciente.isBlank()) {
            return DocumentoResponseBuilder.badRequest(DocumentoConstants.ERROR_DOCUMENTO_ID_PACIENTE_ES_REQUERIDO);
        }
        return null;
    }

    public static Response validateContenido(String contenido) {
        if (contenido == null || contenido.isBlank()) {
            return DocumentoResponseBuilder.badRequest(DocumentoConstants.ERROR_CONTENIDO_ES_REQUERIDO);
        }
        return null;
    }

    public static Response validateCodDocumPaciente(String codDocumPaciente) {
        if (codDocumPaciente == null || codDocumPaciente.isBlank()) {
            return DocumentoResponseBuilder.badRequest(DocumentoConstants.ERROR_COD_DOCUM_PACIENTE_REQUERIDO);
        }
        return null;
    }

    public static Response validateUsuarioId(String usuarioId) {
        if (usuarioId == null || usuarioId.isBlank()) {
            return DocumentoResponseBuilder.unauthorized(DocumentoConstants.ERROR_AUTENTICACION_REQUERIDA);
        }
        return null;
    }
}

