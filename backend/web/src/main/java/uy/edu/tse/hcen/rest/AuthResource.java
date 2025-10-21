package uy.edu.tse.hcen.rest;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import uy.edu.tse.hcen.auth.AuthService;
import uy.edu.tse.hcen.auth.UserDTO;

@Path("/api/auth")
public class AuthResource {
    @Inject
    private AuthService authService;

    @Context
    private HttpServletRequest request;

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(LoginRequest req) {
        if (req == null || req.username == null || req.password == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("username and password required").build();
        }
        UserDTO u = authService.authenticate(req.username, req.password);
        if (u == null) return Response.status(Response.Status.UNAUTHORIZED).build();
        HttpSession session = request.getSession(true);
        session.setAttribute("user", u);
        // Note: Cookie flags (Secure, HttpOnly, SameSite) must be configured at container level.
        return Response.ok(u).build();
    }

    @GET
    @Path("/session")
    @Produces(MediaType.APPLICATION_JSON)
    public Response session() {
        HttpSession session = request.getSession(false);
        if (session == null) return Response.ok(new SessionResponse(false)).build();
        Object o = session.getAttribute("user");
        if (o instanceof UserDTO) {
            UserDTO u = (UserDTO) o;
            SessionResponse sr = new SessionResponse(true, u.username, u.nombre, u.roles);
            return Response.ok(sr).build();
        }
        return Response.ok(new SessionResponse(false)).build();
    }

    @GET
    @Path("/logout")
    public Response logout() {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        // Redirect or return success. Frontend expects a call to /api/auth/logout
        return Response.ok().build();
    }
}
