package uy.edu.tse.hcen.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.bson.Document;
import org.bson.types.Binary;
import org.jboss.logging.Logger;
import uy.edu.tse.hcen.multitenancy.TenantContext;
import uy.edu.tse.hcen.service.DocumentoService;
import uy.edu.tse.hcen.service.PoliticasClient;

import java.util.Map;

/**
 * Recurso REST para obtener contenido y archivos adjuntos de documentos clínicos.
 */
@Path("/documentos")
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM, MediaType.TEXT_PLAIN, MediaType.WILDCARD})
@RequestScoped
public class DocumentoContenidoResource {

    private static final Logger LOG = Logger.getLogger(DocumentoContenidoResource.class);

    private final DocumentoService documentoService;
    private final PoliticasClient politicasClient;
    private final SecurityContext securityContext;

    @Inject
    public DocumentoContenidoResource(DocumentoService documentoService,
                                     PoliticasClient politicasClient,
                                     SecurityContext securityContext) {
        this.documentoService = documentoService;
        this.politicasClient = politicasClient;
        this.securityContext = securityContext;
    }

    /**
     * GET /api/documentos/{id}/contenido
     * 
     * Endpoint mejorado para descargar el contenido de un documento.
     * Verifica permisos antes de devolver el contenido.
     * Registra el acceso para auditoría.
     */
    @GET
    @Path("/{id}/contenido")
    @RolesAllowed("PROFESIONAL")
    public Response obtenerContenido(
            @PathParam("id") String id,
            @HeaderParam("X-Paciente-CI") String pacienteCI) {
        
        Response idValidation = DocumentoValidator.validateId(id);
        if (idValidation != null) {
            return idValidation;
        }

        Document doc = documentoService.buscarPorId(id);
        if (doc == null) {
            return DocumentoResponseBuilder.notFound(DocumentoConstants.ERROR_DOCUMENT_NOT_FOUND);
        }

        String usuarioId = getUsuarioId();
        Response usuarioValidation = DocumentoValidator.validateUsuarioId(usuarioId);
        if (usuarioValidation != null) {
            politicasClient.registrarAcceso(null, null, null, null, false,
                    "Usuario no identificado", "Intento de acceso sin autenticación");
            return usuarioValidation;
        }

        AccesoInfo accesoInfo = verificarAcceso(doc, usuarioId, pacienteCI);
        politicasClient.registrarAcceso(usuarioId, accesoInfo.getCodDocumPaciente(),
                accesoInfo.getDocumentoId(), accesoInfo.getTipoDocumento(),
                accesoInfo.isPermitido(), accesoInfo.getMotivoRechazo(),
                accesoInfo.getReferencia());

        if (!accesoInfo.isPermitido()) {
            return DocumentoResponseBuilder.forbidden(DocumentoConstants.ERROR_NO_TIENE_PERMISOS,
                    accesoInfo.getMotivoRechazo());
        }

        // Intentar devolver archivo adjunto primero
        Response archivoResponse = buildArchivoAdjuntoResponse(doc, id, accesoInfo.getDocumentoId());
        if (archivoResponse != null) {
            return archivoResponse;
        }

        // Intentar devolver PDF
        Response pdfResponse = buildPdfResponse(doc, id, accesoInfo.getDocumentoId());
        if (pdfResponse != null) {
            return pdfResponse;
        }

        // Devolver contenido de texto
        return buildContenidoTextoResponse(doc, id, accesoInfo.getTipoDocumento());
    }

    /**
     * GET /api/documentos/{id}/archivo
     * 
     * Endpoint específico para obtener solo el archivo adjunto de un documento.
     */
    @GET
    @Path("/{id}/archivo")
    @RolesAllowed("PROFESIONAL")
    public Response obtenerArchivoAdjunto(
            @PathParam("id") String id,
            @HeaderParam("X-Paciente-CI") String pacienteCI) {
        
        Response idValidation = DocumentoValidator.validateId(id);
        if (idValidation != null) {
            return idValidation;
        }

        Document doc = documentoService.buscarPorId(id);
        if (doc == null) {
            return DocumentoResponseBuilder.notFound(DocumentoConstants.ERROR_DOCUMENT_NOT_FOUND);
        }

        String usuarioId = getUsuarioId();
        Response usuarioValidation = DocumentoValidator.validateUsuarioId(usuarioId);
        if (usuarioValidation != null) {
            return usuarioValidation;
        }

        String codDocumPaciente = doc.getString("pacienteDoc");
        boolean accesoPermitido = verificarAccesoSimple(usuarioId, codDocumPaciente, pacienteCI);

        if (!accesoPermitido) {
            return DocumentoResponseBuilder.forbidden(DocumentoConstants.ERROR_NO_TIENE_PERMISOS);
        }

        Binary archivoAdjuntoBinary = doc.get(DocumentoConstants.FIELD_ARCHIVO_ADJUNTO, Binary.class);
        if (archivoAdjuntoBinary == null || archivoAdjuntoBinary.getData() == null
                || archivoAdjuntoBinary.getData().length == 0) {
            LOG.infof("Documento %s no tiene archivo adjunto", id);
            return DocumentoResponseBuilder.notFound(DocumentoConstants.ERROR_DOCUMENTO_SIN_ARCHIVO_ADJUNTO);
        }

        return buildArchivoAdjuntoResponse(doc, id, null);
    }

