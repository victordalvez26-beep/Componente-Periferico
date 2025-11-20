package uy.edu.tse.hcen.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.bson.Document;
import uy.edu.tse.hcen.multitenancy.TenantContext;
import uy.edu.tse.hcen.repository.DocumentoPdfRepository;
import uy.edu.tse.hcen.repository.ProfesionalSaludRepository;
import uy.edu.tse.hcen.repository.UsuarioSaludRepository;

import java.time.LocalDate;
import java.util.Map;

/**
 * Servicio para obtener estadísticas de una clínica.
 */
@Stateless
public class StatsService {

    @PersistenceContext(unitName = "hcenPersistenceUnit")
    private EntityManager em;

    @Inject
    private ProfesionalSaludRepository profesionalRepository;

    @Inject
    private UsuarioSaludRepository usuarioSaludRepository;

    @Inject
    private DocumentoPdfRepository documentoPdfRepository;

    /**
     * Obtiene las estadísticas de una clínica.
     * 
     * @param tenantId ID de la clínica
     * @return Map con las estadísticas: profesionales, usuarios, documentos, consultas
     */
    public Map<String, Long> getStats(Long tenantId) {
        // Establecer el tenant context para que los repositorios usen el schema correcto
        TenantContext.setCurrentTenant(String.valueOf(tenantId));

        try {
            // Contar profesionales
            long profesionales = profesionalRepository.findAll().size();

            // Contar usuarios de salud
            long usuarios = usuarioSaludRepository.findByTenant(tenantId).size();

            // Contar documentos PDF en MongoDB
            long documentos = countDocumentos(tenantId);

            // Contar consultas del día (por ahora retornamos 0, se puede implementar después)
            long consultas = 0;

            return Map.of(
                "profesionales", profesionales,
                "usuarios", usuarios,
                "documentos", documentos,
                "consultas", consultas
            );
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Cuenta los documentos PDF en MongoDB para un tenant.
     */
    private long countDocumentos(Long tenantId) {
        try {
            var collection = documentoPdfRepository.getCollectionPublic();
            Document query = new Document("tenantId", tenantId);
            return collection.countDocuments(query);
        } catch (Exception e) {
            return 0L;
        }
    }
}

