package uy.edu.tse.hcen.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;

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

}
