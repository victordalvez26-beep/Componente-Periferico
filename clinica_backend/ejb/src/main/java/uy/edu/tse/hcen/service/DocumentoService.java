package uy.edu.tse.hcen.service;

import uy.edu.tse.hcen.repository.DocumentoClinicoRepository;
import uy.edu.tse.hcen.context.TenantContext;
import uy.edu.tse.hcen.dto.DTMetadatos;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.bson.Document;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
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
@Stateless
public class DocumentoService {

    @Inject
    private DocumentoClinicoRepository repo;

    @Inject
    private TenantContext tenantContext;

    @Inject
    private HcenClient hcenClient;

    public DocumentoService() {
    }

    /**
     * Guarda el contenido asociado a un documento previamente registrado en los metadatos.
     * Lanza IllegalArgumentException si no hay metadatos o si el tenant difiere.
     */
    public Document guardarContenido(String documentoId, String contenido) {
        // 1) Verificar existencia de metadatos llamando al RNDC o Central
        String rndcBase = System.getProperty("RNDC_METADATOS_URL",
                System.getenv().getOrDefault("RNDC_METADATOS_URL", "http://127.0.0.1:8080/api/metadatos"));

        DTMetadatos metadatos = null;
        Client client = null;
        Response resp = null;
        try {
            client = ClientBuilder.newClient();
            resp = client.target(rndcBase + "/" + documentoId)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get();

            if (resp.getStatus() == 404) {
                throw new IllegalArgumentException("No metadatos registered for documentoId");
            }
            if (resp.getStatus() >= 400) {
                throw new RuntimeException("Error querying metadatos: HTTP " + resp.getStatus());
            }

            String json = resp.readEntity(String.class);
            Jsonb jsonb = JsonbBuilder.create();
            try {
                metadatos = jsonb.fromJson(json, DTMetadatos.class);
            } finally {
                try {
                    jsonb.close();
                } catch (Exception _ignore) {
                    // ignore close exceptions
                }
            }

        } catch (ProcessingException ex) {
            // RNDC/Central down -> allow local persistence but signal central not updated
            // Could enqueue for retry. For now, persist locally but throw a specific runtime exception if desired.
            return repo.guardarContenido(documentoId, contenido);
        } finally {
            if (resp != null) resp.close();
            if (client != null) client.close();
        }

        // 2) Validar tenant
        String metaTenant = metadatos != null ? metadatos.getTenantId() : null;
        String currentTenant = tenantContext.getTenantId();
        if (currentTenant == null || metaTenant == null || !currentTenant.equals(metaTenant)) {
            throw new SecurityException("Tenant mismatch: usuario no autorizado para subir este contenido");
        }

        // 3) Persistir contenido localmente y construir URL de acceso
        Document saved = repo.guardarContenido(documentoId, contenido);
        Object id = saved.get("_id");
        String documentoIdLocal = (id != null) ? id.toString() : "";
        String nodoBase = System.getProperty("NODO_BASE_URL", System.getenv().getOrDefault("NODO_BASE_URL", "http://127.0.0.1:8080"));
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
     * Crea un documento clínico completo: guarda el contenido en MongoDB y envía los metadatos al HCEN central.
     * @param body Map con "contenido" (requerido), "documentoIdPaciente" (requerido) y otros campos opcionales de metadatos
     * @return Map con el resultado incluyendo documentoId, urlAcceso, etc.
     */
    public Map<String, Object> crearDocumentoCompleto(Map<String, Object> body) {
        // 1) Validar campos requeridos
        Object contenidoObj = body.get("contenido");
        if (contenidoObj == null) {
            throw new IllegalArgumentException("contenido es requerido");
        }
        String contenido = contenidoObj.toString();

        Object documentoIdPacienteObj = body.get("documentoIdPaciente");
        if (documentoIdPacienteObj == null) {
            throw new IllegalArgumentException("documentoIdPaciente es requerido");
        }
        String documentoIdPaciente = documentoIdPacienteObj.toString();

        // 2) Generar documentoId si no viene
        String documentoId;
        Object docIdObj = body.get("documentoId");
        if (docIdObj != null && !docIdObj.toString().isBlank()) {
            documentoId = docIdObj.toString();
        } else {
            // Generar UUID único para el documento
            documentoId = UUID.randomUUID().toString();
        }

        // 3) Obtener tenantId del contexto
        String tenantId = tenantContext.getTenantId();
        if (tenantId == null) {
            // Intentar obtener desde el body si está disponible
            Object bodyTenantId = body.get("tenantId");
            if (bodyTenantId != null) {
                tenantId = bodyTenantId.toString();
                // Establecer en el contexto para uso posterior
                tenantContext.setTenantId(tenantId);
            } else {
                throw new SecurityException("No hay tenantId en el contexto de la solicitud ni en el body");
            }
        }

        // 4) Guardar contenido en MongoDB
        Document saved = repo.guardarContenido(documentoId, contenido);
        Object id = saved.get("_id");
        String documentoIdLocal = (id != null) ? id.toString() : "";

        // 5) Construir URL de acceso (incluir context root /hcen-web)
        String nodoBase = System.getProperty("NODO_BASE_URL", System.getenv().getOrDefault("NODO_BASE_URL", "http://127.0.0.1:8080"));
        String urlAcceso = nodoBase + "/hcen-web/api/documentos/" + documentoIdLocal;

        // 6) Construir DTO de metadatos
        DTMetadatos metadatos = new DTMetadatos();
        metadatos.setDocumentoId(documentoId);
        metadatos.setDocumentoIdPaciente(documentoIdPaciente);
        metadatos.setTenantId(tenantId);
        metadatos.setUrlAcceso(urlAcceso);
        metadatos.setFechaRegistro(LocalDateTime.now());

        // Campos opcionales del body
        if (body.get("especialidad") != null) {
            metadatos.setEspecialidad(body.get("especialidad").toString());
        }
        if (body.get("fechaCreacion") != null) {
            try {
                if (body.get("fechaCreacion") instanceof String) {
                    metadatos.setFechaCreacion(LocalDateTime.parse(body.get("fechaCreacion").toString()));
                }
            } catch (Exception e) {
                // Si falla el parse, usar fecha actual
                metadatos.setFechaCreacion(LocalDateTime.now());
            }
        } else {
            metadatos.setFechaCreacion(LocalDateTime.now());
        }
        if (body.get("aaPrestador") != null) {
            metadatos.setAaPrestador(body.get("aaPrestador").toString());
        }
        if (body.get("emisorDocumentoOID") != null) {
            metadatos.setEmisorDocumentoOID(body.get("emisorDocumentoOID").toString());
        }
        if (body.get("formato") != null) {
            metadatos.setFormato(body.get("formato").toString());
        }
        if (body.get("autor") != null) {
            metadatos.setAutor(body.get("autor").toString());
        }
        if (body.get("titulo") != null) {
            metadatos.setTitulo(body.get("titulo").toString());
        }
        if (body.get("languageCode") != null) {
            metadatos.setLanguageCode(body.get("languageCode").toString());
        }
        if (body.get("breakingTheGlass") != null) {
            metadatos.setBreakingTheGlass(Boolean.parseBoolean(body.get("breakingTheGlass").toString()));
        }
        if (body.get("hashDocumento") != null) {
            metadatos.setHashDocumento(body.get("hashDocumento").toString());
        }
        if (body.get("descripcion") != null) {
            metadatos.setDescripcion(body.get("descripcion").toString());
        }
        // 7) Construir payload completo para enviar al central (incluye datosPatronimicos si están disponibles)
        Map<String, Object> payloadCompleto = new HashMap<>();
        payloadCompleto.put("documentoId", metadatos.getDocumentoId());
        payloadCompleto.put("documentoIdPaciente", metadatos.getDocumentoIdPaciente());
        payloadCompleto.put("tenantId", metadatos.getTenantId());
        payloadCompleto.put("urlAcceso", metadatos.getUrlAcceso());
        payloadCompleto.put("fechaRegistro", metadatos.getFechaRegistro());
        if (metadatos.getEspecialidad() != null) payloadCompleto.put("especialidad", metadatos.getEspecialidad());
        if (metadatos.getFechaCreacion() != null) payloadCompleto.put("fechaCreacion", metadatos.getFechaCreacion());
        if (metadatos.getAaPrestador() != null) payloadCompleto.put("aaPrestador", metadatos.getAaPrestador());
        if (metadatos.getEmisorDocumentoOID() != null) payloadCompleto.put("emisorDocumentoOID", metadatos.getEmisorDocumentoOID());
        if (metadatos.getFormato() != null) payloadCompleto.put("formato", metadatos.getFormato());
        if (metadatos.getAutor() != null) payloadCompleto.put("autor", metadatos.getAutor());
        if (metadatos.getTitulo() != null) payloadCompleto.put("titulo", metadatos.getTitulo());
        if (metadatos.getLanguageCode() != null) payloadCompleto.put("languageCode", metadatos.getLanguageCode());
        payloadCompleto.put("breakingTheGlass", metadatos.isBreakingTheGlass());
        if (metadatos.getHashDocumento() != null) payloadCompleto.put("hashDocumento", metadatos.getHashDocumento());
        if (metadatos.getDescripcion() != null) payloadCompleto.put("descripcion", metadatos.getDescripcion());
        
        // Incluir datosPatronimicos si están disponibles
        if (body.get("datosPatronimicos") != null) {
            payloadCompleto.put("datosPatronimicos", body.get("datosPatronimicos"));
        }

        // 8) Enviar payload completo al central (que luego encolará en RNDC)
        try {
            hcenClient.registrarMetadatosCompleto(payloadCompleto);
        } catch (uy.edu.tse.hcen.exceptions.HcenUnavailableException ex) {
            // Si el central no está disponible, el documento se guardó localmente pero no se registraron metadatos
            // En producción podría implementarse un mecanismo de reintentos
            throw new RuntimeException("Documento guardado localmente pero no se pudieron registrar metadatos en HCEN: " + ex.getMessage(), ex);
        }

        // 9) Construir respuesta
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("documentoId", documentoId);
        resultado.put("documentoIdPaciente", documentoIdPaciente);
        resultado.put("urlAcceso", urlAcceso);
        resultado.put("status", "created");
        resultado.put("mongoId", documentoIdLocal);
        return resultado;
    }
}
