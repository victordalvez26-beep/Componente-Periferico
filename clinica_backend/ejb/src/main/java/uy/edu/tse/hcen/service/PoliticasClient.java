package uy.edu.tse.hcen.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.logging.Level;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Timeout;
import uy.edu.tse.hcen.common.security.SecurityAuditLogger;

/**
 * Cliente para interactuar con el servicio de políticas de acceso.
 * Verifica permisos y gestiona solicitudes de acceso.
 * Incluye Circuit Breaker (MicroProfile Fault Tolerance) para resiliencia ante fallos del servicio.
 */
@ApplicationScoped
public class PoliticasClient {

    private static final Logger LOG = Logger.getLogger(PoliticasClient.class.getName());

    // URLs y configuración
    private static final String DEFAULT_POLITICAS_URL = "http://127.0.0.1:8080/hcen-politicas-service/api";
    private static final String ENV_POLITICAS_URL = "POLITICAS_SERVICE_URL";
    private static final String ERROR_UNKNOWN = "Unknown error";

    // Constantes para campos JSON
    private static final String FIELD_COD_DOCUM_PACIENTE = "codDocumPaciente";
    private static final String FIELD_TIPO_DOCUMENTO = "tipoDocumento";
    private static final String FIELD_SOLICITANTE_ID = "solicitanteId";
    private static final String FIELD_ESPECIALIDAD = "especialidad";
    private static final String FIELD_DOCUMENTO_ID = "documentoId";
    private static final String FIELD_RAZON_SOLICITUD = "razonSolicitud";
    private static final String FIELD_PROFESIONAL_ID = "profesionalId";
    private static final String FIELD_EXITO = "exito";
    private static final String FIELD_MOTIVO_RECHAZO = "motivoRechazo";
    private static final String FIELD_REFERENCIA = "referencia";
    private static final String FIELD_RESUELTO_POR = "resueltoPor";
    private static final String FIELD_COMENTARIO = "comentario";
    private static final String FIELD_ALCANCE = "alcance";
    private static final String FIELD_DURACION = "duracion";
    private static final String FIELD_GESTION = "gestion";
    private static final String FIELD_PROFESIONAL_AUTORIZADO = "profesionalAutorizado";
    private static final String FIELD_FECHA_VENCIMIENTO = "fechaVencimiento";
    private static final String FIELD_ID = "id";
    private static final String FIELD_TIENE_PERMISO = "tienePermiso";

    // Valores por defecto
    private static final String DEFAULT_ALCANCE = "TODOS_LOS_DOCUMENTOS";
    private static final String DEFAULT_DURACION = "INDEFINIDA";
    private static final String DEFAULT_GESTION = "AUTOMATICA";
    private static final String EMPTY_STRING = "";

    // Códigos HTTP
    private static final int HTTP_OK = 200;
    private static final int HTTP_CREATED = 201;
    private static final int HTTP_NO_CONTENT = 204;

    // Mensajes de error
    private static final String ERROR_PREFIX = "Error ";
    private static final String ERROR_FORMAT = "Error %s: %s";

    /**
     * Verifica si un profesional tiene permiso para acceder a documentos de un paciente.
     * 
     * @param profesionalId ID del profesional (nickname)
     * @param codDocumPaciente CI o documento de identidad del paciente
     * @param tipoDocumento Tipo de documento (opcional)
     * @return true si tiene permiso, false en caso contrario
     */
    public boolean verificarPermiso(String profesionalId, String codDocumPaciente, String tipoDocumento) {
        return verificarPermiso(profesionalId, codDocumPaciente, tipoDocumento, null);
    }

