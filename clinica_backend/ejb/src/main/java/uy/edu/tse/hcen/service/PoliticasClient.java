package uy.edu.tse.hcen.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 * Cliente para interactuar con el servicio de políticas de acceso.
 * Verifica permisos y gestiona solicitudes de acceso.
 */
@ApplicationScoped
public class PoliticasClient {

    private static final Logger LOG = Logger.getLogger(PoliticasClient.class.getName());

    // URL base del servicio de políticas
    private static final String DEFAULT_POLITICAS_URL = "http://127.0.0.1:8080/hcen-politicas-service/api";

    // Constantes para evitar duplicación
    private static final String ENV_POLITICAS_URL = "POLITICAS_SERVICE_URL";
    private static final String ERROR_UNKNOWN = "Unknown error";

    /**
     * Verifica si un profesional tiene permiso para acceder a documentos de un paciente.
     * 
     * @param profesionalId ID del profesional (nickname)
     * @param codDocumPaciente CI o documento de identidad del paciente
     * @param tipoDocumento Tipo de documento (opcional)
     * @return true si tiene permiso, false en caso contrario
     */
    public boolean verificarPermiso(String profesionalId, String codDocumPaciente, String tipoDocumento) {
        String politicasUrl = System.getProperty(ENV_POLITICAS_URL,
                System.getenv().getOrDefault(ENV_POLITICAS_URL, DEFAULT_POLITICAS_URL));
        
        String url = politicasUrl + "/politicas/verificar?profesionalId=" + profesionalId 
                + "&pacienteCI=" + codDocumPaciente;
        
        if (tipoDocumento != null && !tipoDocumento.isBlank()) {
            url += "&tipoDoc=" + tipoDocumento;
        }

        try (Client client = ClientBuilder.newClient();
             jakarta.ws.rs.core.Response response = client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {

            int status = response.getStatus();
            if (status == 200) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = response.readEntity(Map.class);
                Boolean tienePermiso = (Boolean) result.get("tienePermiso");
                return tienePermiso != null && tienePermiso;
            } else {
                LOG.warning(String.format("Error verificando permiso: HTTP %d", status));
                return false;
            }
        } catch (ProcessingException ex) {
            LOG.warning(String.format("Error verificando permiso: %s", ex.getMessage()));
            return false;
        }
    }

