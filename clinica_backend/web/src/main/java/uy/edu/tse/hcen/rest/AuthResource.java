package uy.edu.tse.hcen.rest;

import uy.edu.tse.hcen.dto.LoginRequest;
import uy.edu.tse.hcen.service.ILoginService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    private static final String KEY_ERROR = "error";

    @Inject
    private ILoginService loginService;

    public AuthResource() {
        // public no-arg constructor so RESTEasy/Weld can instantiate and proxy this resource
    }

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        try {
            if (request == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                               .entity(java.util.Map.of(KEY_ERROR, "Request body es requerido"))
                               .build();
            }
            
            uy.edu.tse.hcen.dto.LoginResponse response = loginService.authenticateAndGenerateToken(
                request.getNickname(),
                request.getPassword(),
                request.getTenantId()  // Pasar el tenantId para saber dónde buscar
            );

            return Response.ok(response).build();

        } catch (SecurityException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                           .entity(java.util.Map.of(KEY_ERROR, "Credenciales incorrectas"))
                           .build();
        }
    }

    @POST
    @Path("/logout")
    public Response logout() {
        // Stateless JWT: logout is handled client-side by discarding the token.
        // We provide a simple endpoint so the client has a unified API.
        return Response.ok(java.util.Map.of("message", "Logout successful - discard token on client"))
                       .build();
    }
}
