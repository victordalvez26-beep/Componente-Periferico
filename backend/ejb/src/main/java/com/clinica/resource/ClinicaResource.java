package com.clinica.resource;

import com.clinica.multitenant.TenantContext;
// import com.clinica.model.Paciente; // Modelo no implementado aún
// import com.clinica.repository.PacienteRepository; // Repositorio no implementado aún
// import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

@Path("/v1/clinica/pacientes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
// @RolesAllowed({"ADMIN_CLINICA", "PROFESIONAL"}) // Solo roles autorizados pueden acceder al recurso
public class ClinicaResource {

    @Inject
    private TenantContext tenantContext; // Para saber qué clínica está haciendo la petición

    // @Inject
    // private PacienteRepository pacienteRepository; // Objeto de acceso a datos (DAO) - aún no implementado

    /**
     * Endpoint para el portal de profesionales: crear un nuevo paciente.
     */
    @POST
    // @RolesAllowed("PROFESIONAL") // Solo el profesional puede crear
    public Response crearPaciente(/* Paciente nuevoPaciente */) {
        // Código comentado porque las clases de dominio/repositorio aún no existen.
        // nuevoPaciente.setTenantId(tenantContext.getCurrentTenantId());
        // pacienteRepository.save(nuevoPaciente);
        // return Response.status(Response.Status.CREATED).entity(nuevoPaciente).build();

        // Respuesta temporal para compilar/ejecutar hasta que el modelo/repo estén listos
        return Response.status(Response.Status.NOT_IMPLEMENTED)
                .entity("crearPaciente no implementado: implementar Paciente y PacienteRepository")
                .build();
    }

    /**
     * Endpoint para el portal de clínica/profesional: obtener todos los pacientes.
     */
    @GET
    // @RolesAllowed({"ADMIN_CLINICA", "PROFESIONAL"})
    public List<Object> obtenerPacientes() {
        // Código comentado porque las clases de dominio/repositorio aún no existen.
        // return pacienteRepository.findByTenantId(tenantContext.getCurrentTenantId());

        // Respuesta temporal: lista vacía
        return Collections.emptyList();
    }

    // Otros endpoints (modificar, eliminar, etc.) deben usar el mismo patrón.
}
