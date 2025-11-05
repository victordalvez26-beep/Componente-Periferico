package uy.edu.tse.hcen.repository;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import uy.edu.tse.hcen.model.UsuarioSalud;

import java.util.List;

/**
 * Repositorio para gestionar Usuarios de Salud (pacientes) en el esquema de cada clínica.
 */
@Stateless
public class UsuarioSaludRepository {
    
    @PersistenceContext(unitName = "hcenPersistenceUnit")
    private EntityManager em;
    
    /**
     * Busca un paciente por CI en una clínica específica.
     * 
     * @param ci Documento de identidad
     * @param tenantId ID de la clínica
     * @return El usuario si existe, null en caso contrario
     */
    public UsuarioSalud findByCiAndTenant(String ci, Long tenantId) {
        try {
            return em.createQuery(
                "SELECT u FROM UsuarioSalud u WHERE u.ci = :ci AND u.tenantId = :tenantId",
                UsuarioSalud.class)
                .setParameter("ci", ci)
                .setParameter("tenantId", tenantId)
                .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
    
    /**
     * Obtiene todos los pacientes de una clínica.
     * 
     * @param tenantId ID de la clínica
     * @return Lista de pacientes ordenados por apellido y nombre
     */
    public List<UsuarioSalud> findByTenant(Long tenantId) {
        return em.createQuery(
            "SELECT u FROM UsuarioSalud u WHERE u.tenantId = :tenantId ORDER BY u.apellido, u.nombre",
            UsuarioSalud.class)
            .setParameter("tenantId", tenantId)
            .getResultList();
    }
    
    /**
     * Busca un paciente por ID.
     * 
     * @param id ID del usuario
     * @return El usuario si existe, null en caso contrario
     */
    public UsuarioSalud findById(Long id) {
        return em.find(UsuarioSalud.class, id);
    }
    
    /**
     * Busca un paciente por su hcenUserId.
     * 
     * @param hcenUserId ID del User en HCEN
     * @param tenantId ID de la clínica
     * @return El usuario si existe, null en caso contrario
     */
    public UsuarioSalud findByHcenUserId(Long hcenUserId, Long tenantId) {
        try {
            return em.createQuery(
                "SELECT u FROM UsuarioSalud u WHERE u.hcenUserId = :hcenUserId AND u.tenantId = :tenantId",
                UsuarioSalud.class)
                .setParameter("hcenUserId", hcenUserId)
                .setParameter("tenantId", tenantId)
                .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
    
    /**
     * Persiste un nuevo usuario de salud.
     * 
     * @param usuario El usuario a persistir
     */
    public void persist(UsuarioSalud usuario) {
        em.persist(usuario);
    }
    
    /**
     * Actualiza un usuario de salud existente.
     * 
     * @param usuario El usuario a actualizar
     * @return El usuario actualizado
     */
    public UsuarioSalud merge(UsuarioSalud usuario) {
        return em.merge(usuario);
    }
    
    /**
     * Elimina un usuario de salud.
     * 
     * @param usuario El usuario a eliminar
     */
    public void remove(UsuarioSalud usuario) {
        if (!em.contains(usuario)) {
            usuario = em.merge(usuario);
        }
        em.remove(usuario);
    }
}

