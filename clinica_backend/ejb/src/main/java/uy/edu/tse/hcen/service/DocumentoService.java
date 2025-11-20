package uy.edu.tse.hcen.service;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.bson.Document;
import uy.edu.tse.hcen.dto.DTMetadatos;
import uy.edu.tse.hcen.multitenancy.TenantContext;
import uy.edu.tse.hcen.repository.DocumentoClinicoRepository;
import java.util.function.Consumer;
import uy.edu.tse.hcen.exceptions.HcenUnavailableException;
import uy.edu.tse.hcen.exceptions.MetadatosQueryException;
import uy.edu.tse.hcen.exceptions.MetadatosSyncException;
import uy.edu.tse.hcen.util.PdfGenerator;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio que encapsula la lógica de negocio de documentos clínicos.
 * - valida que existan metadatos para un documento antes de aceptar contenido
 * - verifica que el tenant del metadato coincida con el TenantContext actual
 *
 * Se expone como EJB Stateless para facilidad de integración y posibles necesidades
 * transaccionales en el futuro.
 */
@RequestScoped
public class DocumentoService {

    private static final Logger LOGGER = Logger.getLogger(DocumentoService.class.getName());

    private static final String PROP_RNDC_METADATOS_URL = "RNDC_METADATOS_URL";
    private static final String PROP_NODO_BASE_URL = "NODO_BASE_URL";
    private static final String DEFAULT_RNDC_METADATOS_URL = "http://127.0.0.1:8080/api/metadatos";
    private static final String DEFAULT_NODO_BASE_URL = "http://localhost:8081";

    private static final String KEY_CONTENIDO = "contenido";
    private static final String KEY_DOCUMENTO_ID = "documentoId";
    private static final String KEY_DOCUMENTO_ID_PACIENTE = "documentoIdPaciente";
    private static final String KEY_TENANT_ID = "tenantId";
    private static final String KEY_DATOS_PATRONIMICOS = "datosPatronimicos";
    private static final String KEY_ESPECIALIDAD = "especialidad";
    private static final String KEY_FECHA_CREACION = "fechaCreacion";
    private static final String KEY_AA_PRESTADOR = "aaPrestador";
    private static final String KEY_EMISOR_DOCUMENTO_OID = "emisorDocumentoOID";
    private static final String KEY_FORMATO = "formato";
    private static final String KEY_AUTOR = "autor";
    private static final String KEY_TITULO = "titulo";
    private static final String KEY_LANGUAGE_CODE = "languageCode";
    private static final String KEY_BREAKING_THE_GLASS = "breakingTheGlass";
    private static final String KEY_HASH_DOCUMENTO = "hashDocumento";
    private static final String KEY_DESCRIPCION = "descripcion";

    @Inject
    private DocumentoClinicoRepository repo;
    
    @Inject
    private HcenClient hcenClient;

    /**
     * Constructor sin argumentos requerido por CDI/Weld para crear proxies.
     */
    public DocumentoService() {
        // Constructor sin argumentos para CDI
    }

    /**
     * Guarda el contenido asociado a un documento previamente registrado en los metadatos.
     * Lanza IllegalArgumentException si no hay metadatos o si el tenant difiere.
     */
    public Document guardarContenido(String documentoId, String contenido) {
        // Verificar existencia de metadatos llamando al RNDC o Central
        String rndcBase = resolveConfiguredValue(PROP_RNDC_METADATOS_URL, DEFAULT_RNDC_METADATOS_URL);

        DTMetadatos metadatos;
        try {
            metadatos = fetchMetadatos(rndcBase, documentoId);
        } catch (ProcessingException ex) {
            // RNDC/Central down -> allow local persistence but signal central not updated
            // Could enqueue for retry. For now, persist locally but throw a specific runtime exception if desired.
            return repo.guardarContenido(documentoId, contenido);
        }

        // Validar tenant
        String metaTenant = metadatos != null ? metadatos.getTenantId() : null;
        String currentTenant = TenantContext.getCurrentTenant();
        if (currentTenant == null || metaTenant == null || !currentTenant.equals(metaTenant)) {
            throw new SecurityException("Tenant mismatch: usuario no autorizado para subir este contenido");
        }

        // Persistir contenido localmente y construir URL de acceso
        Document saved = repo.guardarContenido(documentoId, contenido);
        Object id = saved.get("_id");
        String documentoIdLocal = (id != null) ? id.toString() : "";
        String nodoBase = resolveConfiguredValue(PROP_NODO_BASE_URL, DEFAULT_NODO_BASE_URL);
        String url = nodoBase + "/api/documentos/" + documentoIdLocal;

        // Actualizar metadatos si es necesario (best-effort): intentar notificar al central con urlAcceso
        try {
            metadatos.setUrlAcceso(url);
            // Usar HcenClient para notificar al central sobre la URL de acceso actualizada
            hcenClient.registrarMetadatos(metadatos);
        } catch (Exception e) {
            // Si la notificación falla, continuar (se podría encolar para reintento via outbox)
        }

        return saved;
    }

