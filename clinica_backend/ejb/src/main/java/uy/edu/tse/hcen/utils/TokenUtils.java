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

public class TokenUtils {

    private TokenUtils() {
        // utility class - prevent instantiation
    }

    // La clave secreta ahora se lee desde la variable de entorno HCEN_JWT_SECRET_BASE64
    // (ejecutar WildFly con la variable o usar un .env en desarrollo). No almacenar
    // secretos en el código fuente.
    private static final String SECRET_BASE64;
    private static final Key SIGNING_KEY;

    static {
        String secret = System.getenv("JWT_SECRET_BASE64");
        if (secret == null || secret.isBlank()) {
            secret = System.getProperty("hcen.jwt.secret.base64");
        }

        // Clave secreta por defecto para desarrollo (NO usar en producción)
        if (secret == null || secret.isBlank()) {
            secret = "dGVzdC1zZWNyZXQta2V5LWZvci1kZXZlbG9wbWVudC1vbmx5LWRvLW5vdC11c2UtaW4tcHJvZHVjdGlvbg==";
            System.err.println("WARNING: Usando clave JWT por defecto para desarrollo. NO usar en producción!");
        }

        SECRET_BASE64 = secret;
        try {
            SIGNING_KEY = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_BASE64));
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("La clave JWT no es una cadena Base64 válida.", ex);
        }
    }
    private static final long EXPIRATION_TIME_MS = 1000L * 60 * 60; // 1 hora

    /**
     * Genera un JWT, incluyendo el tenantId como Claim.
     */
    public static String generateToken(String subject, String role, String tenantId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("tenantId", tenantId); 

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject) 
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME_MS))
                .signWith(SIGNING_KEY, SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * Valida y parsea el token para obtener sus Claims.
     */
    public static Claims parseToken(String token) {
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(SIGNING_KEY)
                .build()
                .parseClaimsJws(token);
        
        return jws.getBody();
    }
    
    /**
     * Obtiene el ID del Tenant directamente desde el token validado.
     */
    public static String getTenantIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("tenantId", String.class);
    }
}