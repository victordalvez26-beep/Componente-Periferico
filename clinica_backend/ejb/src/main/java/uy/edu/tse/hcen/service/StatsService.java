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
import uy.edu.tse.hcen.multitenancy.TenantContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Servicio para calcular estadísticas del tenant actual.
 */
@Stateless
public class StatsService {

    private static final Logger LOGGER = Logger.getLogger(StatsService.class.getName());

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
        LOGGER.info(String.format("Obteniendo estadísticas para tenant: %s", tenantId));
        
        Long tenantIdLong = null;
        try {
            tenantIdLong = Long.parseLong(tenantId);
        } catch (NumberFormatException e) {
            LOGGER.warning(String.format("TenantId inválido: %s", tenantId));
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
        
        LOGGER.info(String.format("Estadísticas calculadas - Profesionales: %d, Usuarios: %d, Documentos: %d, Consultas Hoy: %d",
                profesionales, usuarios, documentos, consultasHoy));
        
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
        LOGGER.info(String.format("Obteniendo actividad reciente para tenant: %s (limite: %d)", tenantId, limite));
        
        Long tenantIdLong = null;
        try {
            tenantIdLong = Long.parseLong(tenantId);
        } catch (NumberFormatException e) {
            LOGGER.warning(String.format("TenantId inválido: %s", tenantId));
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
            String fechaA = (String) a.get("fecha");
            String fechaB = (String) b.get("fecha");
            if (fechaA == null && fechaB == null) return 0;
            if (fechaA == null) return 1;
            if (fechaB == null) return -1;
            return fechaB.compareTo(fechaA); // Orden descendente lexicográfico funciona para ISO-8601
        });
        
        // Limitar resultados
        if (actividades.size() > limite) {
            actividades = actividades.subList(0, limite);
        }
        
