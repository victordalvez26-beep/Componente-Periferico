package uy.edu.tse.hcen.utils;

import java.util.logging.Logger;

/**
 * Utilidad para obtener la URL base del backend HCEN Central.
 * 
 * La URL se obtiene de la variable de entorno HCEN_CENTRAL_BASE_URL.
 * Si no está definida, se usa el valor por defecto: http://hcen-backend:8080
 * 
 * Esta clase centraliza todas las referencias a la URL del HCEN Central para
 * facilitar la configuración en diferentes ambientes (desarrollo, producción, etc.).
 */
public class HcenCentralUrlUtil {
    
    private static final Logger LOG = Logger.getLogger(HcenCentralUrlUtil.class.getName());
    
    /**
     * Nombre de la variable de entorno para la URL base del HCEN Central.
     */
    private static final String ENV_HCEN_CENTRAL_BASE_URL = "HCEN_CENTRAL_BASE_URL";
    
    /**
     * URL base por defecto del HCEN Central (comunicación interna Docker).
     */
    private static final String DEFAULT_HCEN_CENTRAL_BASE_URL = "http://hcen-backend:8080/hcen";
    
    /**
     * Obtiene la URL base del backend HCEN Central.
     * 
     * @return URL base del HCEN Central (sin trailing slash)
     */
    public static String getBaseUrl() {
        // Primero verificar variable de entorno
        String envUrl = System.getenv(ENV_HCEN_CENTRAL_BASE_URL);
        if (envUrl != null && !envUrl.trim().isEmpty()) {
            String url = envUrl.trim();
            // Remover trailing slash si existe
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }
            LOG.info("Usando HCEN_CENTRAL_BASE_URL desde variable de entorno: " + url);
            return url;
        }
        
        // Segundo verificar propiedad del sistema
        String propUrl = System.getProperty(ENV_HCEN_CENTRAL_BASE_URL);
        if (propUrl != null && !propUrl.trim().isEmpty()) {
            String url = propUrl.trim();
            // Remover trailing slash si existe
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }
            LOG.info("Usando HCEN_CENTRAL_BASE_URL desde propiedad del sistema: " + url);
            return url;
        }
        
        // Usar valor por defecto
        LOG.fine("Usando URL por defecto del HCEN Central: " + DEFAULT_HCEN_CENTRAL_BASE_URL);
        return DEFAULT_HCEN_CENTRAL_BASE_URL;
    }
    
    /**
     * Obtiene la URL base del HCEN Central con el path /api.
     * 
     * @return URL base + /api (sin trailing slash)
     */
    public static String getApiBaseUrl() {
        String baseUrl = getBaseUrl();
        return baseUrl + "/api";
    }
    
    /**
     * Construye una URL completa agregando un path al base URL.
     * 
     * @param path Path a agregar (debe empezar con /)
     * @return URL completa
     */
    public static String buildUrl(String path) {
        String baseUrl = getBaseUrl();
        if (path == null || path.isEmpty()) {
            return baseUrl;
        }
        // Asegurar que el path empiece con /
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return baseUrl + path;
    }
    
    /**
     * Construye una URL completa agregando un path al base URL + /api.
     * 
     * @param path Path a agregar (debe empezar con /)
     * @return URL completa con /api + path
     */
    public static String buildApiUrl(String path) {
        String apiBaseUrl = getApiBaseUrl();
        if (path == null || path.isEmpty()) {
            return apiBaseUrl;
        }
        // Asegurar que el path empiece con /
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return apiBaseUrl + path;
    }
}

