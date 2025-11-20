package uy.edu.tse.hcen.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import uy.edu.tse.hcen.dto.DTUsuarioSalud;
import uy.edu.tse.hcen.model.UsuarioSalud;
import uy.edu.tse.hcen.multitenancy.TenantContext;
import uy.edu.tse.hcen.repository.UsuarioSaludRepository;
import jakarta.ejb.EJB;
import jakarta.annotation.security.RolesAllowed;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Resource para gestionar Usuarios de Salud (pacientes) de una clínica.
 */
@Path("/clinica/{tenantId}/usuarios-salud")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMINISTRADOR", "PROFESIONAL"})
public class UsuarioSaludResource {

    private static final Logger LOG = Logger.getLogger(UsuarioSaludResource.class);

    @EJB
    private UsuarioSaludRepository usuarioSaludRepository;

    /**
     * Obtiene todos los usuarios de salud (pacientes) de una clínica.
     * 
     * @param tenantId ID de la clínica
     * @return Lista de pacientes
     */
    @GET
    public Response listAll(@PathParam("tenantId") String tenantId) {
        LOG.infof("Received GET usuarios-salud request for tenant: %s", tenantId);
        
        try {
            if (tenantId == null || tenantId.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "tenantId is required"))
                        .build();
            }
            
            // Establecer el tenant context
            TenantContext.setCurrentTenant(tenantId);
            
