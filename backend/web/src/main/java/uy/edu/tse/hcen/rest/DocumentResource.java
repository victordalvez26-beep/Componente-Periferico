package uy.edu.tse.hcen.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import uy.edu.tse.hcen.service.MongoDBService;

@Path("/documents")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DocumentResource {

    @EJB
    private MongoDBService mongoService;

    /**
     * Store document metadata (JSON) in MongoDB.
     * Example body:
     * {
     *   "metadataId": "meta-0001",
     *   "filename": "file.pdf",
     *   "contentType": "application/pdf",
     *   "size": 12345,
     *   "downloadUrl": "http://..."
     * }
     */
    @POST
    public Response uploadDocument(String json) {
        try {
            mongoService.insertDocument(json);
            return Response.status(Response.Status.CREATED).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    /**
     * Retrieve document metadata by metadataId
     */
    @GET
    @Path("/{metadataId}")
    public Response getDocument(@PathParam("metadataId") String metadataId) {
        try {
            String json = mongoService.findByCodigo(metadataId);
            if (json == null) return Response.status(Response.Status.NOT_FOUND).build();
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }
}
