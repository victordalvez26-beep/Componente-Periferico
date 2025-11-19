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
import uy.edu.tse.hcen.repository.ProfesionalSaludRepository;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
    private UsuarioSaludRepository usuarioSaludRepository;

    @Inject
    private HcenClient hcenClient;

    @Inject
    private ProfesionalSaludRepository profesionalSaludRepository;

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
        String apellidoPaciente = paciente.getSegundoApellido() != null ? paciente.getSegundoApellido() : "";
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
     * Obtiene un PDF por su ID de MongoDB.
     * 
     * @param mongoId ID del documento en MongoDB (ObjectId hex)
     * @param tenantId ID de la clínica (para validación)
     * @return Bytes del PDF
     */
    public byte[] obtenerPdfPorId(String mongoId, Long tenantId) {
        LOG.info(String.format("🔍 [PERIFERICO] Obteniendo PDF de MongoDB - ID: %s, Clínica: %d", mongoId, tenantId));
        
        // El repositorio ya valida el tenantId en la consulta, así que no necesitamos validar después
        Document documento = documentoPdfRepository.buscarPorId(mongoId, tenantId);
        if (documento == null) {
            LOG.warn(String.format("❌ [PERIFERICO] Documento no encontrado o no pertenece al tenant %d - ID: %s", tenantId, mongoId));
            return null;
        }

        LOG.info(String.format("✅ [PERIFERICO] Documento encontrado en MongoDB - ID: %s", mongoId));

        // Extraer bytes del PDF
        Binary pdfBinary = documento.get("pdfBytes", Binary.class);
        if (pdfBinary == null) {
            LOG.warn(String.format("❌ [PERIFERICO] El documento no tiene campo pdfBytes - ID: %s", mongoId));
            return null;
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
     * 
     * @param ciPaciente CI del paciente
     * @param tenantId ID de la clínica (para validación)
     * @return Lista de metadatos de documentos (sin el contenido del PDF)
     */
    public java.util.List<Map<String, Object>> listarDocumentosPorPaciente(String ciPaciente, Long tenantId) {
        LOG.info(String.format("Listando documentos - Paciente: %s, Clínica: %d", ciPaciente, tenantId));
        
        java.util.List<Document> documentos = documentoPdfRepository.buscarPorPaciente(ciPaciente, tenantId);
        
        // Obtener información del paciente una sola vez
        var paciente = usuarioSaludRepository.findByCiAndTenant(ciPaciente, tenantId);
        String nombrePaciente = paciente != null ? paciente.getNombre() : null;
        String apellidoPaciente = paciente != null ? paciente.getSegundoApellido() : null;
        
        java.util.List<Map<String, Object>> resultado = new java.util.ArrayList<>();
        for (Document doc : documentos) {
            Map<String, Object> metadata = new HashMap<>();
            
            ObjectId objectId = doc.getObjectId("_id");
            if (objectId != null) {
                metadata.put("id", objectId.toHexString());
            }
            
            metadata.put("documentoId", doc.getString("documentoId"));
            metadata.put("ciPaciente", doc.getString("ciPaciente"));
            
            // Fecha de creación
            java.util.Date fechaCreacion = doc.getDate("fechaCreacion");
            if (fechaCreacion != null) {
                metadata.put("fechaCreacion", fechaCreacion);
            }
            
            metadata.put("contentType", doc.getString("contentType"));
            
            // Metadata adicional
            metadata.put("tipoDocumento", doc.getString("tipoDocumento"));
            metadata.put("descripcion", doc.getString("descripcion"));
            metadata.put("profesionalId", doc.getString("profesionalId"));
            
            // Información del paciente
            if (nombrePaciente != null) {
                metadata.put("nombrePaciente", nombrePaciente);
            }
            if (apellidoPaciente != null) {
                metadata.put("apellidoPaciente", apellidoPaciente);
            }
            
            resultado.add(metadata);
        }
        
        LOG.info(String.format("Encontrados %d documentos para el paciente %s", resultado.size(), ciPaciente));
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

