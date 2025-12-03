package uy.edu.tse.hcen.service;

import uy.edu.tse.hcen.dto.ProfesionalDTO;
import uy.edu.tse.hcen.model.ProfesionalSalud;
import uy.edu.tse.hcen.context.TenantContext;
import uy.edu.tse.hcen.repository.ProfesionalSaludRepository;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.Optional;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class ProfesionalSaludService {

    @Inject
    private ProfesionalSaludRepository profesionalRepository;
    
    @Inject
    private TenantContext tenantContext;
    
    @Inject
    private ProfesionalPersistenceHelper persistenceHelper;

    private static final Logger LOGGER = Logger.getLogger(ProfesionalSaludService.class.getName());

    // Constructor público por defecto requerido por EJB
    public ProfesionalSaludService() {
    }

    // nodoRepository is injected and used to associate the newly created ProfesionalSalud
    // with the tenant's NodoPeriferico (clinica). We perform the lookup inside a try/catch
    // and throw a controlled IllegalArgumentException if the tenant's node is missing.

    /**
     * Crea un profesional y lo asocia al tenant actual.
     */
    public ProfesionalSalud create(ProfesionalDTO dto) throws IllegalArgumentException {
        if (tenantContext.getRole() == null || !tenantContext.getRole().equals("ADMINISTRADOR")) {
            throw new SecurityException("Solo los administradores pueden crear profesionales.");
        }

        String tenantId = tenantContext.getTenantId();
        String schema = (tenantId != null && !tenantId.isBlank()) ? "schema_clinica_" + tenantId : "public";
        Long currentTenantId = null;
        if (tenantId != null && !tenantId.isBlank()) {
            try {
                currentTenantId = Long.parseLong(tenantId);
            } catch (NumberFormatException nfe) {
                LOGGER.log(Level.WARNING, "Invalid tenant id format: {0}", tenantId);
            }
        }

        // Build entity without touching DB (validations that don't need DB can go here)
        ProfesionalSalud profesional = new ProfesionalSalud();
        profesional.setNombre(dto.getNombre());
        profesional.setEmail(dto.getEmail());
        profesional.setNickname(dto.getNickname());
        profesional.setEspecialidad(dto.getEspecialidad());
        profesional.setDireccion(dto.getDireccion());
        profesional.setPassword(dto.getPassword());
        
        // Setear teléfono y CI si están presentes
        if (dto.getTelefono() != null) {
            profesional.setTelefono(dto.getTelefono());
        }
        if (dto.getCi() != null) {
            profesional.setCi(dto.getCi());
        }

        // Delegate persistence to the BMT helper which handles UserTransaction and schema apply
        try {
            persistenceHelper.persistWithManualTransaction(profesional, schema);
            // Optionally, after persist we can perform uniqueness checks or other DB reads if needed
            return profesional;
        } catch (IllegalArgumentException e) {
            // Re-lanzar excepciones de validación con contexto adicional
            String contextualMsg = "Error de validación al crear profesional '" + dto.getNickname() + "': " + e.getMessage();
            LOGGER.log(Level.WARNING, contextualMsg);
            throw new IllegalArgumentException(contextualMsg, e);
        } catch (Exception ex) {
            String errorMsg = "Failed to create professional with nickname '" + dto.getNickname() + "': " + ex.getMessage();
            LOGGER.log(Level.SEVERE, errorMsg, ex);
            throw new jakarta.ejb.EJBException(errorMsg, ex);
        }
    }

    public List<ProfesionalSalud> findAllInCurrentTenant() {
        return profesionalRepository.findAll();
    }
    
    public ProfesionalSalud update(Long id, ProfesionalDTO dto) {
        ProfesionalSalud profesional = profesionalRepository.findById(id)
            .orElseThrow(() -> {
                String errorMsg = "Profesional no encontrado con ID: " + id;
                LOGGER.log(Level.WARNING, errorMsg);
                return new IllegalArgumentException(errorMsg);
            });
        // If updating nickname/email, check uniqueness
        if (dto.getNickname() != null && !dto.getNickname().equals(profesional.getNickname())) {
            Optional<ProfesionalSalud> existingNickname = profesionalRepository.findByNickname(dto.getNickname());
            if (existingNickname.isPresent()) {
                String conflictMsg = "El nickname '" + dto.getNickname() + "' ya está en uso";
                LOGGER.log(Level.WARNING, conflictMsg);
                throw new WebApplicationException(Response.status(Response.Status.CONFLICT)
                        .entity(conflictMsg).build());
            }
            profesional.setNickname(dto.getNickname());
        }

        if (dto.getEmail() != null && !dto.getEmail().equals(profesional.getEmail())) {
            Optional<ProfesionalSalud> existingEmail = profesionalRepository.findByEmail(dto.getEmail());
            if (existingEmail.isPresent()) {
                String conflictMsg = "El email '" + dto.getEmail() + "' ya está en uso";
                LOGGER.log(Level.WARNING, conflictMsg);
                throw new WebApplicationException(Response.status(Response.Status.CONFLICT)
                        .entity(conflictMsg).build());
            }
            profesional.setEmail(dto.getEmail());
        }

        if (dto.getNombre() != null) profesional.setNombre(dto.getNombre());
        if (dto.getEspecialidad() != null) profesional.setEspecialidad(dto.getEspecialidad());
        if (dto.getDireccion() != null) profesional.setDireccion(dto.getDireccion());
        if (dto.getTelefono() != null) profesional.setTelefono(dto.getTelefono());
        if (dto.getCi() != null) profesional.setCi(dto.getCi());

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            profesional.setPassword(dto.getPassword());
        }

        return profesionalRepository.save(profesional);
    }

    public Optional<ProfesionalSalud> findById(Long id) {
        return profesionalRepository.findById(id);
    }

    public void delete(Long id) {
        ProfesionalSalud profesional = profesionalRepository.findById(id)
            .orElseThrow(() -> {
                String errorMsg = "Profesional no encontrado con ID: " + id;
                LOGGER.log(Level.WARNING, errorMsg);
                return new IllegalArgumentException(errorMsg);
            });
        profesionalRepository.delete(profesional);
    }
}
