package uy.edu.tse.hcen.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.conversions.Bson;
import com.mongodb.client.model.Filters;
import uy.edu.tse.hcen.repository.ProfesionalSaludRepository;
import uy.edu.tse.hcen.repository.UsuarioSaludRepository;
import uy.edu.tse.hcen.repository.DocumentoPdfRepository;
import uy.edu.tse.hcen.repository.DocumentoClinicoRepository;
import uy.edu.tse.hcen.model.UsuarioSalud;
import uy.edu.tse.hcen.multitenancy.TenantContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Servicio para calcular estadísticas del tenant actual.
 */
@Stateless
public class StatsService {

    private static final Logger LOGGER = Logger.getLogger(StatsService.class.getName());
    
    // Constantes para literales de actividades
    private static final String KEY_TIPO = "tipo";
    private static final String KEY_ICONO = "icono";
    private static final String KEY_TEXTO = "texto";
    private static final String KEY_FECHA = "fecha";
    
    // Constantes para campos de base de datos y HTML
    private static final String KEY_TENANT_ID = "tenantId";
    private static final String KEY_FECHA_CREACION = "fechaCreacion";
    private static final String TAG_STRONG_CLOSE = "</strong>";

    @Inject
    private ProfesionalSaludRepository profesionalRepository;

    @Inject
    private UsuarioSaludRepository usuarioSaludRepository;

    @Inject
    private DocumentoPdfRepository documentoPdfRepository;

    @Inject
    private DocumentoClinicoRepository documentoClinicoRepository;

    /**
     * Obtiene las estadísticas del tenant actual.
     * 
     * @param tenantId ID del tenant (clínica)
     * @return Map con las estadísticas: profesionales, usuarios, documentos, consultasHoy
     */
    public Map<String, Object> obtenerEstadisticas(String tenantId) {
        LOGGER.log(Level.INFO, "Obteniendo estadísticas para tenant: {0}", tenantId);
        
        Long tenantIdLong;
        try {
            tenantIdLong = Long.parseLong(tenantId);
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "TenantId inválido: {0}", tenantId);
            return crearEstadisticasVacias();
        }

        Map<String, Object> stats = new HashMap<>();
        
        // 1. Contar profesionales del tenant (usando multi-tenancy)
        int profesionales = contarProfesionales(tenantId);
        stats.put("profesionales", profesionales);
        
        // 2. Contar usuarios de salud del tenant
        int usuarios = contarUsuariosSalud(tenantIdLong);
        stats.put("usuarios", usuarios);
        
        // 3. Contar documentos clínicos totales del tenant (MongoDB)
        int documentos = contarDocumentosTotales(tenantIdLong);
        stats.put("documentos", documentos);
        
        // 4. Contar documentos creados hoy (consultas hoy)
        int consultasHoy = contarDocumentosHoy(tenantIdLong);
        stats.put("consultas", consultasHoy);
        
        LOGGER.log(Level.INFO, "Estadísticas calculadas - Profesionales: {0}, Usuarios: {1}, Documentos: {2}, Consultas Hoy: {3}", 
                new Object[]{profesionales, usuarios, documentos, consultasHoy});
        
