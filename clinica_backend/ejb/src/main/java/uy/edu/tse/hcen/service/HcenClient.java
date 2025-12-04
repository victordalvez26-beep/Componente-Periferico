package uy.edu.tse.hcen.service;

import uy.edu.tse.hcen.dto.DTMetadatos;
import uy.edu.tse.hcen.utils.ServiceAuthUtil;
import uy.edu.tse.hcen.utils.HcenCentralUrlUtil;
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

        // Constantes para endpoints específicos (se construyen desde la URL base)
        // La URL base se obtiene de HcenCentralUrlUtil que lee HCEN_CENTRAL_BASE_URL
    
    // Cache del token de servicio (para evitar obtener uno nuevo en cada llamada)
    private String cachedServiceToken = null;
    private long tokenExpiryTime = 0;
    
    // Service ID para este componente periférico
    private static final String SERVICE_ID = "componente-periferico";
    private static final String SERVICE_NAME = "Componente Periférico HCEN";
    
    // Constantes para evitar duplicación de literales
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
    private String getServiceToken() {
        // Verificar si el token cacheado sigue siendo válido (con margen de 5 minutos)
        long now = System.currentTimeMillis();
        if (cachedServiceToken != null && tokenExpiryTime > now + (5 * 60 * 1000)) {
            return cachedServiceToken;
        }
        
        // Generar token localmente (más eficiente que llamar al endpoint)
        // TODO: esto debería usar el secret compartido
        try {
            cachedServiceToken = ServiceAuthUtil.generateServiceToken(SERVICE_ID, SERVICE_NAME);
            // Tokens de servicio duran 24 horas
            tokenExpiryTime = now + (24 * 60 * 60 * 1000);
            LOG.fine("Token de servicio generado localmente");
            return cachedServiceToken;
        } catch (Exception e) {
            LOG.warning(String.format("Error generando token de servicio localmente, intentando obtener desde endpoint: %s", e.getMessage()));
            
            // Fallback: intentar obtener desde endpoint (requiere serviceSecret configurado)
            String serviceSecret = System.getenv("HCEN_SERVICE_SECRET");
            if (serviceSecret == null || serviceSecret.isBlank()) {
                serviceSecret = System.getProperty("hcen.service.secret");
            }
            
            if (serviceSecret == null || serviceSecret.isBlank()) {
                LOG.warning("HCEN_SERVICE_SECRET no configurado. Comunicación sin autenticación (no recomendado para producción)");
                return null; // Sin autenticación si no hay secret
            }
            
            String authUrl = HcenCentralUrlUtil.buildApiUrl("/service-auth/token");
            
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
                        LOG.warning(String.format("Error obteniendo token de servicio: HTTP %d", response.getStatus()));
                        return null;
                    }
                }
            } catch (ProcessingException ex) {
                LOG.warning(String.format("Error obteniendo token de servicio desde endpoint: %s", ex.getMessage()));
                return null;
            }
        }
    }

    public void registrarMetadatos(DTMetadatos dto) throws HcenUnavailableException {
        String centralUrl = HcenCentralUrlUtil.buildApiUrl("/metadatos-documento");

        LOG.info(String.format("HcenClient.registrarMetadatos - URL: %s, CI: %s", 
                centralUrl, dto != null ? dto.getDocumentoIdPaciente() : "null"));

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
    
    private void handleTokenRejection(Client client, String centralUrl, Object payload) throws HcenUnavailableException {
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
        String centralUrl = HcenCentralUrlUtil.buildApiUrl("/metadatos-documento");

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
        // Construir URL del endpoint de metadatos por CI
        String metadatosUrl = HcenCentralUrlUtil.buildApiUrl("/metadatos-documento/paciente/" + ciPaciente);
        
        // Agregar query parameters si están disponibles
        if (profesionalId != null && !profesionalId.isBlank()) {
            metadatosUrl += "?profesionalId=" + java.net.URLEncoder.encode(profesionalId, java.nio.charset.StandardCharsets.UTF_8);
            if (tenantId != null && !tenantId.isBlank()) {
                metadatosUrl += "&tenantId=" + java.net.URLEncoder.encode(tenantId, java.nio.charset.StandardCharsets.UTF_8);
            }
            if (especialidad != null && !especialidad.isBlank()) {
                metadatosUrl += "&especialidad=" + java.net.URLEncoder.encode(especialidad, java.nio.charset.StandardCharsets.UTF_8);
            }
            if (nombreProfesional != null && !nombreProfesional.isBlank()) {
                metadatosUrl += "&nombreProfesional=" + java.net.URLEncoder.encode(nombreProfesional, java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        
        LOG.info(String.format("Consultando metadatos desde HCEN - URL: %s, CI: %s, Profesional: %s, Tenant: %s, Especialidad: %s, Nombre: %s", 
                metadatosUrl, ciPaciente, profesionalId, tenantId, especialidad, nombreProfesional));
        
        // Obtener token de servicio
        String serviceToken = getServiceToken();
        
        try (Client client = ClientBuilder.newClient()) {
            Builder requestBuilder = client.target(metadatosUrl)
                    .request(MediaType.APPLICATION_JSON);
            
            // Agregar token de servicio si está disponible
            if (serviceToken != null) {
                requestBuilder.header(HEADER_AUTHORIZATION, BEARER_PREFIX + serviceToken);
            }
            
            try (Response response = requestBuilder.get()) {
                int status = response.getStatus();
                if (status == 200) {
                    @SuppressWarnings("unchecked")
                    java.util.List<Map<String, Object>> metadatos = response.readEntity(java.util.List.class);
                    LOG.info(String.format("Obtenidos %d metadatos (ya filtrados por políticas) desde HCEN para CI: %s", 
                            metadatos.size(), ciPaciente));
                    return metadatos;
                } else if (status == 404) {
                    // 404 puede significar que no hay documentos, no necesariamente un error
                    LOG.warning(String.format("Metadatos no encontrados (404) para paciente %s - retornando lista vacía", ciPaciente));
                    return new ArrayList<>(); // Retornar lista vacía en lugar de lanzar excepción
                } else {
                    String errorMsg = ERROR_UNKNOWN;
                    try {
                        if (response.hasEntity()) {
                            errorMsg = response.readEntity(String.class);
                        }
                    } catch (Exception e) {
                        LOG.log(java.util.logging.Level.WARNING, "No se pudo leer el cuerpo del error", e);
                    }
                    throw new HcenUnavailableException(
                        String.format("Error al obtener metadatos: HTTP %d - %s", status, errorMsg));
                }
            }
        } catch (ProcessingException ex) {
            throw new HcenUnavailableException("HCEN no disponible", ex);
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
        // Construir URL del endpoint de paciente
        String pacienteUrl = HcenCentralUrlUtil.buildApiUrl("/paciente/" + documentoIdPaciente + "/metadatos");

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
                    String.format("Error al consultar metadatos: HTTP %d", status));
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
            // El servicio de políticas está en el mismo servidor que el HCEN central
            // Construir URL usando la base del HCEN central
            String baseUrl = HcenCentralUrlUtil.getBaseUrl();
            String registroUrl = baseUrl + "/hcen-politicas-service/api/registros";
            
            // Construir payload para registrar acceso
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
                payload.put("tipoDocumento", "DESCARGA"); // Descarga de documento si no hay tipo específico
            }
            
            payload.put("exito", exito);
            if (!exito) {
                payload.put("motivoRechazo", "No se pudo acceder al documento");
            }
            payload.put("referencia", documentoId != null ? "Descarga de documento" : "Acceso a documento");
            
            LOG.info(String.format("Registrando acceso - Profesional: %s (%s), Paciente: %s, Clínica: %s, Documento: %s, Éxito: %s", 
                    profesionalId, nombreProfesional, codDocumPaciente, tenantId, documentoId, exito));
            
            // Llamar al servicio de políticas de forma asíncrona
            Client client = ClientBuilder.newClient();
            try {
                Response response = client.target(registroUrl)
                        .request(MediaType.APPLICATION_JSON)
                        .post(Entity.entity(payload, MediaType.APPLICATION_JSON));
                
                int status = response.getStatus();
                if (status == 201 || status == 200) {
                    LOG.info(String.format("Acceso registrado exitosamente - Status: %d", status));
                } else {
                    String errorBody = response.hasEntity() ? response.readEntity(String.class) : "Sin detalles";
                    LOG.warning(String.format("Error al registrar acceso - Status: %d, Response: %s", status, errorBody));
                }
            } finally {
                client.close();
            }
            
        } catch (Exception e) {
            // No propagar excepciones para no afectar la operación principal
            LOG.warning("Error al registrar acceso (no crítico): " + e.getMessage());
        }
    }
}
