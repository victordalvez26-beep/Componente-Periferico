package uy.edu.tse.hcen.service;

import uy.edu.tse.hcen.dto.DTMetadatos;
import uy.edu.tse.hcen.utils.ServiceAuthUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import uy.edu.tse.hcen.exceptions.HcenUnavailableException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Simple client used to register metadatos in HCEN Central.
 * Incluye autenticación JWT entre servicios.
 */
@ApplicationScoped
public class HcenClient {

    private static final Logger LOG = Logger.getLogger(HcenClient.class.getName());

        // The HCEN central endpoint can be overridden via the HCEN_CENTRAL_URL environment variable
        // URL correcta: /api (ApplicationPath) + /metadatos-documento (Path del recurso)
        // Usar nombre del servicio Docker para comunicación entre contenedores
        private static final String DEFAULT_CENTRAL_URL = "http://hcen-backend:8080/api/metadatos-documento";
    
    // URL para obtener token de servicio
    // Usar nombre del servicio Docker para comunicación entre contenedores
    private static final String DEFAULT_SERVICE_AUTH_URL = "http://hcen-backend:8080/api/service-auth/token";
    
    // Cache del token de servicio (para evitar obtener uno nuevo en cada llamada)
    private String cachedServiceToken = null;
    private long tokenExpiryTime = 0;
    
    // Service ID para este componente periférico
    private static final String SERVICE_ID = "componente-periferico";
    private static final String SERVICE_NAME = "Componente Periférico HCEN";
    
    // Constantes para evitar duplicación de literales
    private static final String ENV_HCEN_CENTRAL_URL = "HCEN_CENTRAL_URL";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ERROR_UNKNOWN = "Unknown error";
    private static final String ERROR_MSG_REGISTRAR_METADATOS = "Error al registrar metadatos: HTTP %d - %s";
    