    /**
     * Verifica si un profesional tiene permiso para acceder a documentos de un paciente.
     * Incluye soporte para políticas por clínica.
     * 
     * @param profesionalId ID del profesional (nickname)
     * @param codDocumPaciente CI o documento de identidad del paciente
     * @param tipoDocumento Tipo de documento (opcional)
     * @param tenantId ID del tenant/clínica del profesional (opcional, necesario para políticas por clínica)
     * @return true si tiene permiso, false en caso contrario
     */
    @CircuitBreaker(
        requestVolumeThreshold = 5,
        failureRatio = 0.5,
        delay = 60000,
        successThreshold = 2
    )
    @Timeout(5000)
    @Fallback(fallbackMethod = "verificarPermisoFallback")
    public boolean verificarPermiso(String profesionalId, String codDocumPaciente, String tipoDocumento, String tenantId) {
        String baseUrl = getPoliticasUrl();
        StringBuilder urlBuilder = new StringBuilder(baseUrl)
                .append("/politicas/verificar?profesionalId=").append(profesionalId)
                .append("&pacienteCI=").append(codDocumPaciente);
        
        if (tipoDocumento != null && !tipoDocumento.isBlank()) {
            urlBuilder.append("&tipoDoc=").append(tipoDocumento);
        }
        
        if (tenantId != null && !tenantId.isBlank()) {
            urlBuilder.append("&tenantId=").append(tenantId);
        }

        try (Client client = ClientBuilder.newClient();
             Response response = client.target(urlBuilder.toString())
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {

            int status = response.getStatus();
            if (status == HTTP_OK) {
                Map<String, Object> result = readEntityAsMap(response);
                Object tienePermisoObj = result.get(FIELD_TIENE_PERMISO);
                return tienePermisoObj instanceof Boolean booleanValue && booleanValue;
            } else {
                logError("Error verificando permiso", status, response);
                throw new PoliticasServiceException("HTTP " + status);
            }
        }
    }

    /**
     * Fallback cuando el circuit breaker está abierto o hay un timeout.
     * Este método es llamado automáticamente por MicroProfile Fault Tolerance.
     */
    @SuppressWarnings("unused")
    private boolean verificarPermisoFallback(String profesionalId, String codDocumPaciente, 
                                             String tipoDocumento, String tenantId) {
        LOG.warning("Circuit breaker abierto o timeout para servicio de políticas. Usando fallback.");
        SecurityAuditLogger.logSecurityViolation(profesionalId, "SERVICE_UNAVAILABLE", 
            "Servicio de políticas no disponible (circuit breaker abierto o timeout)");
        return false;
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
        String url = getPoliticasUrl() + "/solicitudes";

        Map<String, Object> body = Map.of(
            FIELD_SOLICITANTE_ID, nullToEmpty(solicitanteId),
            FIELD_COD_DOCUM_PACIENTE, nullToEmpty(codDocumPaciente),
            FIELD_ESPECIALIDAD, nullToEmpty(especialidad),
            FIELD_TIPO_DOCUMENTO, nullToEmpty(tipoDocumento),
            FIELD_DOCUMENTO_ID, nullToEmpty(documentoId),
            FIELD_RAZON_SOLICITUD, nullToEmpty(razonSolicitud)
        );

        return executePostAndExtractId(url, body, "creando solicitud");
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
        String url = getPoliticasUrl() + "/solicitudes/" + solicitudId + "/aprobar";
        Map<String, Object> body = createResolucionBody(resueltoPor, comentario);
        return executePostAndCheckSuccess(url, body, "aprobando solicitud");
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
        String url = getPoliticasUrl() + "/solicitudes/" + solicitudId + "/rechazar";
        Map<String, Object> body = createResolucionBody(resueltoPor, comentario);
        return executePostAndCheckSuccess(url, body, "rechazando solicitud");
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
        String url = getPoliticasUrl() + "/registros";

        Map<String, Object> body = Map.of(
            FIELD_PROFESIONAL_ID, nullToEmpty(profesionalId),
            FIELD_COD_DOCUM_PACIENTE, nullToEmpty(codDocumPaciente),
            FIELD_DOCUMENTO_ID, nullToEmpty(documentoId),
            FIELD_TIPO_DOCUMENTO, nullToEmpty(tipoDocumento),
            FIELD_EXITO, exito,
            FIELD_MOTIVO_RECHAZO, nullToEmpty(motivoRechazo),
            FIELD_REFERENCIA, nullToEmpty(referencia)
        );

        executePostSilently(url, body, "registrando acceso");
    }

    /**
     * DTO para crear políticas de acceso.
     */
    public static class PoliticaRequest {
        private String alcance;
        private String duracion;
        private String gestion;
        private String codDocumPaciente;
        private String profesionalAutorizado;
        private String tipoDocumento;
        private String fechaVencimiento;
        private String referencia;

        // Getters y setters
        public String getAlcance() { return alcance; }
        public void setAlcance(String alcance) { this.alcance = alcance; }
        public String getDuracion() { return duracion; }
        public void setDuracion(String duracion) { this.duracion = duracion; }
        public String getGestion() { return gestion; }
        public void setGestion(String gestion) { this.gestion = gestion; }
        public String getCodDocumPaciente() { return codDocumPaciente; }
        public void setCodDocumPaciente(String codDocumPaciente) { this.codDocumPaciente = codDocumPaciente; }
        public String getProfesionalAutorizado() { return profesionalAutorizado; }
        public void setProfesionalAutorizado(String profesionalAutorizado) { this.profesionalAutorizado = profesionalAutorizado; }
        public String getTipoDocumento() { return tipoDocumento; }
        public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }
        public String getFechaVencimiento() { return fechaVencimiento; }
        public void setFechaVencimiento(String fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }
        public String getReferencia() { return referencia; }
        public void setReferencia(String referencia) { this.referencia = referencia; }
    }

