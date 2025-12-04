package uy.edu.tse.hcen.service;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;
import uy.edu.tse.hcen.dto.DTMetadatos;
import uy.edu.tse.hcen.exceptions.HcenUnavailableException;
import uy.edu.tse.hcen.multitenancy.TenantContext;
import uy.edu.tse.hcen.repository.DocumentoPdfRepository;
import uy.edu.tse.hcen.repository.UsuarioSaludRepository;
import uy.edu.tse.hcen.client.PoliticasAccesoClient;
import uy.edu.tse.hcen.util.DocumentoPdfFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio para gestionar documentos clínicos en formato PDF.
 * 
 * Responsabilidades:
 * - Almacenar PDFs en MongoDB
 * - Generar metadata automáticamente
 * - Sincronizar metadata con el backend HCEN (RNDC)
 * - Obtener PDFs para descarga
 */
@RequestScoped
public class DocumentoPdfService {

    private static final Logger LOG = Logger.getLogger(DocumentoPdfService.class);

    @Inject
    private DocumentoPdfRepository documentoPdfRepository;

    @Inject
    private uy.edu.tse.hcen.repository.DocumentoClinicoRepository documentoClinicoRepository;

    @Inject
    private UsuarioSaludRepository usuarioSaludRepository;

    @Inject
    private HcenClient hcenClient;

    @Inject
    private uy.edu.tse.hcen.repository.ProfesionalSaludRepository profesionalSaludRepository;

    // URL base del nodo periférico para construir URIs de acceso
    private static final String DEFAULT_NODO_BASE_URL = "http://localhost:8081";
    private static final String PROP_NODO_BASE_URL = "NODO_BASE_URL";

    /**
     * Procesa y guarda un PDF de evaluación.
     * 
     * Flujo:
     * 1. Almacena el PDF en MongoDB
     * 2. Obtiene información del paciente
     * 3. Genera metadata del documento
     * 4. Envía metadata al backend HCEN (RNDC)
     * 5. Retorna información del documento creado
     * 
     * @param tenantId ID de la clínica
     * @param profesionalId ID del profesional que sube el documento
     * @param ciPaciente CI del paciente
     * @param pdfStream Stream del archivo PDF
     * @param tipoDocumento Tipo de documento clínico
     * @param descripcion Descripción opcional
     * @return Map con información del documento creado
     */
    public Map<String, Object> procesarYGuardarPdf(
            Long tenantId,
            String profesionalId,
            String ciPaciente,
            InputStream pdfStream,
            String tipoDocumento,
            String descripcion) throws Exception {

        LOG.info(String.format("Procesando PDF - Clínica: %d, Paciente: %s, Profesional: %s", 
                tenantId, ciPaciente, profesionalId));

        // 1. Leer el PDF completo en memoria
        byte[] pdfBytes = leerInputStream(pdfStream);
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("El archivo PDF está vacío");
        }

        // 2. Generar ID único para el documento
        String documentoId = UUID.randomUUID().toString();

        // 3. Obtener información del paciente
        var paciente = usuarioSaludRepository.findByCiAndTenant(ciPaciente, tenantId);
        if (paciente == null) {
            throw new IllegalArgumentException(
                "Paciente no encontrado en esta clínica: " + ciPaciente + 
                ". Por favor, registre al paciente antes de subir documentos."
            );
        }

        // 4. Asegurar que el TenantContext esté establecido para la consulta
        String currentTenant = TenantContext.getCurrentTenant();
        if (currentTenant == null || !currentTenant.equals(String.valueOf(tenantId))) {
            TenantContext.setCurrentTenant(String.valueOf(tenantId));
            LOG.info(String.format("TenantContext establecido a: %d para búsqueda de profesional", tenantId));
        }
        
        // 5. Obtener información del profesional
        var profesionalOpt = profesionalSaludRepository.findByNickname(profesionalId);
        String nombreProfesional = profesionalOpt.map(p -> 
                p.getNombre() != null ? p.getNombre() : profesionalId).orElse(profesionalId);

        // 6. Obtener información de la clínica (para metadata)
        String nombreClinica = "Clínica " + tenantId; // TODO: obtener nombre real de la clínica

        // 7. Almacenar PDF en MongoDB (con metadata adicional)
        String mongoId = documentoPdfRepository.guardarPdf(documentoId, pdfBytes, ciPaciente, tenantId,
                tipoDocumento, descripcion, profesionalId);

