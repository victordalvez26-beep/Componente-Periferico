package uy.edu.tse.hcen.rest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RestApplicationTest {

    @Test
    void testRestApplicationInstantiation() {
        RestApplication app = new RestApplication();
        assertNotNull(app);
    }

    @Test
    void testRestApplicationIsApplication() {
        RestApplication app = new RestApplication();
        assertTrue(app instanceof jakarta.ws.rs.core.Application);
    }
}