    /**
     * Crea una política de acceso.
     * 
     * @param request DTO con los datos de la política
     * @return ID de la política creada o null si falló
     */
    public Long crearPolitica(PoliticaRequest request) {
        String url = getPoliticasUrl() + "/politicas";
        Map<String, Object> body = buildPoliticaBody(request);
        return executePostAndExtractId(url, body, "creando política");
    }

    /**
     * Crea una política de acceso (método legacy con múltiples parámetros).
     * 
     * @param alcance Alcance de la política
     * @param duracion Duración
     * @param gestion Gestión
     * @param codDocumPaciente CI del paciente
     * @param profesionalAutorizado ID del profesional autorizado
     * @param tipoDocumento Tipo de documento
     * @param fechaVencimiento Fecha de vencimiento
     * @param referencia Referencia
     * @return ID de la política creada o null si falló
     * @deprecated Usar {@link #crearPolitica(PoliticaRequest)} en su lugar. Este método será removido en una versión futura.
     */
    @Deprecated(since = "0.0.1-SNAPSHOT", forRemoval = true)
    public Long crearPolitica(String alcance, String duracion, String gestion,
                              String codDocumPaciente, String profesionalAutorizado,
                              String tipoDocumento, String fechaVencimiento, String referencia) {
        PoliticaRequest request = new PoliticaRequest();
        request.setAlcance(alcance);
        request.setDuracion(duracion);
        request.setGestion(gestion);
        request.setCodDocumPaciente(codDocumPaciente);
        request.setProfesionalAutorizado(profesionalAutorizado);
        request.setTipoDocumento(tipoDocumento);
        request.setFechaVencimiento(fechaVencimiento);
        request.setReferencia(referencia);
        return crearPolitica(request);
    }

    /**
     * Obtiene todas las políticas de acceso.
     * 
     * @return Lista de políticas (vacía si falló)
     */
    public List<Map<String, Object>> listarPoliticas() {
        String url = getPoliticasUrl() + "/politicas/listar";
        return executeGetList(url, "listando políticas");
    }

    /**
     * Obtiene políticas por paciente.
     * 
     * @param codDocumPaciente CI del paciente
     * @return Lista de políticas (vacía si falló)
     */
    public List<Map<String, Object>> listarPoliticasPorPaciente(String codDocumPaciente) {
        String url = getPoliticasUrl() + "/politicas/paciente/" + codDocumPaciente;
        return executeGetList(url, "listando políticas por paciente");
    }

    /**
     * Obtiene políticas por profesional.
     * 
     * @param profesionalId ID del profesional
     * @return Lista de políticas (vacía si falló)
     */
    public List<Map<String, Object>> listarPoliticasPorProfesional(String profesionalId) {
        String url = getPoliticasUrl() + "/politicas/profesional/" + profesionalId;
        return executeGetList(url, "listando políticas por profesional");
    }

    /**
     * Elimina una política de acceso.
     * 
     * @param politicaId ID de la política a eliminar
     * @return true si se eliminó exitosamente, false en caso contrario
     */
    public boolean eliminarPolitica(Long politicaId) {
        String url = getPoliticasUrl() + "/politicas/" + politicaId;
        return executeDelete(url, "eliminando política");
    }

    // ========== Métodos helper privados ==========

    /**
     * Obtiene la URL base del servicio de políticas.
     */
    private String getPoliticasUrl() {
        return System.getProperty(ENV_POLITICAS_URL,
                System.getenv().getOrDefault(ENV_POLITICAS_URL, DEFAULT_POLITICAS_URL));
    }

    /**
     * Convierte null a string vacío.
     */
    private String nullToEmpty(String value) {
        return value != null ? value : EMPTY_STRING;
    }

    /**
     * Crea el body para resolver una solicitud (aprobar/rechazar).
     */
    private Map<String, Object> createResolucionBody(String resueltoPor, String comentario) {
        Map<String, Object> body = new HashMap<>();
        body.put(FIELD_RESUELTO_POR, nullToEmpty(resueltoPor));
        body.put(FIELD_COMENTARIO, nullToEmpty(comentario));
        return body;
    }