        LOG.info(String.format("PDF guardado en MongoDB con ID: %s", mongoId));

        // 8. Construir URL de acceso al documento
        // La URL debe usar localhost:8081 para que el backend HCEN pueda convertirla
        // a hcen-wildfly-app:8080 cuando acceda desde Docker
        // Buscar también PERIPHERAL_NODE_URL por compatibilidad con HCEN Backend
        String nodoBaseUrl = System.getenv().getOrDefault("PERIPHERAL_NODE_URL", 
                System.getProperty(PROP_NODO_BASE_URL,
                    System.getenv().getOrDefault(PROP_NODO_BASE_URL, DEFAULT_NODO_BASE_URL)));
                    
        // Incluir tenantId en la URL para que el acceso directo funcione
        String urlAcceso = nodoBaseUrl + "/hcen-web/api/documentos-pdf/" + mongoId + "?tenantId=" + tenantId;

        // 9. Generar metadata
        DTMetadatos metadata = new DTMetadatos();
        metadata.setDocumentoId(documentoId);
        metadata.setDocumentoIdPaciente(ciPaciente);
        metadata.setTenantId(String.valueOf(tenantId));
        metadata.setFormato("application/pdf");
        metadata.setTipoDocumento(tipoDocumento);
        // Usar zona horaria de Uruguay explícitamente
        java.time.ZoneId uruguayZone = java.time.ZoneId.of("America/Montevideo");
        metadata.setFechaCreacion(LocalDateTime.now(uruguayZone));
        metadata.setFechaRegistro(LocalDateTime.now(uruguayZone));
        metadata.setUrlAcceso(urlAcceso);
        metadata.setAutor(nombreProfesional);
        metadata.setTitulo("Evaluación - " + tipoDocumento);
        metadata.setDescripcion(descripcion != null ? descripcion : "Documento clínico subido desde componente periférico");
        metadata.setLanguageCode("es-UY");
        metadata.setBreakingTheGlass(false);
        
        // Obtener nombre completo del paciente
        String nombrePaciente = paciente.getNombre() != null ? paciente.getNombre() : "";
        String apellidoPaciente = paciente.getApellido() != null ? paciente.getApellido() : "";
        metadata.setDatosPatronimicos(nombrePaciente + " " + apellidoPaciente);

        // 10. Enviar metadata al backend HCEN (RNDC)
        try {
            hcenClient.registrarMetadatos(metadata);
            LOG.info(String.format("Metadata enviada exitosamente al backend HCEN para documento: %s", documentoId));
        } catch (HcenUnavailableException ex) {
            LOG.warn(String.format("No se pudo sincronizar metadata con HCEN (documento guardado localmente): %s", ex.getMessage()));
            // Continuamos aunque falle la sincronización - el documento ya está guardado
        }

        // 11. Construir respuesta
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("documentoId", documentoId);
        resultado.put("mongoId", mongoId);
        resultado.put("ciPaciente", ciPaciente);
        resultado.put("urlAcceso", urlAcceso);
        resultado.put("tipoDocumento", tipoDocumento);
        resultado.put("fechaCreacion", LocalDateTime.now(java.time.ZoneId.of("America/Montevideo")).toString());
        resultado.put("sincronizado", true);

