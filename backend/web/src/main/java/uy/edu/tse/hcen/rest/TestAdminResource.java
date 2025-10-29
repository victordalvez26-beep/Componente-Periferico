package uy.edu.tse.hcen.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import uy.edu.tse.hcen.model.NodoPeriferico;
import uy.edu.tse.hcen.model.enums.EstadoNodoPeriferico;
import uy.edu.tse.hcen.repository.NodoPerifericoRepository;
import uy.edu.tse.hcen.rest.dto.NodoPerifericoConverter;
import uy.edu.tse.hcen.rest.dto.NodoPerifericoDTO;

/**
 * Test-only admin endpoints to help local testing (NOT for production).
 * Example: GET /api/test/activate?rut=clinic-1&nombre=ClinicaTest
 */
@Path("/test")
@Produces(MediaType.APPLICATION_JSON)
public class TestAdminResource {

    @EJB
    private NodoPerifericoRepository repo;

    @GET
    @Path("activate")
    public Response activate(@QueryParam("rut") String rut, @QueryParam("nombre") String nombre) {
        if (rut == null || rut.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing rut parameter").build();
        }
        try {
            NodoPeriferico existing = repo.findByRUT(rut);
            if (existing == null) {
                NodoPerifericoDTO dto = new NodoPerifericoDTO();
                dto.setRUT(rut);
                dto.setNombre(nombre != null && !nombre.isBlank() ? nombre : "Clinica " + rut);
                dto.setDepartamento("MONTEVIDEO");
                dto.setEstado(EstadoNodoPeriferico.ACTIVO.name());
                NodoPeriferico toCreate = NodoPerifericoConverter.toEntity(dto);
                toCreate.setEstado(EstadoNodoPeriferico.ACTIVO);
                NodoPeriferico created = repo.create(toCreate);
                return Response.status(Response.Status.CREATED).entity(NodoPerifericoConverter.toDTO(created)).build();
            } else {
                // update estado in new tx to be safe
                repo.updateEstadoInNewTx(existing.getId(), EstadoNodoPeriferico.ACTIVO);
                NodoPeriferico updated = repo.find(existing.getId());
                return Response.ok(NodoPerifericoConverter.toDTO(updated)).build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }
}
