package uy.edu.tse.hcen.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/api")
public class RestApplication extends Application {
    // Activates JAX-RS within the WAR at /api
}
