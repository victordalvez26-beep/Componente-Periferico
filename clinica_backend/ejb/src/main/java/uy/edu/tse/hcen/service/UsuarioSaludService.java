package uy.edu.tse.hcen.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import uy.edu.tse.hcen.client.HcenUsuarioSaludClient;
import uy.edu.tse.hcen.model.UsuarioSalud;
import uy.edu.tse.hcen.repository.UsuarioSaludRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio para gestionar Usuarios de Salud (pacientes) en las clínicas periféricas.
 * Coordina el almacenamiento local y la sincronización con el INUS central del HCEN.
 */
@Stateless
public class UsuarioSaludService {
    
    private static final Logger LOGGER = Logger.getLogger(UsuarioSaludService.class);
    
    @Inject
    private UsuarioSaludRepository repository;
    
    @Inject
    private HcenUsuarioSaludClient hcenClient;
    
    /**
     * Crea un nuevo Usuario de Salud en la clínica.
     * 
     * Flujo:
     * 1. Valida que no exista ya en esta clínica
     * 2. Guarda localmente en el schema de la clínica
     * 3. Registra en el INUS central del HCEN
     * 4. Actualiza el hcenUserId con el ID devuelto por HCEN
     * 
     * @param tenantId ID de la clínica
     * @param usuario Datos del usuario de salud
     * @return El usuario creado y sincronizado con HCEN
     */
    @Transactional
    public UsuarioSalud crearUsuarioSalud(Long tenantId, UsuarioSalud usuario) {
        
        LOGGER.info("Creando Usuario de Salud - CI: " + usuario.getCi() + ", Clínica: " + tenantId);
        
        // 1. Validar que no exista ya en esta clínica
        UsuarioSalud existing = repository.findByCiAndTenant(usuario.getCi(), tenantId);
        if (existing != null) {
            throw new IllegalArgumentException(
                "Ya existe un paciente con CI " + usuario.getCi() + " en esta clínica"
            );
        }
        
        // 2. Setear metadatos
        usuario.setTenantId(tenantId);
        usuario.setFechaAlta(LocalDateTime.now());
        
        // 3. Guardar localmente (sin hcenUserId todavía)
        repository.persist(usuario);
        
        LOGGER.info("Usuario guardado localmente con ID: " + usuario.getId());
        
        // 4. Registrar en HCEN
        try {
            HcenUsuarioSaludClient.HcenUserResponse response = 
                hcenClient.registrarUsuarioEnHcen(tenantId, usuario);
            
            if (response.getUserId() != null) {
                // 5. Actualizar con el ID devuelto por HCEN
                usuario.setHcenUserId(response.getUserId());
                repository.merge(usuario);
                
                LOGGER.info("Usuario sincronizado con HCEN - hcenUserId: " + response.getUserId());
            } else {
                LOGGER.warn("No se pudo obtener hcenUserId del HCEN: " + response.getMensaje());
            }
        } catch (Exception e) {
            LOGGER.error("Error al sincronizar con HCEN: " + e.getMessage(), e);
            // Continuamos con el usuario guardado localmente aunque falle HCEN
        }
        
        return usuario;
    }
    
    /**
     * Actualiza un Usuario de Salud existente.
     * 
     * @param tenantId ID de la clínica
     * @param id ID del usuario a actualizar
     * @param datosActualizados Nuevos datos del usuario
     * @return El usuario actualizado
     */
    @Transactional
    public UsuarioSalud actualizarUsuarioSalud(Long tenantId, Long id, UsuarioSalud datosActualizados) {
        
        LOGGER.info("Actualizando Usuario de Salud ID: " + id + ", Clínica: " + tenantId);
        
        // 1. Buscar el usuario existente
        UsuarioSalud existing = repository.findById(id);
        if (existing == null || !existing.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Usuario no encontrado en esta clínica");
        }
        
        // 2. Actualizar campos locales
        existing.setNombre(datosActualizados.getNombre());
        existing.setApellido(datosActualizados.getApellido());
        existing.setFechaNacimiento(datosActualizados.getFechaNacimiento());
        existing.setDireccion(datosActualizados.getDireccion());
        existing.setTelefono(datosActualizados.getTelefono());
        existing.setEmail(datosActualizados.getEmail());
        existing.setDepartamento(datosActualizados.getDepartamento());
        existing.setLocalidad(datosActualizados.getLocalidad());
        existing.setFechaActualizacion(LocalDateTime.now());
        
        repository.merge(existing);
        
        LOGGER.info("Usuario actualizado localmente");
        
        // 3. Actualizar en HCEN
        try {
            hcenClient.registrarUsuarioEnHcen(tenantId, existing);
            LOGGER.info("Usuario sincronizado con HCEN");
        } catch (Exception e) {
            LOGGER.error("Error al sincronizar actualización con HCEN: " + e.getMessage(), e);
        }
        
        return existing;
    }
    
    /**
     * Obtiene todos los Usuarios de Salud de una clínica.
     * 
     * @param tenantId ID de la clínica
     * @return Lista de pacientes ordenados por apellido y nombre
     */
    public List<UsuarioSalud> listarUsuariosSalud(Long tenantId) {
        LOGGER.info("Listando usuarios de salud - Clínica: " + tenantId);
        return repository.findByTenant(tenantId);
    }
    
    /**
     * Obtiene un Usuario de Salud por su ID.
     * 
     * @param id ID del usuario
     * @param tenantId ID de la clínica (para validación)
     * @return El usuario si existe y pertenece a esta clínica
     */
    public UsuarioSalud obtenerUsuarioSalud(Long id, Long tenantId) {
        UsuarioSalud usuario = repository.findById(id);
        if (usuario != null && !usuario.getTenantId().equals(tenantId)) {
            return null; // No pertenece a esta clínica
        }
        return usuario;
    }
    
    /**
     * Busca un Usuario de Salud por su CI en una clínica.
     * 
     * @param ci Documento de identidad
     * @param tenantId ID de la clínica
     * @return El usuario si existe en esta clínica
     */
    public UsuarioSalud buscarPorCi(String ci, Long tenantId) {
        return repository.findByCiAndTenant(ci, tenantId);
    }
}

