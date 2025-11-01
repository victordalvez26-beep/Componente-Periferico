package uy.edu.tse.hcen.repository;

import uy.edu.tse.hcen.model.NodoPeriferico;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;

@Stateless
public class NodoPerifericoRepository {

    @PersistenceContext(unitName = "hcenPersistenceUnit")
    private EntityManager em;

    public Optional<NodoPeriferico> findById(Long id) {
        return Optional.ofNullable(em.find(NodoPeriferico.class, id));
    }
}
