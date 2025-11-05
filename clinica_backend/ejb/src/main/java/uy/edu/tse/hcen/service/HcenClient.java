package uy.edu.tse.hcen.service;

import uy.edu.tse.hcen.dto.DTMetadatos;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import uy.edu.tse.hcen.exceptions.HcenUnavailableException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
// response type is referenced fully-qualified in code to avoid unused import warnings

/**
 * Simple client used to register metadatos in HCEN Central.
 */
@ApplicationScoped
public class HcenClient {

    // The HCEN central endpoint can be overridden via the HCEN_CENTRAL_URL environment variable
    // URL correcta: /api (ApplicationPath) + /central/api/rndc/metadatos (Path del recurso)
    private static final String DEFAULT_CENTRAL_URL = "http://localhost:8080/api/central/api/rndc/metadatos";

    public void registrarMetadatos(DTMetadatos dto) throws HcenUnavailableException {
        String centralUrl = System.getProperty("HCEN_CENTRAL_URL",
                System.getenv().getOrDefault("HCEN_CENTRAL_URL", DEFAULT_CENTRAL_URL));

        Client client = null;
        jakarta.ws.rs.core.Response response = null;
        try {
            client = ClientBuilder.newClient();
            response = client.target(centralUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(dto));

            int status = response.getStatus();
            if (status != 200 && status != 201 && status != 202) {
                throw new HcenUnavailableException("Error al registrar metadatos: HTTP " + status);
            }

        } catch (ProcessingException ex) {
            throw new HcenUnavailableException("HCEN no disponible", ex);
        } finally {
            if (response != null) {
                response.close();
            }
            if (client != null) {
                client.close();
            }
        }
    }

    /**
     * Envía el payload completo (incluyendo datosPatronimicos) al central.
     */
    public void registrarMetadatosCompleto(Map<String, Object> payload) throws HcenUnavailableException {
        String centralUrl = System.getProperty("HCEN_CENTRAL_URL",
                System.getenv().getOrDefault("HCEN_CENTRAL_URL", DEFAULT_CENTRAL_URL));

        Client client = null;
        jakarta.ws.rs.core.Response response = null;
        try {
            client = ClientBuilder.newClient();
            response = client.target(centralUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(payload));

            int status = response.getStatus();
            if (status != 200 && status != 201 && status != 202) {
                throw new HcenUnavailableException("Error al registrar metadatos: HTTP " + status);
            }

        } catch (ProcessingException ex) {
            throw new HcenUnavailableException("HCEN no disponible", ex);
        } finally {
            if (response != null) {
                response.close();
            }
            if (client != null) {
                client.close();
            }
        }
    }

    /**
     * Consulta metadatos de un paciente desde HCEN central.
     * 
     * @param documentoIdPaciente CI o documento de identidad del paciente
     * @return Lista de metadatos (Map<String, Object>)
     * @throws HcenUnavailableException si HCEN no está disponible
     */
    @SuppressWarnings("unchecked")
    public java.util.List<Map<String, Object>> consultarMetadatosPaciente(String documentoIdPaciente) 
            throws HcenUnavailableException {
        // URL base de HCEN central para endpoints de paciente
        // El endpoint es /api/paciente/{id}/metadatos
        String baseUrl = System.getProperty("HCEN_CENTRAL_BASE_URL",
                System.getenv().getOrDefault("HCEN_CENTRAL_BASE_URL", "http://localhost:8080/api"));
        
        // Construir URL del endpoint de paciente
        String pacienteUrl = baseUrl + "/paciente/" + documentoIdPaciente + "/metadatos";

        Client client = null;
        jakarta.ws.rs.core.Response response = null;
        try {
            client = ClientBuilder.newClient();
            response = client.target(pacienteUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .get();

            int status = response.getStatus();
            if (status == 200) {
                return response.readEntity(java.util.List.class);
            } else if (status == 404) {
                return new java.util.ArrayList<>(); // Lista vacía si no hay documentos
            } else {
                throw new HcenUnavailableException("Error al consultar metadatos: HTTP " + status);
            }

        } catch (ProcessingException ex) {
            throw new HcenUnavailableException("HCEN no disponible", ex);
        } finally {
            if (response != null) {
                response.close();
            }
            if (client != null) {
                client.close();
            }
        }
    }
}
