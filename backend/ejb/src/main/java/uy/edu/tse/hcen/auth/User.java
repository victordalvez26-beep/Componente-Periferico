package uy.edu.tse.hcen.auth;

import java.io.Serializable;

/**
 * Simple user model for in-memory authentication (development use only).
 * In production this should be a proper JPA entity stored in a database.
 */
public class User implements Serializable {
    private String username;
    private String passwordHash; // PBKDF2 hashed password
    private String nombre;
    private String[] roles;

    public User() {}

    public User(String username, String passwordHash, String nombre, String[] roles) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.nombre = nombre;
        this.roles = roles;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getNombre() {
        return nombre;
    }

    public String[] getRoles() {
        return roles;
    }
}
