package uy.edu.tse.hcen.service;

import uy.edu.tse.hcen.dto.ProfesionalDTO;
import uy.edu.tse.hcen.model.ProfesionalSalud;
import uy.edu.tse.hcen.model.NodoPeriferico;
import uy.edu.tse.hcen.context.TenantContext;
import uy.edu.tse.hcen.repository.ProfesionalSaludRepository;
import uy.edu.tse.hcen.repository.NodoPerifericoRepository;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.List;

@Stateless
public class ProfesionalSaludService {

    @Inject
    private ProfesionalSaludRepository profesionalRepository;

    @Inject
    private TenantContext tenantContext; // Contexto del tenant actual

    @Inject
    private NodoPerifericoRepository nodoRepository;

    /**
     * Crea un profesional y lo asocia al tenant actual.
     */
    public ProfesionalSalud create(ProfesionalDTO dto) throws IllegalArgumentException {
        if (tenantContext.getRole() == null || !tenantContext.getRole().equals("ADMINISTRADOR")) {
            throw new SecurityException("Solo los administradores pueden crear profesionales.");
        }
        Long currentTenantId = Long.parseLong(tenantContext.getTenantId());
        NodoPeriferico clinica = nodoRepository.findById(currentTenantId) 
                                              .orElseThrow(() -> new IllegalArgumentException("Clínica no encontrada."));

        ProfesionalSalud profesional = new ProfesionalSalud();
        profesional.setNombre(dto.getNombre());
        profesional.setEmail(dto.getEmail());
        profesional.setNickname(dto.getNickname());
        profesional.setEspecialidad(dto.getEspecialidad());
        profesional.setDireccion(dto.getDireccion());
        profesional.setPassword(dto.getPassword()); // Hash via setter

        profesional.setTrabajaEn(clinica);

        return profesionalRepository.save(profesional);
    }

    public List<ProfesionalSalud> findAllInCurrentTenant() {
        return profesionalRepository.findAll();
    }
    
    public ProfesionalSalud update(Long id, ProfesionalDTO dto) {
        ProfesionalSalud profesional = profesionalRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Profesional no encontrado."));

        profesional.setNombre(dto.getNombre());
        profesional.setEmail(dto.getEmail());
        profesional.setEspecialidad(dto.getEspecialidad());
        profesional.setDireccion(dto.getDireccion());

        return profesionalRepository.save(profesional);
    }

    public void delete(Long id) {
        ProfesionalSalud profesional = profesionalRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Profesional no encontrado."));
        profesionalRepository.delete(profesional);
    }
}
