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
        String nodoBaseUrl = System.getProperty(PROP_NODO_BASE_URL,
                System.getenv().getOrDefault(PROP_NODO_BASE_URL, DEFAULT_NODO_BASE_URL));
        String urlAcceso = nodoBaseUrl + "/hcen-web/api/documentos-pdf/" + mongoId;
        
        LOG.info(String.format("📝 [PERIFERICO] Construyendo URL de acceso - Base URL: %s, MongoId: %s, URL completa: %s", 
                nodoBaseUrl, mongoId, urlAcceso));

        // 9. Generar metadata
        DTMetadatos metadata = new DTMetadatos();
        metadata.setDocumentoId(documentoId);
        metadata.setDocumentoIdPaciente(ciPaciente);
        metadata.setTenantId(String.valueOf(tenantId));
        metadata.setFormato("application/pdf");
        metadata.setTipoDocumento(tipoDocumento);
        metadata.setFechaCreacion(LocalDateTime.now());
        metadata.setFechaRegistro(LocalDateTime.now());
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
        resultado.put("fechaCreacion", LocalDateTime.now().toString());
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
        LOG.info(String.format("🔍 [PERIFERICO] Obteniendo metadata - ID: %s, Tenant: %d", mongoId, tenantId));
        
        // Primero intentar buscar en DocumentoPdfRepository (PDFs subidos directamente)
        Document doc = documentoPdfRepository.buscarPorId(mongoId, tenantId);
        
        // Si no se encuentra, intentar buscar en DocumentoClinicoRepository (documentos completos)
        if (doc == null) {
            LOG.info(String.format("🔍 [PERIFERICO] Metadata no encontrada en DocumentoPdfRepository, buscando en DocumentoClinicoRepository - ID: %s", mongoId));
            try {
                doc = documentoClinicoRepository.buscarPorId(mongoId, tenantId);
                
                if (doc != null) {
                    LOG.info(String.format("✅ [PERIFERICO] Metadata encontrada en DocumentoClinicoRepository - ID: %s", mongoId));
                }
            } catch (Exception ex) {
                LOG.warn(String.format("⚠️ [PERIFERICO] Error al buscar metadata en DocumentoClinicoRepository: %s", ex.getMessage()));
            }
        } else {
            LOG.info(String.format("✅ [PERIFERICO] Metadata encontrada en DocumentoPdfRepository - ID: %s", mongoId));
        }
        
        if (doc == null) {
            LOG.warn(String.format("❌ [PERIFERICO] Metadata no encontrada en ningún repositorio - ID: %s, Tenant: %d", mongoId, tenantId));
            return null;
        }
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", mongoId);
        metadata.put("ciPaciente", doc.getString("ciPaciente"));
        metadata.put("tipoDocumento", doc.getString("tipoDocumento"));
        metadata.put("profesionalId", doc.getString("profesionalId"));
        metadata.put("tenantId", tenantId);
        
        LOG.info(String.format("✅ [PERIFERICO] Metadata obtenida - CI Paciente: %s, Tipo: %s", 
                metadata.get("ciPaciente"), metadata.get("tipoDocumento")));
        
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
        LOG.info(String.format("🔍 [PERIFERICO] Obteniendo PDF de MongoDB - ID: %s, Clínica: %d", mongoId, tenantId));
        
        // Primero intentar buscar en DocumentoPdfRepository (PDFs subidos directamente)
        Document documento = documentoPdfRepository.buscarPorId(mongoId, tenantId);
        
        // Si no se encuentra, intentar buscar en DocumentoClinicoRepository (documentos completos)
        if (documento == null) {
            LOG.info(String.format("🔍 [PERIFERICO] Documento no encontrado en DocumentoPdfRepository, buscando en DocumentoClinicoRepository - ID: %s", mongoId));
            try {
                documento = documentoClinicoRepository.buscarPorId(mongoId, tenantId);
                
                if (documento != null) {
                    LOG.info(String.format("✅ [PERIFERICO] Documento encontrado en DocumentoClinicoRepository - ID: %s", mongoId));
                }
            } catch (Exception ex) {
                LOG.warn(String.format("⚠️ [PERIFERICO] Error al buscar en DocumentoClinicoRepository: %s", ex.getMessage()));
            }
        } else {
            LOG.info(String.format("✅ [PERIFERICO] Documento encontrado en DocumentoPdfRepository - ID: %s", mongoId));
        }
        
        if (documento == null) {
            LOG.warn(String.format("❌ [PERIFERICO] Documento no encontrado en ningún repositorio - ID: %s, Tenant: %d", mongoId, tenantId));
            return null;
        }

        // Extraer bytes del PDF - puede estar en pdfBytes o en pdf (documentos completos)
        org.bson.types.Binary pdfBinary = documento.get("pdfBytes", org.bson.types.Binary.class);
        if (pdfBinary == null) {
            // Intentar con el campo "pdf" usado por documentos completos
            pdfBinary = documento.get("pdf", org.bson.types.Binary.class);
        }
        
        if (pdfBinary == null || pdfBinary.getData() == null || pdfBinary.getData().length == 0) {
            LOG.info(String.format("ℹ️ [PERIFERICO] Documento %s no tiene PDF persistido, generando on-demand", mongoId));
            try {
                byte[] generado = DocumentoPdfFactory.generarDesdeDocumento(documento);
                LOG.info(String.format("✅ [PERIFERICO] PDF generado on-demand - ID: %s, Tamaño: %d bytes", mongoId, generado.length));
                return generado;
            } catch (IOException ex) {
                LOG.error(String.format("❌ [PERIFERICO] Error al generar PDF on-demand - ID: %s", mongoId), ex);
                return null;
            } catch (IllegalArgumentException ex) {
                LOG.warn(String.format("❌ [PERIFERICO] No se pudo generar PDF on-demand (datos incompletos) - ID: %s", mongoId), ex);
                return null;
            }
        }

        byte[] pdfData = pdfBinary.getData();
        LOG.info(String.format("✅ [PERIFERICO] PDF extraído de MongoDB - ID: %s, Tamaño: %d bytes", mongoId, pdfData.length));
        
        // Verificar que los primeros bytes sean de un PDF válido
        if (pdfData.length >= 4) {
            String header = new String(pdfData, 0, 4);
            if (!header.startsWith("%PDF")) {
                LOG.warn(String.format("⚠️ [PERIFERICO] Los primeros bytes no son de un PDF válido: %s", header));
                LOG.warn(String.format("⚠️ [PERIFERICO] Primeros 200 bytes: %s", 
                        new String(pdfData, 0, Math.min(200, pdfData.length))));
            } else {
                LOG.info(String.format("✅ [PERIFERICO] PDF válido detectado en MongoDB - Header: %s", header));
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
        
        // Obtener especialidad del profesional
        String especialidad = null;
        if (profesionalId != null && !profesionalId.isBlank()) {
            try {
                var profesionalOpt = profesionalSaludRepository.findByNickname(profesionalId);
                if (profesionalOpt.isPresent()) {
                    var profesional = profesionalOpt.get();
                    if (profesional.getEspecialidad() != null) {
                        especialidad = profesional.getEspecialidad().name();
                        LOG.info(String.format("Especialidad del profesional %s: %s", profesionalId, especialidad));
                    }
                }
            } catch (Exception e) {
                LOG.warn(String.format("No se pudo obtener especialidad del profesional %s: %s", profesionalId, e.getMessage()));
            }
        }
        
        // Consultar metadatos desde HCEN backend (tabla metadata_documento)
        // El backend filtra por políticas de acceso automáticamente
        java.util.List<Map<String, Object>> metadatosFiltrados;
        try {
            metadatosFiltrados = hcenClient.obtenerMetadatosDocumentosPorCI(
                    ciPaciente, 
                    profesionalId, 
                    tenantIdProfesional, 
                    especialidad);
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

