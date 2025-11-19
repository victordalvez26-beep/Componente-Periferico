package uy.edu.tse.hcen.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.logging.Logger;
import org.bson.Document;
import org.bson.types.Binary;
import uy.edu.tse.hcen.dto.DTMetadatos;
import uy.edu.tse.hcen.exceptions.HcenUnavailableException;
import uy.edu.tse.hcen.service.DocumentoService;
import uy.edu.tse.hcen.service.HcenClient;
import uy.edu.tse.hcen.exceptions.MetadatosSyncException;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Recurso REST para manejo básico de documentos clínicos en MongoDB.
 *
 * Flujo esperado:
 * 1) El profesional crea los metadatos mediante POST /api/documentos/metadatos (enviarMetadatos).
 * 2) Luego sube el contenido del documento mediante POST /api/documentos.
 * 
 * Nota: El path base "/api" está definido en RestApplication, por lo que este recurso usa "/documentos"
 */
@Path("/documentos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class DocumentoClinicoResource {

    private static final Logger LOG = Logger.getLogger(DocumentoClinicoResource.class);

    private final DocumentoService documentoService;
    private final HcenClient hcenClient;

    @Inject
    public DocumentoClinicoResource(DocumentoService documentoService, HcenClient hcenClient) {
        this.documentoService = documentoService;
        this.hcenClient = hcenClient;
    }

    /**
     * Endpoint para enviar metadatos al microservicio.
     */
    @POST
    @Path("/metadatos")
    public Response enviarMetadatos(Map<String, Object> datos) {
        Response validation = DocumentoValidator.validateBody(datos);
        if (validation != null) {
            return validation;
        }

        if (datos.get(DocumentoConstants.FIELD_DOCUMENTO_ID_PACIENTE) == null) {
            return DocumentoResponseBuilder.badRequest(DocumentoConstants.ERROR_DOCUMENTO_ID_PACIENTE_REQUIRED);
        }

        try (Jsonb jsonb = JsonbBuilder.create()) {
            String json = jsonb.toJson(datos);
            DTMetadatos dto = jsonb.fromJson(json, DTMetadatos.class);
            return registrarMetadatos(dto);
        } catch (Exception ex) {
            return DocumentoResponseBuilder.internalServerError("failed to map payload", ex.getMessage());
        }
    }

    /**
     * Guarda el contenido de un documento. Requiere rol de profesional.
     * Espera un JSON como { "documentoId": "<id>", "contenido": "<contenido>" }
     */
    @POST
    @RolesAllowed("PROFESIONAL")
    public Response guardarContenido(Map<String, String> body) {
        Response validation = DocumentoValidator.validateBody(body);
        if (validation != null) {
            return validation;
        }

        String documentoId = body.get(DocumentoConstants.FIELD_DOCUMENTO_ID);
        String contenido = body.get(DocumentoConstants.FIELD_CONTENIDO);

        if (documentoId == null || documentoId.isBlank()) {
            return DocumentoResponseBuilder.badRequest("documentoId required");
        }
        
        Response contenidoValidation = DocumentoValidator.validateContenido(contenido);
        if (contenidoValidation != null) {
            return contenidoValidation;
        }

        try {
            var saved = documentoService.guardarContenido(documentoId, contenido);
            URI location = UriBuilder.fromPath(DocumentoConstants.PATH_DOCUMENTOS_DOCUMENTO_ID).build(documentoId);
            return Response.created(location).entity(saved.toJson()).build();
        } catch (IllegalArgumentException ex) {
            return DocumentoResponseBuilder.badRequest(ex.getMessage());
        } catch (SecurityException ex) {
            return DocumentoResponseBuilder.forbidden(ex.getMessage());
        } catch (Exception ex) {
            return DocumentoResponseBuilder.internalServerError(DocumentoConstants.ERROR_SERVER_ERROR, ex.getMessage());
        }
    }

    /**
     * Recupera un documento de MongoDB por su id (_id hex string).
     * Accesible públicamente para lectura (sin autenticación requerida).
     */
    @GET
    @Path("/{id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM})
    public Response obtenerPorId(@PathParam("id") String id) {
        Response idValidation = DocumentoValidator.validateId(id);
        if (idValidation != null) {
            return idValidation;
        }

        Document doc = documentoService.buscarPorId(id);
        if (doc == null) {
            return DocumentoResponseBuilder.notFound(DocumentoConstants.ERROR_DOCUMENT_NOT_FOUND);
        }

        // Si existe PDF, devolverlo como binario (para compatibilidad con RNDC)
        Binary pdfBinary = doc.get("pdf", Binary.class);
        if (pdfBinary != null && pdfBinary.getData() != null && pdfBinary.getData().length > 0) {
            return buildPdfResponse(doc, id);
        }

        // Devolver JSON con información del documento
        return buildDocumentInfoResponse(doc, id);
    }

    /**
     * Devuelve los ids (hex) de los documentos asociados a un documentoId (p. ej. cédula del paciente).
     */
    @GET
    @Path("/ids/{documentoId}")
    @RolesAllowed("PROFESIONAL")
    public Response obtenerIdsPorDocumento(@PathParam("documentoId") String documentoId) {
        if (documentoId == null || documentoId.isBlank()) {
            return DocumentoResponseBuilder.badRequest("documentoId required");
        }
        
        List<String> ids = documentoService.buscarIdsPorDocumentoPaciente(documentoId);
        if (ids == null || ids.isEmpty()) {
            return DocumentoResponseBuilder.notFound("no documents found for the given documentoId");
        }
        
        return DocumentoResponseBuilder.ok(ids);
    }

    /**
     * Crea un documento clínico completo con archivo adjunto.
     * Acepta multipart/form-data con los siguientes campos:
     * - contenido: texto del documento (requerido)
     * - documentoIdPaciente: CI del paciente (requerido)
     * - titulo: título del documento (opcional)
     * - autor: autor del documento (opcional)
     * - especialidad: especialidad (opcional)
     * - archivo: archivo adjunto (opcional)
     */
    @POST
    @Path("/completo-con-archivo")
    @RolesAllowed("PROFESIONAL")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response crearDocumentoCompletoConArchivo(MultipartFormDataInput input) {
        try {
            String contenido = MultipartHelper.extractTextField(input, DocumentoConstants.FIELD_CONTENIDO);
            String documentoIdPaciente = MultipartHelper.extractTextField(input, DocumentoConstants.FIELD_DOCUMENTO_ID_PACIENTE);
            String titulo = MultipartHelper.extractTextField(input, DocumentoConstants.FIELD_TITULO);
            String autor = MultipartHelper.extractTextField(input, DocumentoConstants.FIELD_AUTOR);
            String especialidad = MultipartHelper.extractTextField(input, DocumentoConstants.FIELD_ESPECIALIDAD);

            Response contenidoValidation = DocumentoValidator.validateContenido(contenido);
            if (contenidoValidation != null) {
                return contenidoValidation;
            }

            Response pacienteValidation = DocumentoValidator.validateDocumentoIdPaciente(documentoIdPaciente);
            if (pacienteValidation != null) {
                return pacienteValidation;
            }

            MultipartHelper.ArchivoAdjunto archivo = MultipartHelper.extractFile(input, DocumentoConstants.FIELD_ARCHIVO);
            byte[] archivoBytes = archivo != null ? archivo.getBytes() : null;
            String nombreArchivo = archivo != null ? archivo.getNombreArchivo() : null;
            String tipoArchivo = archivo != null ? archivo.getTipoArchivo() : null;

            Map<String, Object> metadatos = buildMetadatos(titulo, autor, especialidad);

            var resultado = documentoService.crearDocumentoCompletoConArchivo(
                    contenido, documentoIdPaciente, archivoBytes, nombreArchivo, tipoArchivo, metadatos);

            URI location = UriBuilder.fromPath(DocumentoConstants.PATH_DOCUMENTOS_DOCUMENTO_ID)
                    .build(resultado.get(DocumentoConstants.FIELD_DOCUMENTO_ID));
            return Response.created(location).entity(resultado).build();
        } catch (IllegalArgumentException ex) {
            return DocumentoResponseBuilder.badRequest(ex.getMessage());
        } catch (SecurityException ex) {
            return DocumentoResponseBuilder.forbidden(ex.getMessage());
        } catch (uy.edu.tse.hcen.exceptions.MetadatosSyncException ex) {
            LOG.warnf("Documento guardado localmente pero sin sincronización en HCEN: %s", ex.getMessage());
            return DocumentoResponseBuilder.serviceUnavailable(
                    DocumentoConstants.ERROR_DOCUMENTO_GUARDADO_SIN_SYNC, ex.getMessage());
        } catch (Exception ex) {
            LOG.error("Error al crear documento con archivo", ex);
            return DocumentoResponseBuilder.internalServerError("Error al crear documento", ex.getMessage());
        }
    }

    /**
     * Crea un documento clínico completo: guarda el contenido en MongoDB y envía los metadatos al HCEN central.
     * Espera un JSON con "contenido" y los campos de metadatos (documentoIdPaciente, especialidad, etc.)
     * El documentoId se genera automáticamente si no se proporciona.
     * Convierte automáticamente el contenido a PDF y lo guarda como binario.
     */
    @POST
    @Path("/completo")
    @RolesAllowed("PROFESIONAL")
    public Response crearDocumentoCompleto(Map<String, Object> body) {
        Response validation = DocumentoValidator.validateBody(body);
        if (validation != null) {
            return validation;
        }

        if (body.get(DocumentoConstants.FIELD_DOCUMENTO_ID_PACIENTE) == null) {
            return DocumentoResponseBuilder.badRequest(DocumentoConstants.ERROR_DOCUMENTO_ID_PACIENTE_REQUIRED);
        }

        try {
            var resultado = documentoService.crearDocumentoCompleto(body);
            URI location = UriBuilder.fromPath(DocumentoConstants.PATH_DOCUMENTOS_DOCUMENTO_ID)
                    .build(resultado.get(DocumentoConstants.FIELD_DOCUMENTO_ID));
            return Response.created(location).entity(resultado).build();
        } catch (IllegalArgumentException ex) {
            return DocumentoResponseBuilder.badRequest(ex.getMessage());
        } catch (SecurityException ex) {
            return DocumentoResponseBuilder.forbidden(ex.getMessage());
        } catch (uy.edu.tse.hcen.exceptions.MetadatosSyncException ex) {
            LOG.warnf("Documento guardado localmente pero sin sincronización en HCEN: %s", ex.getMessage());
            return buildResponseForSyncException(body, ex);
        } catch (RuntimeException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains(DocumentoConstants.ERROR_HCEN_UNAVAILABLE)) {
                return DocumentoResponseBuilder.serviceUnavailable(DocumentoConstants.ERROR_HCEN_UNAVAILABLE, ex.getMessage());
            }
            return DocumentoResponseBuilder.internalServerError(DocumentoConstants.ERROR_SERVER_ERROR, ex.getMessage());
        } catch (Exception ex) {
            return DocumentoResponseBuilder.internalServerError(DocumentoConstants.ERROR_SERVER_ERROR, ex.getMessage());
        }
    }

    private Response buildPdfResponse(Document doc, String id) {
        Binary pdfBinary = doc.get("pdf", Binary.class);
        byte[] pdfBytes = pdfBinary.getData();
        String documentoId = doc.getString(DocumentoConstants.FIELD_DOCUMENTO_ID);
        String fileName = DocumentoConstants.PREFIX_DOCUMENTO + id + DocumentoConstants.EXTENSION_PDF;
        if (documentoId != null && !documentoId.isBlank()) {
            fileName = DocumentoConstants.PREFIX_DOCUMENTO + documentoId + DocumentoConstants.EXTENSION_PDF;
        }
        return Response.ok(pdfBytes, MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Type", DocumentoConstants.CONTENT_TYPE_PDF)
                .header(DocumentoConstants.HEADER_CONTENT_DISPOSITION,
                        DocumentoConstants.HEADER_ATTACHMENT_FILENAME + fileName + "\"")
                .build();
    }

    private Response buildDocumentInfoResponse(Document doc, String id) {
        Map<String, Object> docInfo = new HashMap<>();
        docInfo.put("mongoId", id);
        docInfo.put(DocumentoConstants.FIELD_DOCUMENTO_ID, doc.getString(DocumentoConstants.FIELD_DOCUMENTO_ID));
        docInfo.put(DocumentoConstants.FIELD_DOCUMENTO_ID_PACIENTE, doc.getString(DocumentoConstants.FIELD_DOCUMENTO_ID_PACIENTE));

        Binary pdfBinary = doc.get("pdf", Binary.class);
        boolean tienePdf = pdfBinary != null && pdfBinary.getData() != null && pdfBinary.getData().length > 0;
        docInfo.put("tienePdf", tienePdf);

        Binary archivoAdjuntoBinary = doc.get(DocumentoConstants.FIELD_ARCHIVO_ADJUNTO, Binary.class);
        boolean tieneArchivoAdjunto = archivoAdjuntoBinary != null && archivoAdjuntoBinary.getData() != null
                && archivoAdjuntoBinary.getData().length > 0;
        docInfo.put("tieneArchivoAdjunto", tieneArchivoAdjunto);

        if (tieneArchivoAdjunto) {
            docInfo.put(DocumentoConstants.FIELD_NOMBRE_ARCHIVO, doc.getString(DocumentoConstants.FIELD_NOMBRE_ARCHIVO));
            docInfo.put(DocumentoConstants.FIELD_TIPO_ARCHIVO, doc.getString(DocumentoConstants.FIELD_TIPO_ARCHIVO));
        }

        String contenido = doc.getString(DocumentoConstants.FIELD_CONTENIDO);
        if (contenido != null) {
            docInfo.put("tieneContenido", true);
            docInfo.put("contenidoLength", contenido.length());
            if (contenido.length() > 100) {
                docInfo.put("contenidoPreview", contenido.substring(0, 100) + "...");
            } else {
                docInfo.put(DocumentoConstants.FIELD_CONTENIDO, contenido);
            }
        }

        return DocumentoResponseBuilder.ok(docInfo);
    }

    private Map<String, Object> buildMetadatos(String titulo, String autor, String especialidad) {
        Map<String, Object> metadatos = new HashMap<>();
        if (titulo != null && !titulo.isBlank()) {
            metadatos.put(DocumentoConstants.FIELD_TITULO, titulo);
        }
        if (autor != null && !autor.isBlank()) {
            metadatos.put(DocumentoConstants.FIELD_AUTOR, autor);
        }
        if (especialidad != null && !especialidad.isBlank()) {
            metadatos.put(DocumentoConstants.FIELD_ESPECIALIDAD, especialidad);
        }
        return metadatos;
    }

    private Response registrarMetadatos(DTMetadatos dto) {
        try {
            hcenClient.registrarMetadatos(dto);
            return Response.status(Response.Status.CREATED)
                    .entity(Map.of("status", "registered", DocumentoConstants.FIELD_DOCUMENTO_ID, dto.getDocumentoId()))
                    .build();
        } catch (HcenUnavailableException ex) {
            return DocumentoResponseBuilder.serviceUnavailable(DocumentoConstants.ERROR_HCEN_UNAVAILABLE, ex.getMessage());
        }
    }

    private Response buildResponseForSyncException(Map<String, Object> body, MetadatosSyncException ex) {
        try {
            String documentoId = (String) body.get(DocumentoConstants.FIELD_DOCUMENTO_ID);
            if (documentoId == null || documentoId.isBlank()) {
                documentoId = UUID.randomUUID().toString();
            }
            String documentoIdPaciente = (String) body.get(DocumentoConstants.FIELD_DOCUMENTO_ID_PACIENTE);
            Map<String, Object> resultado = Map.of(
                    DocumentoConstants.FIELD_DOCUMENTO_ID, documentoId,
                    DocumentoConstants.FIELD_DOCUMENTO_ID_PACIENTE, documentoIdPaciente != null ? documentoIdPaciente : "",
                    "warning", DocumentoConstants.ERROR_DOCUMENTO_GUARDADO_SIN_SYNC
            );
            URI location = UriBuilder.fromPath(DocumentoConstants.PATH_DOCUMENTOS_DOCUMENTO_ID).build(documentoId);
            return Response.status(Response.Status.CREATED).entity(resultado).location(location).build();
        } catch (Exception e) {
            return DocumentoResponseBuilder.serviceUnavailable(
                    DocumentoConstants.ERROR_DOCUMENTO_GUARDADO_SIN_SYNC, ex.getMessage());
        }
    }
}
