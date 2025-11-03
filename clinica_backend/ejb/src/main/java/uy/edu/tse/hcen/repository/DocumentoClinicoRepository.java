package uy.edu.tse.hcen.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;

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

}
