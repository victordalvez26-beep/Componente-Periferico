package uy.edu.tse.hcen.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class NodoPerifericoDTO {

    private Long id;

    @NotBlank
    @Size(max = 100)
    private String nombre;

    @NotBlank
    @Size(max = 50)
    private String RUT;

    @NotBlank
    private String departamento; 

    @Size(max = 100)
    private String localidad;

    @Size(max = 255)
    private String direccion;

    @Size(max = 100)
    private String contacto;

    @Size(max = 255)
    private String url;

    // --- Configuración Técnica del Nodo Periférico ---
    @Size(max = 255)
    private String nodoPerifericoUrlBase;

    @Size(max = 100)
    private String nodoPerifericoUsuario;

    @Size(max = 100)
    private String nodoPerifericoPassword;

    private String estado;

    public NodoPerifericoDTO() {
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

    public String getRUT() {
        return RUT;
    }

    public void setRUT(String RUT) {
        this.RUT = RUT;
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

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
