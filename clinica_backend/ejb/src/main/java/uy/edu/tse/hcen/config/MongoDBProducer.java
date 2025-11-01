package uy.edu.tse.hcen.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import uy.edu.tse.hcen.business.exceptions.MongoDBConfigurationException;

import org.bson.Document;

import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class MongoDBProducer {
    private static final Logger LOGGER = Logger.getLogger(MongoDBProducer.class.getName());
    private static final String ENV_URI = System.getenv("MONGODB_URI");
    private static final String DB_NAME = System.getenv().getOrDefault("MONGODB_DB", "hcen_db");

    @Produces
    public MongoClient createMongoClient() {
        if (ENV_URI == null || ENV_URI.isBlank()) {
            throw new MongoDBConfigurationException("MongoDBProducer: La variable de entorno MONGODB_URI es obligatoria. Por favor configúrela con los detalles de conexión, incluyendo credenciales.");
        }

        // Build a list of candidate URIs: configured, and replace host 'mongodb' with localhost if needed
        List<String> candidates = new ArrayList<>();
        candidates.add(ENV_URI);
        if (ENV_URI.contains("mongodb://mongodb") || ENV_URI.contains("@mongodb:")) {
            candidates.add(ENV_URI.replace("@mongodb:", "@localhost:"));
        }

        for (String uri : candidates) {
            try {
                MongoClientSettings settings = MongoClientSettings.builder()
                        .applyConnectionString(new ConnectionString(uri))
                        .applyToSocketSettings(builder -> builder.connectTimeout(5, TimeUnit.SECONDS))
                        .build();

                MongoClient client = MongoClients.create(settings);

                if (verifyClientConnection(client, uri)) {
                    LOGGER.log(Level.INFO, "MongoDBProducer: conectado a MongoDB usando URI={0}", uri);
                    return client;
                }
                // otherwise continue to the next candidate
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "MongoDBProducer: no se pudo crear MongoClient para URI={0} -> {1}", new Object[]{uri, e.getMessage()});
            }
        }

        throw new MongoDBConfigurationException(
                "MongoDBProducer: no se pudo conectar a ninguna de las URIs candidatas de MongoDB. Intentadas: " + candidates);
    }

    @Produces
    public MongoDatabase createMongoDatabase(MongoClient client) {
        return client.getDatabase(DB_NAME);
    }

    public void close(@Disposes MongoClient client) {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "MongoDBProducer: error cerrando MongoClient -> {0}", e.getMessage());
            }
        }
    }

    /**
     * Verifies the client by sending a ping command. If ping fails the client is closed and false is returned.
     */
    private boolean verifyClientConnection(MongoClient client, String uri) {
        try {
            client.getDatabase("admin").runCommand(new Document("ping", 1));
            return true;
        } catch (Exception pingEx) {
            try {
                client.close();
            } catch (Exception closeEx) {
                LOGGER.log(Level.FINE, "MongoDBProducer: error cerrando cliente para URI={0} -> {1}", new Object[]{uri, closeEx.getMessage()});
            }
            LOGGER.log(Level.WARNING, "MongoDBProducer: ping falló para URI={0} -> {1}", new Object[]{uri, pingEx.getMessage()});
            return false;
        }
    }
}



