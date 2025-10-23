package uy.edu.tse.hcen.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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

    private final NodoPerifericoHttpClient httpClient;
    private static final com.fasterxml.jackson.databind.ObjectMapper JSON = new com.fasterxml.jackson.databind.ObjectMapper();

    private static final String NOMBRE = "nombre";
    private static final String ERROR = "error";
    private static final String NORMALIZE_REGEX = "[^A-Za-z0-9]";
    private static final String CLINIC_DEFAULT = "clinic-1";
    private static final String INTERRUPTED = "interrupted";

    @Inject
    public ProfesionalResource(NodoPerifericoHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    // No-arg constructor required by some JAX-RS runtimes that instantiate resource classes
    // directly (when CDI isn't used). Provide a simple default client so endpoints remain
    // usable in that case. CDI will use the @Inject constructor when available.
    public ProfesionalResource() {
        this.httpClient = new NodoPerifericoHttpClient();
    }

    // In-memory demo patients per clinicId. In real app this must come from DB.
    private static final Map<String, List<Map<String,String>>> DEMO_PATIENTS = new HashMap<>();
    static {
    DEMO_PATIENTS.put(CLINIC_DEFAULT, Arrays.asList(
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
    public Response pacientesPorProfesional(@PathParam("username") String username, @QueryParam("clinicId") String clinicId, @QueryParam("q") String q) {
        // Patients are scoped to clinic; the profesional path is kept for compatibility but
        // the patients returned are those of the clinic. Support optional query 'q' to
        // search patients by name (partial, case-insensitive).
        if (clinicId == null || clinicId.isEmpty()) clinicId = CLINIC_DEFAULT;
        List<Map<String,String>> list = new ArrayList<>(DEMO_PATIENTS.getOrDefault(clinicId, Collections.emptyList()));
        if (q != null && !q.trim().isEmpty()) {
            String qnorm = q.replaceAll(NORMALIZE_REGEX, "").toLowerCase();
            list.removeIf(p -> {
                String nombre = p.getOrDefault(NOMBRE, "");
                String ci = p.getOrDefault("ci", "");
                String nombreNorm = nombre.replaceAll(NORMALIZE_REGEX, "").toLowerCase();
                String ciNorm = ci.replaceAll(NORMALIZE_REGEX, "").toLowerCase();
                // match if query is substring of name or ci
                return !(nombreNorm.contains(qnorm) || ciNorm.contains(qnorm));
            });
        }
        return Response.ok(list).build();
    }

    // Backwards-compatible endpoint: allow calling without the username path parameter
    @GET
    @Path("/pacientes")
    public Response pacientesPorClinica(@QueryParam("clinicId") String clinicId, @QueryParam("q") String q) {
        if (clinicId == null || clinicId.isEmpty()) clinicId = CLINIC_DEFAULT;
        List<Map<String,String>> list = new ArrayList<>(DEMO_PATIENTS.getOrDefault(clinicId, Collections.emptyList()));
        if (q != null && !q.trim().isEmpty()) {
            String qnorm = q.replaceAll(NORMALIZE_REGEX, "").toLowerCase();
            list.removeIf(p -> {
                String nombre = p.getOrDefault(NOMBRE, "");
                String ci = p.getOrDefault("ci", "");
                String nombreNorm = nombre.replaceAll(NORMALIZE_REGEX, "").toLowerCase();
                String ciNorm = ci.replaceAll(NORMALIZE_REGEX, "").toLowerCase();
                return !(nombreNorm.contains(qnorm) || ciNorm.contains(qnorm));
            });
        }
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "Interrupted while fetching documentos paciente", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Collections.singletonMap(ERROR, INTERRUPTED)).build();
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "Interrupted while verifying permissions", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Collections.singletonMap(ERROR, INTERRUPTED)).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error verifying permissions", e);
            String msg = e.getMessage() != null ? e.getMessage() : e.toString();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Collections.singletonMap(ERROR, msg)).build();
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "Interrupted while creating solicitud", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Collections.singletonMap(ERROR, INTERRUPTED)).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating solicitud", e);
            String msg = e.getMessage() != null ? e.getMessage() : e.toString();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Collections.singletonMap(ERROR, msg)).build();
        }
    }
}
