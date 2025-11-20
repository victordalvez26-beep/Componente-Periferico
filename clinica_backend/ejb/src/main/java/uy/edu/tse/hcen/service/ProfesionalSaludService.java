package uy.edu.tse.hcen.service;

import uy.edu.tse.hcen.dto.DTProfesionalSalud;
import uy.edu.tse.hcen.model.ProfesionalSalud;
import uy.edu.tse.hcen.multitenancy.TenantContext;
import uy.edu.tse.hcen.repository.ProfesionalSaludRepository;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import uy.edu.tse.hcen.repository.NodoPerifericoRepository;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.Optional;
import jakarta.ejb.EJB;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class ProfesionalSaludService {

    @EJB
    private ProfesionalSaludRepository profesionalRepository;

    @PersistenceContext(unitName = "hcenPersistenceUnit")
    private EntityManager em;

    @EJB
    private NodoPerifericoRepository nodoRepository;

    private static final Logger LOGGER = Logger.getLogger(ProfesionalSaludService.class.getName());

    @EJB
    private ProfesionalPersistenceHelper persistenceHelper;

    // nodoRepository is injected and used to associate the newly created ProfesionalSalud
    // with the tenant's NodoPeriferico (clinica). We perform the lookup inside a try/catch
    // and throw a controlled IllegalArgumentException if the tenant's node is missing.

    /**
     * Crea un profesional y lo asocia al tenant actual.
     * El tenantId debe venir en el DTO o estar configurado en el TenantContext thread-local.
     * La validación de permisos se hace a nivel de Resource con @RolesAllowed.
     */
    public ProfesionalSalud create(DTProfesionalSalud dto) throws IllegalArgumentException {
        // Obtener tenantId del DTO o del TenantContext thread-local
        String tenantId = (dto != null && dto.getTenantId() != null && !dto.getTenantId().isBlank()) 
            ? dto.getTenantId() 
            : TenantContext.getCurrentTenant();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required. Set it in TenantContext or provide it in the DTO.");
        }
        String schema = "schema_clinica_" + tenantId;

        // Build entity without touching DB (validations that don't need DB can go here)
        ProfesionalSalud profesional = new ProfesionalSalud();
        profesional.setNombre(dto.getNombre());
        profesional.setEmail(dto.getEmail());
        profesional.setNickname(dto.getNickname());
        profesional.setEspecialidad(dto.getEspecialidad());
        profesional.setDireccion(dto.getDireccion());
        profesional.setPassword(dto.getPassword());

        // Delegate persistence to the BMT helper which handles UserTransaction and schema apply
        try {
            persistenceHelper.persistWithManualTransaction(profesional, schema);
            // Optionally, after persist we can perform uniqueness checks or other DB reads if needed
            return profesional;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error persisting professional: " + ex.getMessage(), ex);
            throw new RuntimeException("Failed to create professional", ex);
        }
    }

    public List<ProfesionalSalud> findAllInCurrentTenant() {
        return profesionalRepository.findAll();
    }

    /**
     * Busca profesionales por especialidad.
     * 
     * @param especialidad Especialidad a buscar (nombre del enum, ej: "MEDICINA_GENERAL")
     * @return Lista de profesionales con esa especialidad
     */
    public List<ProfesionalSalud> findByEspecialidad(String especialidad) {
        return profesionalRepository.findByEspecialidad(especialidad);
    }
    
    public ProfesionalSalud update(Long id, DTProfesionalSalud dto) {
        ProfesionalSalud profesional = profesionalRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Profesional no encontrado."));
        // If updating nickname/email, check uniqueness
        if (dto.getNickname() != null && !dto.getNickname().equals(profesional.getNickname())) {
            profesionalRepository.findByNickname(dto.getNickname()).ifPresent(p -> {
                throw new WebApplicationException(Response.status(Response.Status.CONFLICT)
                        .entity("nickname already exists").build());
            });
            profesional.setNickname(dto.getNickname());
        }

        if (dto.getEmail() != null && !dto.getEmail().equals(profesional.getEmail())) {
            profesionalRepository.findByEmail(dto.getEmail()).ifPresent(p -> {
                throw new WebApplicationException(Response.status(Response.Status.CONFLICT)
                        .entity("email already exists").build());
            });
            profesional.setEmail(dto.getEmail());
        }

        if (dto.getNombre() != null) profesional.setNombre(dto.getNombre());
        if (dto.getEspecialidad() != null) profesional.setEspecialidad(dto.getEspecialidad());
        if (dto.getDireccion() != null) profesional.setDireccion(dto.getDireccion());

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
            .orElseThrow(() -> new IllegalArgumentException("Profesional no encontrado."));
        profesionalRepository.delete(profesional);
    }
}