    /**
     * Construye el body para crear una política.
     */
    private Map<String, Object> buildPoliticaBody(PoliticaRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put(FIELD_ALCANCE, request.getAlcance() != null ? request.getAlcance() : DEFAULT_ALCANCE);
        body.put(FIELD_DURACION, request.getDuracion() != null ? request.getDuracion() : DEFAULT_DURACION);
        body.put(FIELD_GESTION, request.getGestion() != null ? request.getGestion() : DEFAULT_GESTION);
        
        if (request.getCodDocumPaciente() != null && !request.getCodDocumPaciente().isBlank()) {
            body.put(FIELD_COD_DOCUM_PACIENTE, request.getCodDocumPaciente());
        }
        body.put(FIELD_PROFESIONAL_AUTORIZADO, nullToEmpty(request.getProfesionalAutorizado()));
        
        if (request.getTipoDocumento() != null && !request.getTipoDocumento().isBlank()) {
            body.put(FIELD_TIPO_DOCUMENTO, request.getTipoDocumento());
        }
        if (request.getFechaVencimiento() != null && !request.getFechaVencimiento().isBlank()) {
            body.put(FIELD_FECHA_VENCIMIENTO, request.getFechaVencimiento());
        }
        if (request.getReferencia() != null && !request.getReferencia().isBlank()) {
            body.put(FIELD_REFERENCIA, request.getReferencia());
        }
        return body;
    }

    /**
     * Ejecuta un POST y extrae el ID de la respuesta.
     */
    private Long executePostAndExtractId(String url, Map<String, Object> body, String operation) {
        try (Client client = ClientBuilder.newClient();
             Response response = client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(body))) {

            int status = response.getStatus();
            if (status == HTTP_CREATED) {
                Map<String, Object> result = readEntityAsMap(response);
                Object idObj = result.get(FIELD_ID);
                if (idObj instanceof Number number) {
                    return number.longValue();
                }
                return null;
            } else {
                logError(ERROR_PREFIX + operation, status, response);
                return null;
            }
        } catch (ProcessingException ex) {
            LOG.warning(String.format(ERROR_FORMAT, operation, ex.getMessage()));
            return null;
        }
    }

    /**
     * Ejecuta un POST y verifica éxito.
     */
    private boolean executePostAndCheckSuccess(String url, Map<String, Object> body, String operation) {
        try (Client client = ClientBuilder.newClient();
             Response response = client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(body))) {

            int status = response.getStatus();
            if (status == HTTP_OK) {
                return true;
            } else {
                logError(ERROR_PREFIX + operation, status, response);
                return false;
            }
        } catch (ProcessingException ex) {
            LOG.warning(String.format(ERROR_FORMAT, operation, ex.getMessage()));
            return false;
        }
    }

    /**
     * Ejecuta un POST sin retornar valor (para operaciones que no requieren respuesta).
     */
    private void executePostSilently(String url, Map<String, Object> body, String operation) {
        try (Client client = ClientBuilder.newClient();
             Response response = client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(body))) {

            int status = response.getStatus();
            if (status != HTTP_CREATED) {
                logError(ERROR_PREFIX + operation, status, response);
            }
        } catch (ProcessingException ex) {
            LOG.warning(String.format(ERROR_FORMAT, operation, ex.getMessage()));
        }
    }

    /**
     * Ejecuta un GET y retorna una lista.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> executeGetList(String url, String operation) {
        try (Client client = ClientBuilder.newClient();
             Response response = client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {

            int status = response.getStatus();
            if (status == HTTP_OK) {
                return response.readEntity(List.class);
            } else {
                logError(ERROR_PREFIX + operation, status, response);
                return Collections.emptyList();
            }
        } catch (ProcessingException ex) {
            LOG.warning(String.format(ERROR_FORMAT, operation, ex.getMessage()));
            return Collections.emptyList();
        }
    }

    /**
     * Ejecuta un DELETE.
     */
    private boolean executeDelete(String url, String operation) {
        try (Client client = ClientBuilder.newClient();
             Response response = client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .delete()) {

            int status = response.getStatus();
            if (status == HTTP_NO_CONTENT || status == HTTP_OK) {
                return true;
            } else {
                logError(ERROR_PREFIX + operation, status, response);
                return false;
            }
        } catch (ProcessingException ex) {
            LOG.warning(String.format(ERROR_FORMAT, operation, ex.getMessage()));
            return false;
        }
    }

    /**
     * Lee la entidad de respuesta como Map.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> readEntityAsMap(Response response) {
        return response.readEntity(Map.class);
    }

    /**
     * Registra un error de respuesta HTTP.
     */
    private void logError(String message, int status, Response response) {
        String errorMsg = ERROR_UNKNOWN;
        if (response.hasEntity()) {
            try {
                String entity = response.readEntity(String.class);
                if (entity != null && !entity.trim().isEmpty()) {
                    errorMsg = entity;
                }
            } catch (Exception e) {
                // Si no se puede leer la entidad, usar mensaje por defecto
            }
        }
        if (LOG.isLoggable(Level.WARNING)) {
            LOG.log(Level.WARNING, 
                    String.format("%s: HTTP %d - %s", message, status, errorMsg));
        }
    }

    /**
     * Excepción específica para errores del servicio de políticas.
     */
    public static class PoliticasServiceException extends RuntimeException {
        public PoliticasServiceException(String message) {
            super(message);
        }
    }
}
