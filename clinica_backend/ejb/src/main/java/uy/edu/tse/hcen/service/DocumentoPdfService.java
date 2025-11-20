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
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
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
     * Obtiene el CI del paciente de un documento por su ID de MongoDB.
     * Busca en ambas colecciones: documentos_pdf y documentos_clinicos.
     * 
     * @param mongoId ID del documento en MongoDB (ObjectId hex)
     * @param tenantId ID de la clínica (para validación)
     * @return CI del paciente o null si no se encuentra
     */
    public String obtenerCiPacientePorId(String mongoId, Long tenantId) {
        LOG.info(String.format("🔍 [PERIFERICO] Obteniendo CI del paciente - ID: %s, Clínica: %d", mongoId, tenantId));
        
        // 1. Buscar en documentos_pdf
        Document documentoPdf = documentoPdfRepository.buscarPorId(mongoId, tenantId);
        if (documentoPdf != null) {
            String ciPaciente = documentoPdf.getString("ciPaciente");
            if (ciPaciente != null && !ciPaciente.isBlank()) {
                LOG.info(String.format("✅ [PERIFERICO] CI encontrado en documentos_pdf - ID: %s, CI: %s", mongoId, ciPaciente));
                return ciPaciente;
            }
        }

        // 2. Buscar en documentos_clinicos
        MongoDatabase database = documentoPdfRepository.getDatabase();
        MongoCollection<Document> clinicosCollection = database.getCollection("documentos_clinicos");
        try {
            ObjectId objectId = new ObjectId(mongoId);
            Document query = new Document("_id", objectId);
            Document documentoClinico = clinicosCollection.find(query).first();

            if (documentoClinico != null) {
                String ciPaciente = documentoClinico.getString("documentoIdPaciente");
                if (ciPaciente == null || ciPaciente.isBlank()) {
                    ciPaciente = documentoClinico.getString("pacienteDoc");
                }
                if (ciPaciente != null && !ciPaciente.isBlank()) {
                    LOG.info(String.format("✅ [PERIFERICO] CI encontrado en documentos_clinicos - ID: %s, CI: %s", mongoId, ciPaciente));
                    return ciPaciente;
                }
            }
        } catch (IllegalArgumentException ex) {
            LOG.warn(String.format("ID de MongoDB inválido al buscar CI del paciente: %s", mongoId));
        }
        
        LOG.warn(String.format("❌ [PERIFERICO] CI del paciente no encontrado - ID: %s, Tenant: %d", mongoId, tenantId));
        return null;
    }

    /**
     * Obtiene el documentoId (UUID) de un documento por su ID de MongoDB.
     * Busca en ambas colecciones: documentos_pdf y documentos_clinicos.
     * 
     * @param mongoId ID del documento en MongoDB (ObjectId hex)
     * @param tenantId ID de la clínica (para validación)
     * @return documentoId (UUID) o null si no se encuentra
     */
    public String obtenerDocumentoIdPorId(String mongoId, Long tenantId) {
        // 1. Buscar en documentos_pdf
        Document documentoPdf = documentoPdfRepository.buscarPorId(mongoId, tenantId);
        if (documentoPdf != null) {
            String documentoId = documentoPdf.getString("documentoId");
            if (documentoId != null && !documentoId.isBlank()) {
                return documentoId;
            }
        }

        // 2. Buscar en documentos_clinicos
        MongoDatabase database = documentoPdfRepository.getDatabase();
        MongoCollection<Document> clinicosCollection = database.getCollection("documentos_clinicos");
        try {
            ObjectId objectId = new ObjectId(mongoId);
            Document query = new Document("_id", objectId);
            Document documentoClinico = clinicosCollection.find(query).first();

            if (documentoClinico != null) {
                String documentoId = documentoClinico.getString("documentoId");
                if (documentoId != null && !documentoId.isBlank()) {
                    return documentoId;
                }
            }
        } catch (IllegalArgumentException ex) {
            LOG.warn(String.format("ID de MongoDB inválido al buscar documentoId: %s", mongoId));
        }
        
        return null;
    }

    /**
     * Obtiene el tipoDocumento de un documento por su ID de MongoDB.
     * Busca en ambas colecciones: documentos_pdf y documentos_clinicos.
     * 
     * @param mongoId ID del documento en MongoDB (ObjectId hex)
     * @param tenantId ID de la clínica (para validación)
     * @return tipoDocumento o null si no se encuentra
     */
    public String obtenerTipoDocumentoPorId(String mongoId, Long tenantId) {
        // 1. Buscar en documentos_pdf
        Document documentoPdf = documentoPdfRepository.buscarPorId(mongoId, tenantId);
        if (documentoPdf != null) {
            String tipoDocumento = documentoPdf.getString("tipoDocumento");
            if (tipoDocumento != null && !tipoDocumento.isBlank()) {
                return tipoDocumento;
            }
        }

        // 2. Buscar en documentos_clinicos
        MongoDatabase database = documentoPdfRepository.getDatabase();
        MongoCollection<Document> clinicosCollection = database.getCollection("documentos_clinicos");
        try {
            ObjectId objectId = new ObjectId(mongoId);
            Document query = new Document("_id", objectId);
            Document documentoClinico = clinicosCollection.find(query).first();

            if (documentoClinico != null) {
                // En documentos_clinicos, el tipo puede estar en "especialidad"
                String tipoDocumento = documentoClinico.getString("especialidad");
                if (tipoDocumento == null || tipoDocumento.isBlank()) {
                    tipoDocumento = documentoClinico.getString("tipoDocumento");
                }
                if (tipoDocumento != null && !tipoDocumento.isBlank()) {
                    return tipoDocumento;
                }
            }
        } catch (IllegalArgumentException ex) {
            LOG.warn(String.format("ID de MongoDB inválido al buscar tipoDocumento: %s", mongoId));
        }
        
        return null;
    }

    /**
     * Obtiene un PDF por su ID de MongoDB.
     * Busca en ambas colecciones: documentos_pdf y documentos_clinicos
     * 
     * @param mongoId ID del documento en MongoDB (ObjectId hex)
     * @param tenantId ID de la clínica (para validación)
     * @return Bytes del PDF o null si no se encuentra
     */
    public byte[] obtenerPdfPorId(String mongoId, Long tenantId) {
        LOG.info(String.format("🔍 [PERIFERICO] Obteniendo PDF de MongoDB - ID: %s, Clínica: %d", mongoId, tenantId));
        
        // 1. Buscar primero en documentos_pdf
        Document documento = documentoPdfRepository.buscarPorId(mongoId, tenantId);
        if (documento != null) {
            LOG.info(String.format("✅ [PERIFERICO] Documento encontrado en documentos_pdf - ID: %s", mongoId));
            Binary pdfBinary = documento.get("pdfBytes", Binary.class);
            if (pdfBinary != null && pdfBinary.getData() != null && pdfBinary.getData().length > 0) {
                byte[] pdfData = pdfBinary.getData();
                LOG.info(String.format("✅ [PERIFERICO] PDF extraído de documentos_pdf - ID: %s, Tamaño: %d bytes", mongoId, pdfData.length));
                return pdfData;
            }
        }
        
        // 2. Si no se encuentra en documentos_pdf, buscar en documentos_clinicos
        LOG.info(String.format("🔍 [PERIFERICO] Buscando en documentos_clinicos - ID: %s", mongoId));
        try {
            com.mongodb.client.MongoDatabase database = documentoPdfRepository.getDatabase();
            com.mongodb.client.MongoCollection<Document> collection = database.getCollection("documentos_clinicos");
            
            try {
                org.bson.types.ObjectId objectId = new org.bson.types.ObjectId(mongoId);
                Document query = new Document("_id", objectId);
                documento = collection.find(query).first();
                
                if (documento != null) {
                    LOG.info(String.format("✅ [PERIFERICO] Documento encontrado en documentos_clinicos - ID: %s", mongoId));
                    // Buscar PDF en el campo "pdf"
                    Binary pdfBinary = documento.get("pdf", Binary.class);
                    if (pdfBinary != null && pdfBinary.getData() != null && pdfBinary.getData().length > 0) {
                        byte[] pdfData = pdfBinary.getData();
                        LOG.info(String.format("✅ [PERIFERICO] PDF extraído de documentos_clinicos - ID: %s, Tamaño: %d bytes", mongoId, pdfData.length));
                        return pdfData;
                    }
                }
            } catch (IllegalArgumentException e) {
                LOG.warn(String.format("⚠️ [PERIFERICO] ID inválido para ObjectId: %s", mongoId));
            }
        } catch (Exception e) {
            LOG.warn(String.format("⚠️ [PERIFERICO] Error buscando en documentos_clinicos: %s", e.getMessage()));
        }
        
        LOG.warn(String.format("❌ [PERIFERICO] Documento no encontrado en ninguna colección - ID: %s, Tenant: %d", mongoId, tenantId));
        return null;
    }
    
    /**
     * Obtiene el contenido de texto de un documento por su ID de MongoDB.
     * Busca en ambas colecciones: documentos_pdf y documentos_clinicos
     * 
     * @param mongoId ID del documento en MongoDB (ObjectId hex)
     * @param tenantId ID de la clínica (para validación)
     * @return Contenido de texto del documento o null si no se encuentra
     */
    public String obtenerContenidoPorId(String mongoId, Long tenantId) {
        LOG.info(String.format("🔍 [PERIFERICO] Obteniendo contenido de MongoDB - ID: %s, Clínica: %d", mongoId, tenantId));
        
        // 1. Buscar primero en documentos_clinicos (más probable que tenga contenido de texto)
        try {
            com.mongodb.client.MongoDatabase database = documentoPdfRepository.getDatabase();
            com.mongodb.client.MongoCollection<Document> collection = database.getCollection("documentos_clinicos");
            
            try {
                org.bson.types.ObjectId objectId = new org.bson.types.ObjectId(mongoId);
                Document query = new Document("_id", objectId);
                Document documento = collection.find(query).first();
                
                if (documento != null) {
                    String contenido = documento.getString("contenido");
                    if (contenido != null && !contenido.isBlank()) {
                        LOG.info(String.format("✅ [PERIFERICO] Contenido encontrado en documentos_clinicos - ID: %s", mongoId));
                        return contenido;
                    }
                }
            } catch (IllegalArgumentException e) {
                LOG.warn(String.format("⚠️ [PERIFERICO] ID inválido para ObjectId: %s", mongoId));
            }
        } catch (Exception e) {
            LOG.warn(String.format("⚠️ [PERIFERICO] Error buscando contenido en documentos_clinicos: %s", e.getMessage()));
        }
        
        LOG.warn(String.format("❌ [PERIFERICO] Contenido no encontrado - ID: %s, Tenant: %d", mongoId, tenantId));
        return null;
    }

    /**
     * Lista todos los documentos PDF de un paciente por su CI.
     * Busca en ambas colecciones: documentos_pdf y documentos_clinicos
     * 
     * @param ciPaciente CI del paciente
     * @param tenantId ID de la clínica (para validación)
     * @return Lista de metadatos de documentos (sin el contenido del PDF)
     */
    public java.util.List<Map<String, Object>> listarDocumentosPorPaciente(String ciPaciente, Long tenantId) {
        LOG.info(String.format("Listando documentos - Paciente: %s, Clínica: %d", ciPaciente, tenantId));
        
        java.util.List<Map<String, Object>> resultado = new java.util.ArrayList<>();
        
        // 1. Buscar en documentos_pdf (colección antigua)
        java.util.List<Document> documentosPdf = documentoPdfRepository.buscarPorPaciente(ciPaciente, tenantId);
        for (Document doc : documentosPdf) {
            Map<String, Object> metadata = convertirDocumentoPdfAMetadata(doc);
            resultado.add(metadata);
        }
        
        // 2. Buscar en documentos_clinicos (colección nueva usada por crearDocumentoCompleto)
        java.util.List<Document> documentosClinicos = buscarDocumentosClinicosPorPaciente(ciPaciente, tenantId);
        for (Document doc : documentosClinicos) {
            Map<String, Object> metadata = convertirDocumentoClinicoAMetadata(doc);
            resultado.add(metadata);
        }
        
        // Obtener información del paciente una sola vez
        var paciente = usuarioSaludRepository.findByCiAndTenant(ciPaciente, tenantId);
        String nombrePaciente = paciente != null ? paciente.getNombre() : null;
        String apellidoPaciente = paciente != null ? paciente.getApellido() : null;
        
        // Agregar información del paciente a todos los documentos
        for (Map<String, Object> metadata : resultado) {
            if (nombrePaciente != null) {
                metadata.put("nombrePaciente", nombrePaciente);
            }
            if (apellidoPaciente != null) {
                metadata.put("apellidoPaciente", apellidoPaciente);
            }
        }
        
        LOG.info(String.format("Encontrados %d documentos para el paciente %s (%d de documentos_pdf, %d de documentos_clinicos)", 
                resultado.size(), ciPaciente, documentosPdf.size(), documentosClinicos.size()));
        return resultado;
    }
    
    /**
     * Busca documentos en la colección documentos_clinicos por CI del paciente.
     */
    private java.util.List<Document> buscarDocumentosClinicosPorPaciente(String ciPaciente, Long tenantId) {
        try {
            // Obtener la base de datos desde el repositorio
            com.mongodb.client.MongoDatabase database = documentoPdfRepository.getDatabase();
            com.mongodb.client.MongoCollection<Document> collection = database.getCollection("documentos_clinicos");
            
            Document query = new Document();
            // Buscar por documentoIdPaciente o pacienteDoc (ambos campos se usan)
            query.append("$or", Arrays.asList(
                new Document("documentoIdPaciente", ciPaciente),
                new Document("pacienteDoc", ciPaciente)
            ));
            
            java.util.List<Document> resultados = new java.util.ArrayList<>();
            var cursor = collection.find(query).iterator();
            try {
                while (cursor.hasNext()) {
                    resultados.add(cursor.next());
                }
            } finally {
                cursor.close();
            }
            return resultados;
        } catch (Exception e) {
            LOG.warn(String.format("Error buscando en documentos_clinicos: %s", e.getMessage()));
            return new java.util.ArrayList<>();
        }
    }
    
    /**
     * Convierte un Document de documentos_pdf a Map de metadata.
     */
    private Map<String, Object> convertirDocumentoPdfAMetadata(Document doc) {
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
        metadata.put("titulo", doc.getString("titulo"));
        metadata.put("autor", doc.getString("autor"));
        metadata.put("profesionalId", doc.getString("profesionalId"));
        
        return metadata;
    }
    
    /**
     * Convierte un Document de documentos_clinicos a Map de metadata.
     */
    private Map<String, Object> convertirDocumentoClinicoAMetadata(Document doc) {
        Map<String, Object> metadata = new HashMap<>();
        
        ObjectId objectId = doc.getObjectId("_id");
        if (objectId != null) {
            metadata.put("id", objectId.toHexString());
        }
        
        metadata.put("documentoId", doc.getString("documentoId"));
        
        // En documentos_clinicos, el CI puede estar en documentoIdPaciente o pacienteDoc
        String ciPaciente = doc.getString("documentoIdPaciente");
        if (ciPaciente == null || ciPaciente.isBlank()) {
            ciPaciente = doc.getString("pacienteDoc");
        }
        metadata.put("ciPaciente", ciPaciente);
        
        // Fecha de creación
        java.util.Date fechaCreacion = doc.getDate("fechaCreacion");
        if (fechaCreacion == null) {
            Object fechaObj = doc.get("fecha");
            if (fechaObj instanceof java.util.Date) {
                fechaCreacion = (java.util.Date) fechaObj;
            }
        }
        if (fechaCreacion == null && objectId != null) {
            fechaCreacion = new Date(objectId.getTimestamp() * 1000L);
        }
        if (fechaCreacion == null) {
            fechaCreacion = new Date();
        }
        metadata.put("fechaCreacion", fechaCreacion);
        
        // Verificar si tiene PDF
        Binary pdfBinary = doc.get("pdf", Binary.class);
        boolean tienePdf = pdfBinary != null && pdfBinary.getData() != null && pdfBinary.getData().length > 0;
        metadata.put("tienePdf", tienePdf);
        metadata.put("contentType", tienePdf ? "application/pdf" : "text/plain");
        
        String tipoDocumento = doc.getString("tipoDocumento");
        if (tipoDocumento == null || tipoDocumento.isBlank()) {
            tipoDocumento = doc.getString("especialidad");
        }
        if (tipoDocumento == null || tipoDocumento.isBlank()) {
            tipoDocumento = "DOCUMENTO_CLINICO";
        }
        metadata.put("tipoDocumento", tipoDocumento);

        String contenido = doc.getString("contenido");
        String descripcion = doc.getString("descripcion");
        if ((descripcion == null || descripcion.isBlank()) && contenido != null && !contenido.isBlank()) {
            descripcion = contenido.length() > 180 ? contenido.substring(0, 180) + "..." : contenido;
        }
        if (descripcion != null && !descripcion.isBlank()) {
            metadata.put("descripcion", descripcion);
        }

        String titulo = doc.getString("titulo");
        if (titulo == null || titulo.isBlank()) {
            if (descripcion != null && !descripcion.isBlank()) {
                titulo = descripcion.split("\\n")[0];
                if (titulo.length() > 80) {
                    titulo = titulo.substring(0, 80) + "...";
                }
            }
        }
        if (titulo == null || titulo.isBlank()) {
            if (contenido != null && !contenido.isBlank()) {
                String primerLinea = contenido.stripLeading();
                int salto = primerLinea.indexOf('\n');
                titulo = (salto > 0 ? primerLinea.substring(0, salto) : primerLinea);
                if (titulo.length() > 80) {
                    titulo = titulo.substring(0, 80) + "...";
                }
            }
        }
        if (titulo == null || titulo.isBlank()) {
            titulo = tipoDocumento != null ? tipoDocumento : "Documento clínico";
        }
        metadata.put("titulo", titulo);

        String autor = doc.getString("autor");
        if (autor == null || autor.isBlank()) {
            autor = doc.getString("profesionalId");
        }
        if (autor != null && !autor.isBlank()) {
            metadata.put("autor", autor);
        }

        String profesionalId = doc.getString("profesionalId");
        if (profesionalId == null || profesionalId.isBlank()) {
            profesionalId = autor;
        }
        if (profesionalId != null && !profesionalId.isBlank()) {
            metadata.put("profesionalId", profesionalId);
        }
        
        return metadata;
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

