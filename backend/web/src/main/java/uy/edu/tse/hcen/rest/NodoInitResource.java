package uy.edu.tse.hcen.rest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import jakarta.ws.rs.core.Response.Status;

/**
 * Endpoint simple que acepta la llamada de HCEN para inicializar un nodo.
 * Ruta: POST /api/config/init
 */
@Path("/config")
@Consumes(MediaType.APPLICATION_JSON)
public class NodoInitResource {

    public static class InitPayload {
        public String id;
        public String nombre;
    }

    @POST
    @Path("/init")
    public Response init(InitPayload p) {
        // Aquí podrías ejecutar lógica de configuración local como persistir
        // valores, generar claves, o validar la conexión. Para fines de prueba
        // devolvemos 200 OK si el payload tiene un id.
        if (p == null || p.id == null || p.id.isBlank()) {
            return Response.status(Status.BAD_REQUEST).entity("Missing id").build();
        }
        return Response.ok().build();
    }

    @POST
    @Path("/update")
    public Response update(InitPayload p) {
        if (p == null || p.id == null || p.id.isBlank()) {
            return Response.status(Status.BAD_REQUEST).entity("Missing id").build();
        }
        // For testing we accept update requests and respond 200
        return Response.ok().build();
    }

    @POST
    @Path("/delete")
    public Response delete(InitPayload p) {
        if (p == null || p.id == null || p.id.isBlank()) {
            return Response.status(Status.BAD_REQUEST).entity("Missing id").build();
        }
        // Simulate clean-up on the node side
        return Response.ok().build();
    }
}
