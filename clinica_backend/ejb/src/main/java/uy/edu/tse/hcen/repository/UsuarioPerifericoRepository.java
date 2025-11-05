
package uy.edu.tse.hcen.repository;

import uy.edu.tse.hcen.model.UsuarioPeriferico;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import java.math.BigInteger;

@Stateless
public class UsuarioPerifericoRepository {

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
                .setParameter("nickname", nickname)
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
        System.out.println("=== findByNicknameInTenantSchema: nickname=" + nickname + ", schema=" + schemaName);
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
            if (idObj instanceof Long) {
                user.setId((Long) idObj);
            } else if (idObj instanceof BigInteger) {
                user.setId(((BigInteger) idObj).longValue());
            } else if (idObj instanceof Integer) {
                user.setId(((Integer) idObj).longValue());
            }
            
            user.setNickname((String) row[1]);
            user.setPasswordHash((String) row[2]);
            
            // Role: usar el explícito, o deducirlo del dtype
            String role = (String) row[3];
            String dtype = row.length > 8 ? (String) row[8] : null;
            
            if (role == null || role.isBlank()) {
                // Deducir role del dtype
                if ("ProfesionalSalud".equals(dtype)) {
                    role = "PROFESIONAL";
                } else if ("AdministradorClinica".equals(dtype)) {
                    role = "ADMINISTRADOR";
                }
                System.out.println("=== Role deducido del dtype: " + dtype + " → " + role);
            }
            user.setRole(role);
            
            user.setNombre((String) row[4]);
            user.setEmail((String) row[5]);
            
            // Campos adicionales opcionales
            if (row.length > 6 && row[6] != null) {
                System.out.println("=== Especialidad: " + row[6]);
            }
            
            System.out.println("=== Usuario encontrado en tenant schema: " + user.getNickname() + ", role=" + role);
            return user;
        } catch (NoResultException e) {
            System.out.println("=== NoResultException en tenant schema");
            return null;
        } catch (Exception e) {
            System.out.println("=== Exception en findByNicknameInTenantSchema: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Busca un usuario por nickname SOLO en el schema público sin hacer JOINs.
     * Usado específicamente para login donde solo necesitamos datos básicos.
     */
    public UsuarioPeriferico findByNicknameForLogin(String nickname) {
        System.out.println("=== findByNicknameForLogin called with nickname: " + nickname);
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
            
            System.out.println("=== Query created, executing...");
            Object[] row = (Object[]) query.getSingleResult();
            System.out.println("=== Query returned " + row.length + " columns");
            
            // Mapear manualmente a UsuarioPeriferico
            UsuarioPeriferico user = new UsuarioPeriferico();
            
            // Manejar ID que puede venir como Long o BigInteger
            Object idObj = row[0];
            System.out.println("=== ID object type: " + (idObj != null ? idObj.getClass().getName() : "null"));
            if (idObj instanceof Long) {
                user.setId((Long) idObj);
            } else if (idObj instanceof BigInteger) {
                user.setId(((BigInteger) idObj).longValue());
            } else if (idObj instanceof Integer) {
                user.setId(((Integer) idObj).longValue());
            }
            
            user.setNickname((String) row[1]);
            user.setPasswordHash((String) row[2]);
            user.setTenantId((String) row[3]);
            user.setRole((String) row[4]);
            user.setNombre((String) row[5]);
            user.setEmail((String) row[6]);
            
            System.out.println("=== User mapped successfully: " + user.getNickname());
            return user;
        } catch (NoResultException e) {
            System.out.println("=== NoResultException: User not found");
            return null;
        } catch (Exception e) {
            System.out.println("=== Exception in findByNicknameForLogin: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
