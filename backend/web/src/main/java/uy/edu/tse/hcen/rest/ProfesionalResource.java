package uy.edu.tse.hcen.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import uy.edu.tse.hcen.utils.NodoPerifericoHttpClient;

@Path("/profesional")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProfesionalResource {

    private static final Logger LOGGER = Logger.getLogger(ProfesionalResource.class.getName());

    @Inject
    private NodoPerifericoHttpClient httpClient;
    private static final com.fasterxml.jackson.databind.ObjectMapper JSON = new com.fasterxml.jackson.databind.ObjectMapper();

    private static final String NOMBRE = "nombre";
    private static final String ERROR = "error";

    // In-memory demo patients per clinicId. In real app this must come from DB.
    private static final Map<String, List<Map<String,String>>> DEMO_PATIENTS = new HashMap<>();
    static {
        DEMO_PATIENTS.put("clinic-1", Arrays.asList(
                Map.of(NOMBRE,"Juan Perez", "ci","1.234.567-8"),
                Map.of(NOMBRE,"Ana Gomez", "ci","2.345.678-9")
        ));
        DEMO_PATIENTS.put("clinic-2", Arrays.asList(
                Map.of(NOMBRE,"Carlos Ruiz", "ci","9.876.543-2")
        ));
    }

    // Proxy to RNDC: returns the raw JSON array returned by the RNDC service for that patient
    @GET
    @Path("/pacientes/{username}")
    public Response pacientesPorProfesional(@PathParam("username") String username, @QueryParam("clinicId") String clinicId) {
        // Return demo patients for the configured clinicId
        if (clinicId == null || clinicId.isEmpty()) clinicId = "clinic-1";
        List<Map<String,String>> list = DEMO_PATIENTS.getOrDefault(clinicId, Collections.emptyList());
        return Response.ok(list).build();
    }

    @GET
    @Path("/paciente/{ci}/documentos")
    public Response documentosPaciente(@PathParam("ci") String ci, @QueryParam("profesionalId") String profesionalId) {
        try {
            // RNDC service (default): note the RNDC in this workspace uses double /api path in frontend; here we call the backend microservice path
            // default RNDC_URL corrected (previously had duplicate /api/api)
            String rndc = System.getenv().getOrDefault("RNDC_URL", "http://localhost:8180/hcen-rndc-service/api/rndc/documentos");
            String url = rndc + "/paciente/" + java.net.URLEncoder.encode(ci, java.nio.charset.StandardCharsets.UTF_8);
            Map<String,String> headers = new HashMap<>();
            if (profesionalId != null) headers.put("X-Profesional-Id", profesionalId);
            java.net.http.HttpResponse<String> resp = httpClient.getString(url, headers, 10);
            return Response.status(resp.statusCode()).entity(resp.body()).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching documentos paciente", e);
            String msg = e.getMessage() != null ? e.getMessage() : e.toString();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Collections.singletonMap(ERROR, msg)).build();
        }
    }
    

    @GET
    @Path("/verificar")
    public Response verificarPermiso(@QueryParam("profesionalId") String profesionalId, @QueryParam("pacienteCI") String pacienteCI, @QueryParam("tipoDoc") String tipoDoc) {
        try {
            String politicas = System.getenv().getOrDefault("POLITICAS_URL", "http://localhost:8180/hcen-politicas-service/api/politicas");
            String q = String.format("%s/verificar?profesionalId=%s&pacienteCI=%s&tipoDoc=%s",
                    politicas,
                    java.net.URLEncoder.encode(profesionalId == null?"":profesionalId, java.nio.charset.StandardCharsets.UTF_8),
                    java.net.URLEncoder.encode(pacienteCI == null?"":pacienteCI, java.nio.charset.StandardCharsets.UTF_8),
                    java.net.URLEncoder.encode(tipoDoc == null?"":tipoDoc, java.nio.charset.StandardCharsets.UTF_8)
            );
            java.net.http.HttpResponse<String> resp = httpClient.getString(q, null, 5);
            return Response.status(resp.statusCode()).entity(resp.body()).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error verifying permissions", e);
            String msg = e.getMessage() != null ? e.getMessage() : e.toString();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Collections.singletonMap("error", msg)).build();
        }
    }

    @POST
    @Path("/solicitudes")
    public Response crearSolicitud(Map<String,Object> body) {
        try {
            String politicas = System.getenv().getOrDefault("POLITICAS_URL", "http://localhost:8180/hcen-politicas-service/api/politicas");
            String json = JSON.writeValueAsString(body);
            java.net.http.HttpResponse<String> resp = httpClient.postString(politicas + "/solicitudes", Map.of("Content-Type","application/json"), json, 5);
            return Response.status(resp.statusCode()).entity(resp.body()).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating solicitud", e);
            String msg = e.getMessage() != null ? e.getMessage() : e.toString();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Collections.singletonMap("error", msg)).build();
        }
    }
}
