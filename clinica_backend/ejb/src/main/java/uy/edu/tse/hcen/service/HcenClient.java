package uy.edu.tse.hcen.service;

import uy.edu.tse.hcen.dto.DTMetadatos;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import uy.edu.tse.hcen.exceptions.HcenUnavailableException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Simple client used to register metadatos in HCEN Central.
 */
@ApplicationScoped
public class HcenClient {

    private static final String HCEN_URL = "https://hcen.salud.uy/api/rcn";

    public void registrarMetadatos(DTMetadatos dto) throws HcenUnavailableException {
        Client client = null;
        Response response = null;
        try {
            client = ClientBuilder.newClient();
            response = client.target(HCEN_URL + "/registrar")
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(dto));

            if (response.getStatus() != 200) {
                throw new HcenUnavailableException("Error al registrar metadatos: HTTP " + response.getStatus());
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
