package uy.edu.tse.hcen.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import uy.edu.tse.hcen.rest.filter.AuthTokenFilter;
import uy.edu.tse.hcen.rest.admin.AdminTenantResource;
import uy.edu.tse.hcen.rest.StatsResource;
import uy.edu.tse.hcen.rest.UsuarioSaludResource;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/api")
public class RestApplication extends Application {
    
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        // Registrar todos los recursos REST explícitamente
        classes.add(AuthResource.class);
        classes.add(ConfigResource.class);
        classes.add(DocumentoClinicoResource.class);
        classes.add(DocumentoPdfResource.class);
        classes.add(DocumentoPacienteResource.class);
        // DocumentoContenidoResource, DocumentoPoliticasResource, DocumentoSolicitudesResource
        // se descubren automáticamente por JAX-RS (tienen @Path y son manejados por CDI)
        classes.add(ProfesionalSaludResource.class);
        classes.add(PortalConfiguracionResource.class);
        classes.add(StatsResource.class);
        classes.add(UsuarioSaludResource.class);
        classes.add(AdminTenantResource.class);
        // Registrar filtros como providers
        classes.add(AuthTokenFilter.class);
        return classes;
    }
}
