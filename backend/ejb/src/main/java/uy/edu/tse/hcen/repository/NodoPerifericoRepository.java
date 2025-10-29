package uy.edu.tse.hcen.repository;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import uy.edu.tse.hcen.model.NodoPeriferico;
import uy.edu.tse.hcen.model.enums.EstadoNodoPeriferico;

import java.util.List;
import uy.edu.tse.hcen.common.security.PasswordUtil;

@Stateless
public class NodoPerifericoRepository {

    @PersistenceContext
    private EntityManager em;

    public NodoPeriferico create(NodoPeriferico nodo) {

        // Defensive: if a nodo with same RUT already exists, return it instead of inserting.
        if (nodo.getRUT() != null) {
            NodoPeriferico existing = findByRUT(nodo.getRUT());
            if (existing != null) {
                // preserve existing estado and do not modify DB
                return existing;
            }
        }

        if (nodo.getNodoPerifericoPassword() != null && !nodo.getNodoPerifericoPassword().isBlank()) {
            String salt = PasswordUtil.generateSalt();
            String hash = PasswordUtil.hashPassword(nodo.getNodoPerifericoPassword().toCharArray(), salt);
            nodo.setPasswordSalt(salt);
            nodo.setPasswordHash(hash);
            nodo.setNodoPerifericoPassword(null); 
        }
        if (nodo.getEstado() == null) {
            nodo.setEstado(EstadoNodoPeriferico.PENDIENTE);
        }
        if (nodo.getFechaAlta() == null) {
            nodo.setFechaAlta(java.time.OffsetDateTime.now());
        }
        em.persist(nodo);
        return nodo;
    }

    public NodoPeriferico update(NodoPeriferico nodo) {
        if (nodo.getNodoPerifericoPassword() != null && !nodo.getNodoPerifericoPassword().isBlank()) {
            String salt = PasswordUtil.generateSalt();
            String hash = PasswordUtil.hashPassword(nodo.getNodoPerifericoPassword().toCharArray(), salt);
            nodo.setPasswordSalt(salt);
            nodo.setPasswordHash(hash);
            nodo.setNodoPerifericoPassword(null);
        }
        return em.merge(nodo);
    }

    public void delete(Long id) {
        NodoPeriferico ref = em.find(NodoPeriferico.class, id);
        if (ref != null) {
            em.remove(ref);
        }
    }

    public NodoPeriferico find(Long id) {
        return em.find(NodoPeriferico.class, id);
    }

    /**
     * Update only the estado of a Nodo in a new transaction.
     * This is used when the caller transaction is marked for rollback
     * but we still want to persist an Estado change reliably.
     */
    @jakarta.ejb.TransactionAttribute(jakarta.ejb.TransactionAttributeType.REQUIRES_NEW)
    public void updateEstadoInNewTx(Long id, EstadoNodoPeriferico estado) {
        NodoPeriferico ref = em.find(NodoPeriferico.class, id);
        if (ref != null) {
            ref.setEstado(estado);
            em.merge(ref);
        }
    }

    public NodoPeriferico findByRUT(String rut) {
        List<NodoPeriferico> list = em.createQuery("SELECT n FROM NodoPeriferico n WHERE n.RUT = :rut", NodoPeriferico.class)
                .setParameter("rut", rut)
                .getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    public List<NodoPeriferico> findAll() {
        return em.createQuery("SELECT n FROM NodoPeriferico n", NodoPeriferico.class).getResultList();
    }
}
