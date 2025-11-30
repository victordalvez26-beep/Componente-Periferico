package uy.edu.tse.hcen.service;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import org.jboss.logging.Logger;
import uy.edu.tse.hcen.dto.DTMetadatos;
import uy.edu.tse.hcen.exceptions.HcenUnavailableException;
import uy.edu.tse.hcen.multitenancy.TenantContext;
import uy.edu.tse.hcen.repository.DocumentoClinicoRepository;
import uy.edu.tse.hcen.repository.UsuarioSaludRepository;
import uy.edu.tse.hcen.repository.ProfesionalSaludRepository;
import uy.edu.tse.hcen.util.DocumentoPdfFactory;
import org.bson.types.Binary;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio para gestionar documentos clínicos completos (el contenido de texto se transforma a PDF al descargarlo).
 * 
 * Responsabilidades:
 * - Crear documentos clínicos completos con contenido de texto
 * - Generar PDFs on-demand a partir del contenido guardado
 * - Almacenar contenido y archivos adjuntos en MongoDB
 * - Generar metadata automáticamente
 * - Sincronizar metadata con el backend HCEN (RNDC integrado)
 */
@RequestScoped
public class DocumentoService {

    private static final Logger LOG = Logger.getLogger(DocumentoService.class);

    @Inject
    private DocumentoClinicoRepository documentoRepository;

    @Inject
    private UsuarioSaludRepository usuarioSaludRepository;

    @Inject
    private ProfesionalSaludRepository profesionalSaludRepository;

    @Inject
    private HcenClient hcenClient;

    // URL base del nodo periférico para construir URIs de acceso
    private static final String DEFAULT_NODO_BASE_URL = "http://localhost:8081";
    private static final String PROP_NODO_BASE_URL = "NODO_BASE_URL";

