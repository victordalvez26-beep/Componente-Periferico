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
            String sql = "SELECT id, cod_doc, nombre, segundo_apellido, fecha_nacimiento, direccion, telefono, email, " +
                        "departamento, localidad, hcen_user_id, tenant_id, fecha_alta, fecha_actualizacion " +
                        "FROM " + schema + ".usuario_salud " +
                        "WHERE cod_doc = :ci AND tenant_id = :tenantId";
            
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
     * Mapea una fila de SQL nativo a una entidad UsuarioSalud.
     * 
     * @param row Array con los datos de la fila
     * @return Entidad UsuarioSalud mapeada
     */
    private UsuarioSalud mapRowToEntity(Object[] row) {
        UsuarioSalud usuario = new UsuarioSalud();
        
        usuario.setId(row[0] != null ? ((Number) row[0]).longValue() : null);
        usuario.setCodDoc((String) row[1]);
        usuario.setNombre((String) row[2]);
        usuario.setSegundoApellido((String) row[3]);
        
        // fecha_nacimiento (Date SQL -> Date)
        if (row[4] != null) {
            usuario.setFechaNacimiento((Date) row[4]);
        }
        
        usuario.setDireccion((String) row[5]);
        usuario.setTelefono((String) row[6]);
        usuario.setEmail((String) row[7]);
        if (row[8] != null) {
            usuario.setDepartamento(uy.edu.tse.hcen.model.enums.Departamentos.valueOf((String) row[8]));
        }
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

