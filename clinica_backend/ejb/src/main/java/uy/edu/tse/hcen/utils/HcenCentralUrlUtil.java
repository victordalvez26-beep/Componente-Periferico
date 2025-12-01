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
     * Constructor privado para evitar instanciación de esta clase utilitaria.
     */
    private HcenCentralUrlUtil() {
        throw new UnsupportedOperationException("Esta es una clase utilitaria y no debe ser instanciada");
    }
    
    /**
     * Nombre de la variable de entorno para la URL base del HCEN Central.
     */
    private static final String ENV_HCEN_CENTRAL_BASE_URL = "HCEN_CENTRAL_BASE_URL";
    
    /**
     * URL base por defecto del HCEN Central (comunicación interna Docker).
     */
    private static final String DEFAULT_HCEN_CENTRAL_BASE_URL = "http://hcen-backend:8080/hcen";
    
    /**
     * Remueve el trailing slash de una URL si existe.
     * 
     * @param url URL a procesar
     * @return URL sin trailing slash
     */
    private static String removerTrailingSlash(String url) {
        if (url != null && url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
    
    /**
     * Normaliza y valida una URL desde variable de entorno o propiedad del sistema.
     * 
     * @param urlValue Valor de la URL a procesar
     * @param sourceOrigen Origen de la URL (para logging)
     * @return URL normalizada o null si está vacía
     */
    private static String normalizarUrl(String urlValue, String sourceOrigen) {
        if (urlValue != null && !urlValue.trim().isEmpty()) {
            String url = removerTrailingSlash(urlValue.trim());
            if (LOG.isLoggable(java.util.logging.Level.INFO)) {
                LOG.info(String.format("Usando HCEN_CENTRAL_BASE_URL desde %s: %s", sourceOrigen, url));
            }
            return url;
        }
        return null;
    }
    
    /**
     * Obtiene la URL base del backend HCEN Central.
     * 
     * @return URL base del HCEN Central (sin trailing slash)
     */
    public static String getBaseUrl() {
        // Primero verificar variable de entorno
        String envUrl = System.getenv(ENV_HCEN_CENTRAL_BASE_URL);
        String url = normalizarUrl(envUrl, "variable de entorno");
        if (url != null) {
            return url;
        }
        
        // Segundo verificar propiedad del sistema
        String propUrl = System.getProperty(ENV_HCEN_CENTRAL_BASE_URL);
        url = normalizarUrl(propUrl, "propiedad del sistema");
        if (url != null) {
            return url;
        }
        
        // Usar valor por defecto
        if (LOG.isLoggable(java.util.logging.Level.FINE)) {
            LOG.fine(String.format("Usando URL por defecto del HCEN Central: %s", DEFAULT_HCEN_CENTRAL_BASE_URL));
        }
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
     * Normaliza un path asegurando que empiece con /.
     * 
     * @param path Path a normalizar
     * @return Path normalizado o cadena vacía si es null
     */
    private static String normalizarPath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        // Asegurar que el path empiece con /
        if (!path.startsWith("/")) {
            return "/" + path;
        }
        return path;
    }
    
    /**
     * Construye una URL completa agregando un path al base URL.
     * 
     * @param path Path a agregar (debe empezar con /)
     * @return URL completa
     */
    public static String buildUrl(String path) {
        String baseUrl = getBaseUrl();
        String normalizedPath = normalizarPath(path);
        if (normalizedPath.isEmpty()) {
            return baseUrl;
        }
        return baseUrl + normalizedPath;
    }
    
    /**
     * Construye una URL completa agregando un path al base URL + /api.
     * 
     * @param path Path a agregar (debe empezar con /)
     * @return URL completa con /api + path
     */
    public static String buildApiUrl(String path) {
        String apiBaseUrl = getApiBaseUrl();
        String normalizedPath = normalizarPath(path);
        if (normalizedPath.isEmpty()) {
            return apiBaseUrl;
        }
        return apiBaseUrl + normalizedPath;
    }
}