    /**
     * Crea un documento clínico completo con contenido de texto.
     * El contenido se almacena en MongoDB y el PDF se genera on-demand al descargarlo.
     * 
     * @param tenantId ID de la clínica
     * @param profesionalId ID del profesional que crea el documento
     * @param ciPaciente CI del paciente
     * @param contenido Contenido de texto del documento
     * @param tipoDocumento Tipo de documento clínico (EVALUACION, INFORME, etc.)
     * @param descripcion Descripción opcional del documento
     * @param titulo Título opcional del documento
     * @param autor Autor opcional del documento
     * @return Map con información del documento creado
     */
    public Map<String, Object> crearDocumentoCompleto(
            Long tenantId,
            String profesionalId,
            String ciPaciente,
            String contenido,
            String tipoDocumento,
            String descripcion,
            String titulo,
            String autor) throws Exception {

        LOG.info(String.format("Creando documento completo - Clínica: %d, Paciente: %s, Profesional: %s", 
                tenantId, ciPaciente, profesionalId));

        // Validar contenido
        if (contenido == null || contenido.isBlank()) {
            throw new IllegalArgumentException("El contenido no puede estar vacío");
        }

        // 1. Generar ID único para el documento
        String documentoId = UUID.randomUUID().toString();

        // 2. Obtener información del paciente
        var paciente = usuarioSaludRepository.findByCiAndTenant(ciPaciente, tenantId);
        if (paciente == null) {
            throw new IllegalArgumentException(
                "El paciente con CI " + ciPaciente + 
                " no está registrado en esta clínica. Por favor, verifique que la cédula esté completa y registre al paciente antes de crear documentos."
            );
        }

        // 3. Asegurar que el TenantContext esté establecido
        String currentTenant = TenantContext.getCurrentTenant();
        if (currentTenant == null || !currentTenant.equals(String.valueOf(tenantId))) {
            TenantContext.setCurrentTenant(String.valueOf(tenantId));
            LOG.info(String.format("TenantContext establecido a: %d", tenantId));
        }
        
        // 4. Obtener información del profesional
        var profesionalOpt = profesionalSaludRepository.findByNickname(profesionalId);
        String nombreProfesional = profesionalOpt.map(p -> 
                p.getNombre() != null ? p.getNombre() : profesionalId).orElse(profesionalId);
        
        // Usar autor proporcionado o nombre del profesional como fallback
        String autorFinal = (autor != null && !autor.isBlank()) ? autor : nombreProfesional;
        String tituloFinal = (titulo != null && !titulo.isBlank()) ? titulo : 
                (tipoDocumento != null ? tipoDocumento : "Documento Clínico");
        
        // 5. Almacenar documento completo en MongoDB (sin guardar el PDF)
        String mongoId = documentoRepository.guardarDocumentoCompleto(
                documentoId,
                contenido,
                null,
                null, // sin archivo adjunto
                null,
                null,
                ciPaciente,
                tenantId,
                tipoDocumento != null ? tipoDocumento : "EVALUACION",
                descripcion,
                profesionalId,
                tituloFinal,
                autorFinal
        );

        LOG.info(String.format("Documento guardado en MongoDB con ID: %s", mongoId));

        // 7. Construir URL de acceso al documento (PDF) - usar el mismo endpoint que funciona para PDFs subidos
        // IMPORTANTE: Incluir tenantId en la URL para que la descarga funcione correctamente
        String nodoBaseUrl = System.getProperty(PROP_NODO_BASE_URL,
                System.getenv().getOrDefault(PROP_NODO_BASE_URL, DEFAULT_NODO_BASE_URL));
        String urlAcceso = nodoBaseUrl + "/hcen-web/api/documentos-pdf/" + mongoId + "?tenantId=" + tenantId;
        
        LOG.info(String.format("URL de acceso construida: %s", urlAcceso));

        // 8. Generar metadata
        DTMetadatos metadata = new DTMetadatos();
        metadata.setDocumentoId(documentoId);
        metadata.setDocumentoIdPaciente(ciPaciente);
        metadata.setTenantId(String.valueOf(tenantId));
        metadata.setFormato("application/pdf");
        metadata.setTipoDocumento(tipoDocumento != null ? tipoDocumento : "EVALUACION");
        // Usar zona horaria de Uruguay explícitamente
        ZoneId uruguayZone = ZoneId.of("America/Montevideo");
        metadata.setFechaCreacion(LocalDateTime.now(uruguayZone));
        metadata.setFechaRegistro(LocalDateTime.now(uruguayZone));
        metadata.setUrlAcceso(urlAcceso);
        metadata.setAutor(autorFinal);
        metadata.setTitulo(tituloFinal);
        metadata.setDescripcion(descripcion != null ? descripcion : "Documento clínico creado desde componente periférico");
        metadata.setLanguageCode("es-UY");
        metadata.setBreakingTheGlass(false);
        
        // Obtener nombre completo del paciente
        String nombrePaciente = paciente.getNombre() != null ? paciente.getNombre() : "";
        String apellidoPaciente = paciente.getApellido() != null ? paciente.getApellido() : "";
        metadata.setDatosPatronimicos(nombrePaciente + " " + apellidoPaciente);

        // 9. Enviar metadata al backend HCEN (RNDC integrado)
        try {
            hcenClient.registrarMetadatos(metadata);
            LOG.info(String.format("Metadata enviada exitosamente al backend HCEN para documento: %s", documentoId));
        } catch (HcenUnavailableException ex) {
            LOG.warn(String.format("No se pudo sincronizar metadata con HCEN (documento guardado localmente): %s", ex.getMessage()));
            // Continuamos aunque falle la sincronización - el documento ya está guardado
        }

        // 10. Construir respuesta
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("documentoId", documentoId);
        resultado.put("mongoId", mongoId);
        resultado.put("ciPaciente", ciPaciente);
        resultado.put("urlAcceso", urlAcceso);
        resultado.put("tipoDocumento", tipoDocumento != null ? tipoDocumento : "EVALUACION");
        resultado.put("fechaCreacion", LocalDateTime.now(uruguayZone).toString());
        resultado.put("sincronizado", true);

        return resultado;
    }

