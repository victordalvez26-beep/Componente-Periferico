package uy.edu.tse.hcen.rest;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import uy.edu.tse.hcen.dto.UsuarioSaludDTO;
import uy.edu.tse.hcen.model.UsuarioSalud;
import uy.edu.tse.hcen.service.UsuarioSaludService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Endpoint REST para gestionar Usuarios de Salud (pacientes) en las clínicas periféricas.
 * Requiere autenticación con JWT y permisos de administrador de clínica.
 */
@Path("/clinica/{tenantId}/usuarios-salud")
@Stateless
public class UsuarioSaludResource {
    
    private static final Logger LOGGER = Logger.getLogger(UsuarioSaludResource.class);
    
    @Inject
    private UsuarioSaludService service;
    
    /**
     * Crea un nuevo Usuario de Salud (paciente) en la clínica.
     * 
     * @param tenantId ID de la clínica
     * @param dto Datos del paciente
     * @return El paciente creado
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response crearUsuarioSalud(
        @PathParam("tenantId") Long tenantId,
        UsuarioSaludDTO dto) {
        
        LOGGER.info("POST /clinica/" + tenantId + "/usuarios-salud - Crear paciente CI: " + dto.getCi());
        
        try {
            // Convertir DTO a entidad
            UsuarioSalud usuario = dtoToEntity(dto);
            
            // Crear usuario
            UsuarioSalud creado = service.crearUsuarioSalud(tenantId, usuario);
            
            // Convertir a DTO de respuesta
            UsuarioSaludDTO responseDto = entityToDto(creado);
            
            return Response.status(Response.Status.CREATED)
                .entity(responseDto)
                .build();
                
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Error de validación: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
                
        } catch (Exception e) {
            LOGGER.error("Error al crear usuario de salud: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error interno al crear paciente: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Obtiene todos los Usuarios de Salud de una clínica.
     * 
     * @param tenantId ID de la clínica
     * @return Lista de pacientes
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listarUsuariosSalud(@PathParam("tenantId") Long tenantId) {
        
        LOGGER.info("GET /clinica/" + tenantId + "/usuarios-salud - Listar pacientes");
        
        try {
            List<UsuarioSalud> usuarios = service.listarUsuariosSalud(tenantId);
            
            List<UsuarioSaludDTO> dtos = usuarios.stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
            
            return Response.ok(dtos).build();
            
        } catch (Exception e) {
            LOGGER.error("Error al listar usuarios de salud: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error al listar pacientes: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Obtiene un Usuario de Salud específico por su ID.
     * 
     * @param tenantId ID de la clínica
     * @param id ID del paciente
     * @return El paciente si existe
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtenerUsuarioSalud(
        @PathParam("tenantId") Long tenantId,
        @PathParam("id") Long id) {
        
        LOGGER.info("GET /clinica/" + tenantId + "/usuarios-salud/" + id);
        
        try {
            UsuarioSalud usuario = service.obtenerUsuarioSalud(id, tenantId);
            
            if (usuario == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Paciente no encontrado"))
                    .build();
            }
            
            return Response.ok(entityToDto(usuario)).build();
            
        } catch (Exception e) {
            LOGGER.error("Error al obtener usuario de salud: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error al obtener paciente: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Actualiza un Usuario de Salud existente.
     * 
     * @param tenantId ID de la clínica
     * @param id ID del paciente a actualizar
     * @param dto Nuevos datos del paciente
     * @return El paciente actualizado
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response actualizarUsuarioSalud(
        @PathParam("tenantId") Long tenantId,
        @PathParam("id") Long id,
        UsuarioSaludDTO dto) {
        
        LOGGER.info("PUT /clinica/" + tenantId + "/usuarios-salud/" + id);
        
        try {
            UsuarioSalud usuario = dtoToEntity(dto);
            UsuarioSalud actualizado = service.actualizarUsuarioSalud(tenantId, id, usuario);
            
            return Response.ok(entityToDto(actualizado)).build();
            
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Error de validación: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
                
        } catch (Exception e) {
            LOGGER.error("Error al actualizar usuario de salud: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error al actualizar paciente: " + e.getMessage()))
                .build();
        }
    }
    
    // Helper methods para conversión DTO <-> Entity
    
    private UsuarioSalud dtoToEntity(UsuarioSaludDTO dto) {
        UsuarioSalud usuario = new UsuarioSalud();
        usuario.setId(dto.getId());
        usuario.setCi(dto.getCi());
        usuario.setNombre(dto.getNombre());
        usuario.setApellido(dto.getApellido());
        usuario.setFechaNacimiento(dto.getFechaNacimiento());
        usuario.setDireccion(dto.getDireccion());
        usuario.setTelefono(dto.getTelefono());
        usuario.setEmail(dto.getEmail());
        usuario.setDepartamento(dto.getDepartamento());
        usuario.setLocalidad(dto.getLocalidad());
        usuario.setHcenUserId(dto.getHcenUserId());
        return usuario;
    }
    
    private UsuarioSaludDTO entityToDto(UsuarioSalud entity) {
        UsuarioSaludDTO dto = new UsuarioSaludDTO();
        dto.setId(entity.getId());
        dto.setCi(entity.getCi());
        dto.setNombre(entity.getNombre());
        dto.setApellido(entity.getApellido());
        dto.setFechaNacimiento(entity.getFechaNacimiento());
        dto.setDireccion(entity.getDireccion());
        dto.setTelefono(entity.getTelefono());
        dto.setEmail(entity.getEmail());
        dto.setDepartamento(entity.getDepartamento());
        dto.setLocalidad(entity.getLocalidad());
        dto.setHcenUserId(entity.getHcenUserId());
        return dto;
    }
    
    /**
     * Clase interna para respuestas de error.
     */
    public static class ErrorResponse {
        private String error;
        
        public ErrorResponse(String error) {
            this.error = error;
        }
        
        public String getError() {
            return error;
        }
        
        public void setError(String error) {
            this.error = error;
        }
    }
}

