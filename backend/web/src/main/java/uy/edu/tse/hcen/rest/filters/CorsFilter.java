package uy.edu.tse.hcen.rest.filters;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

/**
 * Simple CORS filter for local development. Allows requests from the dev frontend (http://localhost:3000).
 * In production configure appropriate origins and security settings.
 */
@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class CorsFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        // Allow dev origin
        responseContext.getHeaders().putSingle("Access-Control-Allow-Origin", "http://localhost:3000");
        responseContext.getHeaders().putSingle("Access-Control-Allow-Credentials", "true");
        responseContext.getHeaders().putSingle("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
        responseContext.getHeaders().putSingle("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        // Expose headers if necessary
        responseContext.getHeaders().putSingle("Access-Control-Expose-Headers", "Content-Disposition");
    }
}
