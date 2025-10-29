package com.clinica.repository;

import com.clinica.model.Profesional;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import jakarta.persistence.NoResultException;

@ApplicationScoped
@Transactional
public class ProfesionalRepository extends AbstractRepository<Profesional> {

    

    public ProfesionalRepository() {
        super(Profesional.class);
    }

    /**
     * Encuentra profesionales por tenantId (Aislamiento Multi-Tenant)
     */
    public List<Profesional> findByTenantId(String tenantId) {
        return em.createQuery(
            "SELECT p FROM Profesional p WHERE p.tenantId = :tenantId", Profesional.class)
            .setParameter("tenantId", tenantId)
            .getResultList();
    }
    
    /**
     * Busca por matrícula y tenantId para el login.
     */
    public Profesional findByMatriculaAndTenantId(String matricula, String tenantId) {
        try {
            return em.createQuery(
                "SELECT p FROM Profesional p WHERE p.matricula = :matricula AND p.tenantId = :tenantId", Profesional.class)
                .setParameter("matricula", matricula)
                .setParameter("tenantId", tenantId)
                .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Find by id and tenantId to ensure admin operations are limited to the current tenant.
     */
    public Profesional findByIdAndTenantId(Long id, String tenantId) {
        try {
            return em.createQuery(
                "SELECT p FROM Profesional p WHERE p.id = :id AND p.tenantId = :tenantId", Profesional.class)
                .setParameter("id", id)
                .setParameter("tenantId", tenantId)
                .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
