package uy.edu.tse.hcen.repository;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import uy.edu.tse.hcen.model.UsuarioSalud;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
     * Usa SQL nativo para evitar problemas con multi-tenancy de Hibernate.
     * 
     * @param ci Documento de identidad
     * @param tenantId ID de la clínica
     * @return El usuario si existe, null en caso contrario
     */
    public UsuarioSalud findByCiAndTenant(String ci, Long tenantId) {
        try {
            String schema = "schema_clinica_" + tenantId;
            String sql = "SELECT id, ci, nombre, apellido, fecha_nacimiento, direccion, telefono, email, " +
                        "departamento, localidad, hcen_user_id, tenant_id, fecha_alta, fecha_actualizacion " +
                        "FROM " + schema + ".usuario_salud " +
                        "WHERE ci = :ci AND tenant_id = :tenantId";
            
            Query query = em.createNativeQuery(sql);
            query.setParameter("ci", ci);
            query.setParameter("tenantId", tenantId);
            
            Object[] row = (Object[]) query.getSingleResult();
            return mapRowToEntity(row);
            
        } catch (NoResultException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Obtiene todos los pacientes de una clínica.
     * Usa SQL nativo para evitar problemas con multi-tenancy de Hibernate.
     * 
     * @param tenantId ID de la clínica
     * @return Lista de pacientes ordenados por apellido y nombre
     */
    @SuppressWarnings("unchecked")
    public List<UsuarioSalud> findByTenant(Long tenantId) {
        try {
            String schema = "schema_clinica_" + tenantId;
            String sql = "SELECT id, ci, nombre, apellido, fecha_nacimiento, direccion, telefono, email, " +
                        "departamento, localidad, hcen_user_id, tenant_id, fecha_alta, fecha_actualizacion " +
                        "FROM " + schema + ".usuario_salud " +
                        "WHERE tenant_id = :tenantId " +
                        "ORDER BY apellido, nombre";
            
            Query query = em.createNativeQuery(sql);
            query.setParameter("tenantId", tenantId);
            
            List<Object[]> rows = query.getResultList();
            List<UsuarioSalud> result = new ArrayList<>();
            for (Object[] row : rows) {
                result.add(mapRowToEntity(row));
            }
            return result;
            
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * Busca un paciente por ID usando el tenantId para construir el schema.
     * Nota: Requiere tenantId porque no podemos inferir el schema solo con el ID.
     * 
     * @param id ID del usuario
     * @param tenantId ID de la clínica
     * @return El usuario si existe, null en caso contrario
     */
    public UsuarioSalud findById(Long id, Long tenantId) {
        try {
            String schema = "schema_clinica_" + tenantId;
            String sql = "SELECT id, ci, nombre, apellido, fecha_nacimiento, direccion, telefono, email, " +
                        "departamento, localidad, hcen_user_id, tenant_id, fecha_alta, fecha_actualizacion " +
                        "FROM " + schema + ".usuario_salud " +
                        "WHERE id = :id";
            
            Query query = em.createNativeQuery(sql);
            query.setParameter("id", id);
            
            Object[] row = (Object[]) query.getSingleResult();
            return mapRowToEntity(row);
            
        } catch (NoResultException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Busca un paciente por su hcenUserId usando SQL nativo.
     * 
     * @param hcenUserId ID del User en HCEN
     * @param tenantId ID de la clínica
     * @return El usuario si existe, null en caso contrario
     */
    public UsuarioSalud findByHcenUserId(Long hcenUserId, Long tenantId) {
        try {
            String schema = "schema_clinica_" + tenantId;
            String sql = "SELECT id, ci, nombre, apellido, fecha_nacimiento, direccion, telefono, email, " +
                        "departamento, localidad, hcen_user_id, tenant_id, fecha_alta, fecha_actualizacion " +
                        "FROM " + schema + ".usuario_salud " +
                        "WHERE hcen_user_id = :hcenUserId AND tenant_id = :tenantId";
            
            Query query = em.createNativeQuery(sql);
            query.setParameter("hcenUserId", hcenUserId);
            query.setParameter("tenantId", tenantId);
            
            Object[] row = (Object[]) query.getSingleResult();
            return mapRowToEntity(row);
            
        } catch (NoResultException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Persiste un nuevo usuario de salud usando SQL nativo.
     * 
     * @param usuario El usuario a persistir
     */
    public void persist(UsuarioSalud usuario) {
        String schema = "schema_clinica_" + usuario.getTenantId();
        String sql = "INSERT INTO " + schema + ".usuario_salud " +
                    "(ci, nombre, apellido, fecha_nacimiento, direccion, telefono, email, " +
                    "departamento, localidad, hcen_user_id, tenant_id, fecha_alta) " +
                    "VALUES (:ci, :nombre, :apellido, :fechaNac, :direccion, :telefono, :email, " +
                    ":departamento, :localidad, :hcenUserId, :tenantId, :fechaAlta) " +
                    "RETURNING id";
        
        Query query = em.createNativeQuery(sql);
        query.setParameter("ci", usuario.getCi());
        query.setParameter("nombre", usuario.getNombre());
        query.setParameter("apellido", usuario.getApellido());
        query.setParameter("fechaNac", usuario.getFechaNacimiento() != null ? 
            Date.valueOf(usuario.getFechaNacimiento()) : null);
        query.setParameter("direccion", usuario.getDireccion());
        query.setParameter("telefono", usuario.getTelefono());
        query.setParameter("email", usuario.getEmail());
        query.setParameter("departamento", usuario.getDepartamento());
        query.setParameter("localidad", usuario.getLocalidad());
        query.setParameter("hcenUserId", usuario.getHcenUserId());
        query.setParameter("tenantId", usuario.getTenantId());
        query.setParameter("fechaAlta", usuario.getFechaAlta() != null ? 
            java.sql.Timestamp.valueOf(usuario.getFechaAlta()) : null);
        
        Long id = ((Number) query.getSingleResult()).longValue();
        usuario.setId(id);
    }
    
    /**
     * Actualiza un usuario de salud existente usando SQL nativo.
     * 
     * @param usuario El usuario a actualizar
     * @return El usuario actualizado
     */
    public UsuarioSalud merge(UsuarioSalud usuario) {
        String schema = "schema_clinica_" + usuario.getTenantId();
        String sql = "UPDATE " + schema + ".usuario_salud SET " +
                    "nombre = :nombre, apellido = :apellido, fecha_nacimiento = :fechaNac, " +
                    "direccion = :direccion, telefono = :telefono, email = :email, " +
                    "departamento = :departamento, localidad = :localidad, " +
                    "hcen_user_id = :hcenUserId, fecha_actualizacion = :fechaAct " +
                    "WHERE id = :id";
        
        Query query = em.createNativeQuery(sql);
        query.setParameter("nombre", usuario.getNombre());
        query.setParameter("apellido", usuario.getApellido());
        query.setParameter("fechaNac", usuario.getFechaNacimiento() != null ? 
            Date.valueOf(usuario.getFechaNacimiento()) : null);
        query.setParameter("direccion", usuario.getDireccion());
        query.setParameter("telefono", usuario.getTelefono());
        query.setParameter("email", usuario.getEmail());
        query.setParameter("departamento", usuario.getDepartamento());
        query.setParameter("localidad", usuario.getLocalidad());
        query.setParameter("hcenUserId", usuario.getHcenUserId());
        query.setParameter("fechaAct", usuario.getFechaActualizacion() != null ? 
            java.sql.Timestamp.valueOf(usuario.getFechaActualizacion()) : null);
        query.setParameter("id", usuario.getId());
        
        query.executeUpdate();
        return usuario;
    }
    
    /**
     * Elimina un usuario de salud.
     * 
     * @param usuario El usuario a eliminar
     */
    public void remove(UsuarioSalud usuario) {
        String schema = "schema_clinica_" + usuario.getTenantId();
        String sql = "DELETE FROM " + schema + ".usuario_salud WHERE id = :id";
        
        Query query = em.createNativeQuery(sql);
        query.setParameter("id", usuario.getId());
        query.executeUpdate();
    }
    
    /**
     * Mapea una fila de SQL nativo a una entidad UsuarioSalud.
     * 
     * @param row Array con los datos de la fila
     * @return Entidad UsuarioSalud mapeada
     */
    private UsuarioSalud mapRowToEntity(Object[] row) {
        UsuarioSalud usuario = new UsuarioSalud();
        
        usuario.setId(row[0] != null ? ((Number) row[0]).longValue() : null);
        usuario.setCi((String) row[1]);
        usuario.setNombre((String) row[2]);
        usuario.setApellido((String) row[3]);
        
        // fecha_nacimiento (Date SQL -> LocalDate)
        if (row[4] != null) {
            usuario.setFechaNacimiento(((Date) row[4]).toLocalDate());
        }
        
        usuario.setDireccion((String) row[5]);
        usuario.setTelefono((String) row[6]);
        usuario.setEmail((String) row[7]);
        usuario.setDepartamento((String) row[8]);
        usuario.setLocalidad((String) row[9]);
        usuario.setHcenUserId(row[10] != null ? ((Number) row[10]).longValue() : null);
        usuario.setTenantId(row[11] != null ? ((Number) row[11]).longValue() : null);
        
        // fecha_alta (Timestamp SQL -> LocalDateTime)
        if (row[12] != null) {
            usuario.setFechaAlta(((java.sql.Timestamp) row[12]).toLocalDateTime());
        }
        
        // fecha_actualizacion (Timestamp SQL -> LocalDateTime)
        if (row[13] != null) {
            usuario.setFechaActualizacion(((java.sql.Timestamp) row[13]).toLocalDateTime());
        }
        
        return usuario;
    }
}

