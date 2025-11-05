package uy.edu.tse.hcen.client;

import jakarta.ejb.Stateless;
import org.jboss.logging.Logger;
import uy.edu.tse.hcen.model.UsuarioSalud;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Cliente HTTP para comunicarse con el backend de HCEN y registrar
 * Usuarios de Salud en el INUS central.
 */
@Stateless
public class HcenUsuarioSaludClient {
    
    private static final Logger LOGGER = Logger.getLogger(HcenUsuarioSaludClient.class);
    
    // URL del backend de HCEN (comunicación interna Docker)
    private static final String HCEN_BASE_URL = "http://hcen-backend:8080";
    
    // Timeout para las peticiones HTTP
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    
    /**
     * Registra o actualiza un Usuario de Salud en el INUS del HCEN.
     * 
     * @param tenantId ID de la clínica que registra al paciente
     * @param usuario Datos del usuario de salud
     * @return Respuesta con el userId del HCEN
     */
    public HcenUserResponse registrarUsuarioEnHcen(Long tenantId, UsuarioSalud usuario) {
        
        LOGGER.info("Registrando usuario en HCEN - CI: " + usuario.getCi() + ", Clínica: " + tenantId);
        
        try {
            // Construir payload JSON
            String jsonPayload = buildJsonPayload(tenantId, usuario);
            
            // Crear cliente HTTP
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
            
            // Crear request
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HCEN_BASE_URL + "/api/usuarios-salud/crear-modificar"))
                .header("Content-Type", "application/json")
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
            
            // Enviar request
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            // Procesar respuesta
            if (response.statusCode() == 200) {
                LOGGER.info("Usuario registrado exitosamente en HCEN");
                return parseResponse(response.body());
            } else {
                LOGGER.error("Error al registrar usuario en HCEN. Status: " + response.statusCode() + 
                           ", Body: " + response.body());
                return new HcenUserResponse(null, "Error HTTP " + response.statusCode());
            }
            
        } catch (Exception e) {
            LOGGER.error("Excepción al llamar a HCEN: " + e.getMessage(), e);
            return new HcenUserResponse(null, "Error: " + e.getMessage());
        }
    }
    
    /**
     * Construye el payload JSON para enviar al HCEN.
     */
    private String buildJsonPayload(Long tenantId, UsuarioSalud usuario) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"tenantId\":").append(tenantId).append(",");
        json.append("\"ci\":\"").append(escapeJson(usuario.getCi())).append("\",");
        
        // Nombre (puede ser null)
        if (usuario.getNombre() != null) {
            json.append("\"nombre\":\"").append(escapeJson(usuario.getNombre())).append("\",");
        } else {
            json.append("\"nombre\":null,");
        }
        
        // Apellido (puede ser null)
        if (usuario.getApellido() != null) {
            json.append("\"apellido\":\"").append(escapeJson(usuario.getApellido())).append("\",");
        } else {
            json.append("\"apellido\":null,");
        }
        
        // Fecha nacimiento (puede ser null)
        if (usuario.getFechaNacimiento() != null) {
            json.append("\"fechaNacimiento\":\"").append(usuario.getFechaNacimiento().toString()).append("\",");
        } else {
            json.append("\"fechaNacimiento\":null,");
        }
        
        // Dirección (puede ser null)
        if (usuario.getDireccion() != null) {
            json.append("\"direccion\":\"").append(escapeJson(usuario.getDireccion())).append("\",");
        } else {
            json.append("\"direccion\":null,");
        }
        
        // Teléfono (puede ser null)
        if (usuario.getTelefono() != null) {
            json.append("\"telefono\":\"").append(escapeJson(usuario.getTelefono())).append("\",");
        } else {
            json.append("\"telefono\":null,");
        }
        
        // Email (puede ser null)
        if (usuario.getEmail() != null) {
            json.append("\"email\":\"").append(escapeJson(usuario.getEmail())).append("\",");
        } else {
            json.append("\"email\":null,");
        }
        
        // Departamento (puede ser null) - enviar como string
        if (usuario.getDepartamento() != null) {
            json.append("\"departamento\":\"").append(usuario.getDepartamento()).append("\",");
        } else {
            json.append("\"departamento\":null,");
        }
        
        // Localidad (puede ser null)
        if (usuario.getLocalidad() != null) {
            json.append("\"localidad\":\"").append(escapeJson(usuario.getLocalidad())).append("\"");
        } else {
            json.append("\"localidad\":null");
        }
        
        json.append("}");
        
        return json.toString();
    }
    
    /**
     * Escapa caracteres especiales en strings JSON.
     */
    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\"", "\\\"")
                 .replace("\\", "\\\\")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }
    
    /**
     * Parsea la respuesta JSON del HCEN.
     */
    private HcenUserResponse parseResponse(String json) {
        try {
            // Simple parsing manual (podrías usar Jackson/Gson si está disponible)
            Long userId = null;
            String mensaje = "";
            
            // Buscar userId: NUMBER
            String userIdPattern = "\"userId\":";
            int userIdStart = json.indexOf(userIdPattern);
            if (userIdStart != -1) {
                userIdStart += userIdPattern.length();
                int userIdEnd = json.indexOf(",", userIdStart);
                if (userIdEnd == -1) {
                    userIdEnd = json.indexOf("}", userIdStart);
                }
                String userIdStr = json.substring(userIdStart, userIdEnd).trim();
                if (!userIdStr.equals("null")) {
                    userId = Long.parseLong(userIdStr);
                }
            }
            
            // Buscar mensaje: "..."
            String mensajePattern = "\"mensaje\":\"";
            int mensajeStart = json.indexOf(mensajePattern);
            if (mensajeStart != -1) {
                mensajeStart += mensajePattern.length();
                int mensajeEnd = json.indexOf("\"", mensajeStart);
                mensaje = json.substring(mensajeStart, mensajeEnd);
            }
            
            return new HcenUserResponse(userId, mensaje);
            
        } catch (Exception e) {
            LOGGER.error("Error parseando respuesta JSON: " + e.getMessage());
            return new HcenUserResponse(null, "Error parseando respuesta");
        }
    }
    
    /**
     * DTO para la respuesta del HCEN.
     */
    public static class HcenUserResponse {
        private Long userId;
        private String mensaje;
        
        public HcenUserResponse(Long userId, String mensaje) {
            this.userId = userId;
            this.mensaje = mensaje;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public String getMensaje() {
            return mensaje;
        }
        
        @Override
        public String toString() {
            return "HcenUserResponse{userId=" + userId + ", mensaje='" + mensaje + "'}";
        }
    }
}

