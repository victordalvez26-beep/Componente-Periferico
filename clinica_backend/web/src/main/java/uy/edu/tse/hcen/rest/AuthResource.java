package uy.edu.tse.hcen.rest;

import uy.edu.tse.hcen.dto.LoginRequest;
import uy.edu.tse.hcen.dto.LoginResponse;
import uy.edu.tse.hcen.service.LoginService;
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

    private final LoginService loginService;

    @Inject
    public AuthResource(LoginService loginService) {
        this.loginService = loginService;
    }

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        try {
            String token = loginService.authenticateAndGenerateToken(
                request.getNickname(),
                request.getPassword()
            );

            LoginResponse response = new LoginResponse();
            response.setToken(token);
            response.setRole("Desconocido (validar token)");

            return Response.ok(response).build();

        } catch (SecurityException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                           .entity(java.util.Map.of("error", "Credenciales incorrectas"))
                           .build();
        }
    }
}
