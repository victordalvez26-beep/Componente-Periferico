package uy.edu.tse.hcen.repository;

import uy.edu.tse.hcen.model.ProfesionalSalud;
import uy.edu.tse.hcen.model.enums.Especialidad;
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
     */
    public List<ProfesionalSalud> findAll() {
        return em.createQuery("SELECT p FROM ProfesionalSalud p", ProfesionalSalud.class)
                 .getResultList();
    }

    public Optional<ProfesionalSalud> findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        return Optional.ofNullable(em.find(ProfesionalSalud.class, id));
    }

    public Optional<ProfesionalSalud> findByNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("nickname is required");
        }
        List<ProfesionalSalud> list = em.createQuery(
                "SELECT p FROM ProfesionalSalud p WHERE p.nickname = :nick", ProfesionalSalud.class)
                .setParameter("nick", nickname)
                .setMaxResults(1)
                .getResultList();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<ProfesionalSalud> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }
        List<ProfesionalSalud> list = em.createQuery(
                "SELECT p FROM ProfesionalSalud p WHERE p.email = :email", ProfesionalSalud.class)
                .setParameter("email", email)
                .setMaxResults(1)
                .getResultList();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * Busca profesionales por especialidad.
     * 
     * @param especialidad Especialidad a buscar (nombre del enum como String, ej: "MEDICINA_GENERAL")
     * @return Lista de profesionales con esa especialidad
     */
    public List<ProfesionalSalud> findByEspecialidad(String especialidad) {
        try {
            if (especialidad == null || especialidad.isBlank()) {
                throw new IllegalArgumentException("especialidad is required");
            }
            // Convertir String a enum
            Especialidad especialidadEnum = Especialidad.valueOf(especialidad);
            return em.createQuery(
                    "SELECT p FROM ProfesionalSalud p WHERE p.especialidad = :especialidad", 
                    ProfesionalSalud.class)
                    .setParameter("especialidad", especialidadEnum)
                    .getResultList();
        } catch (IllegalArgumentException e) {
            // Si el valor no es válido, retornar lista vacía
            return java.util.Collections.emptyList();
        }
    }
    
    public ProfesionalSalud save(ProfesionalSalud profesional) {
        if (profesional == null) {
            throw new IllegalArgumentException("profesional is required");
        }
        if (profesional.getId() == null) {
            em.persist(profesional); 
        } else {
            profesional = em.merge(profesional);
        }
        return profesional;
    }

    public void delete(ProfesionalSalud profesional) {
        if (profesional == null) {
            throw new IllegalArgumentException("profesional is required");
        }
        ProfesionalSalud merged = em.merge(profesional); 
        em.remove(merged);
    }
}
