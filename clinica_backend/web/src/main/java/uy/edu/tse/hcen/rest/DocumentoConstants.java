package uy.edu.tse.hcen.rest;

/**
 * Constantes utilizadas en los recursos de documentos clínicos.
 */
public final class DocumentoConstants {

    private DocumentoConstants() {
        // Clase de utilidad, no instanciable
    }

    // Campos JSON
    public static final String FIELD_ERROR = "error";
    public static final String FIELD_DETAIL = "detail";
    public static final String FIELD_DOCUMENTO_ID = "documentoId";
    public static final String FIELD_DOCUMENTO_ID_PACIENTE = "documentoIdPaciente";
    public static final String FIELD_CONTENIDO = "contenido";
    public static final String FIELD_TITULO = "titulo";
    public static final String FIELD_AUTOR = "autor";
    public static final String FIELD_ESPECIALIDAD = "especialidad";
    public static final String FIELD_ARCHIVO = "archivo";
    public static final String FIELD_NOMBRE_ARCHIVO = "nombreArchivo";
    public static final String FIELD_TIPO_ARCHIVO = "tipoArchivo";
    public static final String FIELD_ARCHIVO_ADJUNTO = "archivoAdjunto";
    public static final String FIELD_COD_DOCUM_PACIENTE = "codDocumPaciente";
    public static final String FIELD_TIPO_DOCUMENTO = "tipoDocumento";
    public static final String FIELD_RAZON_SOLICITUD = "razonSolicitud";
    public static final String FIELD_RESUELTO_POR = "resueltoPor";
    public static final String FIELD_COMENTARIO = "comentario";
    public static final String FIELD_SOLICITUD_ID = "solicitudId";
    public static final String FIELD_PROFESIONAL_ID = "profesionalId";
    public static final String FIELD_ESTADO = "estado";
    public static final String FIELD_MENSAJE = "mensaje";
    public static final String FIELD_POLITICA_ID = "politicaId";
    public static final String FIELD_ALCANCE = "alcance";
    public static final String FIELD_DURACION = "duracion";
    public static final String FIELD_GESTION = "gestion";
    public static final String FIELD_PROFESIONAL_AUTORIZADO = "profesionalAutorizado";
    public static final String FIELD_FECHA_VENCIMIENTO = "fechaVencimiento";
    public static final String FIELD_REFERENCIA = "referencia";

    // Valores por defecto
    public static final String DEFAULT_ALCANCE = "TODOS_LOS_DOCUMENTOS";
    public static final String DEFAULT_DURACION = "INDEFINIDA";
    public static final String DEFAULT_GESTION = "AUTOMATICA";
    public static final String DEFAULT_ESPECIALIDAD = "General";
    public static final String DEFAULT_RESUELTO_POR = "paciente";
    public static final String PREFIX_ARCHIVO_ADJUNTO = "archivo_adjunto_";
    public static final String PREFIX_DOCUMENTO = "documento_";
    public static final String EXTENSION_PDF = ".pdf";
    public static final String EXTENSION_TXT = ".txt";

    // Mensajes de error
    public static final String ERROR_PAYLOAD_REQUIRED = "payload required";
    public static final String ERROR_REQUEST_BODY_REQUIRED = "Request body required";
    public static final String ERROR_ID_REQUIRED = "id required";
    public static final String ERROR_DOCUMENT_NOT_FOUND = "document not found or invalid id";
    public static final String ERROR_CONTENIDO_REQUIRED = "contenido required";
    public static final String ERROR_DOCUMENTO_ID_PACIENTE_REQUIRED = "documentoIdPaciente required";
    public static final String ERROR_DOCUMENTO_ID_PACIENTE_ES_REQUERIDO = "documentoIdPaciente es requerido";
    public static final String ERROR_CONTENIDO_ES_REQUERIDO = "contenido es requerido";
    public static final String ERROR_COD_DOCUM_PACIENTE_REQUERIDO = "codDocumPaciente es requerido";
    public static final String ERROR_AUTENTICACION_REQUERIDA = "Autenticación requerida";
    public static final String ERROR_NO_TIENE_PERMISOS = "No tiene permisos para acceder a este documento";
    public static final String ERROR_SERVER_ERROR = "server error";
    public static final String ERROR_HCEN_UNAVAILABLE = "HCEN unavailable";
    public static final String ERROR_DOCUMENTO_GUARDADO_SIN_SYNC = "Documento guardado localmente pero sin sincronización en HCEN";
    public static final String ERROR_DOCUMENTO_SIN_CONTENIDO = "document has no content, PDF, or attached file";
    public static final String ERROR_DOCUMENTO_SIN_ARCHIVO_ADJUNTO = "Este documento no tiene archivo adjunto";

    // Headers HTTP
    public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    public static final String HEADER_ATTACHMENT_FILENAME = "attachment; filename=\"";
    public static final String HEADER_INLINE_FILENAME = "inline; filename=\"";

    // Paths
    public static final String PATH_API_DOCUMENTOS = "/api/documentos";
    public static final String PATH_DOCUMENTOS_DOCUMENTO_ID = "/api/documentos/{documentoId}";

    // Content Types
    public static final String CONTENT_TYPE_PDF = "application/pdf";
    public static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";
    public static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";
}