    /**
     * Crea una solicitud de acceso desde un profesional hacia un paciente.
     * 
     * @param solicitanteId ID del profesional que solicita
     * @param especialidad Especialidad del profesional
     * @param codDocumPaciente CI del paciente
     * @param tipoDocumento Tipo de documento (opcional)
     * @param documentoId ID específico del documento (opcional)
     * @param razonSolicitud Razón de la solicitud
     * @return ID de la solicitud creada o null si falló
     */
    public Long crearSolicitudAcceso(String solicitanteId, String especialidad,
                                    String codDocumPaciente, String tipoDocumento,
                                    String documentoId, String razonSolicitud) {
        String politicasUrl = System.getProperty(ENV_POLITICAS_URL,
                System.getenv().getOrDefault(ENV_POLITICAS_URL, DEFAULT_POLITICAS_URL));
        
        String url = politicasUrl + "/solicitudes";

        Map<String, Object> body = Map.of(
            "solicitanteId", solicitanteId != null ? solicitanteId : "",
            "codDocumPaciente", codDocumPaciente != null ? codDocumPaciente : "",
            "especialidad", especialidad != null ? especialidad : "",
            "tipoDocumento", tipoDocumento != null ? tipoDocumento : "",
            "documentoId", documentoId != null ? documentoId : "",
            "razonSolicitud", razonSolicitud != null ? razonSolicitud : ""
        );

        try (Client client = ClientBuilder.newClient();
             jakarta.ws.rs.core.Response response = client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(body))) {

            int status = response.getStatus();
            if (status == 201) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = response.readEntity(Map.class);
                Object idObj = result.get("id");
                if (idObj instanceof Number) {
                    return ((Number) idObj).longValue();
                }
                return null;
            } else {
                String errorMsg = response.hasEntity() ? response.readEntity(String.class) : ERROR_UNKNOWN;
                LOG.warning(String.format("Error creando solicitud: HTTP %d - %s", status, errorMsg));
                return null;
            }
        } catch (ProcessingException ex) {
            LOG.warning(String.format("Error creando solicitud: %s", ex.getMessage()));
            return null;
        }
    }

    /**
     * Aprueba una solicitud de acceso.
     * 
     * @param solicitudId ID de la solicitud a aprobar
     * @param resueltoPor ID de quien resuelve la solicitud (normalmente el paciente)
     * @param comentario Comentario opcional
     * @return true si se aprobó exitosamente, false en caso contrario
     */
    public boolean aprobarSolicitud(Long solicitudId, String resueltoPor, String comentario) {
        String politicasUrl = System.getProperty(ENV_POLITICAS_URL,
                System.getenv().getOrDefault(ENV_POLITICAS_URL, DEFAULT_POLITICAS_URL));
        
        String url = politicasUrl + "/solicitudes/" + solicitudId + "/aprobar";

        Map<String, Object> body = new HashMap<>();
        body.put("resueltoPor", resueltoPor != null ? resueltoPor : "");
        body.put("comentario", comentario != null ? comentario : "");

        try (Client client = ClientBuilder.newClient();
             jakarta.ws.rs.core.Response response = client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(body))) {

            int status = response.getStatus();
            if (status == 200) {
                return true;
            } else {
                String errorMsg = response.hasEntity() ? response.readEntity(String.class) : ERROR_UNKNOWN;
                LOG.warning(String.format("Error aprobando solicitud: HTTP %d - %s", status, errorMsg));
                return false;
            }
        } catch (ProcessingException ex) {
            LOG.warning(String.format("Error aprobando solicitud: %s", ex.getMessage()));
            return false;
        }
    }

    /**
     * Rechaza una solicitud de acceso.
     * 
     * @param solicitudId ID de la solicitud a rechazar
     * @param resueltoPor ID de quien resuelve la solicitud (normalmente el paciente)
     * @param comentario Comentario opcional
     * @return true si se rechazó exitosamente, false en caso contrario
     */
    public boolean rechazarSolicitud(Long solicitudId, String resueltoPor, String comentario) {
        String politicasUrl = System.getProperty(ENV_POLITICAS_URL,
                System.getenv().getOrDefault(ENV_POLITICAS_URL, DEFAULT_POLITICAS_URL));
        
        String url = politicasUrl + "/solicitudes/" + solicitudId + "/rechazar";

        Map<String, Object> body = new HashMap<>();
        body.put("resueltoPor", resueltoPor != null ? resueltoPor : "");
        body.put("comentario", comentario != null ? comentario : "");

        try (Client client = ClientBuilder.newClient();
             jakarta.ws.rs.core.Response response = client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(body))) {

            int status = response.getStatus();
            if (status == 200) {
                return true;
            } else {
                String errorMsg = response.hasEntity() ? response.readEntity(String.class) : ERROR_UNKNOWN;
                LOG.warning(String.format("Error rechazando solicitud: HTTP %d - %s", status, errorMsg));
                return false;
            }
        } catch (ProcessingException ex) {
            LOG.warning(String.format("Error rechazando solicitud: %s", ex.getMessage()));
            return false;
        }
    }

    /**
     * Registra un acceso a un documento para auditoría.
     * 
     * @param profesionalId ID del profesional
     * @param codDocumPaciente CI del paciente
     * @param documentoId ID del documento
     * @param tipoDocumento Tipo de documento
     * @param exito Si el acceso fue exitoso
     * @param motivoRechazo Motivo del rechazo si no fue exitoso
     * @param referencia Referencia adicional
     */
    public void registrarAcceso(String profesionalId, String codDocumPaciente,
                                String documentoId, String tipoDocumento,
                                boolean exito, String motivoRechazo, String referencia) {
        String politicasUrl = System.getProperty(ENV_POLITICAS_URL,
                System.getenv().getOrDefault(ENV_POLITICAS_URL, DEFAULT_POLITICAS_URL));
        
        String url = politicasUrl + "/registros";

        Map<String, Object> body = Map.of(
            "profesionalId", profesionalId != null ? profesionalId : "",
            "codDocumPaciente", codDocumPaciente != null ? codDocumPaciente : "",
            "documentoId", documentoId != null ? documentoId : "",
            "tipoDocumento", tipoDocumento != null ? tipoDocumento : "",
            "exito", exito,
            "motivoRechazo", motivoRechazo != null ? motivoRechazo : "",
            "referencia", referencia != null ? referencia : ""
        );

        try (Client client = ClientBuilder.newClient();
             jakarta.ws.rs.core.Response response = client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(body))) {

            int status = response.getStatus();
            if (status != 201) {
                String errorMsg = response.hasEntity() ? response.readEntity(String.class) : ERROR_UNKNOWN;
                LOG.warning(String.format("Error registrando acceso: HTTP %d - %s", status, errorMsg));
            }
        } catch (ProcessingException ex) {
            LOG.warning(String.format("Error registrando acceso: %s", ex.getMessage()));
        }
    }

    /**
     * Crea una política de acceso.
     * 
     * @param alcance Alcance de la política (TODOS_LOS_DOCUMENTOS, DOCUMENTOS_POR_TIPO, UN_DOCUMENTO_ESPECIFICO)
     * @param duracion Duración (INDEFINIDA, TEMPORAL)
     * @param gestion Gestión (AUTOMATICA, MANUAL)
     * @param codDocumPaciente CI del paciente (null para políticas globales)
     * @param profesionalAutorizado ID del profesional autorizado
     * @param tipoDocumento Tipo de documento (opcional)
     * @param fechaVencimiento Fecha de vencimiento (opcional, requerida si duracion es TEMPORAL)
     * @param referencia Referencia o descripción de la política
     * @return ID de la política creada o null si falló
     */
    public Long crearPolitica(String alcance, String duracion, String gestion,
                              String codDocumPaciente, String profesionalAutorizado,
                              String tipoDocumento, String fechaVencimiento, String referencia) {
        String politicasUrl = System.getProperty(ENV_POLITICAS_URL,
                System.getenv().getOrDefault(ENV_POLITICAS_URL, DEFAULT_POLITICAS_URL));
        
        String url = politicasUrl + "/politicas";

        Map<String, Object> body = new HashMap<>();
        body.put("alcance", alcance != null ? alcance : "TODOS_LOS_DOCUMENTOS");
        body.put("duracion", duracion != null ? duracion : "INDEFINIDA");
        body.put("gestion", gestion != null ? gestion : "AUTOMATICA");
        if (codDocumPaciente != null && !codDocumPaciente.isBlank()) {
            body.put("codDocumPaciente", codDocumPaciente);
        }
        body.put("profesionalAutorizado", profesionalAutorizado != null ? profesionalAutorizado : "");
        if (tipoDocumento != null && !tipoDocumento.isBlank()) {
            body.put("tipoDocumento", tipoDocumento);
        }
        if (fechaVencimiento != null && !fechaVencimiento.isBlank()) {
            body.put("fechaVencimiento", fechaVencimiento);
        }
        if (referencia != null && !referencia.isBlank()) {
            body.put("referencia", referencia);
        }

        try (Client client = ClientBuilder.newClient();
             jakarta.ws.rs.core.Response response = client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(body))) {

            int status = response.getStatus();
            if (status == 201) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = response.readEntity(Map.class);
                Object idObj = result.get("id");
                if (idObj instanceof Number) {
                    return ((Number) idObj).longValue();
                }
                return null;
            } else {
                String errorMsg = response.hasEntity() ? response.readEntity(String.class) : ERROR_UNKNOWN;
                LOG.warning(String.format("Error creando política: HTTP %d - %s", status, errorMsg));
                return null;
            }
        } catch (ProcessingException ex) {
            LOG.warning(String.format("Error creando política: %s", ex.getMessage()));
            return null;
        }
    }

    /**
     * Obtiene todas las políticas de acceso.
     * 
     * @return Lista de políticas o null si falló
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listarPoliticas() {
        String politicasUrl = System.getProperty(ENV_POLITICAS_URL,
                System.getenv().getOrDefault(ENV_POLITICAS_URL, DEFAULT_POLITICAS_URL));
        
        String url = politicasUrl + "/politicas/listar";

        try (Client client = ClientBuilder.newClient();
             jakarta.ws.rs.core.Response response = client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {

            int status = response.getStatus();
            if (status == 200) {
                return response.readEntity(List.class);
            } else {
                String errorMsg = response.hasEntity() ? response.readEntity(String.class) : ERROR_UNKNOWN;
                LOG.warning(String.format("Error listando políticas: HTTP %d - %s", status, errorMsg));
                return null;
            }
        } catch (ProcessingException ex) {
            LOG.warning(String.format("Error listando políticas: %s", ex.getMessage()));
            return null;
        }
    }

    /**
     * Obtiene políticas por paciente.
     * 
     * @param codDocumPaciente CI del paciente
     * @return Lista de políticas o null si falló
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listarPoliticasPorPaciente(String codDocumPaciente) {
        String politicasUrl = System.getProperty(ENV_POLITICAS_URL,
                System.getenv().getOrDefault(ENV_POLITICAS_URL, DEFAULT_POLITICAS_URL));
        
        String url = politicasUrl + "/politicas/paciente/" + codDocumPaciente;

        try (Client client = ClientBuilder.newClient();
             jakarta.ws.rs.core.Response response = client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {

            int status = response.getStatus();
            if (status == 200) {
                return response.readEntity(List.class);
            } else {
                String errorMsg = response.hasEntity() ? response.readEntity(String.class) : ERROR_UNKNOWN;
                LOG.warning(String.format("Error listando políticas por paciente: HTTP %d - %s", status, errorMsg));
                return null;
            }
        } catch (ProcessingException ex) {
            LOG.warning(String.format("Error listando políticas por paciente: %s", ex.getMessage()));
            return null;
        }
    }

    /**
     * Obtiene políticas por profesional.
     * 
     * @param profesionalId ID del profesional
     * @return Lista de políticas o null si falló
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listarPoliticasPorProfesional(String profesionalId) {
        String politicasUrl = System.getProperty(ENV_POLITICAS_URL,
                System.getenv().getOrDefault(ENV_POLITICAS_URL, DEFAULT_POLITICAS_URL));
        
        String url = politicasUrl + "/politicas/profesional/" + profesionalId;

        try (Client client = ClientBuilder.newClient();
             jakarta.ws.rs.core.Response response = client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {

            int status = response.getStatus();
            if (status == 200) {
                return response.readEntity(List.class);
            } else {
                String errorMsg = response.hasEntity() ? response.readEntity(String.class) : ERROR_UNKNOWN;
                LOG.warning(String.format("Error listando políticas por profesional: HTTP %d - %s", status, errorMsg));
                return null;
            }
        } catch (ProcessingException ex) {
            LOG.warning(String.format("Error listando políticas por profesional: %s", ex.getMessage()));
            return null;
        }
    }

    /**
     * Elimina una política de acceso.
     * 
     * @param politicaId ID de la política a eliminar
     * @return true si se eliminó exitosamente, false en caso contrario
     */
    public boolean eliminarPolitica(Long politicaId) {
        String politicasUrl = System.getProperty(ENV_POLITICAS_URL,
                System.getenv().getOrDefault(ENV_POLITICAS_URL, DEFAULT_POLITICAS_URL));
        
        String url = politicasUrl + "/politicas/" + politicaId;

        try (Client client = ClientBuilder.newClient();
             jakarta.ws.rs.core.Response response = client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .delete()) {

            int status = response.getStatus();
            if (status == 204 || status == 200) {
                return true;
            } else {
                String errorMsg = response.hasEntity() ? response.readEntity(String.class) : ERROR_UNKNOWN;
                LOG.warning(String.format("Error eliminando política: HTTP %d - %s", status, errorMsg));
                return false;
            }
        } catch (ProcessingException ex) {
            LOG.warning(String.format("Error eliminando política: %s", ex.getMessage()));
            return false;
        }
    }
}






