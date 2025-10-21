package uy.edu.tse.hcen.rest.filters;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

/**
 * Handles CORS preflight (OPTIONS) requests for development.
 * Returns 200 OK with Access-Control-Allow-* headers so the browser will allow the subsequent request.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class CorsPreflightRequestFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            Response.ResponseBuilder builder = Response.ok();
            builder.header("Access-Control-Allow-Origin", "http://localhost:3000");
            builder.header("Access-Control-Allow-Credentials", "true");
            builder.header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
            builder.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
            builder.header("Access-Control-Expose-Headers", "Content-Disposition");
            requestContext.abortWith(builder.build());
        }
    }
}