    /**
     * Obtiene un token de servicio (con cache para evitar múltiples llamadas).
     * Si el token está expirado o no existe, obtiene uno nuevo.
     * 
     * @return Token JWT de servicio o null si no se pudo obtener
     */
    public String getServiceToken() {
        // Verificar si el token cacheado sigue siendo válido (con margen de 5 minutos)
        long now = System.currentTimeMillis();
        if (cachedServiceToken != null && tokenExpiryTime > now + (5 * 60 * 1000)) {
            return cachedServiceToken;
        }
        
        // Generar token localmente (más eficiente que llamar al endpoint)
        try {
            cachedServiceToken = ServiceAuthUtil.generateServiceToken(SERVICE_ID, SERVICE_NAME);
            // Tokens de servicio duran 24 horas
            tokenExpiryTime = now + (24 * 60 * 60 * 1000);
            LOG.fine("Token de servicio generado localmente");
            return cachedServiceToken;
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "Error generando token de servicio localmente, intentando obtener desde endpoint: {0}", e.getMessage());
            
            // Fallback: intentar obtener desde endpoint (requiere serviceSecret configurado)
            String serviceSecret = System.getenv("HCEN_SERVICE_SECRET");
            if (serviceSecret == null || serviceSecret.isBlank()) {
                serviceSecret = System.getProperty("hcen.service.secret");
            }
            
            if (serviceSecret == null || serviceSecret.isBlank()) {
                LOG.warning("HCEN_SERVICE_SECRET no configurado. Comunicación sin autenticación (no recomendado para producción)");
                return null; // Sin autenticación si no hay secret
            }
            
            String authUrl = System.getProperty("HCEN_SERVICE_AUTH_URL",
                    System.getenv().getOrDefault("HCEN_SERVICE_AUTH_URL", DEFAULT_SERVICE_AUTH_URL));
            
            // Usar try-with-resources para cerrar recursos automáticamente
            try (Client client = ClientBuilder.newClient()) {
                Map<String, String> authRequest = Map.of(
                    "serviceId", SERVICE_ID,
                    "serviceSecret", serviceSecret,
                    "serviceName", SERVICE_NAME
                );
                
                try (Response response = client.target(authUrl)
                        .request(MediaType.APPLICATION_JSON)
                        .post(Entity.json(authRequest))) {
                    
                    if (response.getStatus() == 200) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> authResponse = response.readEntity(Map.class);
                        cachedServiceToken = (String) authResponse.get("token");
                        tokenExpiryTime = now + (24 * 60 * 60 * 1000);
                        LOG.info("Token de servicio obtenido desde endpoint");
                        return cachedServiceToken;
                    } else {
                        LOG.log(java.util.logging.Level.WARNING, "Error obteniendo token de servicio: HTTP {0}", response.getStatus());
                        return null;
                    }
                }
            } catch (ProcessingException ex) {
                LOG.log(java.util.logging.Level.WARNING, "Error obteniendo token de servicio desde endpoint: {0}", ex.getMessage());
                return null;
            }
        }
    }

    public void registrarMetadatos(DTMetadatos dto) throws HcenUnavailableException {
        String centralUrl = System.getProperty(ENV_HCEN_CENTRAL_URL,
                System.getenv().getOrDefault(ENV_HCEN_CENTRAL_URL, DEFAULT_CENTRAL_URL));

        if (LOG.isLoggable(java.util.logging.Level.INFO)) {
            LOG.log(java.util.logging.Level.INFO, "HcenClient.registrarMetadatos - URL: {0}, CI: {1}", 
                    new Object[]{centralUrl, dto != null ? dto.getDocumentoIdPaciente() : "null"});
        }

        // Obtener token de servicio
        String serviceToken = getServiceToken();

        // Usar try-with-resources para cerrar recursos automáticamente
        try (Client client = ClientBuilder.newClient()) {
            Builder requestBuilder = client.target(centralUrl)
                    .request(MediaType.APPLICATION_JSON);
            
            // Agregar token de servicio si está disponible
            if (serviceToken != null) {
                requestBuilder.header(HEADER_AUTHORIZATION, BEARER_PREFIX + serviceToken);
            }
            
            try (Response response = requestBuilder.post(Entity.json(dto))) {
                int status = response.getStatus();
                if (status == 401 || status == 403) {
                    // Token inválido o expirado, limpiar cache y reintentar una vez
                    handleTokenRejection(client, centralUrl, dto);
                } else if (status != 200 && status != 201 && status != 202) {
                    String errorMsg = response.hasEntity() ? response.readEntity(String.class) : ERROR_UNKNOWN;
                    throw new HcenUnavailableException(
                        String.format(ERROR_MSG_REGISTRAR_METADATOS, status, errorMsg));
                }
            }
        } catch (ProcessingException ex) {
            throw new HcenUnavailableException("HCEN no disponible", ex);
        }
    }
    
    public void handleTokenRejection(Client client, String centralUrl, Object payload) throws HcenUnavailableException {
        LOG.warning("Token de servicio rechazado, limpiando cache");
        cachedServiceToken = null;
        tokenExpiryTime = 0;
        
        // Reintentar con nuevo token
        String newToken = getServiceToken();
        if (newToken != null) {
            Builder retryBuilder = client.target(centralUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .header(HEADER_AUTHORIZATION, BEARER_PREFIX + newToken);
            try (Response retryResponse = retryBuilder.post(Entity.json(payload))) {
                int retryStatus = retryResponse.getStatus();
                if (retryStatus != 200 && retryStatus != 201 && retryStatus != 202) {
                    String errorMsg = retryResponse.hasEntity() ? retryResponse.readEntity(String.class) : ERROR_UNKNOWN;
                    throw new HcenUnavailableException(
                        String.format(ERROR_MSG_REGISTRAR_METADATOS, retryStatus, errorMsg));
                }
            }
        }
    }

    /**
     * Envía el payload completo (incluyendo datosPatronimicos) al central.
     */
    public void registrarMetadatosCompleto(Map<String, Object> payload) throws HcenUnavailableException {
        String centralUrl = System.getProperty(ENV_HCEN_CENTRAL_URL,
                System.getenv().getOrDefault(ENV_HCEN_CENTRAL_URL, DEFAULT_CENTRAL_URL));

        // Obtener token de servicio
        String serviceToken = getServiceToken();

        // Usar try-with-resources para cerrar recursos automáticamente
        try (Client client = ClientBuilder.newClient()) {
            jakarta.ws.rs.client.Invocation.Builder requestBuilder = client.target(centralUrl)
                    .request(MediaType.APPLICATION_JSON);
            
            // Agregar token de servicio si está disponible
            if (serviceToken != null) {
                requestBuilder.header(HEADER_AUTHORIZATION, BEARER_PREFIX + serviceToken);
            }
            
            try (Response response = requestBuilder.post(Entity.json(payload))) {
                int status = response.getStatus();
                if (status == 401 || status == 403) {
                    // Token inválido o expirado, limpiar cache y reintentar una vez
                    handleTokenRejection(client, centralUrl, payload);
                } else if (status != 200 && status != 201 && status != 202) {
                    String errorMsg = response.hasEntity() ? response.readEntity(String.class) : ERROR_UNKNOWN;
                    throw new HcenUnavailableException(
                        String.format(ERROR_MSG_REGISTRAR_METADATOS, status, errorMsg));
                }
            }
        } catch (ProcessingException ex) {
            throw new HcenUnavailableException("HCEN no disponible", ex);
        }
    }

    /**
     * Obtiene todos los metadatos de documentos de un paciente desde HCEN central.
     * El filtrado por políticas de acceso se hace en el HCEN backend.
     * 
     * @param ciPaciente CI del paciente
     * @param profesionalId ID del profesional que está consultando
     * @param tenantId ID de la clínica del profesional
     * @param especialidad Especialidad del profesional
     * @param nombreProfesional Nombre completo del profesional
     * @return Lista de metadatos de documentos (ya filtrados por políticas)
     */
    public java.util.List<Map<String, Object>> obtenerMetadatosDocumentosPorCI(
            String ciPaciente, String profesionalId, String tenantId, String especialidad, String nombreProfesional) 
            throws HcenUnavailableException {
        String metadatosUrl = construirUrlMetadatos(ciPaciente, profesionalId, tenantId, especialidad, nombreProfesional);
        
        logConsultaMetadatos(metadatosUrl, ciPaciente, profesionalId, tenantId, especialidad, nombreProfesional);
        
        String serviceToken = getServiceToken();
        
        return realizarPeticionMetadatos(metadatosUrl, serviceToken, ciPaciente);
    }
    
    private String construirUrlMetadatos(String ciPaciente, String profesionalId, String tenantId, 
                                         String especialidad, String nombreProfesional) {
        String baseUrl = System.getProperty(ENV_HCEN_CENTRAL_URL,
                System.getenv().getOrDefault(ENV_HCEN_CENTRAL_URL, "http://hcen-backend:8080/api"));
        String metadatosUrl = baseUrl.replace("/metadatos-documento", "") + "/metadatos-documento/paciente/" + ciPaciente;
        
        if (profesionalId != null && !profesionalId.isBlank()) {
            metadatosUrl += "?profesionalId=" + java.net.URLEncoder.encode(profesionalId, java.nio.charset.StandardCharsets.UTF_8);
            metadatosUrl = agregarQueryParam(metadatosUrl, "tenantId", tenantId);
            metadatosUrl = agregarQueryParam(metadatosUrl, "especialidad", especialidad);
            metadatosUrl = agregarQueryParam(metadatosUrl, "nombreProfesional", nombreProfesional);
        }
        return metadatosUrl;
    }
    
    private String agregarQueryParam(String url, String paramName, String paramValue) {
        if (paramValue != null && !paramValue.isBlank()) {
            return url + "&" + paramName + "=" + java.net.URLEncoder.encode(paramValue, java.nio.charset.StandardCharsets.UTF_8);
        }
        return url;
    }
    
    private void logConsultaMetadatos(String metadatosUrl, String ciPaciente, String profesionalId, 
                                      String tenantId, String especialidad, String nombreProfesional) {
        if (LOG.isLoggable(java.util.logging.Level.INFO)) {
            LOG.log(java.util.logging.Level.INFO, 
                    "Consultando metadatos desde HCEN - URL: {0}, CI: {1}, Profesional: {2}, Tenant: {3}, Especialidad: {4}, Nombre: {5}", 
                    new Object[]{metadatosUrl, ciPaciente, profesionalId, tenantId, especialidad, nombreProfesional});
        }
    }
    
    private java.util.List<Map<String, Object>> realizarPeticionMetadatos(String metadatosUrl, 
                                                                           String serviceToken, String ciPaciente) 
            throws HcenUnavailableException {
        try (Client client = ClientBuilder.newClient()) {
            Builder requestBuilder = client.target(metadatosUrl)
                    .request(MediaType.APPLICATION_JSON);
            
            if (serviceToken != null) {
                requestBuilder.header(HEADER_AUTHORIZATION, BEARER_PREFIX + serviceToken);
            }
            
            try (Response response = requestBuilder.get()) {
                return procesarRespuestaMetadatos(response, ciPaciente);
            }
        } catch (ProcessingException ex) {
            throw new HcenUnavailableException("HCEN no disponible", ex);
        }
    }
    
    private java.util.List<Map<String, Object>> procesarRespuestaMetadatos(Response response, String ciPaciente) 
            throws HcenUnavailableException {
        int status = response.getStatus();
        if (status == 200) {
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> metadatos = response.readEntity(java.util.List.class);
            if (LOG.isLoggable(java.util.logging.Level.INFO)) {
                LOG.log(java.util.logging.Level.INFO, 
                        "Obtenidos {0} metadatos (ya filtrados por políticas) desde HCEN para CI: {1}", 
                        new Object[]{metadatos.size(), ciPaciente});
            }
            return metadatos;
        } else {
            String errorMsg = response.hasEntity() ? response.readEntity(String.class) : ERROR_UNKNOWN;
            throw new HcenUnavailableException(
                "Error al obtener metadatos: HTTP " + status + " - " + errorMsg);
        }
    }
    
    /**
     * Consulta metadatos de un paciente desde HCEN central.
     * 
     * @param documentoIdPaciente CI o documento de identidad del paciente
     * @return Lista de metadatos (Map&lt;String, Object&gt;)
     * @throws HcenUnavailableException si HCEN no está disponible
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> consultarMetadatosPaciente(String documentoIdPaciente) 
            throws HcenUnavailableException {
        // URL base de HCEN central para endpoints de paciente
        // El endpoint es /api/paciente/{id}/metadatos
        String baseUrl = System.getProperty("HCEN_CENTRAL_BASE_URL",
                System.getenv().getOrDefault("HCEN_CENTRAL_BASE_URL", "http://127.0.0.1:8080/api"));
        
        // Construir URL del endpoint de paciente
        String pacienteUrl = baseUrl + "/paciente/" + documentoIdPaciente + "/metadatos";

        // Usar try-with-resources para cerrar recursos automáticamente
        try (Client client = ClientBuilder.newClient();
             Response response = client.target(pacienteUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {

            int status = response.getStatus();
            if (status == 200) {
                return response.readEntity(List.class);
            } else if (status == 404) {
                return new ArrayList<>(); // Lista vacía si no hay documentos
            } else {
                throw new HcenUnavailableException(
                    "Error al consultar metadatos: HTTP " + status);
            }

        } catch (ProcessingException ex) {
            throw new HcenUnavailableException("HCEN no disponible", ex);
        }
    }
    
    /**
     * Registra un acceso a la historia clínica de un paciente en HCEN Central.
     * Este método es llamado cuando un profesional del componente periférico
     * descarga o accede a un documento de un paciente.
     * 
     * @param profesionalId ID del profesional (nickname)
     * @param nombreProfesional Nombre completo del profesional
     * @param especialidad Especialidad del profesional
     * @param tenantId ID de la clínica (tenant) del profesional
     * @param codDocumPaciente CI del paciente
     * @param documentoId ID del documento (mongoId) o null si es búsqueda general
     * @param tipoDocumento Tipo de documento o null
     * @param exito Si el acceso fue exitoso
     */
    public void registrarAccesoHistoriaClinica(
            String profesionalId, String nombreProfesional, String especialidad,
            String tenantId, String codDocumPaciente, String documentoId, String tipoDocumento, boolean exito) {
        
        // Registrar de forma asíncrona para no bloquear la respuesta
        try {
            String registroUrl = obtenerUrlRegistro();
            Map<String, Object> payload = construirPayloadAcceso(profesionalId, nombreProfesional, especialidad, tenantId, codDocumPaciente, documentoId, tipoDocumento, exito);
            
            logRegistroAcceso(profesionalId, nombreProfesional, codDocumPaciente, tenantId, documentoId, exito);
            
            enviarRegistroAcceso(registroUrl, payload);
            
        } catch (Exception e) {
            // No propagar excepciones para no afectar la operación principal
            LOG.warning("Error al registrar acceso (no crítico): " + e.getMessage());
        }
    }

    private String obtenerUrlRegistro() {
        String politicasUrl = System.getenv("POLITICAS_SERVICE_URL");
        if (politicasUrl == null || politicasUrl.isEmpty()) {
            politicasUrl = "http://hcen-backend:8080/hcen-politicas-service/api";
        }
        return politicasUrl + "/registros";
    }

    private Map<String, Object> construirPayloadAcceso(String profesionalId, String nombreProfesional, String especialidad,
            String tenantId, String codDocumPaciente, String documentoId, String tipoDocumento, boolean exito) {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("profesionalId", profesionalId);
        payload.put("codDocumPaciente", codDocumPaciente);
        payload.put("clinicaId", tenantId);
        
        if (nombreProfesional != null && !nombreProfesional.isBlank()) {
            payload.put("nombreProfesional", nombreProfesional);
        }
        if (especialidad != null && !especialidad.isBlank()) {
            payload.put("especialidad", especialidad);
        }
        if (documentoId != null && !documentoId.isBlank()) {
            payload.put("documentoId", documentoId);
        }
        if (tipoDocumento != null && !tipoDocumento.isBlank()) {
            payload.put("tipoDocumento", tipoDocumento);
        } else {
            payload.put("tipoDocumento", "DESCARGA");
        }
        
        payload.put("exito", exito);
        if (!exito) {
            payload.put("motivoRechazo", "No se pudo acceder al documento");
        }
        payload.put("referencia", documentoId != null ? "Descarga de documento" : "Acceso a documento");
        return payload;
    }

    private void logRegistroAcceso(String profesionalId, String nombreProfesional, String codDocumPaciente, String tenantId, String documentoId, boolean exito) {
        if (LOG.isLoggable(java.util.logging.Level.INFO)) {
            LOG.log(java.util.logging.Level.INFO, 
                    "Registrando acceso - Profesional: {0} ({1}), Paciente: {2}, Clínica: {3}, Documento: {4}, Éxito: {5}", 
                    new Object[]{profesionalId, nombreProfesional, codDocumPaciente, tenantId, documentoId, exito});
        }
    }

    private void enviarRegistroAcceso(String registroUrl, Map<String, Object> payload) {
        try (Client client = ClientBuilder.newClient()) {
            Response response = client.target(registroUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(payload, MediaType.APPLICATION_JSON));
            
            int status = response.getStatus();
            if (status == 201 || status == 200) {
                LOG.log(java.util.logging.Level.INFO, "\u2705 Acceso registrado exitosamente - Status: {0}", status);
            } else {
                String errorBody = response.hasEntity() ? response.readEntity(String.class) : "Sin detalles";
                LOG.log(java.util.logging.Level.WARNING, "\u26a0\ufe0f Error al registrar acceso - Status: {0}, Response: {1}", 
                        new Object[]{status, errorBody});
            }
        }
    }
}
