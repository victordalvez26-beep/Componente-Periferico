
package uy.edu.tse.hcen.repository;

import uy.edu.tse.hcen.model.UsuarioPeriferico;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import java.math.BigInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class UsuarioPerifericoRepository {

    @PersistenceContext(unitName = "hcenPersistenceUnit")
    private EntityManager em;

    private static final Logger LOGGER = Logger.getLogger(UsuarioPerifericoRepository.class.getName());

    /**
     * Busca un usuario por nickname en el schema/tenant activo.
     */
    public UsuarioPeriferico findByNickname(String nickname) {

        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("nickname is required");
        }
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
        
        LOGGER.log(Level.FINE, "findByNicknameForLogin called with nickname: {0}", nickname);
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("nickname is required");
        }
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
            
            LOGGER.log(Level.FINE, "Query created, executing...");
            Object[] row = (Object[]) query.getSingleResult();
            LOGGER.log(Level.FINE, "Query returned {0} columns", row.length);
            
            // Mapear manualmente a UsuarioPeriferico
            UsuarioPeriferico user = new UsuarioPeriferico();
            
            // Manejar ID que puede venir como Long o BigInteger
            Object idObj = row[0];
            LOGGER.log(Level.FINE, "ID object type: {0}", idObj != null ? idObj.getClass().getName() : "null");
            if (idObj instanceof Long longId) {
                user.setId(longId);
            } else if (idObj instanceof BigInteger bigIntegerId) {
                user.setId(bigIntegerId.longValue());
            } else if (idObj instanceof Integer integerId) {
                user.setId(integerId.longValue());
            }
            
            user.setNickname((String) row[1]);
            user.setPasswordHash((String) row[2]);
            user.setTenantId((String) row[3]);
            user.setRole((String) row[4]);
            user.setNombre((String) row[5]);
            user.setEmail((String) row[6]);
            
            LOGGER.log(Level.FINE, "User mapped successfully: {0}", user.getNickname());
            return user;
        } catch (NoResultException e) {
            LOGGER.log(Level.FINE, "NoResultException: User not found");
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Exception in findByNicknameForLogin: {0} - {1}", 
                new Object[]{e.getClass().getName(), e.getMessage()});
            return null;
        }
    }
}
