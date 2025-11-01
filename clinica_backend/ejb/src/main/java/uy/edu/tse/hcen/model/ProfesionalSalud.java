package uy.edu.tse.hcen.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import uy.edu.tse.hcen.business.model.enums.Especialidad;
import uy.edu.tse.hcen.business.model.enums.Departamentos;

@Entity
public class ProfesionalSalud extends UsuarioPeriferico {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Especialidad especialidad; // Atributo de ProfesionalSalud

    @Enumerated(EnumType.STRING)
    private Departamentos departamento; // Atributo de ProfesionalSalud

    private String calidad; // Atributo de ProfesionalSalud

    private String direccion; // Atributo de ProfesionalSalud

    @ManyToOne
    @JoinColumn(name = "nodo_periferico_id")
    private NodoPeriferico trabajaEn; // Relación con NodoPeriferico

    public ProfesionalSalud() { super(); }

    public ProfesionalSalud(String nombre, String email, String nickname, String password, Especialidad especialidad, Departamentos departamento, String calidad) {
        super();
        setNombre(nombre);
        setEmail(email);
        setNickname(nickname);
        if (password != null) {
            setPassword(password);
        }

        this.especialidad = especialidad;
        this.departamento = departamento;
        this.calidad = calidad;
    }

    public Especialidad getEspecialidad() { return especialidad; }
    public void setEspecialidad(Especialidad especialidad) { this.especialidad = especialidad; }

    public Departamentos getDepartamento() { return departamento; }
    public void setDepartamento(Departamentos departamento) { this.departamento = departamento; }

    public String getCalidad() { return calidad; }
    public void setCalidad(String calidad) { this.calidad = calidad; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public void setTrabajaEn(NodoPeriferico trabajaEn) { this.trabajaEn = trabajaEn; }

    //Obtiene el ID del Tenant (el ID del NodoPeriferico asociado).
    public Long getTenantId() {
        return this.trabajaEn != null ? this.trabajaEn.getId() : null;
    }
}
