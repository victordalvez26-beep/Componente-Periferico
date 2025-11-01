
package uy.edu.tse.hcen.repository;

import uy.edu.tse.hcen.model.UsuarioPeriferico;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.NoResultException;

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
}
