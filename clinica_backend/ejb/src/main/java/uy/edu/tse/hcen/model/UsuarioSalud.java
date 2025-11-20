package uy.edu.tse.hcen.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad que representa un Usuario de Salud (paciente) registrado en una clínica.
 * Se almacena en la tabla usuario_salud del schema específico de cada clínica (tenant).
 */
@Entity
@Table(name = "usuario_salud",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_ci_tenant",
           columnNames = {"ci", "tenant_id"}
       ))
public class UsuarioSalud {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 20)
    private String ci;              // Documento de identidad (cédula)
    
    @Column(length = 255)
    private String nombre;
    
    @Column(length = 255)
    private String apellido;
    
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;
    
    @Column(length = 255)
    private String direccion;
    
    @Column(length = 50)
    private String telefono;
    
    @Column(length = 255)
    private String email;
    
    @Column(length = 100)
    private String departamento;
    
    @Column(length = 100)
    private String localidad;
    
    @Column(name = "hcen_user_id")
    private Long hcenUserId;        // ID del User en HCEN (referencia al INUS)
    
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;          // ID de la clínica a la que pertenece
    
    @Column(name = "fecha_alta")
    private LocalDateTime fechaAlta;
    
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
    
    // Constructors
    public UsuarioSalud() {
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getCi() {
        return ci;
    }
    
    public void setCi(String ci) {
        this.ci = ci;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    public String getApellido() {
        return apellido;
    }
    
    public void setApellido(String apellido) {
        this.apellido = apellido;
    }
    
    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }
    
    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }
    
    public String getDireccion() {
        return direccion;
    }
    
    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }
    
    public String getTelefono() {
        return telefono;
    }
    
    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getDepartamento() {
        return departamento;
    }
    
    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }
    
    public String getLocalidad() {
        return localidad;
    }
    
    public void setLocalidad(String localidad) {
        this.localidad = localidad;
    }
    
    public Long getHcenUserId() {
        return hcenUserId;
    }
    
    public void setHcenUserId(Long hcenUserId) {
        this.hcenUserId = hcenUserId;
    }
    
    public Long getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
    
    public LocalDateTime getFechaAlta() {
        return fechaAlta;
    }
    
    public void setFechaAlta(LocalDateTime fechaAlta) {
        this.fechaAlta = fechaAlta;
    }
    
    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
    
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
    
    @Override
    public String toString() {
        return "UsuarioSalud{" +
                "id=" + id +
                ", ci='" + ci + '\'' +
                ", nombre='" + nombre + '\'' +
                ", apellido='" + apellido + '\'' +
                ", tenantId=" + tenantId +
                ", hcenUserId=" + hcenUserId +
                '}';
    }
}

