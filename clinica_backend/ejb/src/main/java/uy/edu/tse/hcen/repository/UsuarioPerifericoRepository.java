
package uy.edu.tse.hcen.repository;

import uy.edu.tse.hcen.model.UsuarioPeriferico;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import java.math.BigInteger;
import java.util.logging.Logger;
import java.util.logging.Level;

@Stateless
public class UsuarioPerifericoRepository {

    private static final Logger LOG = Logger.getLogger(UsuarioPerifericoRepository.class.getName());
    
    // Constantes para literales duplicados
    private static final String DTYPE_PROFESIONAL_SALUD = "ProfesionalSalud";
    private static final String DTYPE_ADMINISTRADOR_CLINICA = "AdministradorClinica";
    private static final String ROLE_PROFESIONAL = "PROFESIONAL";
    private static final String ROLE_ADMINISTRADOR = "ADMINISTRADOR";
    private static final String PARAM_NICKNAME = "nickname";

    @PersistenceContext(unitName = "hcenPersistenceUnit")
    private EntityManager em;

    /**
     * Busca un usuario por nickname en el schema/tenant activo usando JPA.
     * ADVERTENCIA: Este método usa herencia JOINED y puede fallar si las tablas
     * secundarias (profesionalsalud, administradorclinica) no existen o están vacías.
     */
    public UsuarioPeriferico findByNickname(String nickname) {
        try {
            return em.createQuery(
                "SELECT u FROM UsuarioPeriferico u WHERE u.nickname = :nickname", UsuarioPeriferico.class)
                .setParameter(PARAM_NICKNAME, nickname)
                .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Busca un usuario por nickname en el schema activo del tenant usando SQL nativo.
     * Útil para buscar profesionales en schema_clinica_XXX.usuario sin problemas de herencia.
     */
    public UsuarioPeriferico findByNicknameInTenantSchema(String nickname, String schemaName) {
        LOG.log(Level.FINE, "findByNicknameInTenantSchema: nickname={0}, schema={1}", new Object[]{nickname, schemaName});
        try {
            // Query nativa SQL en el schema del tenant - buscar en usuarioperiferico
            // NOTA: No incluye tenant_id porque el schema YA define el tenant
            Query query = em.createNativeQuery(
                "SELECT up.id, up.nickname, up.password_hash, up.role, " +
                "       up.nombre, up.email, up.especialidad, up.departamento, up.dtype " +
                "FROM " + schemaName + ".usuarioperiferico up " +
                "WHERE up.nickname = ?1"
            );
            query.setParameter(1, nickname);
            
            Object[] row = (Object[]) query.getSingleResult();
            
            // Mapear a UsuarioPeriferico
            UsuarioPeriferico user = new UsuarioPeriferico();
            
            Object idObj = row[0];
            if (idObj instanceof Long longValue) {
                user.setId(longValue);
            } else if (idObj instanceof BigInteger bigInteger) {
                user.setId(bigInteger.longValue());
            } else if (idObj instanceof Integer integerValue) {
                user.setId(integerValue.longValue());
            }
            
            user.setNickname((String) row[1]);
            user.setPasswordHash((String) row[2]);
            
            // Role: usar el explícito, o deducirlo del dtype
            String role = (String) row[3];
            String dtype = row.length > 8 ? (String) row[8] : null;
            
            if (role == null || role.isBlank()) {
                // Deducir role del dtype
                if (DTYPE_PROFESIONAL_SALUD.equals(dtype)) {
                    role = ROLE_PROFESIONAL;
                } else if (DTYPE_ADMINISTRADOR_CLINICA.equals(dtype)) {
                    role = ROLE_ADMINISTRADOR;
                }
                LOG.log(Level.FINE, "Role deducido del dtype: {0} → {1}", new Object[]{dtype, role});
            }
            user.setRole(role);
            
            user.setNombre((String) row[4]);
            user.setEmail((String) row[5]);
            
            LOG.log(Level.FINE, "Usuario encontrado en tenant schema: {0}, role={1}", new Object[]{user.getNickname(), role});
            return user;
        } catch (NoResultException e) {
            LOG.log(Level.FINE, "NoResultException en tenant schema");
            return null;
        } catch (Exception e) {
            String errorMsg = String.format("Exception en findByNicknameInTenantSchema: %s", e.getMessage());
            LOG.log(Level.WARNING, errorMsg, e);
            return null;
        }
    }

    /**
     * Busca un usuario por nickname SOLO en el schema público sin hacer JOINs.
     * Usado específicamente para login donde solo necesitamos datos básicos.
     */
    public UsuarioPeriferico findByNicknameForLogin(String nickname) {
        LOG.log(Level.FINE, "findByNicknameForLogin called with nickname: {0}", nickname);
        try {
            // Query nativa SQL para evitar JOINs de herencia
            Query query = em.createNativeQuery(
                "SELECT up.id, up.nickname, up.password_hash, up.tenant_id, up.role, " +
                "       u.nombre, u.email " +
                "FROM public.usuarioperiferico up " +
                "JOIN public.usuario u ON up.id = u.id " +
                "WHERE up.nickname = ?1"
            );
            query.setParameter(1, nickname);
            
            LOG.log(Level.FINE, "Query created, executing...");
            Object[] row = (Object[]) query.getSingleResult();
            LOG.log(Level.FINE, "Query returned {0} columns", row.length);
            
            // Mapear manualmente a UsuarioPeriferico
            UsuarioPeriferico user = new UsuarioPeriferico();
            
            // Manejar ID que puede venir como Long o BigInteger
            Object idObj = row[0];
            LOG.log(Level.FINE, "ID object type: {0}", idObj != null ? idObj.getClass().getName() : "null");
            if (idObj instanceof Long longValue) {
                user.setId(longValue);
            } else if (idObj instanceof BigInteger bigInteger) {
                user.setId(bigInteger.longValue());
            } else if (idObj instanceof Integer integerValue) {
                user.setId(integerValue.longValue());
            }
            
            user.setNickname((String) row[1]);
            user.setPasswordHash((String) row[2]);
            user.setTenantId((String) row[3]);
            user.setRole((String) row[4]);
            user.setNombre((String) row[5]);
            user.setEmail((String) row[6]);
            
            LOG.log(Level.FINE, "User mapped successfully: {0}", user.getNickname());
            return user;
        } catch (NoResultException e) {
            LOG.log(Level.FINE, "NoResultException: User not found");
            return null;
        } catch (Exception e) {
            String errorMsg = String.format("Exception in findByNicknameForLogin: %s - %s", e.getClass().getName(), e.getMessage());
            LOG.log(Level.WARNING, errorMsg, e);
            return null;
        }
    }
}
