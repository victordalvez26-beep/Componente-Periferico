package uy.edu.tse.hcen.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Repositorio para almacenar documentos PDF en MongoDB.
 * 
 * Los PDFs se almacenan en la colección 'documentos_pdf' con la siguiente estructura:
 * {
 *   "_id": ObjectId,
 *   "documentoId": UUID (identificador único del documento),
 *   "pdfBytes": Binary (contenido del PDF),
 *   "ciPaciente": String (CI del paciente),
 *   "tenantId": Long (ID de la clínica),
 *   "fechaCreacion": Date,
 *   "contentType": "application/pdf"
 * }
 */
@ApplicationScoped
public class DocumentoPdfRepository {

    @Inject
    private MongoDatabase database;

    public DocumentoPdfRepository() {
    }

    /**
     * Obtiene la colección de documentos PDF.
     */
    private MongoCollection<Document> getCollection() {
        return database.getCollection("documentos_pdf");
    }

    /**
     * Guarda un PDF en MongoDB.
     * 
     * @param documentoId UUID único del documento
     * @param pdfBytes Bytes del archivo PDF
     * @param ciPaciente CI del paciente
     * @param tenantId ID de la clínica
     * @param tipoDocumento Tipo de documento (EVALUACION, INFORME, etc.)
     * @param descripcion Descripción del documento
     * @param profesionalId ID del profesional que subió el documento
     * @return ID de MongoDB (ObjectId en hex string) del documento guardado
     */
    public String guardarPdf(String documentoId, byte[] pdfBytes, String ciPaciente, Long tenantId,
                               String tipoDocumento, String descripcion, String profesionalId) {
        Document documento = new Document();
        documento.append("documentoId", documentoId);
        documento.append("pdfBytes", new Binary(pdfBytes));
        documento.append("ciPaciente", ciPaciente);
        documento.append("tenantId", tenantId);
        documento.append("fechaCreacion", new java.util.Date());
        documento.append("contentType", "application/pdf");
        
        // Metadata adicional
        if (tipoDocumento != null) {
            documento.append("tipoDocumento", tipoDocumento);
        }
        if (descripcion != null) {
            documento.append("descripcion", descripcion);
        }
        if (profesionalId != null) {
            documento.append("profesionalId", profesionalId);
        }

        getCollection().insertOne(documento);

        ObjectId objectId = documento.getObjectId("_id");
        return objectId != null ? objectId.toHexString() : null;
    }

    /**
     * Busca un PDF por su ID de MongoDB y valida que pertenezca al tenant especificado.
     * 
     * @param mongoId ID de MongoDB (ObjectId en hex string)
     * @param tenantId ID de la clínica (para validación de seguridad multi-tenant)
     * @return Document con el PDF o null si no existe o no pertenece al tenant
     */
    public Document buscarPorId(String mongoId, Long tenantId) {
        try {
            ObjectId objectId = new ObjectId(mongoId);
            Document query = new Document("_id", objectId);
            query.append("tenantId", tenantId); // Validación multi-tenant: solo documentos de esta clínica
            
            return getCollection().find(query).first();
        } catch (IllegalArgumentException ex) {
            // ID inválido
            return null;
        }
    }

    /**
     * Busca PDFs por CI del paciente y tenant.
     * 
     * @param ciPaciente CI del paciente
     * @param tenantId ID de la clínica
     * @return Lista de documentos encontrados
     */
    public java.util.List<Document> buscarPorPaciente(String ciPaciente, Long tenantId) {
        Document query = new Document();
        query.append("ciPaciente", ciPaciente);
        query.append("tenantId", tenantId);

        java.util.List<Document> resultados = new java.util.ArrayList<>();
        var cursor = getCollection().find(query).iterator();
        try {
            while (cursor.hasNext()) {
                resultados.add(cursor.next());
            }
        } finally {
            cursor.close();
        }
        return resultados;
    }

    /**
     * Obtiene la colección de documentos PDF de forma pública.
     * Usado por StatsService para contar documentos.
     */
    public MongoCollection<Document> getCollectionPublic() {
        return getCollection();
    }
    
    /**
     * Obtiene la base de datos MongoDB de forma pública.
     * Usado para acceder a otras colecciones.
     */
    public MongoDatabase getDatabase() {
        return database;
    }
}