    /**
     * Crea un documento clínico completo con contenido de texto y archivo adjunto.
     * Similar a crearDocumentoCompleto pero incluye un archivo adjunto.
     * 
     * @param tenantId ID de la clínica
     * @param profesionalId ID del profesional que crea el documento
     * @param ciPaciente CI del paciente
     * @param contenido Contenido de texto del documento
     * @param tipoDocumento Tipo de documento clínico
     * @param descripcion Descripción opcional
     * @param titulo Título opcional
     * @param autor Autor opcional
     * @param archivoAdjuntoBytes Bytes del archivo adjunto
     * @param nombreArchivoAdjunto Nombre del archivo adjunto
     * @param tipoArchivoAdjunto Tipo MIME del archivo adjunto
     * @return Map con información del documento creado
     */
    public Map<String, Object> crearDocumentoCompletoConArchivo(
            Long tenantId,
            String profesionalId,
            String ciPaciente,
            String contenido,
            String tipoDocumento,
            String descripcion,
            String titulo,
            String autor,
            byte[] archivoAdjuntoBytes,
            String nombreArchivoAdjunto,
            String tipoArchivoAdjunto) throws Exception {

        LOG.info(String.format("Creando documento completo con archivo - Clínica: %d, Paciente: %s, Profesional: %s", 
                tenantId, ciPaciente, profesionalId));

        // Validar contenido
        if (contenido == null || contenido.isBlank()) {
            throw new IllegalArgumentException("El contenido no puede estar vacío");
        }

        // 1. Generar ID único
        String documentoId = UUID.randomUUID().toString();

        // 2. Obtener información del paciente
        var paciente = usuarioSaludRepository.findByCiAndTenant(ciPaciente, tenantId);
        if (paciente == null) {
            throw new IllegalArgumentException(
                "El paciente con CI " + ciPaciente + 
                " no está registrado en esta clínica. Por favor, verifique que la cédula esté completa y registre al paciente antes de crear documentos."
            );
        }

        // 3. Establecer TenantContext
        String currentTenant = TenantContext.getCurrentTenant();
        if (currentTenant == null || !currentTenant.equals(String.valueOf(tenantId))) {
            TenantContext.setCurrentTenant(String.valueOf(tenantId));
        }
        
        // 4. Obtener información del profesional
        var profesionalOpt = profesionalSaludRepository.findByNickname(profesionalId);
        String nombreProfesional = profesionalOpt.map(p -> 
                p.getNombre() != null ? p.getNombre() : profesionalId).orElse(profesionalId);
        
        String autorFinal = (autor != null && !autor.isBlank()) ? autor : nombreProfesional;
        String tituloFinal = (titulo != null && !titulo.isBlank()) ? titulo : 
                (tipoDocumento != null ? tipoDocumento : "Documento Clínico");
        
        // 5. Almacenar documento completo con archivo adjunto (sin guardar el PDF)
        String mongoId = documentoRepository.guardarDocumentoCompleto(
                documentoId,
                contenido,
                null,
                archivoAdjuntoBytes,
                nombreArchivoAdjunto,
                tipoArchivoAdjunto,
                ciPaciente,
                tenantId,
                tipoDocumento != null ? tipoDocumento : "EVALUACION",
                descripcion,
                profesionalId,
                tituloFinal,
                autorFinal
        );

        LOG.info(String.format("Documento con archivo adjunto guardado en MongoDB con ID: %s", mongoId));

        // 7. Construir URL de acceso - usar el mismo endpoint que funciona para PDFs subidos
        String nodoBaseUrl = System.getProperty(PROP_NODO_BASE_URL,
                System.getenv().getOrDefault(PROP_NODO_BASE_URL, DEFAULT_NODO_BASE_URL));
        String urlAcceso = nodoBaseUrl + "/hcen-web/api/documentos-pdf/" + mongoId;

        // 8. Generar metadata
        DTMetadatos metadata = new DTMetadatos();
        metadata.setDocumentoId(documentoId);
        metadata.setDocumentoIdPaciente(ciPaciente);
        metadata.setTenantId(String.valueOf(tenantId));
        metadata.setFormato("application/pdf");
        metadata.setTipoDocumento(tipoDocumento != null ? tipoDocumento : "EVALUACION");
        // Usar zona horaria de Uruguay explícitamente
        ZoneId uruguayZone = ZoneId.of("America/Montevideo");
        metadata.setFechaCreacion(LocalDateTime.now(uruguayZone));
        metadata.setFechaRegistro(LocalDateTime.now(uruguayZone));
        metadata.setUrlAcceso(urlAcceso);
        metadata.setAutor(autorFinal);
        metadata.setTitulo(tituloFinal);
        metadata.setDescripcion(descripcion != null ? descripcion : "Documento clínico con archivo adjunto");
        metadata.setLanguageCode("es-UY");
        metadata.setBreakingTheGlass(false);
        
        String nombrePaciente = paciente.getNombre() != null ? paciente.getNombre() : "";
        String apellidoPaciente = paciente.getApellido() != null ? paciente.getApellido() : "";
        metadata.setDatosPatronimicos(nombrePaciente + " " + apellidoPaciente);

        // 9. Enviar metadata al backend HCEN
        try {
            hcenClient.registrarMetadatos(metadata);
            LOG.info(String.format("Metadata enviada exitosamente al backend HCEN para documento: %s", documentoId));
        } catch (HcenUnavailableException ex) {
            LOG.warn(String.format("No se pudo sincronizar metadata con HCEN (documento guardado localmente): %s", ex.getMessage()));
        }

        // 10. Construir respuesta
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("documentoId", documentoId);
        resultado.put("mongoId", mongoId);
        resultado.put("ciPaciente", ciPaciente);
        resultado.put("urlAcceso", urlAcceso);
        resultado.put("tipoDocumento", tipoDocumento != null ? tipoDocumento : "EVALUACION");
        resultado.put("fechaCreacion", LocalDateTime.now(uruguayZone).toString());
        resultado.put("sincronizado", true);
        resultado.put("tieneArchivoAdjunto", archivoAdjuntoBytes != null && archivoAdjuntoBytes.length > 0);

        return resultado;
    }

