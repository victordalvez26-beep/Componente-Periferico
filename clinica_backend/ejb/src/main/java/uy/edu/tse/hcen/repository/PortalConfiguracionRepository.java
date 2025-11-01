package uy.edu.tse.hcen.repository;
import jakarta.persistence.NoResultException;
import uy.edu.tse.hcen.model.PortalConfiguracion;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;

@Stateless
public class PortalConfiguracionRepository {

    @PersistenceContext(unitName = "myPersistenceUnit")
    private EntityManager em;

    /**
     * Busca la única configuración existente en el schema del tenant actual.
     */
    public Optional<PortalConfiguracion> findCurrentConfig() {
        try {
            // Asume que solo existe una configuración o la encuentra por el ID fijo (1L)
            PortalConfiguracion config = em.createQuery(
                "SELECT c FROM PortalConfiguracion c", PortalConfiguracion.class)
                .setMaxResults(1)
                .getSingleResult();
            return Optional.of(config);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public PortalConfiguracion save(PortalConfiguracion config) {
        if (config.getId() == null) {
            em.persist(config);
            return config;
        } else {
            return em.merge(config);
        }
    }
}
