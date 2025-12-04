package uy.edu.tse.hcen.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio para interactuar con OpenAI a través de OpenRouter API.
 */
@ApplicationScoped
public class OpenAIService {

    private static final Logger LOG = Logger.getLogger(OpenAIService.class.getName());
    
    private static final String OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1";
    private static final String CHAT_COMPLETIONS_ENDPOINT = "/chat/completions";
    private static final String MODEL_NAME = "openai/gpt-oss-20b:free";
    
    // Token de OpenRouter (debe configurarse como variable de entorno en .env)
    private static final String ENV_OPENROUTER_API_KEY = "OPENROUTER_API_KEY";
    // NO hardcodear tokens aquí - deben venir de variables de entorno
    private static final String DEFAULT_TOKEN = null; // Se debe configurar OPENROUTER_API_KEY en .env

    /**
     * Genera un resumen de la historia clínica usando OpenAI o3.
     * 
     * @param historiaClinica Texto completo de la historia clínica a resumir
     * @return Resumen generado por la IA
     */
    public String generarResumenHistoriaClinica(String historiaClinica) {
        if (historiaClinica == null || historiaClinica.isBlank()) {
            throw new IllegalArgumentException("La historia clínica no puede estar vacía");
        }

        String token = System.getProperty(ENV_OPENROUTER_API_KEY,
                System.getenv().getOrDefault(ENV_OPENROUTER_API_KEY, null));
        
        if (token == null || token.isBlank()) {
            String errorMsg = "OPENROUTER_API_KEY no está configurado. Por favor, configure la variable de entorno OPENROUTER_API_KEY en el archivo .env";
            LOG.log(Level.SEVERE, errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        LOG.log(Level.INFO, "Usando OPENROUTER_API_KEY (primeros 10 caracteres): {0}", 
                token != null && token.length() > 10 ? token.substring(0, 10) + "..." : "null");

        String url = OPENROUTER_BASE_URL + CHAT_COMPLETIONS_ENDPOINT;

        // Construir el prompt para el resumen
        String prompt = "Genera un resumen médico profesional y estructurado de la siguiente historia clínica. " +
                "Incluye: diagnóstico principal, tratamientos realizados, medicamentos prescritos, " +
                "evolución del paciente, y recomendaciones importantes. " +
                "Mantén un lenguaje médico apropiado y sé conciso pero completo.\n\n" +
                "Historia clínica:\n" + historiaClinica;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", MODEL_NAME);
        
        List<Map<String, String>> messages = List.of(
            Map.of("role", "user", "content", prompt)
        );
        requestBody.put("messages", messages);

        try (Client client = ClientBuilder.newClient();
             Response response = client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .post(Entity.json(requestBody))) {

            int status = response.getStatus();
            if (status == 200) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = response.readEntity(Map.class);
                
                @SuppressWarnings({"unchecked", "rawtypes"})
                List<Map<String, Object>> choices = (List) result.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                    if (message != null) {
                        Object content = message.get("content");
                        return content != null ? content.toString() : "No se pudo generar el resumen";
                    }
                }
                throw new RuntimeException("Respuesta de OpenAI sin contenido válido");
            } else {
                String errorMsg = "Unknown error";
                if (response.hasEntity()) {
                    try {
                        String entity = response.readEntity(String.class);
                        if (entity != null && !entity.trim().isEmpty()) {
                            errorMsg = entity;
                        }
                    } catch (Exception e) {
                        // Usar mensaje por defecto
                    }
                }
                LOG.log(Level.WARNING, "Error generando resumen: HTTP {0} - {1}. URL: {2}, Token configurado: {3}", 
                        new Object[]{status, errorMsg, url, token != null && token.length() > 10 ? token.substring(0, 10) + "..." : "null"});
                throw new RuntimeException("Error al generar resumen: HTTP " + status + " - " + errorMsg);
            }
        } catch (ProcessingException ex) {
            LOG.log(Level.SEVERE, "Error de conexión con OpenAI: {0}", ex.getMessage());
            throw new RuntimeException("Error de conexión con OpenAI: " + ex.getMessage(), ex);
        }
    }

}

