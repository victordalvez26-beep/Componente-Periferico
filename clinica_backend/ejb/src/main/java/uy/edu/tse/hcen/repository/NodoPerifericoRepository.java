package uy.edu.tse.hcen.repository;

import uy.edu.tse.hcen.model.NodoPeriferico;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class NodoPerifericoRepository {

    @PersistenceContext(unitName = "hcenPersistenceUnit")
    private EntityManager em;

    private static final Logger LOGGER = Logger.getLogger(NodoPerifericoRepository.class.getName());

    public Optional<NodoPeriferico> findById(Long id) {
        try {
            NodoPeriferico found = em.find(NodoPeriferico.class, id);
            if (found != null) {
                return Optional.of(found);
            }
            // If not found via find, try getReference (returns a proxy). This helps when
            // joined-inheritance rows exist but Hibernate resolution differs between find
            // and queries on some environments. getReference will not hit the DB immediately.
            return tryGetReference(id);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "NodoPerifericoRepository.findById error", ex);
            return Optional.empty();
        }
    }

    /**
     * Intenta obtener una referencia proxy del NodoPeriferico usando getReference.
     * 
     * @param id ID del nodo periférico
     * @return Optional con la referencia si tiene éxito, Optional.empty() si falla
     */
    private Optional<NodoPeriferico> tryGetReference(Long id) {
        try {
            NodoPeriferico ref = em.getReference(NodoPeriferico.class, id);
            return Optional.of(ref);
        } catch (Exception ex) {
            LOGGER.log(Level.FINE, "getReference failed for NodoPeriferico id={0}", id);
            return Optional.empty();
        }
    }
}
