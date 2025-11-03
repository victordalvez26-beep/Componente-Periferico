package uy.edu.tse.hcen.dto;

import uy.edu.tse.hcen.model.Usuario;

public class DTUsuario {

    private Long id;
    private String nombre;
    private String email;

    public DTUsuario() {
    }

    public DTUsuario(Long id, String nombre, String email) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Create a DTO from an entity instance.
     */
    public static DTUsuario fromEntity(Usuario usuario) {
        if (usuario == null) return null;
        return new DTUsuario(usuario.getId(), usuario.getNombre(), usuario.getEmail());
    }
}
