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

/**ok, 
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
    
    // Constantes para literales duplicados
    private static final String KEY_MODEL = "model";
    private static final String KEY_MESSAGES = "messages";
    private static final String KEY_CHOICES = "choices";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_CONTENT = "content";
    private static final String KEY_ROLE = "role";
    private static final String ROLE_USER = "user";
    private static final String ERROR_HISTORIA_VACIA = "La historia clínica no puede estar vacía";
    private static final String ERROR_API_KEY_NO_CONFIGURADA = "OPENROUTER_API_KEY no está configurado. Por favor, configure la variable de entorno OPENROUTER_API_KEY en el archivo .env";
    private static final String ERROR_RESPUESTA_SIN_CONTENIDO = "Respuesta de OpenAI sin contenido válido";
    private static final String ERROR_DESCONOCIDO = "Unknown error";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int HTTP_OK = 200;
    private static final int TOKEN_PREVIEW_LENGTH = 10;

    /**
     * Genera un resumen de la historia clínica usando OpenAI o3.
     * 
     * @param historiaClinica Texto completo de la historia clínica a resumir
     * @return Resumen generado por la IA
     */
    public String generarResumenHistoriaClinica(String historiaClinica) {
        validarHistoriaClinica(historiaClinica);
        String token = obtenerTokenOpenRouter();
        String url = OPENROUTER_BASE_URL + CHAT_COMPLETIONS_ENDPOINT;
        String prompt = construirPrompt(historiaClinica);
        Map<String, Object> requestBody = construirRequestBody(prompt);

        return realizarPeticionOpenAI(url, token, requestBody);
    }
    
    /**
     * Valida que la historia clínica no esté vacía.
     */
    private void validarHistoriaClinica(String historiaClinica) {
        if (historiaClinica == null || historiaClinica.isBlank()) {
            throw new IllegalArgumentException(ERROR_HISTORIA_VACIA);
        }
    }
    
    /**
     * Obtiene el token de OpenRouter desde variables de entorno o propiedades del sistema.
     */
    private String obtenerTokenOpenRouter() {
        String token = System.getProperty(ENV_OPENROUTER_API_KEY,
                System.getenv().getOrDefault(ENV_OPENROUTER_API_KEY, null));
        
        if (token == null || token.isBlank()) {
            LOG.log(Level.SEVERE, ERROR_API_KEY_NO_CONFIGURADA);
            throw new IllegalStateException(ERROR_API_KEY_NO_CONFIGURADA);
        }
        
        logTokenInfo(token);
        return token;
    }
    
    /**
     * Registra información del token (sin exponer el token completo).
     */
    private void logTokenInfo(String token) {
        if (token.length() > TOKEN_PREVIEW_LENGTH) {
            LOG.log(Level.INFO, "Usando OPENROUTER_API_KEY (primeros {0} caracteres): {1}", 
                    new Object[]{TOKEN_PREVIEW_LENGTH, token.substring(0, TOKEN_PREVIEW_LENGTH) + "..."});
        } else {
            LOG.log(Level.INFO, "OPENROUTER_API_KEY no configurado o muy corto");
        }
    }
    
    /**
     * Construye el prompt para el resumen médico.
     */
    private String construirPrompt(String historiaClinica) {
        return "Genera un resumen médico profesional y estructurado de la siguiente historia clínica. " +
                "Incluye: diagnóstico principal, tratamientos realizados, medicamentos prescritos, " +
                "evolución del paciente, y recomendaciones importantes. " +
                "Mantén un lenguaje médico apropiado y sé conciso pero completo.\n\n" +
                "Historia clínica:\n" + historiaClinica;
    }
    
    /**
     * Construye el cuerpo de la petición HTTP para OpenAI.
     */
    private Map<String, Object> construirRequestBody(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put(KEY_MODEL, MODEL_NAME);
        
        List<Map<String, String>> messages = List.of(
            Map.of(KEY_ROLE, ROLE_USER, KEY_CONTENT, prompt)
        );
        requestBody.put(KEY_MESSAGES, messages);
        return requestBody;
    }
    
    /**
     * Realiza la petición HTTP a OpenAI y procesa la respuesta.
     */
    private String realizarPeticionOpenAI(String url, String token, Map<String, Object> requestBody) {
        try (Client client = ClientBuilder.newClient();
             Response response = client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .header(HEADER_AUTHORIZATION, BEARER_PREFIX + token)
                    .post(Entity.json(requestBody))) {

            int status = response.getStatus();
            if (status == HTTP_OK) {
                return procesarRespuestaExitosa(response);
            } else {
                return procesarRespuestaError(response, status, url, token);
            }
        } catch (ProcessingException ex) {
            String errorMsg = "Error de conexión con OpenAI: " + ex.getMessage();
            throw new IllegalStateException(errorMsg, ex);
        }
    }
    
    /**
     * Procesa una respuesta exitosa de OpenAI.
     */
    @SuppressWarnings("unchecked")
    private String procesarRespuestaExitosa(Response response) {
        Map<String, Object> result = response.readEntity(Map.class);
        
        @SuppressWarnings("rawtypes")
        List<Map<String, Object>> choices = (List) result.get(KEY_CHOICES);
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> firstChoice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) firstChoice.get(KEY_MESSAGE);
            if (message != null) {
                Object content = message.get(KEY_CONTENT);
                if (content != null) {
                    return content.toString();
                }
            }
        }
        throw new IllegalStateException(ERROR_RESPUESTA_SIN_CONTENIDO);
    }
    
    /**
     * Procesa una respuesta de error de OpenAI.
     */
    private String procesarRespuestaError(Response response, int status, String url, String token) {
        String errorMsg = extractErrorMessage(response);
        String tokenPreview = token.length() > TOKEN_PREVIEW_LENGTH 
                ? token.substring(0, TOKEN_PREVIEW_LENGTH) + "..." 
                : "null";
        LOG.log(Level.WARNING, "Error generando resumen: HTTP {0} - {1}. URL: {2}, Token configurado: {3}", 
                new Object[]{status, errorMsg, url, tokenPreview});
        throw new IllegalStateException("Error al generar resumen: HTTP " + status + " - " + errorMsg);
    }
    
    /**
     * Extrae el mensaje de error de la respuesta HTTP.
     * 
     * @param response Respuesta HTTP
     * @return Mensaje de error extraído o "Unknown error" por defecto
     */
    private String extractErrorMessage(Response response) {
        String errorMsg = ERROR_DESCONOCIDO;
        if (response.hasEntity()) {
            try {
                String entity = response.readEntity(String.class);
                if (entity != null && !entity.trim().isEmpty()) {
                    errorMsg = entity;
                }
            } catch (ProcessingException e) {
                String logMsg = "No se pudo leer el cuerpo de la respuesta de error: " + e.getMessage();
                LOG.log(Level.FINE, logMsg, e);
            }
        }
        return errorMsg;
    }

}