    /**
     * Obtiene un documento por su ID de MongoDB.
     * 
     * @param mongoId ID de MongoDB (ObjectId en hex string)
     * @param tenantId ID de la clínica (para validación)
     * @return Document de MongoDB o null si no existe
     */
    public Document obtenerDocumentoPorId(String mongoId, Long tenantId) {
        return documentoRepository.buscarPorId(mongoId, tenantId);
    }

    /**
     * Obtiene el contenido de texto de un documento.
     * 
     * @param mongoId ID de MongoDB
     * @param tenantId ID de la clínica
     * @return Contenido de texto o null si no existe
     */
    public String obtenerContenido(String mongoId, Long tenantId) {
        Document doc = documentoRepository.buscarPorId(mongoId, tenantId);
        if (doc != null) {
            return doc.getString("contenido");
        }
        return null;
    }

    /**
     * Obtiene el PDF de un documento.
     * 
     * @param mongoId ID de MongoDB
     * @param tenantId ID de la clínica
     * @return Bytes del PDF o null si no existe
     */
    public byte[] obtenerPdf(String mongoId, Long tenantId) {
        Document doc = documentoRepository.buscarPorId(mongoId, tenantId);
        if (doc == null) {
            return null;
        }

        Binary pdfBinary = doc.get("pdfBytes", Binary.class);
        if (pdfBinary != null && pdfBinary.getData() != null && pdfBinary.getData().length > 0) {
            return pdfBinary.getData();
        }

        // Generar PDF on-demand usando el contenido guardado
        try {
            return DocumentoPdfFactory.generarDesdeDocumento(doc);
        } catch (IOException ex) {
            LOG.error(String.format("Error al generar PDF on-demand para documento %s", mongoId), ex);
            throw new RuntimeException("No se pudo generar el PDF en línea", ex);
        } catch (IllegalArgumentException ex) {
            LOG.warn(String.format("Documento %s no tiene contenido suficiente para generar PDF", mongoId), ex);
            return null;
        }
    }

    /**
     * Obtiene el archivo adjunto de un documento.
     * 
     * @param mongoId ID de MongoDB
     * @param tenantId ID de la clínica
     * @return Información del archivo adjunto {bytes, nombre, tipo} o null si no existe
     */
    public Map<String, Object> obtenerArchivoAdjunto(String mongoId, Long tenantId) {
        Document doc = documentoRepository.buscarPorId(mongoId, tenantId);
        if (doc != null) {
            Binary archivoBinary = doc.get("archivoAdjunto", Binary.class);
            if (archivoBinary != null && archivoBinary.getData() != null && archivoBinary.getData().length > 0) {
                Map<String, Object> archivo = new HashMap<>();
                archivo.put("bytes", archivoBinary.getData());
                archivo.put("nombre", doc.getString("nombreArchivoAdjunto"));
                archivo.put("tipo", doc.getString("tipoArchivoAdjunto"));
                return archivo;
            }
        }
        return null;
    }

    /**
     * Obtiene los contenidos de texto de todos los documentos de un paciente.
     * Busca en todas las clínicas (sin filtrar por tenant).
     * 
     * @param ciPaciente CI del paciente
     * @return Lista de contenidos de texto de los documentos del paciente
     */
    public List<String> obtenerContenidosPorPaciente(String ciPaciente) {
        if (ciPaciente == null || ciPaciente.isBlank()) {
            return new ArrayList<>();
        }
        
        // Buscar en todas las clínicas
        List<Document> documentos = documentoRepository.buscarPorCiPaciente(ciPaciente, null);
        
        List<String> contenidos = new ArrayList<>();
        for (Document doc : documentos) {
            String contenido = doc.getString("contenido");
            if (contenido != null && !contenido.isBlank()) {
                contenidos.add(contenido);
            }
        }
        
        return contenidos;
    }
}

