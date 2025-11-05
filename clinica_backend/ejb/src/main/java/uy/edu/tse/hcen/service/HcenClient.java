package uy.edu.tse.hcen.service;

import uy.edu.tse.hcen.dto.DTMetadatos;
import uy.edu.tse.hcen.utils.ServiceAuthUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import uy.edu.tse.hcen.exceptions.HcenUnavailableException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import java.util.logging.Logger;
// response type is referenced fully-qualified in code to avoid unused import warnings

/**
 * Simple client used to register metadatos in HCEN Central.
 * Incluye autenticación JWT entre servicios.
 */
@ApplicationScoped
public class HcenClient {

    private static final Logger LOG = Logger.getLogger(HcenClient.class.getName());

    // The HCEN central endpoint can be overridden via the HCEN_CENTRAL_URL environment variable
    // URL correcta: /api (ApplicationPath) + /central/api/rndc/metadatos (Path del recurso)
    private static final String DEFAULT_CENTRAL_URL = "http://localhost:8080/api/central/api/rndc/metadatos";
    
    // URL para obtener token de servicio
    private static final String DEFAULT_SERVICE_AUTH_URL = "http://localhost:8080/api/service-auth/token";
    
    // Cache del token de servicio (para evitar obtener uno nuevo en cada llamada)
    private String cachedServiceToken = null;
    private long tokenExpiryTime = 0;
    
    // Service ID para este componente periférico
    private static final String SERVICE_ID = "componente-periferico";
    private static final String SERVICE_NAME = "Componente Periférico HCEN";
    