    public Document buscarPorId(String idHex) {
        return repo.buscarPorId(idHex);
    }

    public List<String> buscarIdsPorDocumentoPaciente(String documentoId) {
        return repo.buscarIdsPorDocumentoPaciente(documentoId);
    }

    /**
     * Obtiene todos los contenidos de documentos de un paciente.
     * Busca documentos por pacienteDoc, documentoIdPaciente o documentoId.
     * 
     * @param documentoIdPaciente CI o documento de identidad del paciente
     * @return Lista de contenidos de documentos del paciente
     */
    public List<String> obtenerContenidosPorPaciente(String documentoIdPaciente) {
        if (documentoIdPaciente == null || documentoIdPaciente.isBlank()) {
            throw new IllegalArgumentException("documentoIdPaciente is required");
        }
        List<String> contenidos = new java.util.ArrayList<>();
        var collection = repo.getCollection();
        
        // Buscar por pacienteDoc (campo antiguo)
        var cursor1 = collection.find(new Document("pacienteDoc", documentoIdPaciente)).iterator();
        try {
            while (cursor1.hasNext()) {
                Document doc = cursor1.next();
                String contenido = doc.getString(KEY_CONTENIDO);
                if (contenido != null && !contenido.isBlank() && !contenidos.contains(contenido)) {
                    contenidos.add(contenido);
                }
            }
        } finally {
            cursor1.close();
        }
        
        // Buscar por documentoIdPaciente (campo nuevo)
        var cursor2 = collection.find(new Document(KEY_DOCUMENTO_ID_PACIENTE, documentoIdPaciente)).iterator();
        try {
            while (cursor2.hasNext()) {
                Document doc = cursor2.next();
                String contenido = doc.getString(KEY_CONTENIDO);
                if (contenido != null && !contenido.isBlank() && !contenidos.contains(contenido)) {
                    contenidos.add(contenido);
                }
            }
        } finally {
            cursor2.close();
        }
        
        return contenidos;
    }

