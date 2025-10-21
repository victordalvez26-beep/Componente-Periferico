package uy.edu.tse.hcen.rest;

public class SessionResponse {
    public boolean authenticated;
    public String username;
    public String nombre;
    public String[] roles;

    public SessionResponse() {}

    public SessionResponse(boolean authenticated) { this.authenticated = authenticated; }

    public SessionResponse(boolean authenticated, String username, String nombre, String[] roles) {
        this.authenticated = authenticated;
        this.username = username;
        this.nombre = nombre;
        this.roles = roles;
    }
}
