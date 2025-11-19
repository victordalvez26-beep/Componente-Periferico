package uy.edu.tse.hcen.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.Date;
import uy.edu.tse.hcen.model.enums.Departamentos;

@Entity
public class UsuarioSalud extends Usuario {

    @Column
    private String tipDoc;

    @Column
    private String codDoc;

    @Column
    private String nacionalidad;

    @Column
    private Date fechaNacimiento;

    @Enumerated(EnumType.STRING)
    private Departamentos departamento;

    @Column
    private String localidad;

    @Column(nullable = true)
    private String segundoNombre;  

    @Column(nullable = true)
    private String segundoApellido;

    @Column
    private String telefono;
    @Column
    private String direccion;

    @Column(name = "hcen_user_id")
    private Long hcenUserId;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "fecha_alta")
    private java.time.LocalDateTime fechaAlta;

    @Column(name = "fecha_actualizacion")
    private java.time.LocalDateTime fechaActualizacion;

    public UsuarioSalud() { super(); }

    public String getTipDoc() { return tipDoc; }
    public void setTipDoc(String tipDoc) { this.tipDoc = tipDoc; }

    public String getCodDoc() { return codDoc; }
    public void setCodDoc(String codDoc) { this.codDoc = codDoc; }

    public String getNacionalidad() { return nacionalidad; }
    public void setNacionalidad(String nacionalidad) { this.nacionalidad = nacionalidad; }

    public Date getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(Date fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public Departamentos getDepartamento() { return departamento; }
    public void setDepartamento(Departamentos departamento) { this.departamento = departamento; }

    public String getLocalidad() { return localidad; }
    public void setLocalidad(String localidad) { this.localidad = localidad; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getSegundoNombre() { return segundoNombre; }
    public void setSegundoNombre(String segundoNombre) { this.segundoNombre = segundoNombre; }

    public String getSegundoApellido() { return segundoApellido; }
    public void setSegundoApellido(String segundoApellido) { this.segundoApellido = segundoApellido; }

    public Long getHcenUserId() { return hcenUserId; }
    public void setHcenUserId(Long hcenUserId) { this.hcenUserId = hcenUserId; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public java.time.LocalDateTime getFechaAlta() { return fechaAlta; }
    public void setFechaAlta(java.time.LocalDateTime fechaAlta) { this.fechaAlta = fechaAlta; }

    public java.time.LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(java.time.LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

}
