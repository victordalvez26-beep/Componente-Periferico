package uy.edu.tse.hcen.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.ZoneId;
import java.time.ZonedDateTime;
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
     * Obtiene la colección de documentos clínicos (método público para uso en servicios).
     */
    public MongoCollection<Document> getCollectionPublic() {
        return database.getCollection("documentos_clinicos");
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
     * Guarda un documento clínico completo con contenido, PDF y archivo adjunto.
     * 
     * @param documentoId ID único del documento (UUID)
     * @param contenido Contenido de texto del documento
     * @param pdfBytes Bytes del PDF generado desde el contenido (opcional, puede ser null)
     * @param archivoAdjuntoBytes Bytes del archivo adjunto (opcional, puede ser null)
     * @param nombreArchivoAdjunto Nombre del archivo adjunto (opcional)
     * @param tipoArchivoAdjunto Tipo MIME del archivo adjunto (opcional)
     * @param ciPaciente CI del paciente
     * @param tenantId ID de la clínica
     * @param tipoDocumento Tipo de documento clínico
     * @param descripcion Descripción del documento
     * @param profesionalId ID del profesional
     * @param titulo Título del documento (opcional)
     * @param autor Autor del documento (opcional)
     * @return ID de MongoDB (ObjectId en hex string) del documento guardado
     */
    public String guardarDocumentoCompleto(
            String documentoId,
            String contenido,
            byte[] pdfBytes,
            byte[] archivoAdjuntoBytes,
            String nombreArchivoAdjunto,
            String tipoArchivoAdjunto,
            String ciPaciente,
            Long tenantId,
            String tipoDocumento,
            String descripcion,
            String profesionalId,
            String titulo,
            String autor) {
        
        Document documento = new Document();
        documento.append("documentoId", documentoId);
        documento.append("contenido", contenido);
        if (pdfBytes != null && pdfBytes.length > 0) {
            documento.append("pdfBytes", new Binary(pdfBytes));
        }
        documento.append("ciPaciente", ciPaciente);
        documento.append("tenantId", tenantId);
        // Usar zona horaria de Uruguay para guardar la fecha
        ZoneId uruguayZone = ZoneId.of("America/Montevideo");
        ZonedDateTime fechaCreacionUruguay = ZonedDateTime.now(uruguayZone);
        documento.append("fechaCreacion", java.util.Date.from(fechaCreacionUruguay.toInstant()));
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
        if (titulo != null) {
            documento.append("titulo", titulo);
        }
        if (autor != null) {
            documento.append("autor", autor);
        }
        
        // Archivo adjunto (opcional)
        if (archivoAdjuntoBytes != null && archivoAdjuntoBytes.length > 0) {
            documento.append("archivoAdjunto", new Binary(archivoAdjuntoBytes));
            if (nombreArchivoAdjunto != null) {
                documento.append("nombreArchivoAdjunto", nombreArchivoAdjunto);
            }
            if (tipoArchivoAdjunto != null) {
                documento.append("tipoArchivoAdjunto", tipoArchivoAdjunto);
            }
        }
        
        getCollection().insertOne(documento);
        
        ObjectId objectId = documento.getObjectId("_id");
        return objectId != null ? objectId.toHexString() : null;
    }

    /**
     * Busca un documento por su ID de MongoDB y valida que pertenezca al tenant especificado.
     * 
     * @param mongoId ID de MongoDB (ObjectId en hex string)
     * @param tenantId ID de la clínica (para validación de seguridad multi-tenant)
     * @return Document con el documento o null si no existe o no pertenece al tenant
     */
    public Document buscarPorId(String mongoId, Long tenantId) {
        try {
            ObjectId objectId = new ObjectId(mongoId);
            Document query = new Document("_id", objectId);
            if (tenantId != null) {
                query.append("tenantId", tenantId); // Validación multi-tenant
            }
            
            return getCollection().find(query).first();
        } catch (IllegalArgumentException ex) {
            // ID inválido
            return null;
        }
    }

    /**
     * Busca documentos por CI del paciente y tenant.
     * 
     * @param ciPaciente CI del paciente
     * @param tenantId ID de la clínica
     * @return Lista de documentos encontrados
     */
    public List<Document> buscarPorCiPaciente(String ciPaciente, Long tenantId) {
        Document query = new Document("ciPaciente", ciPaciente);
        if (tenantId != null) {
            query.append("tenantId", tenantId);
        }
        return getCollection().find(query).into(new ArrayList<>());
    }

    /**
     * Busca documentos por documentoId (UUID) y tenant.
     * 
     * @param documentoId ID único del documento
     * @param tenantId ID de la clínica
     * @return Document o null si no existe
     */
    public Document buscarPorDocumentoId(String documentoId, Long tenantId) {
        Document query = new Document("documentoId", documentoId);
        if (tenantId != null) {
            query.append("tenantId", tenantId);
        }
        return getCollection().find(query).first();
    }
}