    private String getUsuarioId() {
        if (securityContext != null && securityContext.getUserPrincipal() != null) {
            return securityContext.getUserPrincipal().getName();
        }
        return null;
    }

    private AccesoInfo verificarAcceso(Document doc, String usuarioId, String pacienteCI) {
        String documentoId = doc.getString(DocumentoConstants.FIELD_DOCUMENTO_ID);
        String codDocumPaciente = null;
        String tipoDocumento = null;
        boolean accesoPermitido = false;
        String motivoRechazo = null;
        boolean esAccesoPaciente = false;

        if (documentoId != null && !documentoId.isBlank()) {
            Map<String, Object> metadatos = documentoService.obtenerMetadatosPorDocumentoId(documentoId);
            if (metadatos != null) {
                codDocumPaciente = (String) metadatos.get(DocumentoConstants.FIELD_DOCUMENTO_ID_PACIENTE);
                tipoDocumento = (String) metadatos.get(DocumentoConstants.FIELD_TIPO_DOCUMENTO);
                if (tipoDocumento == null || tipoDocumento.isBlank()) {
                    tipoDocumento = (String) metadatos.get("formato");
                }
            }
        }

        if (codDocumPaciente == null || codDocumPaciente.isBlank()) {
            codDocumPaciente = doc.getString("pacienteDoc");
        }

        if (codDocumPaciente != null && !codDocumPaciente.isBlank()) {
            if (pacienteCI != null && !pacienteCI.isBlank() && pacienteCI.equals(codDocumPaciente)) {
                accesoPermitido = true;
                esAccesoPaciente = true;
                LOG.infof("Acceso permitido: Paciente %s accediendo a su propio documento %s", pacienteCI, doc.get("_id"));
            } else {
                String tenantId = TenantContext.getCurrentTenant();
                accesoPermitido = politicasClient.verificarPermiso(usuarioId, codDocumPaciente, tipoDocumento, tenantId);
                if (!accesoPermitido) {
                    motivoRechazo = "No tiene permisos para acceder a este documento";
                    uy.edu.tse.hcen.common.security.SecurityAuditLogger.logFailedAccess(
                            usuarioId, "/api/documentos/" + doc.get("_id"), motivoRechazo);
                } else {
                    uy.edu.tse.hcen.common.security.SecurityAuditLogger.logSensitiveAccess(
                            usuarioId, "/api/documentos/" + doc.get("_id"));
                }
            }
        } else {
            accesoPermitido = true; // Por compatibilidad
            LOG.warnf("No se pudo determinar paciente para documento %s, permitiendo acceso", doc.get("_id"));
        }

        String referencia = esAccesoPaciente ? "Acceso del paciente a su propio documento"
                : "Acceso desde componente periférico";

        return new AccesoInfo(codDocumPaciente, documentoId, tipoDocumento, accesoPermitido, motivoRechazo, referencia);
    }

    private boolean verificarAccesoSimple(String usuarioId, String codDocumPaciente, String pacienteCI) {
        if (codDocumPaciente != null && !codDocumPaciente.isBlank()) {
            if (pacienteCI != null && !pacienteCI.isBlank() && pacienteCI.equals(codDocumPaciente)) {
                return true;
            } else {
                String tenantId = TenantContext.getCurrentTenant();
                return politicasClient.verificarPermiso(usuarioId, codDocumPaciente, null, tenantId);
            }
        }
        return true; // Por compatibilidad
    }

