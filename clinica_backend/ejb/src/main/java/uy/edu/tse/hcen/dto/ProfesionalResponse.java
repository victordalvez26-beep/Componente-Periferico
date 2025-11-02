package uy.edu.tse.hcen.dto;

import uy.edu.tse.hcen.model.ProfesionalSalud;

/**
 * Response DTO for ProfesionalSalud 
 */
public class ProfesionalResponse {
    private Long id;
    private String nombre;
    private String email;
    private String nickname;
    private String especialidad;
    private String direccion;

    public ProfesionalResponse() {}

    public static ProfesionalResponse fromEntity(ProfesionalSalud p) {
        ProfesionalResponse r = new ProfesionalResponse();
        r.id = p.getId();
        r.nombre = p.getNombre();
        r.email = p.getEmail();
        r.nickname = p.getNickname();
        r.especialidad = p.getEspecialidad() != null ? p.getEspecialidad().name() : null;
        r.direccion = p.getDireccion();
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
}