        LOGGER.info(String.format("Actividad reciente obtenida: %d actividades", actividades.size()));
        return actividades;
    }

    private int contarProfesionales(String tenantId) {
        try {
            // Guardar el tenant actual
            String tenantAnterior = TenantContext.getCurrentTenant();
            
            // Establecer el tenant para esta consulta
            TenantContext.setCurrentTenant(tenantId);
            
            try {
                // Contar profesionales en el schema del tenant
                List<?> profesionales = profesionalRepository.findAll();
                int count = profesionales != null ? profesionales.size() : 0;
                LOGGER.info(String.format("Profesionales encontrados para tenant %s: %d", tenantId, count));
                return count;
            } finally {
                // Restaurar el tenant anterior
                if (tenantAnterior != null) {
                    TenantContext.setCurrentTenant(tenantAnterior);
                } else {
                    TenantContext.clear();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, String.format("Error al contar profesionales para tenant %s: %s", tenantId, e.getMessage()), e);
            return 0;
        }
    }

    private int contarUsuariosSalud(Long tenantId) {
        try {
            List<?> usuarios = usuarioSaludRepository.findByTenant(tenantId);
            int count = usuarios != null ? usuarios.size() : 0;
            LOGGER.info(String.format("Usuarios de salud encontrados para tenant %d: %d", tenantId, count));
            return count;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, String.format("Error al contar usuarios de salud para tenant %d: %s", tenantId, e.getMessage()), e);
            return 0;
        }
    }

    private int contarDocumentosTotales(Long tenantId) {
        try {
            // Contar en documentos_pdf
            long countPdf = documentoPdfRepository.getCollectionPublic()
                .countDocuments(Filters.eq("tenantId", tenantId));
            
            // Contar en documentos_clinicos
            long countClinicos = documentoClinicoRepository.getCollection()
                .countDocuments(Filters.eq("tenantId", tenantId));
            
            int total = (int) (countPdf + countClinicos);
            LOGGER.info(String.format("Documentos totales para tenant %d: %d (PDFs: %d, Clínicos: %d)", 
                    tenantId, total, countPdf, countClinicos));
            return total;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, String.format("Error al contar documentos para tenant %d: %s", tenantId, e.getMessage()), e);
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
                Filters.eq("tenantId", tenantId),
                Filters.gte("fechaCreacion", fechaInicio),
                Filters.lte("fechaCreacion", fechaFin)
            );
            long countPdf = documentoPdfRepository.getCollectionPublic()
                .countDocuments(filtroPdf);
            
            // Contar documentos creados hoy en documentos_clinicos
            Bson filtroClinicos = Filters.and(
                Filters.eq("tenantId", tenantId),
                Filters.gte("fechaCreacion", fechaInicio),
                Filters.lte("fechaCreacion", fechaFin)
            );
            long countClinicos = documentoClinicoRepository.getCollection()
                .countDocuments(filtroClinicos);
            
            int total = (int) (countPdf + countClinicos);
            LOGGER.info(String.format("Documentos creados hoy para tenant %d: %d (PDFs: %d, Clínicos: %d)", 
                    tenantId, total, countPdf, countClinicos));
            return total;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, String.format("Error al contar documentos de hoy para tenant %d: %s", tenantId, e.getMessage()), e);
            return 0;
        }
    }

    private List<Map<String, Object>> obtenerUltimosDocumentos(Long tenantId, int limite) {
        List<Map<String, Object>> actividades = new ArrayList<>();
        try {
            // Obtener últimos documentos de documentos_pdf
            List<Document> documentosPdf = documentoPdfRepository.getCollectionPublic()
                .find(Filters.eq("tenantId", tenantId))
                .sort(new Document("fechaCreacion", -1))
                .limit(limite)
                .into(new ArrayList<>());
            
            for (Document doc : documentosPdf) {
                Map<String, Object> actividad = new HashMap<>();
                actividad.put("tipo", "documento");
                actividad.put("icono", "📄");
                String profesionalId = doc.getString("profesionalId");
                // Intentar obtener nombre del profesional, si falla usar el ID o "Profesional"
                String nombreProfesional = "Profesional";
                if (profesionalId != null && !profesionalId.isBlank()) {
                    try {
                        nombreProfesional = obtenerNombreProfesional(profesionalId, tenantId);
                    } catch (Exception e) {
                        LOGGER.warning(String.format("No se pudo obtener nombre del profesional %s: %s", profesionalId, e.getMessage()));
                        nombreProfesional = profesionalId;
                    }
                }
                actividad.put("texto", String.format("Documento clínico agregado por <strong>%s</strong>", nombreProfesional));
                actividad.put("fecha", formatearFecha(doc.getDate("fechaCreacion")));
                actividades.add(actividad);
            }
            
            // Obtener últimos documentos de documentos_clinicos
            List<Document> documentosClinicos = documentoClinicoRepository.getCollectionPublic()
                .find(Filters.eq("tenantId", tenantId))
                .sort(new Document("fechaCreacion", -1))
                .limit(limite)
                .into(new ArrayList<>());
            
            for (Document doc : documentosClinicos) {
                Map<String, Object> actividad = new HashMap<>();
                actividad.put("tipo", "documento");
                actividad.put("icono", "📄");
                String profesionalId = doc.getString("profesionalId");
                // Intentar obtener nombre del profesional, si falla usar el ID o "Profesional"
                String nombreProfesional = "Profesional";
                if (profesionalId != null && !profesionalId.isBlank()) {
                    try {
                        nombreProfesional = obtenerNombreProfesional(profesionalId, tenantId);
                    } catch (Exception e) {
                        LOGGER.warning(String.format("No se pudo obtener nombre del profesional %s: %s", profesionalId, e.getMessage()));
                        nombreProfesional = profesionalId;
                    }
                }
                // Usar el campo "autor" si está disponible (más descriptivo)
                String autor = doc.getString("autor");
                if (autor != null && !autor.isBlank()) {
                    nombreProfesional = autor;
                }
                actividad.put("texto", String.format("Documento clínico agregado por <strong>%s</strong>", nombreProfesional));
                actividad.put("fecha", formatearFecha(doc.getDate("fechaCreacion")));
                actividades.add(actividad);
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, String.format("Error al obtener últimos documentos para tenant %d: %s", tenantId, e.getMessage()), e);
        }
        return actividades;
    }

    private List<Map<String, Object>> obtenerUltimosUsuarios(Long tenantId, int limite) {
        List<Map<String, Object>> actividades = new ArrayList<>();
        try {
            List<uy.edu.tse.hcen.model.UsuarioSalud> usuarios = usuarioSaludRepository.findByTenant(tenantId);
            if (usuarios != null && !usuarios.isEmpty()) {
                // Ordenar por fechaAlta descendente y tomar los primeros
                usuarios = usuarios.stream()
                    .sorted((u1, u2) -> {
                        LocalDateTime fecha1 = u1.getFechaAlta();
                        LocalDateTime fecha2 = u2.getFechaAlta();
                        if (fecha1 == null && fecha2 == null) return 0;
                        if (fecha1 == null) return 1;
                        if (fecha2 == null) return -1;
                        return fecha2.compareTo(fecha1);
                    })
                    .limit(limite)
                    .collect(Collectors.toList());
                
                for (uy.edu.tse.hcen.model.UsuarioSalud usuario : usuarios) {
                    String nombre = usuario.getNombre();
                    String apellido = usuario.getApellido();
                    LocalDateTime fechaAlta = usuario.getFechaAlta();
                    
                    Map<String, Object> actividad = new HashMap<>();
                    actividad.put("tipo", "usuario");
                    actividad.put("icono", "👤");
                    String nombreCompleto = (nombre != null ? nombre : "") + " " + (apellido != null ? apellido : "");
                    actividad.put("texto", String.format("Nuevo usuario de salud registrado en INUS: <strong>%s</strong>", nombreCompleto.trim()));
                    if (fechaAlta != null) {
                        actividad.put("fecha", fechaAlta.atZone(ZoneId.systemDefault()).toInstant().toString());
                    } else {
                        actividad.put("fecha", new Date().toInstant().toString());
                    }
                    actividades.add(actividad);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, String.format("Error al obtener últimos usuarios para tenant %d: %s", tenantId, e.getMessage()), e);
        }
        return actividades;
    }

    private List<Map<String, Object>> obtenerUltimosProfesionales(String tenantId, int limite) {
        List<Map<String, Object>> actividades = new ArrayList<>();
        try {
            // Guardar el tenant actual
            String tenantAnterior = TenantContext.getCurrentTenant();
            
            // Establecer el tenant para esta consulta
            TenantContext.setCurrentTenant(tenantId);
            
            try {
                List<uy.edu.tse.hcen.model.ProfesionalSalud> profesionales = profesionalRepository.findAll();
                if (profesionales != null && !profesionales.isEmpty()) {
                    // Tomar solo los últimos (invertir la lista si es necesario)
                    int start = Math.max(0, profesionales.size() - limite);
                    for (int i = start; i < profesionales.size(); i++) {
                        uy.edu.tse.hcen.model.ProfesionalSalud prof = profesionales.get(i);
                        String nombre = prof.getNombre();
                        
                        Map<String, Object> actividad = new HashMap<>();
                        actividad.put("tipo", "profesional");
                        actividad.put("icono", "🩺");
                        actividad.put("texto", String.format("Nuevo profesional registrado: <strong>%s</strong>", nombre != null ? nombre : "Profesional"));
                        // No tenemos fecha de creación, usar fecha actual como aproximación
                        actividad.put("fecha", new Date().toInstant().toString());
                        actividades.add(actividad);
                    }
                }
            } finally {
                // Restaurar el tenant anterior
                if (tenantAnterior != null) {
                    TenantContext.setCurrentTenant(tenantAnterior);
                } else {
                    TenantContext.clear();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, String.format("Error al obtener últimos profesionales para tenant %s: %s", tenantId, e.getMessage()), e);
        }
        return actividades;
    }

    private String obtenerNombreProfesional(String profesionalId, Long tenantId) {
        try {
            // Guardar el tenant actual
            String tenantAnterior = TenantContext.getCurrentTenant();
            
            // Establecer el tenant para esta consulta
            String tenantIdStr = tenantId != null ? tenantId.toString() : null;
            if (tenantIdStr != null) {
                TenantContext.setCurrentTenant(tenantIdStr);
            }
            
            try {
                var profesionalOpt = profesionalRepository.findByNickname(profesionalId);
                if (profesionalOpt.isPresent()) {
                    var profesional = profesionalOpt.get();
                    String nombre = profesional.getNombre();
                    if (nombre != null && !nombre.isBlank()) {
                        return nombre;
                    }
                }
            } finally {
                // Restaurar el tenant anterior
                if (tenantAnterior != null) {
                    TenantContext.setCurrentTenant(tenantAnterior);
                } else {
                    TenantContext.clear();
                }
            }
        } catch (Exception e) {
            LOGGER.warning(String.format("Error al obtener nombre del profesional %s: %s", profesionalId, e.getMessage()));
        }
        return profesionalId;
    }

    private String formatearFecha(Date fecha) {
        if (fecha == null) return null;
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

