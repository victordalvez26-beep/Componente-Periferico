package uy.edu.tse.hcen.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Utilidad para generar y validar tokens JWT entre servicios.
 * Los tokens de servicio tienen un formato especial y una expiración más larga.
 */
public class ServiceAuthUtil {

    private static final Logger LOGGER = Logger.getLogger(ServiceAuthUtil.class.getName());
    
    // Clave secreta compartida entre servicios (debe ser la misma en todos los servicios)
    private static final String SERVICE_SECRET_BASE64;
    private static final Key SIGNING_KEY;
    
    static {
        String secret = System.getenv("HCEN_SERVICE_SECRET_BASE64");
        if (secret == null || secret.isBlank()) {
            secret = System.getProperty("hcen.service.secret.base64");
        }
        
        // Para desarrollo, usar una clave por defecto (NO usar en producción)
        // Este secret debe coincidir con el de HCEN Central (mismo secret en BASE64)
        if (secret == null || secret.isBlank()) {
            // BASE64 de "HCEN_SERVICE_SECRET_KEY_2025_PRODUCTION_CHANGE_THIS"
            secret = "SENFTl9TRVJWSUNFX1NFQ1JFVF9LRVlfMjAyNV9QUk9EVUNUSU9OX0NIQU5HRV9USElT";
            LOGGER.warning("⚠️ Usando clave de servicio por defecto. Configurar HCEN_SERVICE_SECRET_BASE64 en producción!");
        }
        
        SERVICE_SECRET_BASE64 = secret;
        try {
            SIGNING_KEY = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SERVICE_SECRET_BASE64));
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("La clave de servicio JWT no es una cadena Base64 válida.", ex);
        }
    }
    
    private static final long SERVICE_TOKEN_EXPIRATION_MS = 1000L * 60 * 60 * 24; // 24 horas para tokens de servicio

    /**
     * Genera un token JWT para autenticación entre servicios.
     * 
     * @param serviceId Identificador del servicio (ej: "componente-periferico", "rndc")
     * @param serviceName Nombre descriptivo del servicio
     * @return Token JWT firmado
     */
    public static String generateServiceToken(String serviceId, String serviceName) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("service", serviceName != null ? serviceName : serviceId);
        claims.put("type", "service"); // Marca que es un token de servicio

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(serviceId)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + SERVICE_TOKEN_EXPIRATION_MS))
                .setIssuer("HCEN")
                .signWith(SIGNING_KEY, SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * Valida y parsea el token de servicio para obtener sus Claims.
     * 
     * @param token Token JWT a validar
     * @return Claims si es válido
     * @throws io.jsonwebtoken.JwtException si el token es inválido
     */
    public static Claims parseServiceToken(String token) {
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(SIGNING_KEY)
                .require("type", "service") // Asegurar que es un token de servicio
                .build()
                .parseClaimsJws(token);
        
        return jws.getBody();
    }
    
    /**
     * Obtiene el serviceId directamente desde el token validado.
     */
    public static String getServiceIdFromToken(String token) {
        Claims claims = parseServiceToken(token);
        return claims.getSubject();
    }
    
    /**
     * Valida si un token es un token de servicio válido.
     * 
     * @param token Token a validar
     * @return true si es válido, false si no
     */
    public static boolean isValidServiceToken(String token) {
        try {
            parseServiceToken(token);
            return true;
        } catch (Exception e) {
            LOGGER.warning("Token de servicio inválido: " + e.getMessage());
            return false;
        }
    }
}

