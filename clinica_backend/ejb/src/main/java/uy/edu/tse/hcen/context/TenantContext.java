package uy.edu.tse.hcen.context;

import jakarta.enterprise.context.RequestScoped;
import java.io.Serializable;

/**
 * Contenedor CDI de ámbito de solicitud para almacenar el contexto del Tenant y Usuario
 * extraído del JWT. Es inyectado por el TenantAuthFilter y consumido por los Servicios (EJB/CDI).
 */
@RequestScoped
public class TenantContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tenantId;
    private String nickname;
    private String role; // Rol del usuario (e.g., "PROFESIONAL", "ADMINISTRADOR")

    public TenantContext() {
        // Constructor requerido por CDI
    }

    // --- Getters y Setters ---

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Verifica si el contexto está autenticado.
     */
    public boolean isAuthenticated() {
        return this.tenantId != null && this.nickname != null;
    }
}
