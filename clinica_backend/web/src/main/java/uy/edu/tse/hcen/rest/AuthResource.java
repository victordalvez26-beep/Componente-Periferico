package uy.edu.tse.hcen.rest;

import uy.edu.tse.hcen.dto.LoginRequest;
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

    @Inject
    private LoginService loginService;

    // public no-arg constructor so RESTEasy/Weld can instantiate and proxy this resource
    public AuthResource() {
    }

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        System.out.println("[AuthResource] Login intentado - nickname: " + (request != null ? request.getNickname() : "null"));
        try {
            if (request == null || request.getNickname() == null || request.getPassword() == null) {
                System.out.println("[AuthResource] Request inválido - request o credenciales null");
                return Response.status(Response.Status.BAD_REQUEST)
                               .entity(java.util.Map.of("error", "Nickname y password son requeridos"))
                               .build();
            }
            
            uy.edu.tse.hcen.dto.LoginResponse response = loginService.authenticateAndGenerateToken(
                request.getNickname(),
                request.getPassword(),
                request.getTenantId()  // Pasar el tenantId para saber dónde buscar
            );

            System.out.println("[AuthResource] Login exitoso para: " + request.getNickname());
            return Response.ok(response).build();

        } catch (SecurityException e) {
            System.out.println("[AuthResource] Login fallido: " + e.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED)
                           .entity(java.util.Map.of("error", "Credenciales incorrectas"))
                           .build();
        } catch (Exception e) {
            System.out.println("[AuthResource] Error inesperado en login: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(java.util.Map.of("error", "Error interno del servidor"))
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
