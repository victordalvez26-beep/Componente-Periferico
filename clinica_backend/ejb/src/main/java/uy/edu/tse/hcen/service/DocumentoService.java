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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    private static final String PROP_RNDC_METADATOS_URL = "RNDC_METADATOS_URL";
    private static final String PROP_NODO_BASE_URL = "NODO_BASE_URL";
    private static final String DEFAULT_RNDC_METADATOS_URL = "http://127.0.0.1:8080/api/metadatos";
    private static final String DEFAULT_NODO_BASE_URL = "http://127.0.0.1:8080";

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
        // 1) Verificar existencia de metadatos llamando al RNDC o Central
        String rndcBase = resolveConfiguredValue(PROP_RNDC_METADATOS_URL, DEFAULT_RNDC_METADATOS_URL);

        DTMetadatos metadatos;
        try {
            metadatos = fetchMetadatos(rndcBase, documentoId);
        } catch (ProcessingException ex) {
            // RNDC/Central down -> allow local persistence but signal central not updated
            // Could enqueue for retry. For now, persist locally but throw a specific runtime exception if desired.
            return repo.guardarContenido(documentoId, contenido);
        }

        // 2) Validar tenant
        String metaTenant = metadatos != null ? metadatos.getTenantId() : null;
        String currentTenant = TenantContext.getCurrentTenant();
        if (currentTenant == null || metaTenant == null || !currentTenant.equals(metaTenant)) {
            throw new SecurityException("Tenant mismatch: usuario no autorizado para subir este contenido");
        }

        // 3) Persistir contenido localmente y construir URL de acceso
        Document saved = repo.guardarContenido(documentoId, contenido);
        Object id = saved.get("_id");
        String documentoIdLocal = (id != null) ? id.toString() : "";
        String nodoBase = resolveConfiguredValue(PROP_NODO_BASE_URL, DEFAULT_NODO_BASE_URL);
        String url = nodoBase + "/api/documentos/" + documentoIdLocal;

        // 4) Actualizar metadatos si es necesario (best-effort): intentar notificar al central con urlAcceso
        try {
            metadatos.setUrlAcceso(url);
            // use injected HcenClient to notify central about updated access URL
            hcenClient.registrarMetadatos(metadatos);
        } catch (Exception e) {
            // If notification fails, continue (could enqueue for retry via outbox)
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
     * Obtiene los metadatos de un documento por su documentoId.
     * Consulta RNDC vía HCEN central.
     * 
     * @param documentoId ID del documento (UUID)
     * @return Metadatos del documento o null si no se encuentra
     */
    public Map<String, Object> obtenerMetadatosPorDocumentoId(String documentoId) {
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
     * @param body Map con "contenido" (requerido), "documentoIdPaciente" (requerido) y otros campos opcionales de metadatos
     * @return Map con el resultado incluyendo documentoId, urlAcceso, etc.
     */
    public Map<String, Object> crearDocumentoCompleto(Map<String, Object> body) {
        String contenido = extractRequiredString(body, KEY_CONTENIDO);
        String documentoIdPaciente = extractRequiredString(body, KEY_DOCUMENTO_ID_PACIENTE);
        String documentoId = resolveDocumentoId(body);
        String tenantId = resolveTenantId(body);

        Document saved = repo.guardarContenido(documentoId, contenido);
        String documentoIdLocal = extractMongoId(saved);
        String urlAcceso = buildUrlAcceso(documentoIdLocal);

        DTMetadatos metadatos = buildMetadatos(body, documentoId, documentoIdPaciente, tenantId, urlAcceso);
        Map<String, Object> payloadCompleto = buildPayload(metadatos, body);

        try {
            hcenClient.registrarMetadatosCompleto(payloadCompleto);
        } catch (HcenUnavailableException ex) {
            throw new MetadatosSyncException("Documento guardado localmente pero sin sincronización en HCEN", ex);
        }

        return buildResultado(documentoId, documentoIdPaciente, urlAcceso, documentoIdLocal);
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
        return nodoBase + "/hcen-web/api/documentos/" + documentoIdLocal;
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
}
