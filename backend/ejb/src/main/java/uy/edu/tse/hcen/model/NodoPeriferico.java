package uy.edu.tse.hcen.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import uy.edu.tse.hcen.model.enums.Departamento;
import uy.edu.tse.hcen.model.enums.EstadoNodoPeriferico;

@Entity
public class NodoPeriferico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    @Column(unique = true)
    private String RUT;

    @Enumerated(EnumType.STRING)
    private Departamento departamento;

    private String localidad;

    private String direccion;

    private String contacto;

    private String url;

    // --- Configuración Técnica del Nodo Periférico ---
    private String nodoPerifericoUrlBase; // Ej: https://clinica-x.com/hc-api
    private String nodoPerifericoUsuario; // Credenciales para el Componente Central
    // Transient holder for incoming raw password (not persisted)
    @Transient
    private String nodoPerifericoPassword;

    // Persisted secure password data
    private String passwordHash;
    private String passwordSalt;

    @Enumerated(EnumType.STRING)
    private EstadoNodoPeriferico estado;

    // Fecha de alta cuando el nodo es registrado en HCEN
    private OffsetDateTime fechaAlta;

    // Getters and setters

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

    public String getRUT() {
        return RUT;
    }

    public void setRUT(String rUT) {
        RUT = rUT;
    }

    public Departamento getDepartamento() {
        return departamento;
    }

    public void setDepartamento(Departamento departamento) {
        this.departamento = departamento;
    }

    public String getLocalidad() {
        return localidad;
    }

    public void setLocalidad(String localidad) {
        this.localidad = localidad;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getContacto() {
        return contacto;
    }

    public void setContacto(String contacto) {
        this.contacto = contacto;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getNodoPerifericoUrlBase() {
        return nodoPerifericoUrlBase;
    }

    public void setNodoPerifericoUrlBase(String nodoPerifericoUrlBase) {
        this.nodoPerifericoUrlBase = nodoPerifericoUrlBase;
    }

    public String getNodoPerifericoUsuario() {
        return nodoPerifericoUsuario;
    }

    public void setNodoPerifericoUsuario(String nodoPerifericoUsuario) {
        this.nodoPerifericoUsuario = nodoPerifericoUsuario;
    }

    public String getNodoPerifericoPassword() {
        return nodoPerifericoPassword;
    }

    public void setNodoPerifericoPassword(String nodoPerifericoPassword) {
        this.nodoPerifericoPassword = nodoPerifericoPassword;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    public EstadoNodoPeriferico getEstado() {
        return estado;
    }

    public void setEstado(EstadoNodoPeriferico estado) {
        this.estado = estado;
    }

    public OffsetDateTime getFechaAlta() {
        return fechaAlta;
    }

    public void setFechaAlta(OffsetDateTime fechaAlta) {
        this.fechaAlta = fechaAlta;
    }
}