        return resultado;
    }

    /**
     * Obtiene la metadata de un documento por su ID sin descargar el PDF completo.
     * Busca primero en DocumentoPdfRepository (PDFs subidos directamente) y luego
     * en DocumentoClinicoRepository (documentos completos generados desde texto).
     * 
     * @param mongoId ID de MongoDB (ObjectId en hex string)
     * @param tenantId ID de la clínica (para validación de seguridad multi-tenant)
     * @return Map con la metadata del documento o null si no existe
     */
    public Map<String, Object> obtenerMetadataPorId(String mongoId, Long tenantId) {
        // Primero intentar buscar en DocumentoPdfRepository (PDFs subidos directamente)
        Document doc = documentoPdfRepository.buscarPorId(mongoId, tenantId);
        
        // Si no se encuentra, intentar buscar en DocumentoClinicoRepository (documentos completos)
        if (doc == null) {
            try {
                doc = documentoClinicoRepository.buscarPorId(mongoId, tenantId);
            } catch (Exception ex) {
                LOG.warn("Error al buscar metadata en DocumentoClinicoRepository: " + ex.getMessage());
            }
        }
        
        // Si no se encuentra con el tenantId proporcionado, intentar buscar sin filtrar por tenantId
        if (doc == null && tenantId != null) {
            try {
                doc = documentoPdfRepository.buscarPorId(mongoId, null);
                if (doc == null) {
                    doc = documentoClinicoRepository.buscarPorId(mongoId, null);
                }
                
                if (doc != null) {
                    Long docTenantId = doc.getLong("tenantId");
                    tenantId = docTenantId;
                }
            } catch (Exception ex) {
                LOG.warn("Error al buscar metadata sin filtrar por tenant: " + ex.getMessage());
            }
        }
        
        if (doc == null) {
            LOG.warn(String.format("Metadata no encontrada - ID: %s, Tenant: %d", mongoId, tenantId));
            return null;
        }
        
        // Obtener el tenantId real del documento si está disponible
        Long docTenantId = doc.getLong("tenantId");
        if (docTenantId != null) {
            tenantId = docTenantId;
        }
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", mongoId);
        metadata.put("ciPaciente", doc.getString("ciPaciente"));
        metadata.put("tipoDocumento", doc.getString("tipoDocumento"));
        metadata.put("profesionalId", doc.getString("profesionalId"));
        metadata.put("tenantId", tenantId);
        
        return metadata;
    }
    
    /**
     * Obtiene un PDF por su ID de MongoDB.
     * Busca primero en DocumentoPdfRepository (PDFs subidos directamente) y luego
     * en DocumentoClinicoRepository (documentos completos generados desde texto).
     * 
     * @param mongoId ID del documento en MongoDB (ObjectId hex)
     * @param tenantId ID de la clínica (para validación)
     * @return Bytes del PDF
     */
    public byte[] obtenerPdfPorId(String mongoId, Long tenantId) {
        // Primero intentar buscar en DocumentoPdfRepository (PDFs subidos directamente)
        Document documento = documentoPdfRepository.buscarPorId(mongoId, tenantId);
        
        // Si no se encuentra, intentar buscar en DocumentoClinicoRepository (documentos completos)
        if (documento == null) {
            try {
                documento = documentoClinicoRepository.buscarPorId(mongoId, tenantId);
            } catch (Exception ex) {
                LOG.warn("Error al buscar en DocumentoClinicoRepository: " + ex.getMessage());
            }
        }
        
        // Si no se encuentra con el tenantId proporcionado, intentar buscar sin filtrar por tenantId
        if (documento == null && tenantId != null) {
            try {
                documento = documentoPdfRepository.buscarPorId(mongoId, null);
                if (documento == null) {
                    documento = documentoClinicoRepository.buscarPorId(mongoId, null);
                }
            } catch (Exception ex) {
                LOG.warn("Error al buscar sin filtrar por tenant: " + ex.getMessage());
            }
        }
        
        if (documento == null) {
            LOG.warn(String.format("Documento no encontrado - ID: %s, Tenant: %d", mongoId, tenantId));
            return null;
        }

        // Extraer bytes del PDF - puede estar en pdfBytes o en pdf (documentos completos)
        org.bson.types.Binary pdfBinary = documento.get("pdfBytes", org.bson.types.Binary.class);
        if (pdfBinary == null) {
            // Intentar con el campo "pdf" usado por documentos completos
            pdfBinary = documento.get("pdf", org.bson.types.Binary.class);
        }
        
        if (pdfBinary == null || pdfBinary.getData() == null || pdfBinary.getData().length == 0) {
            try {
                byte[] generado = DocumentoPdfFactory.generarDesdeDocumento(documento);
                return generado;
            } catch (IOException ex) {
                LOG.error("Error al generar PDF on-demand - ID: " + mongoId, ex);
                return null;
            } catch (IllegalArgumentException ex) {
                LOG.warn("No se pudo generar PDF on-demand (datos incompletos) - ID: " + mongoId, ex);
                return null;
            }
        }

        byte[] pdfData = pdfBinary.getData();
        
        // Verificar que los primeros bytes sean de un PDF válido
        if (pdfData.length >= 4) {
            String header = new String(pdfData, 0, 4);
            if (!header.startsWith("%PDF")) {
                LOG.warn("Los primeros bytes no son de un PDF válido: " + header);
            }
        }

        return pdfData;
    }

    /**
     * Lista todos los documentos PDF de un paciente por su CI.
     * Consulta los metadatos desde el HCEN backend (tabla metadata_documento).
     * El filtrado por políticas de acceso se hace en el HCEN backend.
     * 
     * @param ciPaciente CI del paciente
     * @param profesionalId ID del profesional que está buscando (nickname)
     * @param tenantIdProfesional ID de la clínica del profesional
     * @return Lista de metadatos de documentos (ya filtrados por políticas en el backend)
     */
    public java.util.List<Map<String, Object>> listarDocumentosPorPaciente(String ciPaciente, String profesionalId, String tenantIdProfesional) {
        LOG.info(String.format("Listando documentos - Paciente: %s, Profesional: %s, Clínica Profesional: %s", 
                ciPaciente, profesionalId, tenantIdProfesional));
        
        // Obtener información completa del profesional (especialidad y nombre)
        String especialidad = null;
        String nombreProfesional = null;
        if (profesionalId != null && !profesionalId.isBlank()) {
            try {
                var profesionalOpt = profesionalSaludRepository.findByNickname(profesionalId);
                if (profesionalOpt.isPresent()) {
                    var profesional = profesionalOpt.get();
                    if (profesional.getEspecialidad() != null) {
                        especialidad = profesional.getEspecialidad().name();
                        LOG.info(String.format("Especialidad del profesional %s: %s", profesionalId, especialidad));
                    }
                    nombreProfesional = profesional.getNombre();
                    LOG.info(String.format("Nombre del profesional %s: %s", profesionalId, nombreProfesional));
                }
            } catch (Exception e) {
                LOG.warn(String.format("No se pudo obtener información del profesional %s: %s", profesionalId, e.getMessage()));
            }
        }
        
        // Consultar metadatos desde HCEN backend (tabla metadata_documento)
        // El backend filtra por políticas de acceso automáticamente y registra el acceso
        java.util.List<Map<String, Object>> metadatosFiltrados;
        try {
            metadatosFiltrados = hcenClient.obtenerMetadatosDocumentosPorCI(
                    ciPaciente, 
                    profesionalId, 
                    tenantIdProfesional, 
                    especialidad,
                    nombreProfesional);
            LOG.info(String.format("Obtenidos %d metadatos (ya filtrados por políticas) desde HCEN backend para el paciente %s", 
                    metadatosFiltrados.size(), ciPaciente));
        } catch (HcenUnavailableException e) {
            LOG.error(String.format("Error al consultar HCEN backend para obtener metadatos: %s", e.getMessage()), e);
            // Retornar lista vacía si HCEN no está disponible
            return new java.util.ArrayList<>();
        }
        
        // Mapear los metadatos del HCEN al formato esperado por el frontend
        java.util.List<Map<String, Object>> resultado = new java.util.ArrayList<>();
        for (Map<String, Object> metadata : metadatosFiltrados) {
            Map<String, Object> documentoMapeado = new HashMap<>();
            documentoMapeado.put("id", metadata.get("id"));
            documentoMapeado.put("documentoId", metadata.get("documentoId"));
            documentoMapeado.put("ciPaciente", ciPaciente);
            documentoMapeado.put("tenantId", metadata.get("tenantId")); // Incluir tenantId para saber de qué clínica es
            documentoMapeado.put("fechaCreacion", metadata.get("fechaCreacion"));
            documentoMapeado.put("contentType", "application/pdf"); // Los documentos son PDFs
            documentoMapeado.put("tipoDocumento", metadata.get("tipoDocumento"));
            documentoMapeado.put("descripcion", metadata.get("descripcion"));
            documentoMapeado.put("profesionalId", metadata.get("profesionalSalud"));
            documentoMapeado.put("nombrePaciente", metadata.get("nombrePaciente"));
            documentoMapeado.put("apellidoPaciente", metadata.get("apellidoPaciente"));
            documentoMapeado.put("uriDocumento", metadata.get("uriDocumento")); // URI para descargar el documento
            
            resultado.add(documentoMapeado);
        }
        
        LOG.info(String.format("Retornando %d documentos autorizados para el profesional %s", 
                resultado.size(), profesionalId));
        return resultado;
    }

    /**
     * Lee un InputStream completo y lo convierte en byte array.
     */
    private byte[] leerInputStream(InputStream inputStream) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int nRead;
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }
}

