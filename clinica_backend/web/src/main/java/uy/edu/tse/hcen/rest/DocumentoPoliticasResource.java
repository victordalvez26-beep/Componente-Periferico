package uy.edu.tse.hcen.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import uy.edu.tse.hcen.model.ProfesionalSalud;
import uy.edu.tse.hcen.service.PoliticasClient;
import uy.edu.tse.hcen.service.ProfesionalSaludService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Recurso REST para gestión de políticas de acceso a documentos clínicos.
 */
@Path("/documentos/politicas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class DocumentoPoliticasResource {

    private static final Logger LOG = Logger.getLogger(DocumentoPoliticasResource.class);

    private final PoliticasClient politicasClient;
    private final ProfesionalSaludService profesionalSaludService;

    @Inject
    public DocumentoPoliticasResource(PoliticasClient politicasClient,
                                     ProfesionalSaludService profesionalSaludService) {
        this.politicasClient = politicasClient;
        this.profesionalSaludService = profesionalSaludService;
    }

    /**
     * POST /api/documentos/politicas
     * 
     * Crea una política de acceso para un profesional específico y un paciente.
     */
    @POST
    @RolesAllowed({"ADMINISTRADOR", "PROFESIONAL"})
    public Response crearPolitica(Map<String, Object> body) {
        try {
            String alcance = (String) body.getOrDefault(DocumentoConstants.FIELD_ALCANCE, DocumentoConstants.DEFAULT_ALCANCE);
            String duracion = (String) body.getOrDefault(DocumentoConstants.FIELD_DURACION, DocumentoConstants.DEFAULT_DURACION);
            String gestion = (String) body.getOrDefault(DocumentoConstants.FIELD_GESTION, DocumentoConstants.DEFAULT_GESTION);
            String codDocumPaciente = (String) body.get(DocumentoConstants.FIELD_COD_DOCUM_PACIENTE);
            String profesionalAutorizado = (String) body.get(DocumentoConstants.FIELD_PROFESIONAL_AUTORIZADO);
            String tipoDocumento = (String) body.get(DocumentoConstants.FIELD_TIPO_DOCUMENTO);
            String fechaVencimiento = (String) body.get(DocumentoConstants.FIELD_FECHA_VENCIMIENTO);
            String referencia = (String) body.get(DocumentoConstants.FIELD_REFERENCIA);

            if (profesionalAutorizado == null || profesionalAutorizado.isBlank()) {
                return DocumentoResponseBuilder.badRequest("profesionalAutorizado es requerido");
            }

            PoliticasClient.PoliticaRequest request = new PoliticasClient.PoliticaRequest();
            request.setAlcance(alcance);
            request.setDuracion(duracion);
            request.setGestion(gestion);
            request.setCodDocumPaciente(codDocumPaciente);
            request.setProfesionalAutorizado(profesionalAutorizado);
            request.setTipoDocumento(tipoDocumento);
            request.setFechaVencimiento(fechaVencimiento);
            request.setReferencia(referencia);

            Long politicaId = politicasClient.crearPolitica(request);

            if (politicaId == null) {
                return DocumentoResponseBuilder.internalServerError("Error al crear la política");
            }

            return Response.status(Response.Status.CREATED)
                    .entity(Map.of(
                            DocumentoConstants.FIELD_POLITICA_ID, politicaId,
                            DocumentoConstants.FIELD_MENSAJE, "Política creada exitosamente",
                            DocumentoConstants.FIELD_ALCANCE, alcance,
                            DocumentoConstants.FIELD_DURACION, duracion,
                            DocumentoConstants.FIELD_GESTION, gestion
                    ))
                    .build();
        } catch (Exception e) {
            LOG.error("Error creando política", e);
            return DocumentoResponseBuilder.internalServerError("Error al crear la política: " + e.getMessage());
        }
    }

    /**
     * POST /api/documentos/politicas/global
     * 
     * Crea una política global que permite a un profesional acceder a TODOS los pacientes.
     */
    @POST
    @Path("/global")
    @RolesAllowed("ADMINISTRADOR")
    public Response crearPoliticaGlobal(Map<String, Object> body) {
        try {
            String alcance = (String) body.getOrDefault(DocumentoConstants.FIELD_ALCANCE, DocumentoConstants.DEFAULT_ALCANCE);
            String duracion = (String) body.getOrDefault(DocumentoConstants.FIELD_DURACION, DocumentoConstants.DEFAULT_DURACION);
            String gestion = (String) body.getOrDefault(DocumentoConstants.FIELD_GESTION, DocumentoConstants.DEFAULT_GESTION);
            String profesionalAutorizado = (String) body.get(DocumentoConstants.FIELD_PROFESIONAL_AUTORIZADO);
            String tipoDocumento = (String) body.get(DocumentoConstants.FIELD_TIPO_DOCUMENTO);
            String fechaVencimiento = (String) body.get(DocumentoConstants.FIELD_FECHA_VENCIMIENTO);
            String referencia = (String) body.getOrDefault(DocumentoConstants.FIELD_REFERENCIA, "Política global");

            if (profesionalAutorizado == null || profesionalAutorizado.isBlank()) {
                return DocumentoResponseBuilder.badRequest("profesionalAutorizado es requerido");
            }

            PoliticasClient.PoliticaRequest request = new PoliticasClient.PoliticaRequest();
            request.setAlcance(alcance);
            request.setDuracion(duracion);
            request.setGestion(gestion);
            request.setCodDocumPaciente("*"); // Valor especial para políticas globales
            request.setProfesionalAutorizado(profesionalAutorizado);
            request.setTipoDocumento(tipoDocumento);
            request.setFechaVencimiento(fechaVencimiento);
            request.setReferencia(referencia);

            Long politicaId = politicasClient.crearPolitica(request);

            if (politicaId == null) {
                return DocumentoResponseBuilder.internalServerError("Error al crear la política global");
            }

            return Response.status(Response.Status.CREATED)
                    .entity(Map.of(
                            DocumentoConstants.FIELD_POLITICA_ID, politicaId,
                            DocumentoConstants.FIELD_MENSAJE, "Política global creada exitosamente",
                            "tipo", "GLOBAL",
                            DocumentoConstants.FIELD_PROFESIONAL_AUTORIZADO, profesionalAutorizado,
                            DocumentoConstants.FIELD_ALCANCE, alcance
                    ))
                    .build();
        } catch (Exception e) {
            LOG.error("Error creando política global", e);
            return DocumentoResponseBuilder.internalServerError("Error al crear la política global: " + e.getMessage());
        }
    }

    /**
     * POST /api/documentos/politicas/especialidad
     * 
     * Crea políticas de acceso para todos los profesionales de una especialidad específica.
     */
    @POST
    @Path("/especialidad")
    @RolesAllowed("ADMINISTRADOR")
    public Response crearPoliticasPorEspecialidad(Map<String, Object> body) {
        try {
            String especialidad = (String) body.get(DocumentoConstants.FIELD_ESPECIALIDAD);
            String codDocumPaciente = (String) body.get(DocumentoConstants.FIELD_COD_DOCUM_PACIENTE);
            String alcance = (String) body.getOrDefault(DocumentoConstants.FIELD_ALCANCE, DocumentoConstants.DEFAULT_ALCANCE);
            String duracion = (String) body.getOrDefault(DocumentoConstants.FIELD_DURACION, DocumentoConstants.DEFAULT_DURACION);
            String gestion = (String) body.getOrDefault(DocumentoConstants.FIELD_GESTION, DocumentoConstants.DEFAULT_GESTION);
            String tipoDocumento = (String) body.get(DocumentoConstants.FIELD_TIPO_DOCUMENTO);
            String fechaVencimiento = (String) body.get(DocumentoConstants.FIELD_FECHA_VENCIMIENTO);
            String referencia = (String) body.getOrDefault(DocumentoConstants.FIELD_REFERENCIA,
                    "Política por especialidad: " + especialidad);

            if (especialidad == null || especialidad.isBlank()) {
                return DocumentoResponseBuilder.badRequest("especialidad es requerida");
            }

            Response validation = DocumentoValidator.validateCodDocumPaciente(codDocumPaciente);
            if (validation != null) {
                return validation;
            }

            List<ProfesionalSalud> profesionales = profesionalSaludService.findByEspecialidad(especialidad);

            if (profesionales.isEmpty()) {
                return DocumentoResponseBuilder.notFound("No se encontraron profesionales con la especialidad: " + especialidad);
            }

            List<Map<String, Object>> politicasCreadas = new ArrayList<>();
            int exitosas = 0;
            int fallidas = 0;

            for (ProfesionalSalud profesional : profesionales) {
                String profesionalNickname = profesional.getNickname();
                
                PoliticasClient.PoliticaRequest request = new PoliticasClient.PoliticaRequest();
                request.setAlcance(alcance);
                request.setDuracion(duracion);
                request.setGestion(gestion);
                request.setCodDocumPaciente(codDocumPaciente);
                request.setProfesionalAutorizado(profesionalNickname);
                request.setTipoDocumento(tipoDocumento);
                request.setFechaVencimiento(fechaVencimiento);
                request.setReferencia(referencia);

                Long politicaId = politicasClient.crearPolitica(request);

                if (politicaId != null) {
                    exitosas++;
                    politicasCreadas.add(Map.of(
                            DocumentoConstants.FIELD_POLITICA_ID, politicaId,
                            DocumentoConstants.FIELD_PROFESIONAL_ID, profesionalNickname,
                            "profesionalNombre", profesional.getNombre()
                    ));
                } else {
                    fallidas++;
                }
            }

            return Response.status(Response.Status.CREATED)
                    .entity(Map.of(
                            DocumentoConstants.FIELD_MENSAJE, "Políticas creadas por especialidad",
                            DocumentoConstants.FIELD_ESPECIALIDAD, especialidad,
                            DocumentoConstants.FIELD_COD_DOCUM_PACIENTE, codDocumPaciente,
                            "totalProfesionales", profesionales.size(),
                            "politicasExitosas", exitosas,
                            "politicasFallidas", fallidas,
                            "politicas", politicasCreadas
                    ))
                    .build();
        } catch (Exception e) {
            LOG.error("Error creando políticas por especialidad", e);
            return DocumentoResponseBuilder.internalServerError("Error al crear políticas por especialidad: " + e.getMessage());
        }
    }

    /**
     * GET /api/documentos/politicas
     * 
     * Lista todas las políticas de acceso.
     */
    @GET
    @RolesAllowed({"ADMINISTRADOR", "PROFESIONAL"})
    public Response listarPoliticas() {
        try {
            List<Map<String, Object>> politicas = politicasClient.listarPoliticas();
            if (politicas == null) {
                return DocumentoResponseBuilder.internalServerError("Error al listar políticas");
            }
            return DocumentoResponseBuilder.ok(politicas);
        } catch (Exception e) {
            LOG.error("Error listando políticas", e);
            return DocumentoResponseBuilder.internalServerError("Error al listar políticas: " + e.getMessage());
        }
    }

    /**
     * GET /api/documentos/politicas/paciente/{ci}
     * 
     * Lista las políticas de acceso de un paciente específico.
     */
    @GET
    @Path("/paciente/{ci}")
    @RolesAllowed({"ADMINISTRADOR", "PROFESIONAL"})
    public Response listarPoliticasPorPaciente(@PathParam("ci") String ci) {
        try {
            List<Map<String, Object>> politicas = politicasClient.listarPoliticasPorPaciente(ci);
            if (politicas == null) {
                return DocumentoResponseBuilder.internalServerError("Error al listar políticas del paciente");
            }
            return DocumentoResponseBuilder.ok(politicas);
        } catch (Exception e) {
            LOG.error("Error listando políticas por paciente", e);
            return DocumentoResponseBuilder.internalServerError("Error al listar políticas del paciente: " + e.getMessage());
        }
    }

    /**
     * GET /api/documentos/politicas/profesional/{id}
     * 
     * Lista las políticas de acceso de un profesional específico.
     */
    @GET
    @Path("/profesional/{id}")
    @RolesAllowed({"ADMINISTRADOR", "PROFESIONAL"})
    public Response listarPoliticasPorProfesional(@PathParam("id") String id) {
        try {
            List<Map<String, Object>> politicas = politicasClient.listarPoliticasPorProfesional(id);
            if (politicas == null) {
                return DocumentoResponseBuilder.internalServerError("Error al listar políticas del profesional");
            }
            return DocumentoResponseBuilder.ok(politicas);
        } catch (Exception e) {
            LOG.error("Error listando políticas por profesional", e);
            return DocumentoResponseBuilder.internalServerError("Error al listar políticas del profesional: " + e.getMessage());
        }
    }

    /**
     * DELETE /api/documentos/politicas/{id}
     * 
     * Elimina una política de acceso.
     */
    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMINISTRADOR")
    public Response eliminarPolitica(@PathParam("id") Long id) {
        try {
            boolean exito = politicasClient.eliminarPolitica(id);
            if (!exito) {
                return DocumentoResponseBuilder.notFound("Política no encontrada o error al eliminar");
            }
            return DocumentoResponseBuilder.ok(Map.of(
                    DocumentoConstants.FIELD_POLITICA_ID, id,
                    DocumentoConstants.FIELD_MENSAJE, "Política eliminada exitosamente"
            ));
        } catch (Exception e) {
            LOG.error("Error eliminando política", e);
            return DocumentoResponseBuilder.internalServerError("Error al eliminar la política: " + e.getMessage());
        }
    }
}



