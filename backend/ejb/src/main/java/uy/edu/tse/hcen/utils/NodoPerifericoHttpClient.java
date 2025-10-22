package uy.edu.tse.hcen.utils;

import uy.edu.tse.hcen.model.NodoPeriferico;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
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
                logger.log(Level.INFO, "NodoPeriferico init sent to {0}, status={1}", new Object[]{url, status});
                return true;
            } else {
                logger.log(Level.WARNING, "NodoPeriferico responded with non-2xx: {0} body={1}", new Object[]{status, resp.body()});
                return false;
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            logger.log(Level.SEVERE, "Thread interrupted while sending init to nodo periférico", ie);
            return false;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error sending init to nodo periférico", e);
            return false;
        }
    }

    /**
     * Elimina una configuración del nodo periférico. Construye el endpoint
     * `${nodo.getNodoPerifericoUrlBase()}/config/delete` y hace POST con JSON mínimo.
     * Retorna true si el código HTTP es 2xx.
     */
    public boolean enviarConfiguracionDelete(NodoPeriferico nodo) {
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
                logger.log(Level.INFO, "NodoPeriferico delete sent to {0}, status={1}", new Object[]{url, status});
                return true;
            } else {
                logger.log(Level.WARNING, "NodoPeriferico responded with non-2xx: {0} body={1}", new Object[]{status, resp.body()});
                return false;
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            logger.log(Level.SEVERE, "Thread interrupted while sending delete to nodo periférico", ie);
            return false;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error sending delete to nodo periférico", e);
            return false;
        }
    }

    /**
     * Compatibilidad: método llamado desde otros módulos. Delegar a
     * enviarConfiguracionDelete para no romper callers que esperan
     * enviarBaja(NodoPeriferico).
     */
    public boolean enviarBaja(NodoPeriferico nodo) {
        return enviarConfiguracionDelete(nodo);
    }

    /**
     * Helper to perform a GET returning a String body.
     * Throws Exception on failures to let caller decide recovery.
     */
    public HttpResponse<String> getString(String url, Map<String,String> headers, int timeoutSeconds) throws java.io.IOException, InterruptedException {
        HttpRequest.Builder rb = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(Math.max(1, timeoutSeconds))).GET();
        if (headers != null) {
            headers.forEach(rb::header);
        }
        HttpRequest req = rb.build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Helper to perform a GET returning an InputStream body.
     */
    public HttpResponse<java.io.InputStream> getInputStream(String url, Map<String,String> headers, int timeoutSeconds) throws java.io.IOException, InterruptedException {
        HttpRequest.Builder rb = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(Math.max(1, timeoutSeconds))).GET();
        if (headers != null) {
            headers.forEach(rb::header);
        }
        HttpRequest req = rb.build();
        return client.send(req, HttpResponse.BodyHandlers.ofInputStream());
    }

    /**
     * Helper to perform a POST with String body and return a String response.
     */
    public HttpResponse<String> postString(String url, Map<String,String> headers, String body, int timeoutSeconds) throws java.io.IOException, InterruptedException {
        HttpRequest.Builder rb = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(Math.max(1, timeoutSeconds))).POST(HttpRequest.BodyPublishers.ofString(body));
        if (headers != null) headers.forEach(rb::header);
        HttpRequest req = rb.build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private String escapeJson(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
