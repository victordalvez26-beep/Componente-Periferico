package uy.edu.tse.hcen.auth;

import java.io.Serializable;

/**
 * Data transferred to web layer (safe view of authenticated user).
 */
public class UserDTO implements Serializable {
    public String username;
    public String nombre;
    public String[] roles;

    public UserDTO() {}

    public UserDTO(String username, String nombre, String[] roles) {
        this.username = username;
        this.nombre = nombre;
        this.roles = roles;
    }
}
