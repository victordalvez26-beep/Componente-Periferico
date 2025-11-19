package uy.edu.tse.hcen.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.Map;
import uy.edu.tse.hcen.service.PoliticasClient;

/**
 * Recurso REST para gestión de solicitudes de acceso a documentos clínicos.
 */
@Path("/documentos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class DocumentoSolicitudesResource {

    private final PoliticasClient politicasClient;
    private final SecurityContext securityContext;

    @Inject
    public DocumentoSolicitudesResource(PoliticasClient politicasClient,
                                       SecurityContext securityContext) {
        this.politicasClient = politicasClient;
        this.securityContext = securityContext;
    }

    /**
     * POST /api/documentos/solicitar-acceso
     * 
     * Permite a un profesional solicitar acceso a documentos de un paciente.
     */
    @POST
    @Path("/solicitar-acceso")
    @RolesAllowed("PROFESIONAL")
    public Response solicitarAcceso(Map<String, Object> body) {
        String profesionalId = getUsuarioId();
        Response validation = DocumentoValidator.validateUsuarioId(profesionalId);
        if (validation != null) {
            return validation;
        }

        Response codDocumValidation = DocumentoValidator.validateCodDocumPaciente(
                (String) body.get(DocumentoConstants.FIELD_COD_DOCUM_PACIENTE));
        if (codDocumValidation != null) {
            return codDocumValidation;
        }

        String codDocumPaciente = (String) body.get(DocumentoConstants.FIELD_COD_DOCUM_PACIENTE);
        String tipoDocumento = (String) body.get(DocumentoConstants.FIELD_TIPO_DOCUMENTO);
        String documentoId = (String) body.get(DocumentoConstants.FIELD_DOCUMENTO_ID);
        String razonSolicitud = (String) body.get(DocumentoConstants.FIELD_RAZON_SOLICITUD);
        String especialidad = (String) body.getOrDefault(DocumentoConstants.FIELD_ESPECIALIDAD, DocumentoConstants.DEFAULT_ESPECIALIDAD);

        Long solicitudId = politicasClient.crearSolicitudAcceso(
                profesionalId, especialidad, codDocumPaciente,
                tipoDocumento, documentoId, razonSolicitud);

        if (solicitudId == null) {
            return DocumentoResponseBuilder.internalServerError("Error al crear solicitud de acceso");
        }

        return Response.status(Response.Status.CREATED)
                .entity(Map.of(
                        DocumentoConstants.FIELD_SOLICITUD_ID, solicitudId,
                        DocumentoConstants.FIELD_PROFESIONAL_ID, profesionalId,
                        DocumentoConstants.FIELD_COD_DOCUM_PACIENTE, codDocumPaciente,
                        DocumentoConstants.FIELD_ESTADO, "PENDIENTE",
                        DocumentoConstants.FIELD_MENSAJE, "Solicitud de acceso creada exitosamente"
                ))
                .build();
    }

    /**
     * POST /api/documentos/solicitar-acceso-historia-clinica
     * 
     * Permite a un profesional solicitar acceso a toda la historia clínica de un paciente.
     */
    @POST
    @Path("/solicitar-acceso-historia-clinica")
    @RolesAllowed("PROFESIONAL")
    public Response solicitarAccesoHistoriaClinica(Map<String, Object> body) {
        String profesionalId = getUsuarioId();
        Response validation = DocumentoValidator.validateUsuarioId(profesionalId);
        if (validation != null) {
            return validation;
        }

        Response codDocumValidation = DocumentoValidator.validateCodDocumPaciente(
                (String) body.get(DocumentoConstants.FIELD_COD_DOCUM_PACIENTE));
        if (codDocumValidation != null) {
            return codDocumValidation;
        }

        String codDocumPaciente = (String) body.get(DocumentoConstants.FIELD_COD_DOCUM_PACIENTE);
        String razonSolicitud = (String) body.get(DocumentoConstants.FIELD_RAZON_SOLICITUD);
        if (razonSolicitud == null || razonSolicitud.isBlank()) {
            razonSolicitud = "Solicitud de acceso a toda la historia clínica del paciente";
        }

        String especialidad = (String) body.getOrDefault(DocumentoConstants.FIELD_ESPECIALIDAD, DocumentoConstants.DEFAULT_ESPECIALIDAD);

        // tipoDocumento y documentoId son null para indicar acceso a todos los documentos
        Long solicitudId = politicasClient.crearSolicitudAcceso(
                profesionalId, especialidad, codDocumPaciente,
                null, // tipoDocumento = null indica todos los tipos
                null, // documentoId = null indica todos los documentos
                razonSolicitud);

        if (solicitudId == null) {
            return DocumentoResponseBuilder.internalServerError("Error al crear solicitud de acceso");
        }

        return Response.status(Response.Status.CREATED)
                .entity(Map.of(
                        DocumentoConstants.FIELD_SOLICITUD_ID, solicitudId,
                        DocumentoConstants.FIELD_PROFESIONAL_ID, profesionalId,
                        DocumentoConstants.FIELD_COD_DOCUM_PACIENTE, codDocumPaciente,
                        "tipoSolicitud", "HISTORIA_CLINICA_COMPLETA",
                        DocumentoConstants.FIELD_ESTADO, "PENDIENTE",
                        DocumentoConstants.FIELD_MENSAJE, "Solicitud de acceso a historia clínica completa creada exitosamente"
                ))
                .build();
    }

    /**
     * POST /api/documentos/solicitudes/{id}/aprobar
     * 
     * Permite a un paciente o administrador aprobar una solicitud de acceso pendiente.
     */
    @POST
    @Path("/solicitudes/{id}/aprobar")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.WILDCARD})
    public Response aprobarSolicitud(@PathParam("id") Long id, Map<String, Object> body) {
        String resueltoPor = getResueltoPor(body);
        String comentario = body != null ? (String) body.get(DocumentoConstants.FIELD_COMENTARIO) : null;

        boolean exito = politicasClient.aprobarSolicitud(id, resueltoPor, comentario);

        if (!exito) {
            return DocumentoResponseBuilder.internalServerError("Error al aprobar la solicitud");
        }

        return DocumentoResponseBuilder.ok(Map.of(
                DocumentoConstants.FIELD_SOLICITUD_ID, id,
                DocumentoConstants.FIELD_ESTADO, "APROBADA",
                DocumentoConstants.FIELD_RESUELTO_POR, resueltoPor,
                DocumentoConstants.FIELD_MENSAJE, "Solicitud aprobada exitosamente"
        ));
    }

    /**
     * POST /api/documentos/solicitudes/{id}/rechazar
     * 
     * Permite a un paciente o administrador rechazar una solicitud de acceso pendiente.
     */
    @POST
    @Path("/solicitudes/{id}/rechazar")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.WILDCARD})
    public Response rechazarSolicitud(@PathParam("id") Long id, Map<String, Object> body) {
        String resueltoPor = getResueltoPor(body);
        String comentario = body != null ? (String) body.get(DocumentoConstants.FIELD_COMENTARIO) : null;

        boolean exito = politicasClient.rechazarSolicitud(id, resueltoPor, comentario);

        if (!exito) {
            return DocumentoResponseBuilder.internalServerError("Error al rechazar la solicitud");
        }

        return DocumentoResponseBuilder.ok(Map.of(
                DocumentoConstants.FIELD_SOLICITUD_ID, id,
                DocumentoConstants.FIELD_ESTADO, "RECHAZADA",
                DocumentoConstants.FIELD_RESUELTO_POR, resueltoPor,
                DocumentoConstants.FIELD_MENSAJE, "Solicitud rechazada exitosamente"
        ));
    }

    private String getUsuarioId() {
        if (securityContext != null && securityContext.getUserPrincipal() != null) {
            return securityContext.getUserPrincipal().getName();
        }
        return null;
    }

    private String getResueltoPor(Map<String, Object> body) {
        String resueltoPor = getUsuarioId();
        if (body != null && body.containsKey(DocumentoConstants.FIELD_RESUELTO_POR)) {
            resueltoPor = (String) body.get(DocumentoConstants.FIELD_RESUELTO_POR);
        }
        if (resueltoPor == null || resueltoPor.isBlank()) {
            resueltoPor = DocumentoConstants.DEFAULT_RESUELTO_POR;
        }
        return resueltoPor;
    }
}

