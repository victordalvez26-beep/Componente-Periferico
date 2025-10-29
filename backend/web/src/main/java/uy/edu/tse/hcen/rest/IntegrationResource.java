package uy.edu.tse.hcen.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import uy.edu.tse.hcen.repository.NodoPerifericoRepository;
import uy.edu.tse.hcen.rest.dto.NodoPerifericoDTO;
import uy.edu.tse.hcen.model.NodoPeriferico;

import java.time.OffsetDateTime;
import java.util.List;

@Path("/integration")
@Produces(MediaType.APPLICATION_JSON)
public class IntegrationResource {

    @EJB
    private NodoPerifericoRepository repo;

    /**
     * Returns a simple integration status for a clinic. Query param clinicId is optional.
     * The method attempts a best-effort match against existing registered nodos.
     */
    @GET
    @Path("/status")
    public Response status(@QueryParam("clinicId") String clinicId) {
        boolean integrated = false;
        String matched = null;
        List<NodoPeriferico> list = repo.findAll();
        if (clinicId != null && !clinicId.isBlank()) {
            for (NodoPeriferico n : list) {
                //  match by RUT or nombre containing clinicId
                if ((n.getRUT() != null && n.getRUT().equalsIgnoreCase(clinicId)) ||
                        (n.getNombre() != null && n.getNombre().toLowerCase().contains(clinicId.toLowerCase()))) {
                    matched = n.getNombre();
                    if (n.getEstado() != null && n.getEstado().name().equalsIgnoreCase("ACTIVO")) {
                        integrated = true;
                    }
                    break;
                }
            }
        } else if (!list.isEmpty()) {
            // no clinicId provided: if any nodo is ACTIVO treat as integrated=false (no specific clinic)
            // keep integrated=false but include count and last registered
            NodoPeriferico any = list.get(0);
            matched = any.getNombre();
            integrated = any.getEstado() != null && any.getEstado().name().equalsIgnoreCase("ACTIVO");
        }

        IntegrationStatus s = new IntegrationStatus(integrated, matched, OffsetDateTime.now().toString());
        return Response.ok(s).build();
    }

    public static class IntegrationStatus {
        public boolean integrated;
        public String matchedNodo;
        public String lastCheck;

        public IntegrationStatus(boolean integrated, String matchedNodo, String lastCheck) {
            this.integrated = integrated;
            this.matchedNodo = matchedNodo;
            this.lastCheck = lastCheck;
        }
    }
}
