package uy.edu.tse.hcen.multitenancy;

import jakarta.enterprise.context.RequestScoped;

/**
 * Resuelve el identificador del tenant y lo convierte en el nombre del esquema.
 * Si no hay un tenant establecido, devuelve el esquema por defecto.
 */
@RequestScoped
public class SchemaTenantResolver extends MultiTenantResolver {

    private static final String DEFAULT_SCHEMA = "public";

    @Override
    public String resolveCurrentTenantIdentifier() {
        if (this.tenantIdentifier != null && !this.tenantIdentifier.isBlank()) {
            return "schema_clinica_" + this.tenantIdentifier;
        }

        return DEFAULT_SCHEMA;
    }

    /**
     * Indica si se deben validar las sesiones actuales existentes.
     * En un contenedor gestionado (por ejemplo WildFly/Jakarta EE) devolvemos 'false'
     * para permitir que Hibernate utilice la estrategia de cambio de conexión por petición.
     */
    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}