    /**
     * Obtiene un token de servicio (con cache para evitar múltiples llamadas).
     * Si el token está expirado o no existe, obtiene uno nuevo.
     */
    private String getServiceToken() throws HcenUnavailableException {
        // Verificar si el token cacheado sigue siendo válido (con margen de 5 minutos)
        long now = System.currentTimeMillis();
        if (cachedServiceToken != null && tokenExpiryTime > now + (5 * 60 * 1000)) {
            return cachedServiceToken;
        }
        
        // Generar token localmente (más eficiente que llamar al endpoint)
        // En producción, esto debería usar el secret compartido
        try {
            cachedServiceToken = ServiceAuthUtil.generateServiceToken(SERVICE_ID, SERVICE_NAME);
            // Tokens de servicio duran 24 horas
            tokenExpiryTime = now + (24 * 60 * 60 * 1000);
            LOG.fine("Token de servicio generado localmente");
            return cachedServiceToken;
        } catch (Exception e) {
            LOG.warning("Error generando token de servicio localmente, intentando obtener desde endpoint: " + e.getMessage());
            
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
            
            Client client = null;
            jakarta.ws.rs.core.Response response = null;
            try {
                client = ClientBuilder.newClient();
                
                Map<String, String> authRequest = Map.of(
                    "serviceId", SERVICE_ID,
                    "serviceSecret", serviceSecret,
                    "serviceName", SERVICE_NAME
                );
                
                response = client.target(authUrl)
                        .request(MediaType.APPLICATION_JSON)
                        .post(Entity.json(authRequest));
                
                if (response.getStatus() == 200) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> authResponse = response.readEntity(Map.class);
                    cachedServiceToken = (String) authResponse.get("token");
                    tokenExpiryTime = now + (24 * 60 * 60 * 1000);
                    LOG.info("Token de servicio obtenido desde endpoint");
                    return cachedServiceToken;
                } else {
                    LOG.warning("Error obteniendo token de servicio: HTTP " + response.getStatus());
                    return null;
                }
            } catch (Exception ex) {
                LOG.warning("Error obteniendo token de servicio desde endpoint: " + ex.getMessage());
                return null;
            } finally {
                if (response != null) response.close();
                if (client != null) client.close();
            }
        }
    }

    public void registrarMetadatos(DTMetadatos dto) throws HcenUnavailableException {
        String centralUrl = System.getProperty("HCEN_CENTRAL_URL",
                System.getenv().getOrDefault("HCEN_CENTRAL_URL", DEFAULT_CENTRAL_URL));

        // Obtener token de servicio
        String serviceToken = getServiceToken();

        Client client = null;
        jakarta.ws.rs.core.Response response = null;
        try {
            client = ClientBuilder.newClient();
            jakarta.ws.rs.client.Invocation.Builder requestBuilder = client.target(centralUrl)
                    .request(MediaType.APPLICATION_JSON);
            
            // Agregar token de servicio si está disponible
            if (serviceToken != null) {
                requestBuilder.header("Authorization", "Bearer " + serviceToken);
            }
            
            response = requestBuilder.post(Entity.json(dto));

            int status = response.getStatus();
            if (status == 401 || status == 403) {
                // Token inválido o expirado, limpiar cache y reintentar una vez
                LOG.warning("Token de servicio rechazado, limpiando cache");
                cachedServiceToken = null;
                tokenExpiryTime = 0;
                
                // Reintentar con nuevo token
                serviceToken = getServiceToken();
                if (serviceToken != null) {
                    requestBuilder = client.target(centralUrl)
                            .request(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + serviceToken);
                    response.close();
                    response = requestBuilder.post(Entity.json(dto));
                    status = response.getStatus();
                }
            }
            
            if (status != 200 && status != 201 && status != 202) {
                String errorMsg = response.hasEntity() ? response.readEntity(String.class) : "Unknown error";
                throw new HcenUnavailableException("Error al registrar metadatos: HTTP " + status + " - " + errorMsg);
            }

        } catch (ProcessingException ex) {
            throw new HcenUnavailableException("HCEN no disponible", ex);
        } finally {
            if (response != null) {
                response.close();
            }
            if (client != null) {
                client.close();
            }
        }
    }

    /**
     * Envía el payload completo (incluyendo datosPatronimicos) al central.
     */
    public void registrarMetadatosCompleto(Map<String, Object> payload) throws HcenUnavailableException {
        String centralUrl = System.getProperty("HCEN_CENTRAL_URL",
                System.getenv().getOrDefault("HCEN_CENTRAL_URL", DEFAULT_CENTRAL_URL));

        // Obtener token de servicio
        String serviceToken = getServiceToken();

        Client client = null;
        jakarta.ws.rs.core.Response response = null;
        try {
            client = ClientBuilder.newClient();
            jakarta.ws.rs.client.Invocation.Builder requestBuilder = client.target(centralUrl)
                    .request(MediaType.APPLICATION_JSON);
            
            // Agregar token de servicio si está disponible
            if (serviceToken != null) {
                requestBuilder.header("Authorization", "Bearer " + serviceToken);
            }
            
            response = requestBuilder.post(Entity.json(payload));

            int status = response.getStatus();
            if (status == 401 || status == 403) {
                // Token inválido o expirado, limpiar cache y reintentar una vez
                LOG.warning("Token de servicio rechazado, limpiando cache");
                cachedServiceToken = null;
                tokenExpiryTime = 0;
                
                // Reintentar con nuevo token
                serviceToken = getServiceToken();
                if (serviceToken != null) {
                    requestBuilder = client.target(centralUrl)
                            .request(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + serviceToken);
                    response.close();
                    response = requestBuilder.post(Entity.json(payload));
                    status = response.getStatus();
                }
            }
            
            if (status != 200 && status != 201 && status != 202) {
                String errorMsg = response.hasEntity() ? response.readEntity(String.class) : "Unknown error";
                throw new HcenUnavailableException("Error al registrar metadatos: HTTP " + status + " - " + errorMsg);
            }

        } catch (ProcessingException ex) {
            throw new HcenUnavailableException("HCEN no disponible", ex);
        } finally {
            if (response != null) {
                response.close();
            }
            if (client != null) {
                client.close();
            }
        }
    }

    /**
     * Consulta metadatos de un paciente desde HCEN central.
     * 
     * @param documentoIdPaciente CI o documento de identidad del paciente
     * @return Lista de metadatos (Map<String, Object>)
     * @throws HcenUnavailableException si HCEN no está disponible
     */
    @SuppressWarnings("unchecked")
    public java.util.List<Map<String, Object>> consultarMetadatosPaciente(String documentoIdPaciente) 
            throws HcenUnavailableException {
        // URL base de HCEN central para endpoints de paciente
        // El endpoint es /api/paciente/{id}/metadatos
        String baseUrl = System.getProperty("HCEN_CENTRAL_BASE_URL",
                System.getenv().getOrDefault("HCEN_CENTRAL_BASE_URL", "http://localhost:8080/api"));
        
        // Construir URL del endpoint de paciente
        String pacienteUrl = baseUrl + "/paciente/" + documentoIdPaciente + "/metadatos";

        Client client = null;
        jakarta.ws.rs.core.Response response = null;
        try {
            client = ClientBuilder.newClient();
            response = client.target(pacienteUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .get();

            int status = response.getStatus();
            if (status == 200) {
                return response.readEntity(java.util.List.class);
            } else if (status == 404) {
                return new java.util.ArrayList<>(); // Lista vacía si no hay documentos
            } else {
                throw new HcenUnavailableException("Error al consultar metadatos: HTTP " + status);
            }

        } catch (ProcessingException ex) {
            throw new HcenUnavailableException("HCEN no disponible", ex);
        } finally {
            if (response != null) {
                response.close();
            }
            if (client != null) {
                client.close();
            }
        }
    }
}
