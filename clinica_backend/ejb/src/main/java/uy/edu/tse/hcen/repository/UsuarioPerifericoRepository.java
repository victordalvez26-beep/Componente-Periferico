
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
     * Busca un usuario por nickname en el schema/tenant activo.
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
