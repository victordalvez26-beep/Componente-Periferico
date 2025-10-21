package com.clinica.multitenant;

import jakarta.enterprise.context.RequestScoped;
import java.io.Serializable;

/**
 * Almacena el ID de la clínica para la solicitud actual.
 */
@RequestScoped
public class TenantContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private String currentTenantId;

    public String getCurrentTenantId() {
        return currentTenantId;
    }

    public void setCurrentTenantId(String currentTenantId) {
        this.currentTenantId = currentTenantId;
    }
}
