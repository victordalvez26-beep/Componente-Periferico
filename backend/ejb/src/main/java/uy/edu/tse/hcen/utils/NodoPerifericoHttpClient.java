package uy.edu.tse.hcen.utils;

import uy.edu.tse.hcen.model.NodoPeriferico;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NodoPerifericoHttpClient {

    private static final Logger logger = Logger.getLogger(NodoPerifericoHttpClient.class.getName());
    private final HttpClient client;

    public NodoPerifericoHttpClient() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Envía una configuración inicial al nodo periférico. Construye el endpoint
     * `${nodo.getNodoPerifericoUrlBase()}/config/init` y hace POST con JSON mínimo.
     * Retorna true si el código HTTP es 2xx.
     */
    public boolean enviarConfiguracionInicial(NodoPeriferico nodo) {
        if (nodo == null || nodo.getNodoPerifericoUrlBase() == null) return false;
        try {
            String url = nodo.getNodoPerifericoUrlBase();
            if (!url.endsWith("/")) url += "/";
            url += "config/init";

            // id is numeric Long (DB-generated) so do not quote the id field
            String payload = "{\"id\": " + nodo.getId() + ", \"nombre\": \"" + escapeJson(nodo.getNombre()) + "\"}";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            int status = resp.statusCode();
            if (status >= 200 && status < 300) {
                logger.info("NodoPeriferico init sent to " + url + ", status=" + status);
                return true;
            } else {
                logger.log(Level.WARNING, "NodoPeriferico responded with non-2xx: " + status + " body=" + resp.body());
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error sending init to nodo periférico", e);
            return false;
        }
    }

    /**
     * Envia un request para indicar baja/config delete al nodo periférico.
     */
    public boolean enviarBaja(NodoPeriferico nodo) {
        if (nodo == null || nodo.getNodoPerifericoUrlBase() == null) return false;
        try {
            String url = nodo.getNodoPerifericoUrlBase();
            if (!url.endsWith("/")) url += "/";
            url += "config/delete";

            String payload = "{\"id\": " + nodo.getId() + "}";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            int status = resp.statusCode();
            if (status >= 200 && status < 300) {
                logger.info("NodoPeriferico delete sent to " + url + ", status=" + status);
                return true;
            } else {
                logger.log(Level.WARNING, "NodoPeriferico responded with non-2xx: " + status + " body=" + resp.body());
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error sending delete to nodo periférico", e);
            return false;
        }
    }

    private String escapeJson(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
