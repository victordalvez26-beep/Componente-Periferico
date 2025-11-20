package uy.edu.tse.hcen.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DocumentoClinicoRepository {

    @Inject // Inyecta la instancia producida por MongoDBProducer
    private MongoDatabase database;

    // Public no-arg constructor required so CDI can create proxies for application-scoped beans
    public DocumentoClinicoRepository() {
    }

    public MongoCollection<Document> getCollection() {
        // 'documentos_clinicos' es el nombre de la colección
        return database.getCollection("documentos_clinicos");
    }

    /**
     * Retorna la colección donde se guardan los metadatos de los documentos.
     * Se usa una colección separada para metadatos: 'metadatos_documentos'.
     */
    public MongoCollection<Document> getMetadatosCollection() {
        return database.getCollection("metadatos_documentos");
    }

    public Document crearDocumentoClinico(String pacienteDoc, String contenido) {
        Document documento = new Document();
        documento.append("pacienteDoc", pacienteDoc);
        documento.append("contenido", contenido);
        guardarDocumento(documento);
        //crear metadata y almacenarla 

        return documento;

    }

    public void guardarDocumento(Document documento) {
        getCollection().insertOne(documento);
    }

  
    public Document buscarPorDocumentoPaciente(String documento) {
        return getCollection().find(new Document("pacienteDoc", documento)).first();
    }

    /**
     * Guarda el contenido asociado a un documento previamente registrado (referenciado por documentoId).
     * Este método crea un documento BSON mínimo con el identificador y el contenido y lo persiste en MongoDB.
     *
     * @param documentoId identificador del documento (creado por enviarMetadatos)
     * @param contenido contenido binario/texto representado como String
     * @return el Document insertado
     */
    public Document guardarContenido(String documentoId, String contenido) {
        Document documento = new Document();
        documento.append("documentoId", documentoId);
        documento.append("contenido", contenido);
        // Puede añadirse metadatos adicionales si están disponibles
        guardarDocumento(documento);
        return documento;
    }

    /**
     * Guarda el contenido asociado a un documento con información del paciente.
     * 
     * @param documentoId identificador del documento (UUID)
     * @param contenido contenido binario/texto representado como String
     * @param documentoIdPaciente CI o documento de identidad del paciente
     * @return el Document insertado
     */
    public Document guardarContenidoConPaciente(String documentoId, String contenido, String documentoIdPaciente) {
        Document documento = new Document();
        documento.append("documentoId", documentoId);
        documento.append("contenido", contenido);
        if (documentoIdPaciente != null && !documentoIdPaciente.isBlank()) {
            documento.append("documentoIdPaciente", documentoIdPaciente);
            documento.append("pacienteDoc", documentoIdPaciente); // También para compatibilidad
        }
        guardarDocumento(documento);
        return documento;
    }

    /**
     * Guarda el contenido asociado a un documento con información del paciente, incluyendo PDF binario.
     * 
     * @param documentoId identificador del documento (UUID)
     * @param contenido contenido de texto (String)
     * @param pdfBytes contenido PDF como array de bytes (opcional, puede ser null)
     * @param archivoAdjunto contenido de archivo adjunto como array de bytes (opcional, puede ser null)
     * @param nombreArchivo nombre del archivo adjunto (opcional)
     * @param tipoArchivo tipo MIME del archivo adjunto (opcional)
     * @param documentoIdPaciente CI o documento de identidad del paciente
     * @return el Document insertado
     */
    public Document guardarContenidoConPacienteYArchivos(String documentoId, String contenido,
            byte[] pdfBytes, byte[] archivoAdjunto, String nombreArchivo, String tipoArchivo,
            String documentoIdPaciente, Map<String, Object> metadata) {
        Document documento = new Document();
        documento.append("documentoId", documentoId);
        documento.append("contenido", contenido);
        
        // Guardar PDF como binario si está presente
        if (pdfBytes != null && pdfBytes.length > 0) {
            documento.append("pdf", new Binary(pdfBytes));
            documento.append("tienePdf", true);
        } else {
            documento.append("tienePdf", false);
        }
        
        // Guardar archivo adjunto si está presente
        if (archivoAdjunto != null && archivoAdjunto.length > 0) {
            documento.append("archivoAdjunto", new Binary(archivoAdjunto));
            documento.append("tieneArchivoAdjunto", true);
            if (nombreArchivo != null && !nombreArchivo.isBlank()) {
                documento.append("nombreArchivo", nombreArchivo);
            }
            if (tipoArchivo != null && !tipoArchivo.isBlank()) {
                documento.append("tipoArchivo", tipoArchivo);
            }
        } else {
            documento.append("tieneArchivoAdjunto", false);
        }
        
        if (documentoIdPaciente != null && !documentoIdPaciente.isBlank()) {
            documento.append("documentoIdPaciente", documentoIdPaciente);
            documento.append("pacienteDoc", documentoIdPaciente); // También para compatibilidad
        }

        if (metadata != null) {
            appendIfNotBlank(documento, "titulo", metadata.get("titulo"));
            appendIfNotBlank(documento, "descripcion", metadata.get("descripcion"));
            appendIfNotBlank(documento, "tipoDocumento", metadata.get("tipoDocumento"));
            appendIfNotBlank(documento, "autor", metadata.get("autor"));
            appendIfNotBlank(documento, "especialidad", metadata.get("especialidad"));
            appendIfNotBlank(documento, "profesionalId", metadata.get("profesionalId"));

            Object fechaCreacionObj = metadata.get("fechaCreacion");
            if (fechaCreacionObj instanceof Date fechaCreacion) {
                documento.append("fechaCreacion", fechaCreacion);
            }
        }

        if (!documento.containsKey("fechaCreacion")) {
            documento.append("fechaCreacion", new Date());
        }

        guardarDocumento(documento);
        return documento;
    }

    /**
     * Busca un documento por su _id de MongoDB (hex string).
     * @param idHex id en formato hex string (ObjectId)
     * @return el Document encontrado o un Document vacío si no existe / id inválido
     */
    public Document buscarPorId(String idHex) {
        try {
            ObjectId oid = new ObjectId(idHex);
            return getCollection().find(new Document("_id", oid)).first();
        } catch (IllegalArgumentException ex) {
            // ObjectId constructor lanza IllegalArgumentException si el string no es válido
            return new Document();
        }
    }

    /**
     * Devuelve la lista de ids (hex string) de documentos almacenados para un documentoId
     * (por ejemplo el documento de identidad de la persona).
     *
     * @param documentoId valor del campo "pacienteDoc" o campo equivalente que identifica a la persona
     * @return lista de ids en formato hex (puede estar vacía)
     */
    public List<String> buscarIdsPorDocumentoPaciente(String documentoId) {
        List<String> ids = new ArrayList<>();
        var cursor = getCollection().find(new Document("pacienteDoc", documentoId)).iterator();
        try {
            while (cursor.hasNext()) {
                Document d = cursor.next();
                ObjectId oid = d.getObjectId("_id");
                if (oid != null) ids.add(oid.toHexString());
            }
        } finally {
            cursor.close();
        }
        return ids;
    }

    private void appendIfNotBlank(Document documento, String key, Object value) {
        if (value instanceof String str) {
            if (!str.isBlank()) {
                documento.append(key, str);
            }
        } else if (value != null) {
            documento.append(key, value);
        }
    }

}
