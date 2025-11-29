package uy.edu.tse.hcen.repository;

import uy.edu.tse.hcen.model.ProfesionalSalud;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@Stateless
public class ProfesionalSaludRepository {

    @PersistenceContext(unitName = "hcenPersistenceUnit")
    private EntityManager em;

    /**
     * @return Todos los profesionales en el schema de la clínica actual.
     * El EntityManager debería estar usando el esquema del tenant actual
     * gracias a la configuración multi-tenancy de Hibernate.
     */
    public List<ProfesionalSalud> findAll() {
        // Verificar que el tenant esté establecido
        String currentTenant = uy.edu.tse.hcen.multitenancy.TenantContext.getCurrentTenant();
        if (currentTenant == null || currentTenant.isBlank()) {
            java.util.logging.Logger.getLogger(ProfesionalSaludRepository.class.getName())
                .warning("⚠️ TenantContext no está establecido al listar profesionales");
        } else {
            java.util.logging.Logger.getLogger(ProfesionalSaludRepository.class.getName())
                .info("🔍 Listando profesionales para tenant: " + currentTenant);
        }
        
        return em.createQuery("SELECT p FROM ProfesionalSalud p", ProfesionalSalud.class)
                 .getResultList();
    }

    public Optional<ProfesionalSalud> findById(Long id) {
        return Optional.ofNullable(em.find(ProfesionalSalud.class, id));
    }

    public Optional<ProfesionalSalud> findByNickname(String nickname) {
        List<ProfesionalSalud> list = em.createQuery(
                "SELECT p FROM ProfesionalSalud p WHERE p.nickname = :nick", ProfesionalSalud.class)
                .setParameter("nick", nickname)
                .setMaxResults(1)
                .getResultList();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<ProfesionalSalud> findByEmail(String email) {
        List<ProfesionalSalud> list = em.createQuery(
                "SELECT p FROM ProfesionalSalud p WHERE p.email = :email", ProfesionalSalud.class)
                .setParameter("email", email)
                .setMaxResults(1)
                .getResultList();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
    
    public ProfesionalSalud save(ProfesionalSalud profesional) {
        if (profesional.getId() == null) {
            em.persist(profesional); 
        } else {
            profesional = em.merge(profesional);
        }
        return profesional;
    }

    public void delete(ProfesionalSalud profesional) {
        ProfesionalSalud merged = em.merge(profesional); 
        em.remove(merged);
    }
}