            try {
                Long tenantIdLong = Long.parseLong(tenantId);
                List<UsuarioSalud> usuarios = usuarioSaludRepository.findByTenant(tenantIdLong);
                List<DTUsuarioSalud> dtos = usuarios.stream()
                        .map(DTUsuarioSalud::fromEntity)
                        .collect(Collectors.toList());
                
                return Response.ok(dtos).build();
                
            } finally {
                TenantContext.clear();
            }
            
        } catch (NumberFormatException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "tenantId must be a valid number"))
                    .build();
        } catch (Exception ex) {
            LOG.error("Error listing usuarios-salud", ex);
            return Response.serverError()
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }

    /**
     * Obtiene un usuario de salud por su ID.
     * 
     * @param tenantId ID de la clínica
     * @param id ID del usuario
     * @return Usuario de salud
     */
    @GET
    @Path("/{id}")
    public Response getById(@PathParam("tenantId") String tenantId, @PathParam("id") Long id) {
        LOG.infof("Received GET usuarios-salud request for tenant: %s, id: %s", tenantId, id);
        
        try {
            if (tenantId == null || tenantId.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "tenantId is required"))
                        .build();
            }
            
            if (id == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "id is required"))
                        .build();
            }
            
            // Establecer el tenant context
            TenantContext.setCurrentTenant(tenantId);
            
            try {
                Long tenantIdLong = Long.parseLong(tenantId);
                UsuarioSalud usuario = usuarioSaludRepository.findById(id, tenantIdLong);
                
                if (usuario == null) {
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(Map.of("error", "Usuario no encontrado"))
                            .build();
                }
                
                return Response.ok(DTUsuarioSalud.fromEntity(usuario)).build();
                
            } finally {
                TenantContext.clear();
            }
            
        } catch (NumberFormatException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "tenantId must be a valid number"))
                    .build();
        } catch (Exception ex) {
            LOG.error("Error getting usuario-salud", ex);
            return Response.serverError()
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }

    /**
     * Crea un nuevo usuario de salud (paciente).
     * 
     * @param tenantId ID de la clínica
     * @param dto Datos del usuario
     * @return Usuario creado
     */
    @POST
    public Response create(@PathParam("tenantId") String tenantId, DTUsuarioSalud dto) {
        LOG.infof("Received POST usuarios-salud request for tenant: %s", tenantId);
        
        try {
            if (tenantId == null || tenantId.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "tenantId is required"))
                        .build();
            }
            
            if (dto == null || dto.getCodDoc() == null || dto.getCodDoc().isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "CI (codDoc) is required"))
                        .build();
            }
            
            // Establecer el tenant context
            TenantContext.setCurrentTenant(tenantId);
            
            try {
                Long tenantIdLong = Long.parseLong(tenantId);
                
                // Verificar si ya existe un usuario con ese CI
                UsuarioSalud existente = usuarioSaludRepository.findByCiAndTenant(dto.getCodDoc(), tenantIdLong);
                if (existente != null) {
                    return Response.status(Response.Status.CONFLICT)
                            .entity(Map.of("error", "Ya existe un usuario con ese CI"))
                            .build();
                }
                
                // Convertir DTO a entidad
                UsuarioSalud usuario = dto.toEntity();
                usuario.setTenantId(tenantIdLong);
                usuario.setFechaAlta(LocalDateTime.now());
                
                // Persistir
                usuarioSaludRepository.persist(usuario);
                
                return Response.status(Response.Status.CREATED)
                        .entity(DTUsuarioSalud.fromEntity(usuario))
                        .build();
                
            } finally {
                TenantContext.clear();
            }
            
        } catch (NumberFormatException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "tenantId must be a valid number"))
                    .build();
        } catch (Exception ex) {
            LOG.error("Error creating usuario-salud", ex);
            return Response.serverError()
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }

    /**
     * Actualiza un usuario de salud existente.
     * 
     * @param tenantId ID de la clínica
     * @param id ID del usuario
     * @param dto Datos actualizados
     * @return Usuario actualizado
     */
    @PUT
    @Path("/{id}")
    public Response update(@PathParam("tenantId") String tenantId, @PathParam("id") Long id, DTUsuarioSalud dto) {
        LOG.infof("Received PUT usuarios-salud request for tenant: %s, id: %s", tenantId, id);
        
        try {
            if (tenantId == null || tenantId.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "tenantId is required"))
                        .build();
            }
            
            if (id == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "id is required"))
                        .build();
            }
            
            // Establecer el tenant context
            TenantContext.setCurrentTenant(tenantId);
            
            try {
                Long tenantIdLong = Long.parseLong(tenantId);
                
                // Buscar usuario existente
                UsuarioSalud usuario = usuarioSaludRepository.findById(id, tenantIdLong);
                if (usuario == null) {
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(Map.of("error", "Usuario no encontrado"))
                            .build();
                }
                
                // Actualizar campos
                if (dto.getNombre() != null) usuario.setNombre(dto.getNombre());
                if (dto.getApellido() != null) usuario.setApellido(dto.getApellido());
                if (dto.getFechaNacimiento() != null) {
                    usuario.setFechaNacimiento(dto.getFechaNacimiento().toInstant()
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate());
                }
                if (dto.getDireccion() != null) usuario.setDireccion(dto.getDireccion());
                if (dto.getTelefono() != null) usuario.setTelefono(dto.getTelefono());
                if (dto.getEmail() != null) usuario.setEmail(dto.getEmail());
                if (dto.getDepartamento() != null) usuario.setDepartamento(dto.getDepartamento());
                if (dto.getLocalidad() != null) usuario.setLocalidad(dto.getLocalidad());
                
                usuario.setFechaActualizacion(LocalDateTime.now());
                
                // Actualizar
                usuarioSaludRepository.merge(usuario);
                
                return Response.ok(DTUsuarioSalud.fromEntity(usuario)).build();
                
            } finally {
                TenantContext.clear();
            }
            
        } catch (NumberFormatException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "tenantId must be a valid number"))
                    .build();
        } catch (Exception ex) {
            LOG.error("Error updating usuario-salud", ex);
            return Response.serverError()
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }

    /**
     * Elimina un usuario de salud.
     * 
     * @param tenantId ID de la clínica
     * @param id ID del usuario
     * @return 204 No Content
     */
    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("tenantId") String tenantId, @PathParam("id") Long id) {
        LOG.infof("Received DELETE usuarios-salud request for tenant: %s, id: %s", tenantId, id);
        
        try {
            if (tenantId == null || tenantId.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "tenantId is required"))
                        .build();
            }
            
            if (id == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "id is required"))
                        .build();
            }
            
            // Establecer el tenant context
            TenantContext.setCurrentTenant(tenantId);
            
            try {
                Long tenantIdLong = Long.parseLong(tenantId);
                
                // Buscar usuario existente
                UsuarioSalud usuario = usuarioSaludRepository.findById(id, tenantIdLong);
                if (usuario == null) {
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(Map.of("error", "Usuario no encontrado"))
                            .build();
                }
                
                // Eliminar
                usuarioSaludRepository.remove(usuario);
                
                return Response.noContent().build();
                
            } finally {
                TenantContext.clear();
            }
            
        } catch (NumberFormatException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "tenantId must be a valid number"))
                    .build();
        } catch (Exception ex) {
            LOG.error("Error deleting usuario-salud", ex);
            return Response.serverError()
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }
}
