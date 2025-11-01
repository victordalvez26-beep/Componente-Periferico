package uy.edu.tse.hcen.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.Document;
import com.mongodb.client.MongoCollection;
import uy.edu.tse.hcen.service.MongoDBService;

//Clase de prueba para verificar la conexión con MongoDB y realizar operaciones básicas
@Path("/mongo")
public class MongoTestResource {

    @EJB
    private MongoDBService mongoDBService;

    private MongoCollection<Document> getCollection() {
        return mongoDBService.getDatabase().getCollection("documentos_clinicos");
    }

    @GET
    @Path("/health")
    @Produces(MediaType.TEXT_PLAIN)
    public Response health() {
        try {
            long count = mongoDBService == null ? -1 : getCollection().countDocuments();
            return Response.ok("ok - collection count: " + count).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("mongo error: " + e.getMessage()).build();
        }
    }

    @POST
    @Path("/document")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response insertDocument(String bodyJson) {
        try {
            Document doc = Document.parse(bodyJson);
            getCollection().insertOne(doc);
            return Response.status(Response.Status.CREATED)
                    .entity(doc.toJson()).type(MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            Document err = new Document("error", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(err.toJson()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/document/{inus}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findByInus(@PathParam("inus") String inus) {
        try {
            Document found = getCollection().find(new Document("pacienteInus", inus)).first();
            if (found == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(found.toJson()).type(MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            Document err = new Document("error", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(err.toJson()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
