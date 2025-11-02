package uy.edu.tse.hcen.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import uy.edu.tse.hcen.business.model.enums.Especialidad;
import uy.edu.tse.hcen.business.model.enums.Departamentos;

@Entity
public class ProfesionalSalud extends UsuarioPeriferico {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Especialidad especialidad; // Atributo de ProfesionalSalud

    @Enumerated(EnumType.STRING)
    private Departamentos departamento; // Atributo de ProfesionalSalud

    private String direccion; // Atributo de ProfesionalSalud

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nodo_periferico_id")
    private NodoPeriferico trabajaEn; // Relación con NodoPeriferico

    public ProfesionalSalud() { super(); }

    public ProfesionalSalud(String nombre, String email, String nickname, String password, Especialidad especialidad, Departamentos departamento) {
        super();
        setNombre(nombre);
        setEmail(email);
        setNickname(nickname);
        if (password != null) {
            setPassword(password);
        }

        this.especialidad = especialidad;
        this.departamento = departamento;
    }

    public Especialidad getEspecialidad() { return especialidad; }
    public void setEspecialidad(Especialidad especialidad) { this.especialidad = especialidad; }

    public Departamentos getDepartamento() { return departamento; }
    public void setDepartamento(Departamentos departamento) { this.departamento = departamento; }

    // 'calidad' column/attribute removed - not used anymore

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public void setTrabajaEn(NodoPeriferico trabajaEn) { this.trabajaEn = trabajaEn; }

    // Obtiene el ID del NodoPeriferico asociado (tenant ID numérico).
    // NOTE: no sobrescribe UsuarioPeriferico.getTenantId() que ahora devuelve
    // una cadena opcional usada para usuarios globales; este método devuelve
    // el id numérico del nodo asociado cuando aplica.
    public Long getTenantNodeId() {
        return this.trabajaEn != null ? this.trabajaEn.getId() : null;
    }
}
