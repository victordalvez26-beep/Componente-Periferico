package uy.edu.tse.hcen.service;

import jakarta.ejb.Stateless;

@Stateless
public class MongoDBServiceBean implements MongoDBService {

    @Override
    public boolean health() {
        return true;
    }

    @Override
    public void insertDocument(String json) {
        // No-op for tests/resources
    }

    @Override
    public String findByCodigo(String codigo) {
        // return null to indicate not found; tests/resources won't depend on content
        return null;
    }
}
