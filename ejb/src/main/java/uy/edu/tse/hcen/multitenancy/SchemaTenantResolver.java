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
    public Object resolveCurrentTenantIdentifier() {
        String tenant = TenantContext.getCurrentTenant();
        if (tenant != null && !tenant.isBlank()) {
            return "schema_clinica_" + tenant;
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