    private Response buildArchivoAdjuntoResponse(Document doc, String id, String documentoId) {
        Binary archivoAdjuntoBinary = doc.get(DocumentoConstants.FIELD_ARCHIVO_ADJUNTO, Binary.class);
        if (archivoAdjuntoBinary == null || archivoAdjuntoBinary.getData() == null
                || archivoAdjuntoBinary.getData().length == 0) {
            return null;
        }

        byte[] archivoBytes = archivoAdjuntoBinary.getData();
        String nombreArchivo = doc.getString(DocumentoConstants.FIELD_NOMBRE_ARCHIVO);
        String tipoArchivo = doc.getString(DocumentoConstants.FIELD_TIPO_ARCHIVO);

        if (nombreArchivo == null || nombreArchivo.isBlank()) {
            nombreArchivo = DocumentoConstants.PREFIX_ARCHIVO_ADJUNTO + id;
        }

        String contentTypeArchivo = tipoArchivo != null && !tipoArchivo.isBlank()
                ? tipoArchivo
                : DocumentoConstants.CONTENT_TYPE_OCTET_STREAM;

        LOG.infof("Devolviendo archivo adjunto: %s, tamaño: %d bytes, tipo: %s",
                nombreArchivo, archivoBytes.length, contentTypeArchivo);

        return Response.ok(archivoBytes, contentTypeArchivo)
                .header(DocumentoConstants.HEADER_CONTENT_DISPOSITION,
                        DocumentoConstants.HEADER_ATTACHMENT_FILENAME + nombreArchivo + "\"")
                .build();
    }

    private Response buildPdfResponse(Document doc, String id, String documentoId) {
        Binary pdfBinary = doc.get("pdf", Binary.class);
        if (pdfBinary == null || pdfBinary.getData() == null || pdfBinary.getData().length == 0) {
            return null;
        }

        byte[] pdfBytes = pdfBinary.getData();
        String fileName = DocumentoConstants.PREFIX_DOCUMENTO + id + DocumentoConstants.EXTENSION_PDF;
        if (documentoId != null && !documentoId.isBlank()) {
            fileName = DocumentoConstants.PREFIX_DOCUMENTO + documentoId + DocumentoConstants.EXTENSION_PDF;
        }

        LOG.infof("Devolviendo PDF: %s, tamaño: %d bytes", fileName, pdfBytes.length);
        return Response.ok(pdfBytes, MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Type", DocumentoConstants.CONTENT_TYPE_PDF)
                .header(DocumentoConstants.HEADER_CONTENT_DISPOSITION,
                        DocumentoConstants.HEADER_ATTACHMENT_FILENAME + fileName + "\"")
                .build();
    }

    private Response buildContenidoTextoResponse(Document doc, String id, String tipoDocumento) {
        String contenido = doc.getString(DocumentoConstants.FIELD_CONTENIDO);
        if (contenido == null) {
            return DocumentoResponseBuilder.notFound(DocumentoConstants.ERROR_DOCUMENTO_SIN_CONTENIDO);
        }

        String contentType = tipoDocumento != null && !tipoDocumento.isBlank()
                ? tipoDocumento
                : DocumentoConstants.CONTENT_TYPE_TEXT_PLAIN;

        LOG.infof("Devolviendo contenido de texto, tipo: %s", contentType);

        return Response.ok(contenido, contentType)
                .header(DocumentoConstants.HEADER_CONTENT_DISPOSITION,
                        DocumentoConstants.HEADER_INLINE_FILENAME + DocumentoConstants.PREFIX_DOCUMENTO
                                + id + DocumentoConstants.EXTENSION_TXT + "\"")
                .build();
    }

    /**
     * Clase para encapsular información de acceso.
     */
    private static class AccesoInfo {
        private final String codDocumPaciente;
        private final String documentoId;
        private final String tipoDocumento;
        private final boolean permitido;
        private final String motivoRechazo;
        private final String referencia;

        public AccesoInfo(String codDocumPaciente, String documentoId, String tipoDocumento,
                         boolean permitido, String motivoRechazo, String referencia) {
            this.codDocumPaciente = codDocumPaciente;
            this.documentoId = documentoId;
            this.tipoDocumento = tipoDocumento;
            this.permitido = permitido;
            this.motivoRechazo = motivoRechazo;
            this.referencia = referencia;
        }

        public String getCodDocumPaciente() {
            return codDocumPaciente;
        }

        public String getDocumentoId() {
            return documentoId;
        }

        public String getTipoDocumento() {
            return tipoDocumento;
        }

        public boolean isPermitido() {
            return permitido;
        }

        public String getMotivoRechazo() {
            return motivoRechazo;
        }

        public String getReferencia() {
            return referencia;
        }
    }
}