    /**
     * Obtiene los metadatos de un documento por su documentoId.
     * Consulta RNDC vía HCEN central.
     * 
     * @param documentoId ID del documento (UUID)
     * @return Metadatos del documento o null si no se encuentra
     */
    public Map<String, Object> obtenerMetadatosPorDocumentoId(String documentoId) {
        if (documentoId == null || documentoId.isBlank()) {
            throw new IllegalArgumentException("documentoId is required");
        }
        try {
            // Consultar RNDC directamente o vía HCEN central
            String rndcBase = resolveConfiguredValue(PROP_RNDC_METADATOS_URL, "http://127.0.0.1:8080/rndc/api");
            
            String url = rndcBase + "/metadatos/" + documentoId;
            
            try (Client client = ClientBuilder.newClient();
                 Response response = client.target(url)
                        .request(MediaType.APPLICATION_JSON)
                        .get()) {
                
                if (response.getStatus() == 200) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> metadatos = response.readEntity(Map.class);
                    return metadatos;
                } else if (response.getStatus() == 404) {
                    return Collections.emptyMap();
                } else {
                    return Collections.emptyMap();
                }
            }
        } catch (Exception ex) {
            return Collections.emptyMap();
        }
    }

    /**
     * Crea un documento clínico completo: guarda el contenido en MongoDB y envía los metadatos al HCEN central.
     * Convierte automáticamente el contenido a PDF y lo guarda como binario.
     * @param body Map con "contenido" (requerido), "documentoIdPaciente" (requerido) y otros campos opcionales de metadatos
     * @return Map con el resultado incluyendo documentoId, urlAcceso, etc.
     */
    public Map<String, Object> crearDocumentoCompleto(Map<String, Object> body) {
        String contenido = extractRequiredString(body, KEY_CONTENIDO);
        String documentoIdPaciente = extractRequiredString(body, KEY_DOCUMENTO_ID_PACIENTE);
        String documentoId = resolveDocumentoId(body);
        String tenantId = resolveTenantId(body);

        // Convertir contenido a PDF
        byte[] pdfBytes = null;
        try {
            String titulo = (String) body.get("titulo");
            String autor = (String) body.get("autor");
            pdfBytes = PdfGenerator.textoAPdf(contenido, titulo, autor, documentoIdPaciente);
            LOGGER.log(Level.INFO, "PDF generado exitosamente para documento {0}", documentoId);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error al generar PDF, continuando sin PDF: {0}", e.getMessage());
            // Continuar sin PDF si hay error
        }

        // Guardar contenido con información del paciente y PDF
        Map<String, Object> storageMetadata = buildStorageMetadata(body, contenido);
        Document saved = repo.guardarContenidoConPacienteYArchivos(
            documentoId, contenido, pdfBytes, null, null, null, documentoIdPaciente, storageMetadata);
        String documentoIdLocal = extractMongoId(saved);
        String urlAcceso = buildUrlAcceso(documentoIdLocal);

        DTMetadatos metadatos = buildMetadatos(body, documentoId, documentoIdPaciente, tenantId, urlAcceso);
        Map<String, Object> payloadCompleto = buildPayload(metadatos, body);

        try {
            hcenClient.registrarMetadatosCompleto(payloadCompleto);
            // Si llegamos aquí, la sincronización fue exitosa
        } catch (HcenUnavailableException ex) {
            // Log del error pero no fallar la creación del documento
            LOGGER.log(Level.WARNING, "Error sincronizando metadatos con HCEN central: {0}", ex.getMessage());
            throw new MetadatosSyncException("Documento guardado localmente pero sin sincronización en HCEN", ex);
        }

        Map<String, Object> resultado = buildResultado(documentoId, documentoIdPaciente, urlAcceso, documentoIdLocal);
        resultado.put("tienePdf", pdfBytes != null && pdfBytes.length > 0);
        return resultado;
    }

    /**
     * Crea un documento clínico completo con archivo adjunto: guarda el contenido en MongoDB, 
     * convierte a PDF, guarda el archivo adjunto y envía los metadatos al HCEN central.
     * @param contenido Contenido de texto del documento
     * @param documentoIdPaciente CI del paciente
     * @param archivoAdjunto Bytes del archivo adjunto (opcional)
     * @param nombreArchivo Nombre del archivo adjunto (opcional)
     * @param tipoArchivo Tipo MIME del archivo adjunto (opcional)
     * @param metadatos Map con campos adicionales de metadatos (titulo, autor, especialidad, etc.)
     * @return Map con el resultado incluyendo documentoId, urlAcceso, etc.
     */
    public Map<String, Object> crearDocumentoCompletoConArchivo(String contenido, String documentoIdPaciente,
            byte[] archivoAdjunto, String nombreArchivo, String tipoArchivo, Map<String, Object> metadatos) {
        if (contenido == null || contenido.isBlank()) {
            throw new IllegalArgumentException("contenido es requerido");
        }
        if (documentoIdPaciente == null || documentoIdPaciente.isBlank()) {
            throw new IllegalArgumentException("documentoIdPaciente es requerido");
        }

        String documentoId = resolveDocumentoId(metadatos);
        String tenantId = resolveTenantId(metadatos);

        // Convertir contenido a PDF
        byte[] pdfBytes = null;
        try {
            String titulo = (String) metadatos.get("titulo");
            String autor = (String) metadatos.get("autor");
            pdfBytes = PdfGenerator.textoAPdf(contenido, titulo, autor, documentoIdPaciente);
            LOGGER.log(Level.INFO, "PDF generado exitosamente para documento {0}", documentoId);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error al generar PDF, continuando sin PDF: {0}", e.getMessage());
            // Continuar sin PDF si hay error
        }

        // Guardar contenido con información del paciente, PDF y archivo adjunto
        LOGGER.log(Level.INFO, "Guardando documento con archivo adjunto - documentoId: {0}, archivoAdjunto: {1} bytes, nombreArchivo: {2}, tipoArchivo: {3}",
                new Object[]{documentoId,
                    archivoAdjunto != null ? archivoAdjunto.length : 0,
                    nombreArchivo,
                    tipoArchivo});

        Map<String, Object> bodyCompleto = new HashMap<>(metadatos);
        bodyCompleto.put(KEY_CONTENIDO, contenido);
        bodyCompleto.put(KEY_DOCUMENTO_ID_PACIENTE, documentoIdPaciente);
        bodyCompleto.put(KEY_DOCUMENTO_ID, documentoId);

        Map<String, Object> storageMetadata = buildStorageMetadata(bodyCompleto, contenido);
        Document saved = repo.guardarContenidoConPacienteYArchivos(
            documentoId, contenido, pdfBytes, archivoAdjunto, nombreArchivo, tipoArchivo, documentoIdPaciente, storageMetadata);
        String documentoIdLocal = extractMongoId(saved);
        String urlAcceso = buildUrlAcceso(documentoIdLocal);
        
        // Verificar que se guardó correctamente
        Document savedDoc = repo.buscarPorId(documentoIdLocal);
        if (savedDoc != null) {
            Boolean tieneArchivoAdjunto = savedDoc.getBoolean("tieneArchivoAdjunto");
            org.bson.types.Binary archivoGuardado = savedDoc.get("archivoAdjunto", org.bson.types.Binary.class);
            LOGGER.log(Level.INFO, "Documento guardado - mongoId: {0}, tieneArchivoAdjunto: {1}, tamaño archivo guardado: {2} bytes", 
                    new Object[]{documentoIdLocal, 
                        tieneArchivoAdjunto, 
                        archivoGuardado != null && archivoGuardado.getData() != null ? archivoGuardado.getData().length : 0});
        } else {
            LOGGER.log(Level.WARNING, "No se pudo verificar el documento guardado - mongoId: {0}", documentoIdLocal);
        }

        DTMetadatos dtMetadatos = buildMetadatos(bodyCompleto, documentoId, documentoIdPaciente, tenantId, urlAcceso);
        Map<String, Object> payloadCompleto = buildPayload(dtMetadatos, bodyCompleto);

        try {
            hcenClient.registrarMetadatosCompleto(payloadCompleto);
            // Si llegamos aquí, la sincronización fue exitosa
        } catch (HcenUnavailableException ex) {
            // Log del error pero no fallar la creación del documento
            LOGGER.log(Level.WARNING, "Error sincronizando metadatos con HCEN central: {0}", ex.getMessage());
            throw new MetadatosSyncException("Documento guardado localmente pero sin sincronización en HCEN", ex);
        }

        Map<String, Object> resultado = buildResultado(documentoId, documentoIdPaciente, urlAcceso, documentoIdLocal);
        resultado.put("tienePdf", pdfBytes != null && pdfBytes.length > 0);
        resultado.put("tieneArchivoAdjunto", archivoAdjunto != null && archivoAdjunto.length > 0);
        if (nombreArchivo != null && !nombreArchivo.isBlank()) {
            resultado.put("nombreArchivo", nombreArchivo);
        }
        return resultado;
    }

    private DTMetadatos fetchMetadatos(String rndcBase, String documentoId) {
        String url = rndcBase + "/" + documentoId;
        try (Client client = ClientBuilder.newClient();
             Response resp = client.target(url)
                     .request(MediaType.APPLICATION_JSON_TYPE)
                     .get()) {

            if (resp.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                throw new IllegalArgumentException("No metadatos registered for documentoId");
            }
            if (resp.getStatus() >= Response.Status.BAD_REQUEST.getStatusCode()) {
                throw new MetadatosQueryException("Error querying metadatos: HTTP " + resp.getStatus());
            }

            String json = resp.readEntity(String.class);
            return parseMetadatos(json);
        }
    }

    private DTMetadatos parseMetadatos(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("json is required");
        }
        try (Jsonb jsonb = JsonbBuilder.create()) {
            return jsonb.fromJson(json, DTMetadatos.class);
        } catch (Exception e) {
            throw new MetadatosQueryException("Error parsing metadatos JSON", e);
        }
    }

    private String resolveConfiguredValue(String key, String defaultValue) {
        return System.getProperty(key, System.getenv().getOrDefault(key, defaultValue));
    }

    private String extractRequiredString(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException(key + " es requerido");
        }
        return value.toString();
    }

    private String resolveDocumentoId(Map<String, Object> body) {
        Object docIdObj = body.get(KEY_DOCUMENTO_ID);
        if (docIdObj != null && !docIdObj.toString().isBlank()) {
            return docIdObj.toString();
        }
        return UUID.randomUUID().toString();
    }

    private String resolveTenantId(Map<String, Object> body) {
        String currentTenant = TenantContext.getCurrentTenant();
        if (currentTenant != null && !currentTenant.isBlank()) {
            return currentTenant;
        }
        Object tenantFromBody = body.get(KEY_TENANT_ID);
        if (tenantFromBody != null && !tenantFromBody.toString().isBlank()) {
            String tenantId = tenantFromBody.toString();
            TenantContext.setCurrentTenant(tenantId);
            return tenantId;
        }
        throw new SecurityException("No hay tenantId en el contexto de la solicitud ni en el body");
    }

    private String extractMongoId(Document saved) {
        Object id = saved.get("_id");
        return id != null ? id.toString() : "";
    }

    private String buildUrlAcceso(String documentoIdLocal) {
        String nodoBase = resolveConfiguredValue(PROP_NODO_BASE_URL, DEFAULT_NODO_BASE_URL);
        // El periférico tiene su propio context root, pero la URL de acceso debe apuntar al periférico
        // La URL debe apuntar al endpoint /contenido que devuelve el PDF binario
        // Si el nodoBase ya incluye el path completo, usarlo directamente
        if (nodoBase.contains("/hcen-web") || nodoBase.contains("/nodo-periferico")) {
            return nodoBase + "/api/documentos/" + documentoIdLocal + "/contenido";
        }
        // Si no, asumir que es el base host y usar el context root del periférico
        return nodoBase + "/hcen-web/api/documentos/" + documentoIdLocal + "/contenido";
    }

    private DTMetadatos buildMetadatos(Map<String, Object> body,
                                       String documentoId,
                                       String documentoIdPaciente,
                                       String tenantId,
                                       String urlAcceso) {
        DTMetadatos metadatos = new DTMetadatos();
        metadatos.setDocumentoId(documentoId);
        metadatos.setDocumentoIdPaciente(documentoIdPaciente);
        metadatos.setTenantId(tenantId);
        metadatos.setUrlAcceso(urlAcceso);
        metadatos.setFechaRegistro(LocalDateTime.now());
        metadatos.setFechaCreacion(parseFechaCreacion(body.get(KEY_FECHA_CREACION)));

        setIfPresent(body, KEY_ESPECIALIDAD, metadatos::setEspecialidad);
        setIfPresent(body, KEY_AA_PRESTADOR, metadatos::setAaPrestador);
        setIfPresent(body, KEY_EMISOR_DOCUMENTO_OID, metadatos::setEmisorDocumentoOID);
        setIfPresent(body, KEY_FORMATO, metadatos::setFormato);
        setIfPresent(body, KEY_AUTOR, metadatos::setAutor);
        setIfPresent(body, KEY_TITULO, metadatos::setTitulo);
        setIfPresent(body, KEY_LANGUAGE_CODE, metadatos::setLanguageCode);
        setIfPresent(body, KEY_HASH_DOCUMENTO, metadatos::setHashDocumento);
        setIfPresent(body, KEY_DESCRIPCION, metadatos::setDescripcion);

        Object breakingTheGlass = body.get(KEY_BREAKING_THE_GLASS);
        if (breakingTheGlass != null) {
            metadatos.setBreakingTheGlass(Boolean.parseBoolean(breakingTheGlass.toString()));
        }

        return metadatos;
    }

    private LocalDateTime parseFechaCreacion(Object value) {
        if (value instanceof String str && !str.isBlank()) {
            try {
                return LocalDateTime.parse(str);
            } catch (Exception e) {
                return LocalDateTime.now();
            }
        }
        return LocalDateTime.now();
    }

    private Map<String, Object> buildPayload(DTMetadatos metadatos, Map<String, Object> body) {
        Map<String, Object> payloadCompleto = new HashMap<>();
        payloadCompleto.put(KEY_DOCUMENTO_ID, metadatos.getDocumentoId());
        payloadCompleto.put(KEY_DOCUMENTO_ID_PACIENTE, metadatos.getDocumentoIdPaciente());
        payloadCompleto.put(KEY_TENANT_ID, metadatos.getTenantId());
        payloadCompleto.put("urlAcceso", metadatos.getUrlAcceso());
        payloadCompleto.put("fechaRegistro", metadatos.getFechaRegistro());
        putIfNotNull(payloadCompleto, KEY_ESPECIALIDAD, metadatos.getEspecialidad());
        putIfNotNull(payloadCompleto, KEY_FECHA_CREACION, metadatos.getFechaCreacion());
        putIfNotNull(payloadCompleto, KEY_AA_PRESTADOR, metadatos.getAaPrestador());
        putIfNotNull(payloadCompleto, KEY_EMISOR_DOCUMENTO_OID, metadatos.getEmisorDocumentoOID());
        putIfNotNull(payloadCompleto, KEY_FORMATO, metadatos.getFormato());
        putIfNotNull(payloadCompleto, KEY_AUTOR, metadatos.getAutor());
        putIfNotNull(payloadCompleto, KEY_TITULO, metadatos.getTitulo());
        putIfNotNull(payloadCompleto, KEY_LANGUAGE_CODE, metadatos.getLanguageCode());
        payloadCompleto.put(KEY_BREAKING_THE_GLASS, metadatos.isBreakingTheGlass());
        putIfNotNull(payloadCompleto, KEY_HASH_DOCUMENTO, metadatos.getHashDocumento());
        putIfNotNull(payloadCompleto, KEY_DESCRIPCION, metadatos.getDescripcion());

        Object datosPatronimicos = body.get(KEY_DATOS_PATRONIMICOS);
        if (datosPatronimicos != null) {
            payloadCompleto.put(KEY_DATOS_PATRONIMICOS, datosPatronimicos);
        }

        return payloadCompleto;
    }

    private Map<String, Object> buildResultado(String documentoId,
                                               String documentoIdPaciente,
                                               String urlAcceso,
                                               String documentoIdLocal) {
        Map<String, Object> resultado = new HashMap<>();
        resultado.put(KEY_DOCUMENTO_ID, documentoId);
        resultado.put(KEY_DOCUMENTO_ID_PACIENTE, documentoIdPaciente);
        resultado.put("urlAcceso", urlAcceso);
        resultado.put("status", "created");
        resultado.put("mongoId", documentoIdLocal);
        return resultado;
    }

    private void setIfPresent(Map<String, Object> body, String key, Consumer<String> setter) {
        Object value = body.get(key);
        if (value != null && !value.toString().isBlank()) {
            setter.accept(value.toString());
        }
    }

    private void putIfNotNull(Map<String, Object> payload, String key, Object value) {
        if (value != null) {
            payload.put(key, value);
        }
    }

    private Map<String, Object> buildStorageMetadata(Map<String, Object> source, String contenido) {
        Map<String, Object> metadata = new HashMap<>();

        String titulo = normalizeString(source.get(KEY_TITULO));
        if (titulo == null || titulo.isBlank()) {
            titulo = normalizeString(source.get(KEY_ESPECIALIDAD));
        }
        if (titulo == null || titulo.isBlank()) {
            titulo = "Documento clínico";
        }
        metadata.put("titulo", titulo);

        String descripcion = normalizeString(source.get(KEY_DESCRIPCION));
        if ((descripcion == null || descripcion.isBlank()) && contenido != null) {
            descripcion = contenido.length() > 140 ? contenido.substring(0, 140) + "..." : contenido;
        }
        if (descripcion != null && !descripcion.isBlank()) {
            metadata.put("descripcion", descripcion);
        }

        String tipoDocumento = normalizeString(source.get("tipoDocumento"));
        if (tipoDocumento == null || tipoDocumento.isBlank()) {
            tipoDocumento = normalizeString(source.get(KEY_ESPECIALIDAD));
        }
        if (tipoDocumento == null || tipoDocumento.isBlank()) {
            tipoDocumento = "DOCUMENTO_CLINICO";
        }
        metadata.put("tipoDocumento", tipoDocumento);

        String autor = normalizeString(source.get(KEY_AUTOR));
        if (autor != null && !autor.isBlank()) {
            metadata.put("autor", autor);
        }

        String especialidad = normalizeString(source.get(KEY_ESPECIALIDAD));
        if (especialidad != null && !especialidad.isBlank()) {
            metadata.put("especialidad", especialidad);
        }

        String profesionalId = normalizeString(source.get("profesionalId"));
        if ((profesionalId == null || profesionalId.isBlank()) && autor != null && !autor.isBlank()) {
            profesionalId = autor;
        }
        if (profesionalId != null && !profesionalId.isBlank()) {
            metadata.put("profesionalId", profesionalId);
        }

        metadata.put("fechaCreacion", new Date());
        return metadata;
    }

    private String normalizeString(Object value) {
        if (value == null) {
            return null;
        }
        String str = value.toString();
        return str != null ? str.trim() : null;
    }
}
