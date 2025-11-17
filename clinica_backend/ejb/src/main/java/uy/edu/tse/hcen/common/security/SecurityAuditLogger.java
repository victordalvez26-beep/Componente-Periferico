package uy.edu.tse.hcen.common.security;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Componente para el registro de eventos de seguridad y auditoría.
 * Centraliza el logging de acciones críticas para facilitar la monitorización y el análisis forense.
 */
public class SecurityAuditLogger {

    private static final Logger AUDIT_LOG = Logger.getLogger("SecurityAuditLogger");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Constructor privado para evitar instanciación de esta clase de utilidad.
     */
    private SecurityAuditLogger() {
        throw new UnsupportedOperationException("Esta es una clase de utilidad y no debe ser instanciada");
    }

    /**
     * Registra un evento de acceso fallido.
     * @param userId ID del usuario que intentó acceder.
     * @param resource Recurso al que se intentó acceder.
     * @param reason Razón del fallo (ej. credenciales inválidas, permiso denegado).
     * @param sourceIp Dirección IP de origen.
     */
    public static void logFailedAccess(String userId, String resource, String reason, String sourceIp) {
        AUDIT_LOG.log(Level.WARNING,
            "AUDIT_FAILED_ACCESS | Timestamp: {0} | User: {1} | Resource: {2} | Reason: {3} | Source IP: {4}",
            new Object[]{LocalDateTime.now().format(FORMATTER), userId, resource, reason, sourceIp});
    }

    /**
     * Registra un evento de acceso fallido (sin IP).
     * @param userId ID del usuario que intentó acceder.
     * @param resource Recurso al que se intentó acceder.
     * @param reason Razón del fallo (ej. credenciales inválidas, permiso denegado).
     */
    public static void logFailedAccess(String userId, String resource, String reason) {
        AUDIT_LOG.log(Level.WARNING,
            "AUDIT_FAILED_ACCESS | Timestamp: {0} | User: {1} | Resource: {2} | Reason: {3}",
            new Object[]{LocalDateTime.now().format(FORMATTER), userId, resource, reason});
    }

    /**
     * Registra un acceso exitoso a un recurso sensible.
     * @param userId ID del usuario que accedió.
     * @param resource Recurso sensible accedido.
     * @param sourceIp Dirección IP de origen.
     */
    public static void logSensitiveAccess(String userId, String resource, String sourceIp) {
        AUDIT_LOG.log(Level.INFO,
            "AUDIT_SENSITIVE_ACCESS | Timestamp: {0} | User: {1} | Resource: {2} | Source IP: {3}",
            new Object[]{LocalDateTime.now().format(FORMATTER), userId, resource, sourceIp});
    }

    /**
     * Registra un acceso exitoso a un recurso sensible (sin IP).
     * @param userId ID del usuario que accedió.
     * @param resource Recurso sensible accedido.
     */
    public static void logSensitiveAccess(String userId, String resource) {
        AUDIT_LOG.log(Level.INFO,
            "AUDIT_SENSITIVE_ACCESS | Timestamp: {0} | User: {1} | Resource: {2}",
            new Object[]{LocalDateTime.now().format(FORMATTER), userId, resource});
    }

    /**
     * Registra un cambio en una política de seguridad.
     * @param adminId ID del administrador que realizó el cambio.
     * @param policyId ID de la política modificada.
     * @param changeDetails Detalles del cambio (ej. "alcance modificado de GLOBAL a POR_CLINICA").
     * @param sourceIp Dirección IP de origen.
     */
    public static void logPolicyChange(String adminId, String policyId, String changeDetails, String sourceIp) {
        AUDIT_LOG.log(Level.INFO,
            "AUDIT_POLICY_CHANGE | Timestamp: {0} | Admin: {1} | Policy ID: {2} | Details: {3} | Source IP: {4}",
            new Object[]{LocalDateTime.now().format(FORMATTER), adminId, policyId, changeDetails, sourceIp});
    }

    /**
     * Registra un cambio en una política de seguridad (sin IP).
     * @param adminId ID del administrador que realizó el cambio.
     * @param policyId ID de la política modificada.
     * @param changeDetails Detalles del cambio (ej. "alcance modificado de GLOBAL a POR_CLINICA").
     */
    public static void logPolicyChange(String adminId, String policyId, String changeDetails) {
        AUDIT_LOG.log(Level.INFO,
            "AUDIT_POLICY_CHANGE | Timestamp: {0} | Admin: {1} | Policy ID: {2} | Details: {3}",
            new Object[]{LocalDateTime.now().format(FORMATTER), adminId, policyId, changeDetails});
    }

    /**
     * Registra una violación de seguridad detectada (ej. intento de inyección SQL, ataque de fuerza bruta).
     * @param detectionType Tipo de detección (ej. "SQL_INJECTION", "RATE_LIMIT_EXCEEDED").
     * @param details Detalles de la violación.
     * @param sourceIp Dirección IP de origen.
     * @param userId ID del usuario asociado (si conocido).
     */
    public static void logSecurityViolation(String detectionType, String details, String sourceIp, String userId) {
        AUDIT_LOG.log(Level.SEVERE,
            "AUDIT_SECURITY_VIOLATION | Timestamp: {0} | Type: {1} | Details: {2} | Source IP: {3} | User: {4}",
            new Object[]{LocalDateTime.now().format(FORMATTER), detectionType, details, sourceIp, userId});
    }

    /**
     * Registra una violación de seguridad detectada (sin IP).
     * @param userId ID del usuario asociado.
     * @param detectionType Tipo de detección (ej. "SQL_INJECTION", "RATE_LIMIT_EXCEEDED").
     * @param details Detalles de la violación.
     */
    public static void logSecurityViolation(String userId, String detectionType, String details) {
        AUDIT_LOG.log(Level.SEVERE,
            "AUDIT_SECURITY_VIOLATION | Timestamp: {0} | Type: {1} | Details: {2} | User: {3}",
            new Object[]{LocalDateTime.now().format(FORMATTER), detectionType, details, userId});
    }

    /**
     * Registra un evento de autenticación (éxito o fallo).
     * @param userId ID del usuario.
     * @param success true si la autenticación fue exitosa, false si falló.
     * @param sourceIp Dirección IP de origen.
     * @param details Detalles adicionales (ej. "credenciales incorrectas").
     */
    public static void logAuthenticationEvent(String userId, boolean success, String sourceIp, String details) {
        Level level = success ? Level.INFO : Level.WARNING;
        String status = success ? "SUCCESS" : "FAILURE";
        AUDIT_LOG.log(level,
            "AUDIT_AUTHENTICATION | Timestamp: {0} | User: {1} | Status: {2} | Source IP: {3} | Details: {4}",
            new Object[]{LocalDateTime.now().format(FORMATTER), userId, status, sourceIp, details});
    }
}