        return stats;
    }

    /**
     * Obtiene la actividad reciente del tenant.
     * 
     * @param tenantId ID del tenant (clínica)
     * @param limite Número máximo de actividades a devolver
     * @return Lista de actividades recientes ordenadas por fecha descendente
     */
    public List<Map<String, Object>> obtenerActividadReciente(String tenantId, int limite) {
        LOGGER.log(Level.INFO, "Obteniendo actividad reciente para tenant: {0} (limite: {1})", new Object[]{tenantId, limite});
        
        Long tenantIdLong;
        try {
            tenantIdLong = Long.parseLong(tenantId);
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "TenantId inválido: {0}", tenantId);
            return new ArrayList<>();
        }

        List<Map<String, Object>> actividades = new ArrayList<>();
        
        // 1. Últimos documentos agregados
        actividades.addAll(obtenerUltimosDocumentos(tenantIdLong, limite));
        
        // 2. Últimos usuarios registrados
        actividades.addAll(obtenerUltimosUsuarios(tenantIdLong, limite));
        
        // 3. Últimos profesionales registrados
        actividades.addAll(obtenerUltimosProfesionales(tenantId, limite));
        
        // Ordenar por fecha descendente y tomar los más recientes
        actividades.sort((a, b) -> {
            String fechaA = (String) a.get(KEY_FECHA);
            String fechaB = (String) b.get(KEY_FECHA);
            return Comparator.<String>nullsLast(Comparator.reverseOrder()).compare(fechaB, fechaA);
        });
        
        // Limitar resultados
        if (actividades.size() > limite) {
            actividades = actividades.subList(0, limite);
        }
        
        LOGGER.log(Level.INFO, "Actividad reciente obtenida: {0} actividades", actividades.size());
        return actividades;
    }

    private int contarProfesionales(String tenantId) {
        try {
            return ejecutarConTenant(tenantId, () -> {
                List<?> profesionales = profesionalRepository.findAll();
                int count = profesionales != null ? profesionales.size() : 0;
                LOGGER.log(Level.INFO, "Profesionales encontrados para tenant {0}: {1}", new Object[]{tenantId, count});
                return count;
            });
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e, () -> "Error al contar profesionales para tenant " + tenantId + ": " + e.getMessage());
            return 0;
        }
    }

    private int contarUsuariosSalud(Long tenantId) {
        try {
            List<?> usuarios = usuarioSaludRepository.findByTenant(tenantId);
            int count = usuarios != null ? usuarios.size() : 0;
            LOGGER.log(Level.INFO, "Usuarios de salud encontrados para tenant {0}: {1}", new Object[]{tenantId, count});
            return count;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e, () -> "Error al contar usuarios de salud para tenant " + tenantId + ": " + e.getMessage());
            return 0;
        }
    }

    private int contarDocumentosTotales(Long tenantId) {
        try {
            // Contar en documentos_pdf
            long countPdf = documentoPdfRepository.getCollectionPublic()
                .countDocuments(Filters.eq(KEY_TENANT_ID, tenantId));
            
            // Contar en documentos_clinicos
            long countClinicos = documentoClinicoRepository.getCollection()
                .countDocuments(Filters.eq(KEY_TENANT_ID, tenantId));
            
            int total = (int) (countPdf + countClinicos);
            LOGGER.log(Level.INFO, "Documentos totales para tenant {0}: {1} (PDFs: {2}, Clínicos: {3})", 
                    new Object[]{tenantId, total, countPdf, countClinicos});
            return total;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e, () -> "Error al contar documentos para tenant " + tenantId + ": " + e.getMessage());
            return 0;
        }
    }

    private int contarDocumentosHoy(Long tenantId) {
        try {
            // Obtener fecha de inicio de hoy (00:00:00)
            LocalDate hoy = LocalDate.now();
            ZonedDateTime inicioHoy = hoy.atStartOfDay(ZoneId.systemDefault());
            Date fechaInicio = Date.from(inicioHoy.toInstant());
            
            // Obtener fecha de fin de hoy (23:59:59)
            ZonedDateTime finHoy = hoy.atTime(23, 59, 59).atZone(ZoneId.systemDefault());
            Date fechaFin = Date.from(finHoy.toInstant());
            
            // Contar documentos creados hoy en documentos_pdf
            Bson filtroPdf = Filters.and(
                Filters.eq(KEY_TENANT_ID, tenantId),
                Filters.gte(KEY_FECHA_CREACION, fechaInicio),
                Filters.lte(KEY_FECHA_CREACION, fechaFin)
            );
            long countPdf = documentoPdfRepository.getCollectionPublic()
                .countDocuments(filtroPdf);
            
            // Contar documentos creados hoy en documentos_clinicos
            Bson filtroClinicos = Filters.and(
                Filters.eq(KEY_TENANT_ID, tenantId),
                Filters.gte(KEY_FECHA_CREACION, fechaInicio),
                Filters.lte(KEY_FECHA_CREACION, fechaFin)
            );
            long countClinicos = documentoClinicoRepository.getCollection()
                .countDocuments(filtroClinicos);
            
            int total = (int) (countPdf + countClinicos);
            LOGGER.log(Level.INFO, "Documentos creados hoy para tenant {0}: {1} (PDFs: {2}, Clínicos: {3})", 
                    new Object[]{tenantId, total, countPdf, countClinicos});
            return total;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e, () -> "Error al contar documentos de hoy para tenant " + tenantId + ": " + e.getMessage());
            return 0;
        }
    }

    private List<Map<String, Object>> obtenerUltimosDocumentos(Long tenantId, int limite) {
        List<Map<String, Object>> actividades = new ArrayList<>();
        try {
            // Obtener últimos documentos de documentos_pdf
            List<Document> documentosPdf = documentoPdfRepository.getCollectionPublic()
                .find(Filters.eq(KEY_TENANT_ID, tenantId))
                .sort(new Document(KEY_FECHA_CREACION, -1))
                .limit(limite)
                .into(new ArrayList<>());
            
            procesarDocumentos(documentosPdf, actividades, tenantId);
            
            // Obtener últimos documentos de documentos_clinicos
            List<Document> documentosClinicos = documentoClinicoRepository.getCollectionPublic()
                .find(Filters.eq(KEY_TENANT_ID, tenantId))
                .sort(new Document(KEY_FECHA_CREACION, -1))
                .limit(limite)
                .into(new ArrayList<>());
            
            procesarDocumentos(documentosClinicos, actividades, tenantId);
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e, () -> "Error al obtener últimos documentos para tenant " + tenantId + ": " + e.getMessage());
        }
        return actividades;
    }

    private void procesarDocumentos(List<Document> documentos, List<Map<String, Object>> actividades, Long tenantId) {
        for (Document doc : documentos) {
            Map<String, Object> actividad = new HashMap<>();
            actividad.put(KEY_TIPO, "documento");
            actividad.put(KEY_ICONO, "📄");
            String profesionalId = doc.getString("profesionalId");
            String nombreProfesional = obtenerNombreProfesionalSafe(profesionalId, tenantId);
            
            // Usar el campo "autor" si está disponible (más descriptivo)
            String autor = doc.getString("autor");
            if (autor != null && !autor.isBlank()) {
                nombreProfesional = autor;
            }
            
            actividad.put(KEY_TEXTO, "Documento clínico agregado por <strong>" + nombreProfesional + TAG_STRONG_CLOSE);
            actividad.put(KEY_FECHA, formatearFecha(doc.getDate(KEY_FECHA_CREACION)));
            actividades.add(actividad);
        }
    }

    private String obtenerNombreProfesionalSafe(String profesionalId, Long tenantId) {
        if (profesionalId == null || profesionalId.isBlank()) {
            return "Profesional";
        }
        try {
            return obtenerNombreProfesional(profesionalId, tenantId);
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING, () -> "No se pudo obtener nombre del profesional " + profesionalId + ": " + e.getMessage());
            }
            return profesionalId;
        }
    }

    private List<Map<String, Object>> obtenerUltimosUsuarios(Long tenantId, int limite) {
        List<Map<String, Object>> actividades = new ArrayList<>();
        try {
            List<UsuarioSalud> usuarios = usuarioSaludRepository.findByTenant(tenantId);
            if (usuarios != null && !usuarios.isEmpty()) {
                // Ordenar por fechaAlta descendente y tomar los primeros
                usuarios = usuarios.stream()
                    .sorted(Comparator.comparing(UsuarioSalud::getFechaAlta, 
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(limite)
                    .toList();
                
                for (UsuarioSalud usuario : usuarios) {
                    String nombre = usuario.getNombre();
                    String apellido = usuario.getApellido();
                    LocalDateTime fechaAlta = usuario.getFechaAlta();
                    
                    Map<String, Object> actividad = new HashMap<>();
                    actividad.put(KEY_TIPO, "usuario");
                    actividad.put(KEY_ICONO, "👤");
                    String nombreCompleto = (nombre != null ? nombre : "") + " " + (apellido != null ? apellido : "");
                    actividad.put(KEY_TEXTO, "Nuevo usuario de salud registrado en INUS: <strong>" + nombreCompleto.trim() + TAG_STRONG_CLOSE);
                    if (fechaAlta != null) {
                        actividad.put(KEY_FECHA, fechaAlta.atZone(ZoneId.systemDefault()).toInstant().toString());
                    } else {
                        actividad.put(KEY_FECHA, new Date().toInstant().toString());
                    }
                    actividades.add(actividad);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e, () -> "Error al obtener últimos usuarios para tenant " + tenantId + ": " + e.getMessage());
        }
        return actividades;
    }

    private List<Map<String, Object>> obtenerUltimosProfesionales(String tenantId, int limite) {
        List<Map<String, Object>> actividades = new ArrayList<>();
        try {
            ejecutarConTenant(tenantId, () -> {
                List<uy.edu.tse.hcen.model.ProfesionalSalud> profesionales = profesionalRepository.findAll();
                if (profesionales != null && !profesionales.isEmpty()) {
                    int start = Math.max(0, profesionales.size() - limite);
                    crearActividadesProfesionales(profesionales, start, actividades);
                }
                return null;
            });
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e, () -> "Error al obtener últimos profesionales para tenant " + tenantId + ": " + e.getMessage());
        }
        return actividades;
    }

    private void crearActividadesProfesionales(List<uy.edu.tse.hcen.model.ProfesionalSalud> profesionales, 
                                                int start, List<Map<String, Object>> actividades) {
        for (int i = start; i < profesionales.size(); i++) {
            uy.edu.tse.hcen.model.ProfesionalSalud prof = profesionales.get(i);
            String nombre = prof.getNombre();
            
            Map<String, Object> actividad = new HashMap<>();
            actividad.put(KEY_TIPO, "profesional");
            actividad.put(KEY_ICONO, "🩺");
            actividad.put(KEY_TEXTO, "Nuevo profesional registrado: <strong>" + (nombre != null ? nombre : "Profesional") + TAG_STRONG_CLOSE);
            actividad.put(KEY_FECHA, new Date().toInstant().toString());
            actividades.add(actividad);
        }
    }

    private String obtenerNombreProfesional(String profesionalId, Long tenantId) {
        try {
            String tenantIdStr = tenantId != null ? tenantId.toString() : null;
            if (tenantIdStr == null) {
                return profesionalId;
            }
            
            return ejecutarConTenant(tenantIdStr, () -> {
                var profesionalOpt = profesionalRepository.findByNickname(profesionalId);
                if (profesionalOpt.isPresent()) {
                    String nombre = profesionalOpt.get().getNombre();
                    if (nombre != null && !nombre.isBlank()) {
                        return nombre;
                    }
                }
                return profesionalId;
            });
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al obtener nombre del profesional {0}: {1}", new Object[]{profesionalId, e.getMessage()});
            return profesionalId;
        }
    }

    private <T> T ejecutarConTenant(String tenantId, java.util.function.Supplier<T> operation) {
        String tenantAnterior = TenantContext.getCurrentTenant();
        try {
            TenantContext.setCurrentTenant(tenantId);
            return operation.get();
        } finally {
            if (tenantAnterior != null) {
                TenantContext.setCurrentTenant(tenantAnterior);
            } else {
                TenantContext.clear();
            }
        }
    }

    private String formatearFecha(Date fecha) {
        if (fecha == null) {
            return null;
        }
        return fecha.toInstant().toString();
    }

    private Map<String, Object> crearEstadisticasVacias() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("profesionales", 0);
        stats.put("usuarios", 0);
        stats.put("documentos", 0);
        stats.put("consultas", 0);
        return stats;
    }
}

