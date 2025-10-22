package uy.edu.tse.hcen.service;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Stateless;
import java.time.Instant;

@Stateless
public class MongoDBServiceBean implements MongoDBService {

    private MongoClient client;
    private MongoDatabase database;

    private String uri;
    private String dbName;

    @PostConstruct
    public void init() {
        uri = System.getenv().getOrDefault("MONGO_URI", "mongodb://localhost:27018");
        dbName = System.getenv().getOrDefault("MONGO_DB", "hcen_documents_db");
        client = MongoClients.create(MongoClientSettings.builder().applyConnectionString(new com.mongodb.ConnectionString(uri)).build());
        database = client.getDatabase(dbName);
    }

    @PreDestroy
    public void cleanup() {
        if (client != null) client.close();
    }

    @Override
    public boolean health() {
        try {
            database.runCommand(new Document("ping", 1));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void insertDocument(String json) {
        MongoCollection<Document> col = database.getCollection("documents");
        Document d = Document.parse(json);
        if (!d.containsKey("createdAt")) d.append("createdAt", Instant.now().toString());
        col.insertOne(d);
    }

    @Override
    public String findByCodigo(String codigo) {
        MongoCollection<Document> col = database.getCollection("documents");
        Document q = col.find(new Document("metadataId", codigo)).first();
        if (q == null) return null;
        return q.toJson();
    }
}
