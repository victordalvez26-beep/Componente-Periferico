package uy.edu.tse.hcen.service;

/**
 * Minimal MongoDB service interface used by test resource.
 * Using String payloads to avoid introducing the MongoDB driver here.
 */
public interface MongoDBService {
    boolean health();
    void insertDocument(String json);
    /**
     * Returns a JSON string representation of the found document or null.
     */
    String findByCodigo(String codigo);
}
