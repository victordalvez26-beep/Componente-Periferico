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
    private static final String UTF8_ENCODING = "UTF-8";
    
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
        
        try (Client client = ClientBuilder.newClient()) {
            String politicasUrl = getPoliticasUrl();
            String url = buildVerificationUrl(politicasUrl, profesionalId, pacienteCI, tipoDocumento, tenantId);
            
            LOG.info(String.format("Verificando permiso - URL: %s", url));
            
            Response response = client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .get();
            
            try {
                if (response.getStatus() == 200) {
                    return processSuccessfulResponse(response, profesionalId, pacienteCI);
                } else {
                    return processErrorResponse(response, profesionalId, pacienteCI);
                }
            } finally {
                response.close();
            }
            
        } catch (Exception e) {
            LOG.error(String.format("Error al consultar servicio de políticas - Profesional: %s, Paciente: %s", 
                    profesionalId, pacienteCI), e);
            // En caso de error, por defecto denegar acceso (fail-secure)
            return false;
        }
    }
    
    /**
     * Construye la URL de verificación de permisos con los parámetros codificados.
     */
    private String buildVerificationUrl(String politicasUrl, String profesionalId, String pacienteCI, 
                                       String tipoDocumento, String tenantId) throws java.io.UnsupportedEncodingException {
        String url = politicasUrl + "/politicas/verificar" +
                "?profesionalId=" + java.net.URLEncoder.encode(profesionalId, UTF8_ENCODING) +
                "&pacienteCI=" + java.net.URLEncoder.encode(pacienteCI, UTF8_ENCODING);
        
        if (tipoDocumento != null && !tipoDocumento.isBlank()) {
            url += "&tipoDoc=" + java.net.URLEncoder.encode(tipoDocumento, UTF8_ENCODING);
        }
        
        if (tenantId != null && !tenantId.isBlank()) {
            url += "&tenantId=" + java.net.URLEncoder.encode(tenantId, UTF8_ENCODING);
        }
        
        return url;
    }
    
    /**
     * Procesa una respuesta exitosa (200) de verificación de permisos.
     */
    @SuppressWarnings("unused")
    private boolean processSuccessfulResponse(Response response, String profesionalId, String pacienteCI) {
        @SuppressWarnings("unchecked")
        Map<String, Object> result = response.readEntity(Map.class);
        LOG.info(String.format("Respuesta de verificación: %s", result));
        
        Boolean tienePermiso = (Boolean) result.get("tienePermiso");
        if (Boolean.TRUE.equals(tienePermiso)) {
            LOG.info(String.format("✅ Permiso concedido - Profesional: %s, Paciente: %s", 
                    profesionalId, pacienteCI));
            return true;
        } else {
            LOG.warn(String.format("❌ Permiso denegado - Profesional: %s, Paciente: %s", 
                    profesionalId, pacienteCI));
            return false;
        }
    }
    
    /**
     * Procesa una respuesta de error de verificación de permisos.
     */
    private boolean processErrorResponse(Response response, @SuppressWarnings("unused") String profesionalId, 
                                       @SuppressWarnings("unused") String pacienteCI) {
        String errorBody = response.readEntity(String.class);
        LOG.warn(String.format("Error al verificar permiso - Status: %d, Response: %s", 
                response.getStatus(), errorBody));
        // En caso de error, por defecto denegar acceso (fail-secure)
        return false;
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

