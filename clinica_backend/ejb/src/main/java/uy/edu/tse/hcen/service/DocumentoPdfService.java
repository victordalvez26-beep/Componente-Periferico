package uy.edu.tse.hcen.service;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.types.Binary;
import org.jboss.logging.Logger;
import uy.edu.tse.hcen.dto.DTMetadatos;
import uy.edu.tse.hcen.exceptions.HcenUnavailableException;
import uy.edu.tse.hcen.multitenancy.TenantContext;
import uy.edu.tse.hcen.repository.DocumentoPdfRepository;
import uy.edu.tse.hcen.repository.UsuarioSaludRepository;
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
    private static final String ENV_PERIPHERAL_NODE_URL = "PERIPHERAL_NODE_URL";
    
    // Constantes para literales duplicados
    private static final String KEY_DOCUMENTO_ID = "documentoId";
    private static final String KEY_MONGO_ID = "mongoId";
    private static final String KEY_CI_PACIENTE = "ciPaciente";
    private static final String KEY_URL_ACCESO = "urlAcceso";
    private static final String KEY_TIPO_DOCUMENTO = "tipoDocumento";
    private static final String KEY_FECHA_CREACION = "fechaCreacion";
    private static final String KEY_SINCRONIZADO = "sincronizado";
    private static final String KEY_TENANT_ID = "tenantId";
    private static final String KEY_ID = "id";
    private static final String KEY_PROFESIONAL_ID = "profesionalId";
    private static final String MIME_TYPE_PDF = "application/pdf";
    private static final String ZONE_URUGUAY = "America/Montevideo";
    private static final String LANG_CODE = "es-UY";
    private static final String KEY_PDF_BYTES = "pdfBytes";
    private static final String KEY_PDF = "pdf";
    private static final String PDF_HEADER = "%PDF";
    private static final String TITULO_EVALUACION_PREFIX = "Evaluación - ";
    private static final String DESC_DOC_SUBIDO = "Documento clínico subido desde componente periférico";
    private static final String ERROR_PDF_VACIO = "El archivo PDF está vacío";
    private static final String ERROR_PACIENTE_NO_ENCONTRADO = "Paciente no encontrado en esta clínica: %s. Por favor, registre al paciente antes de subir documentos.";
    private static final String PATH_DOCUMENTOS_PDF = "/hcen-web/api/documentos-pdf/";
    private static final String QUERY_TENANT_ID = "?tenantId=";
    private static final String KEY_CONTENT_TYPE = "contentType";
    private static final String KEY_DESCRIPCION = "descripcion";
    private static final String KEY_NOMBRE_PACIENTE = "nombrePaciente";
    private static final String KEY_APELLIDO_PACIENTE = "apellidoPaciente";
    private static final String KEY_URI_DOCUMENTO = "uriDocumento";
    private static final String KEY_PROFESIONAL_SALUD = "profesionalSalud";

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
            String descripcion) throws IOException {

        LOG.infof("Procesando PDF - Clínica: %d, Paciente: %s, Profesional: %s", 
                tenantId, ciPaciente, profesionalId);

        // 1. Leer el PDF completo en memoria
        byte[] pdfBytes = leerInputStream(pdfStream);
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException(ERROR_PDF_VACIO);
        }

        // 2. Generar ID único para el documento
        String documentoId = UUID.randomUUID().toString();

        // 3. Obtener información del paciente
        var paciente = usuarioSaludRepository.findByCiAndTenant(ciPaciente, tenantId);
        if (paciente == null) {
            throw new IllegalArgumentException(
                String.format(ERROR_PACIENTE_NO_ENCONTRADO, ciPaciente)
            );
        }

        // 4. Asegurar que el TenantContext esté establecido para la consulta
        String currentTenant = TenantContext.getCurrentTenant();
        String tenantIdStr = String.valueOf(tenantId);
        if (currentTenant == null || !tenantIdStr.equals(currentTenant)) {
            TenantContext.setCurrentTenant(tenantIdStr);
            LOG.infof("TenantContext establecido a: %d para búsqueda de profesional", tenantId);
        }
        
        // 5. Obtener información del profesional
        var profesionalOpt = profesionalSaludRepository.findByNickname(profesionalId);
        String nombreProfesional = profesionalOpt.map(p -> 
                p.getNombre() != null ? p.getNombre() : profesionalId).orElse(profesionalId);

        // 7. Almacenar PDF en MongoDB (con metadata adicional)
        String mongoId = documentoPdfRepository.guardarPdf(documentoId, pdfBytes, ciPaciente, tenantId,
                tipoDocumento, descripcion, profesionalId);

        LOG.infof("PDF guardado en MongoDB con ID: %s", mongoId);

        // 8. Construir URL de acceso al documento
        // La URL debe usar localhost:8081 para que el backend HCEN pueda convertirla
        // a hcen-wildfly-app:8080 cuando acceda desde Docker
        // Buscar también PERIPHERAL_NODE_URL por compatibilidad con HCEN Backend
        String nodoBaseUrl = System.getenv().getOrDefault(ENV_PERIPHERAL_NODE_URL, 
                System.getProperty(PROP_NODO_BASE_URL,
                    System.getenv().getOrDefault(PROP_NODO_BASE_URL, DEFAULT_NODO_BASE_URL)));
                    
        // Incluir tenantId en la URL para que el acceso directo funcione
        String pathDocumentos = System.getenv().getOrDefault("PATH_DOCUMENTOS_PDF", PATH_DOCUMENTOS_PDF);
        String urlAcceso = nodoBaseUrl + pathDocumentos + mongoId + QUERY_TENANT_ID + tenantId;
        
        LOG.infof("📝 [PERIFERICO] Construyendo URL de acceso - Base URL: %s, MongoId: %s, URL completa: %s", 
                nodoBaseUrl, mongoId, urlAcceso);

        // 9. Generar metadata
        DTMetadatos metadata = new DTMetadatos();
        metadata.setDocumentoId(documentoId);
        metadata.setDocumentoIdPaciente(ciPaciente);
        metadata.setTenantId(String.valueOf(tenantId));
        metadata.setFormato(MIME_TYPE_PDF);
        metadata.setTipoDocumento(tipoDocumento);
        // Usar zona horaria de Uruguay explícitamente
        java.time.ZoneId uruguayZone = java.time.ZoneId.of(ZONE_URUGUAY);
        metadata.setFechaCreacion(LocalDateTime.now(uruguayZone));
        metadata.setFechaRegistro(LocalDateTime.now(uruguayZone));
        metadata.setUrlAcceso(urlAcceso);
        metadata.setAutor(nombreProfesional);
        metadata.setTitulo(TITULO_EVALUACION_PREFIX + tipoDocumento);
        metadata.setDescripcion(descripcion != null ? descripcion : DESC_DOC_SUBIDO);
        metadata.setLanguageCode(LANG_CODE);
        metadata.setBreakingTheGlass(false);
        
        // Obtener nombre completo del paciente
        String nombrePaciente = paciente.getNombre() != null ? paciente.getNombre() : "";
        String apellidoPaciente = paciente.getApellido() != null ? paciente.getApellido() : "";
        metadata.setDatosPatronimicos(nombrePaciente + " " + apellidoPaciente);

        // 10. Enviar metadata al backend HCEN (RNDC)
        boolean sincronizado = true;
        try {
            hcenClient.registrarMetadatos(metadata);
            LOG.infof("Metadata enviada exitosamente al backend HCEN para documento: %s", documentoId);
        } catch (HcenUnavailableException ex) {
            sincronizado = false;
            LOG.warnf("No se pudo sincronizar metadata con HCEN (documento guardado localmente): %s", ex.getMessage());
            // Continuamos aunque falle la sincronización - el documento ya está guardado
        }

        // 11. Construir respuesta
        Map<String, Object> resultado = new HashMap<>();
        resultado.put(KEY_DOCUMENTO_ID, documentoId);
        resultado.put(KEY_MONGO_ID, mongoId);
        resultado.put(KEY_CI_PACIENTE, ciPaciente);
        resultado.put(KEY_URL_ACCESO, urlAcceso);
        resultado.put(KEY_TIPO_DOCUMENTO, tipoDocumento);
        resultado.put(KEY_FECHA_CREACION, LocalDateTime.now(java.time.ZoneId.of(ZONE_URUGUAY)).toString());
        resultado.put(KEY_SINCRONIZADO, sincronizado);

        return resultado;
    }

    /**
     * Busca un documento en los repositorios disponibles (DocumentoPdfRepository y DocumentoClinicoRepository).
     * Primero busca en DocumentoPdfRepository y luego en DocumentoClinicoRepository.
     * Si no encuentra con el tenantId proporcionado, busca sin filtrar por tenant.
     * 
     * @param mongoId ID del documento en MongoDB
     * @param tenantId ID de la clínica (puede ser null para buscar sin filtrar)
     * @return Document encontrado o null si no existe
     */
    private Document buscarDocumentoEnRepositorios(String mongoId, Long tenantId) {
        // Primero intentar buscar en DocumentoPdfRepository (PDFs subidos directamente)
        Document doc = documentoPdfRepository.buscarPorId(mongoId, tenantId);
        
        // Si no se encuentra, intentar buscar en DocumentoClinicoRepository (documentos completos)
        if (doc == null) {
            LOG.infof("🔍 [PERIFERICO] Documento no encontrado en DocumentoPdfRepository, buscando en DocumentoClinicoRepository - ID: %s", mongoId);
            try {
                doc = documentoClinicoRepository.buscarPorId(mongoId, tenantId);
                
                if (doc != null) {
                    LOG.infof("✅ [PERIFERICO] Documento encontrado en DocumentoClinicoRepository - ID: %s", mongoId);
                }
            } catch (Exception ex) {
                LOG.warnf(ex, "⚠️ [PERIFERICO] Error al buscar en DocumentoClinicoRepository: %s", ex.getMessage());
            }
        } else {
            LOG.infof("✅ [PERIFERICO] Documento encontrado en DocumentoPdfRepository - ID: %s", mongoId);
        }
        
        // Si no se encuentra con el tenantId proporcionado, intentar buscar sin filtrar por tenantId
        // (útil cuando la URL de descarga no incluye el tenantId correcto)
        if (doc == null && tenantId != null) {
            LOG.infof("🔍 [PERIFERICO] Documento no encontrado con tenant %d, buscando sin filtrar por tenant - ID: %s", tenantId, mongoId);
            try {
                // Buscar en DocumentoPdfRepository sin filtrar por tenant
                doc = documentoPdfRepository.buscarPorId(mongoId, null);
                if (doc == null) {
                    // Buscar en DocumentoClinicoRepository sin filtrar por tenant
                    doc = documentoClinicoRepository.buscarPorId(mongoId, null);
                }
                
                if (doc != null) {
                    Long docTenantId = doc.getLong(KEY_TENANT_ID);
                    LOG.infof("✅ [PERIFERICO] Documento encontrado sin filtrar por tenant - ID: %s, Tenant real: %d", mongoId, docTenantId);
                }
            } catch (Exception ex) {
                LOG.warnf(ex, "⚠️ [PERIFERICO] Error al buscar sin filtrar por tenant: %s", ex.getMessage());
            }
        }
        
        return doc;
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
        LOG.infof("🔍 [PERIFERICO] Obteniendo metadata - ID: %s, Tenant: %d", mongoId, tenantId);
        
        // Buscar documento en repositorios
        Document doc = buscarDocumentoEnRepositorios(mongoId, tenantId);
        
        if (doc == null) {
            LOG.warnf("❌ [PERIFERICO] Metadata no encontrada en ningún repositorio - ID: %s, Tenant: %d", mongoId, tenantId);
            // Retornar null indica que el documento no fue encontrado (más apropiado que Map vacío)
            return null;
        }
        
        // Obtener el tenantId real del documento si está disponible
        Long docTenantId = doc.getLong(KEY_TENANT_ID);
        if (docTenantId != null) {
            tenantId = docTenantId;
        }
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(KEY_ID, mongoId);
        metadata.put(KEY_CI_PACIENTE, doc.getString(KEY_CI_PACIENTE));
        metadata.put(KEY_TIPO_DOCUMENTO, doc.getString(KEY_TIPO_DOCUMENTO));
        metadata.put(KEY_PROFESIONAL_ID, doc.getString(KEY_PROFESIONAL_ID));
        metadata.put(KEY_TENANT_ID, tenantId);
        
        LOG.infof("✅ [PERIFERICO] Metadata obtenida - CI Paciente: %s, Tipo: %s, Tenant: %d", 
                metadata.get(KEY_CI_PACIENTE), metadata.get(KEY_TIPO_DOCUMENTO), tenantId);
        
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
        LOG.infof("🔍 [PERIFERICO] Obteniendo PDF de MongoDB - ID: %s, Clínica: %d", mongoId, tenantId);
        
        // Buscar documento en repositorios
        Document documento = buscarDocumentoEnRepositorios(mongoId, tenantId);
        
        if (documento == null) {
            LOG.warnf("❌ [PERIFERICO] Documento no encontrado en ningún repositorio - ID: %s, Tenant: %d", mongoId, tenantId);
            // Retornar null indica que el documento no fue encontrado (más apropiado que array vacío)
            return null;
        }

        // Extraer bytes del PDF - puede estar en pdfBytes o en pdf (documentos completos)
        Binary pdfBinary = documento.get(KEY_PDF_BYTES, Binary.class);
        if (pdfBinary == null) {
            // Intentar con el campo "pdf" usado por documentos completos
            pdfBinary = documento.get(KEY_PDF, Binary.class);
        }
        
        if (pdfBinary == null || pdfBinary.getData() == null || pdfBinary.getData().length == 0) {
            LOG.infof("ℹ️ [PERIFERICO] Documento %s no tiene PDF persistido, generando on-demand", mongoId);
            try {
                byte[] generado = DocumentoPdfFactory.generarDesdeDocumento(documento);
                LOG.infof("✅ [PERIFERICO] PDF generado on-demand - ID: %s, Tamaño: %d bytes", mongoId, generado.length);
                return generado;
            } catch (IOException ex) {
                LOG.errorf(ex, "❌ [PERIFERICO] Error al generar PDF on-demand - ID: %s", mongoId);
                // Retornar null indica que no se pudo generar el PDF
                return null;
            } catch (IllegalArgumentException ex) {
                LOG.warnf(ex, "❌ [PERIFERICO] No se pudo generar PDF on-demand (datos incompletos) - ID: %s", mongoId);
                // Retornar null indica que no se pudo generar el PDF
                return null;
            }
        }

        byte[] pdfData = pdfBinary.getData();
        LOG.infof("✅ [PERIFERICO] PDF extraído de MongoDB - ID: %s, Tamaño: %d bytes", mongoId, pdfData.length);
        
        // Verificar que los primeros bytes sean de un PDF válido
        if (pdfData.length >= 4) {
            String header = new String(pdfData, 0, 4);
            if (!header.startsWith(PDF_HEADER)) {
                LOG.warnf("⚠️ [PERIFERICO] Los primeros bytes no son de un PDF válido: %s", header);
                LOG.warnf("⚠️ [PERIFERICO] Primeros 200 bytes: %s", 
                        new String(pdfData, 0, Math.min(200, pdfData.length)));
            } else {
                LOG.infof("✅ [PERIFERICO] PDF válido detectado en MongoDB - Header: %s", header);
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
        LOG.infof("Listando documentos - Paciente: %s, Profesional: %s, Clínica Profesional: %s", 
                ciPaciente, profesionalId, tenantIdProfesional);
        
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
                        LOG.infof("Especialidad del profesional %s: %s", profesionalId, especialidad);
                    }
                    nombreProfesional = profesional.getNombre();
                    LOG.infof("Nombre del profesional %s: %s", profesionalId, nombreProfesional);
                }
            } catch (Exception e) {
                LOG.warnf(e, "No se pudo obtener información del profesional %s: %s", profesionalId, e.getMessage());
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
            LOG.infof("Obtenidos %d metadatos (ya filtrados por políticas) desde HCEN backend para el paciente %s", 
                    metadatosFiltrados.size(), ciPaciente);
        } catch (HcenUnavailableException e) {
            LOG.errorf(e, "Error al consultar HCEN backend para obtener metadatos: %s", e.getMessage());
            // Retornar lista vacía si HCEN no está disponible
            return new java.util.ArrayList<>();
        }
        
        // Mapear los metadatos del HCEN al formato esperado por el frontend
        java.util.List<Map<String, Object>> resultado = new java.util.ArrayList<>();
        for (Map<String, Object> metadata : metadatosFiltrados) {
            Map<String, Object> documentoMapeado = new HashMap<>();
            documentoMapeado.put(KEY_ID, metadata.get(KEY_ID));
            documentoMapeado.put(KEY_DOCUMENTO_ID, metadata.get(KEY_DOCUMENTO_ID));
            documentoMapeado.put(KEY_CI_PACIENTE, ciPaciente);
            documentoMapeado.put(KEY_TENANT_ID, metadata.get(KEY_TENANT_ID)); // Incluir tenantId para saber de qué clínica es
            documentoMapeado.put(KEY_FECHA_CREACION, metadata.get(KEY_FECHA_CREACION));
            documentoMapeado.put(KEY_CONTENT_TYPE, MIME_TYPE_PDF); // Los documentos son PDFs
            documentoMapeado.put(KEY_TIPO_DOCUMENTO, metadata.get(KEY_TIPO_DOCUMENTO));
            documentoMapeado.put(KEY_DESCRIPCION, metadata.get(KEY_DESCRIPCION));
            documentoMapeado.put(KEY_PROFESIONAL_ID, metadata.get(KEY_PROFESIONAL_SALUD));
            documentoMapeado.put(KEY_NOMBRE_PACIENTE, metadata.get(KEY_NOMBRE_PACIENTE));
            documentoMapeado.put(KEY_APELLIDO_PACIENTE, metadata.get(KEY_APELLIDO_PACIENTE));
            documentoMapeado.put(KEY_URI_DOCUMENTO, metadata.get(KEY_URI_DOCUMENTO)); // URI para descargar el documento
            
            resultado.add(documentoMapeado);
        }
        
        LOG.infof("Retornando %d documentos autorizados para el profesional %s", 
                resultado.size(), profesionalId);
        return resultado;
    }

    /**
     * Lee un InputStream completo y lo convierte en byte array.
     */
    private byte[] leerInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int nRead;
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }
}


