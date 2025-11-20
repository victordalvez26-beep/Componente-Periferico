package uy.edu.tse.hcen.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.logging.Logger;

/**
 * Utilidad para generar tokens JWT de servicio para autenticación entre servicios.
 * 
 * Los tokens de servicio permiten que el componente periférico se autentique
 * con el backend HCEN sin necesidad de credenciales de usuario.
 */
public class ServiceAuthUtil {
    
    private static final Logger LOG = Logger.getLogger(ServiceAuthUtil.class.getName());
    
    // Secret compartido para firmar tokens de servicio
    // En producción, esto debería venir de una variable de entorno
    private static final String DEFAULT_SERVICE_SECRET = "TSE_2025_HCEN_SERVICE_SECRET_KEY";
    private static final String ALGORITHM = "HmacSHA256";
    
    /**
     * Genera un token JWT de servicio.
     * 
     * @param serviceId ID del servicio (ej: "componente-periferico")
     * @param serviceName Nombre descriptivo del servicio
     * @return Token JWT firmado
     */
    public static String generateServiceToken(String serviceId, String serviceName) {
        try {
            String secret = getServiceSecret();
            
            long now = Instant.now().getEpochSecond();
            long exp = now + (24 * 60 * 60); // 24 horas de validez
            
            // Header
            String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String encodedHeader = base64UrlEncode(header);
            
            // Payload
            String payload = String.format(
                "{\"sub\":\"%s\",\"serviceName\":\"%s\",\"iat\":%d,\"exp\":%d,\"iss\":\"HCEN-Service\"}",
                serviceId, serviceName, now, exp
            );
            String encodedPayload = base64UrlEncode(payload);
            
            // Signature
            String dataToSign = encodedHeader + "." + encodedPayload;
            String signature = signHmacSHA256(dataToSign, secret);
            
            return dataToSign + "." + signature;
            
        } catch (Exception e) {
            LOG.severe("Error generando token de servicio: " + e.getMessage());
            throw new RuntimeException("Error generando token de servicio", e);
        }
    }
    
    /**
     * Obtiene el secret para firmar tokens de servicio.
     * Busca en variables de entorno primero, luego en propiedades del sistema.
     */
    private static String getServiceSecret() {
        String secret = System.getenv("HCEN_SERVICE_SECRET");
        if (secret == null || secret.isBlank()) {
            secret = System.getProperty("hcen.service.secret");
        }
        if (secret == null || secret.isBlank()) {
            LOG.warning("HCEN_SERVICE_SECRET no configurado, usando secret por defecto (NO RECOMENDADO PARA PRODUCCIÓN)");
            return DEFAULT_SERVICE_SECRET;
        }
        return secret;
    }
    
    /**
     * Codifica en Base64 URL-safe.
     */
    private static String base64UrlEncode(String data) {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Firma datos usando HMAC-SHA256.
     */
    private static String signHmacSHA256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance(ALGORITHM);
        SecretKeySpec secretKeySpec = new SecretKeySpec(
            secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        mac.init(secretKeySpec);
        byte[] signatureBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return base64UrlEncode(new String(signatureBytes, StandardCharsets.UTF_8));
    }
}

