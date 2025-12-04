package uy.edu.tse.hcen.client;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import uy.edu.tse.hcen.utils.HcenCentralUrlUtil;

import java.util.Map;

/**
 * Cliente HTTP para consultar el servicio de políticas de acceso del HCEN backend.
 * 
 * Este cliente permite verificar si un profesional tiene permiso para acceder
 * a los documentos de un paciente según las políticas configuradas.
 */
@RequestScoped
public class PoliticasAccesoClient {

    private static final Logger LOG = Logger.getLogger(PoliticasAccesoClient.class);
    
    private static final String PROP_POLITICAS_URL = "POLITICAS_SERVICE_URL";
    
    /**
     * Verifica si un profesional tiene permiso para acceder a los documentos de un paciente.
     * 
     * @param profesionalId ID del profesional (nickname)
     * @param pacienteCI CI del paciente
     * @param tipoDocumento Tipo de documento (opcional, puede ser null)
     * @param tenantId ID del tenant/clínica
     * @return true si tiene permiso, false en caso contrario
     */
    public boolean verificarPermiso(String profesionalId, String pacienteCI, String tipoDocumento, String tenantId) {
        if (profesionalId == null || profesionalId.isBlank() || 
            pacienteCI == null || pacienteCI.isBlank()) {
            LOG.warn("Verificación de permiso rechazada: profesionalId o pacienteCI vacíos");
            return false;
        }
        
        Client client = null;
        try {
            String politicasUrl = getPoliticasUrl();
            String url = politicasUrl + "/politicas/verificar" +
                    "?profesionalId=" + java.net.URLEncoder.encode(profesionalId, "UTF-8") +
                    "&pacienteCI=" + java.net.URLEncoder.encode(pacienteCI, "UTF-8");
            
            if (tipoDocumento != null && !tipoDocumento.isBlank()) {
                url += "&tipoDoc=" + java.net.URLEncoder.encode(tipoDocumento, "UTF-8");
            }
            
            if (tenantId != null && !tenantId.isBlank()) {
                url += "&tenantId=" + java.net.URLEncoder.encode(tenantId, "UTF-8");
            }
            
            LOG.info(String.format("Verificando permiso - URL: %s", url));
            
            client = ClientBuilder.newClient();
            Response response = client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .get();
            
            if (response.getStatus() == 200) {
                Map<String, Object> result = response.readEntity(Map.class);
                LOG.info(String.format("Respuesta de verificación: %s", result));
                
                Boolean tienePermiso = (Boolean) result.get("tienePermiso");
                if (tienePermiso != null && tienePermiso) {
                    LOG.info(String.format("Permiso concedido - Profesional: %s, Paciente: %s", 
                            profesionalId, pacienteCI));
                    client.close();
                    return true;
                } else {
                    LOG.warn(String.format("Permiso denegado - Profesional: %s, Paciente: %s", 
                            profesionalId, pacienteCI));
                    client.close();
                    return false;
                }
            } else {
                String errorBody = response.readEntity(String.class);
                LOG.warn(String.format("Error al verificar permiso - Status: %d, Response: %s", 
                        response.getStatus(), errorBody));
                client.close();
                // En caso de error, por defecto denegar acceso (fail-secure)
                return false;
            }
            
        } catch (Exception e) {
            LOG.error(String.format("Error al consultar servicio de políticas - Profesional: %s, Paciente: %s", 
                    profesionalId, pacienteCI), e);
            // En caso de error, por defecto denegar acceso (fail-secure)
            if (client != null) {
                client.close();
            }
            return false;
        }
    }
    
    /**
     * Obtiene la URL del servicio de políticas desde variables de entorno o construye desde HCEN base URL.
     */
    private String getPoliticasUrl() {
        String envUrl = System.getenv(PROP_POLITICAS_URL);
        if (envUrl != null && !envUrl.isBlank()) {
            LOG.info("Usando POLITICAS_SERVICE_URL desde variable de entorno: " + envUrl);
            return envUrl;
        }
        String sysPropUrl = System.getProperty(PROP_POLITICAS_URL);
        if (sysPropUrl != null && !sysPropUrl.isBlank()) {
            LOG.info("Usando POLITICAS_SERVICE_URL desde propiedad del sistema: " + sysPropUrl);
            return sysPropUrl;
        }
        // Construir desde la URL base del HCEN central (el servicio de políticas está en el mismo servidor)
        String baseUrl = HcenCentralUrlUtil.getBaseUrl();
        return baseUrl + "/hcen-politicas-service/api";
    }
}

