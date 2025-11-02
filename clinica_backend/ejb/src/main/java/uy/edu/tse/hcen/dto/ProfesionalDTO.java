package uy.edu.tse.hcen.dto;

import uy.edu.tse.hcen.model.enums.Especialidad;

/**
 * DTO ligero para crear/actualizar ProfesionalSalud.
 */
public class ProfesionalDTO {
    private String nombre;
    private String email;
    private String nickname;
    private Especialidad especialidad;
    private String direccion;
    private String password;

    public ProfesionalDTO() {
        // DTO vacío
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public Especialidad getEspecialidad() { return especialidad; }
    public void setEspecialidad(Especialidad especialidad) { this.especialidad = especialidad; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
