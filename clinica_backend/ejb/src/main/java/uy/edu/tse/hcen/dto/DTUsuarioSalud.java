package uy.edu.tse.hcen.dto;

import uy.edu.tse.hcen.model.UsuarioSalud;
import uy.edu.tse.hcen.model.enums.Departamentos;
import java.util.Date;

public class DTUsuarioSalud {
    private Long id;
    private String tipDoc;
    private String codDoc;
    private String nacionalidad;
    private Date fechaNacimiento;
    private String departamento;
    private String localidad;
    private String direccion;
    private String nombre;
    private String email;
    private String telefono;
    private String segundoNombre;
    private String segundoApellido;


    public DTUsuarioSalud() {}

    public static DTUsuarioSalud fromEntity(UsuarioSalud u) {
        if (u == null) return null;
        DTUsuarioSalud d = new DTUsuarioSalud();
        d.id = u.getId();
        d.tipDoc = u.getTipDoc();
        d.codDoc = u.getCodDoc();
        d.nacionalidad = u.getNacionalidad();
        d.fechaNacimiento = u.getFechaNacimiento();
        d.departamento = u.getDepartamento() != null ? u.getDepartamento().name() : null;
        d.localidad = u.getLocalidad();
        d.direccion = u.getDireccion();
        d.nombre = u.getNombre();
        d.email = u.getEmail();
        d.telefono = u.getTelefono();
        d.segundoNombre = u.getSegundoNombre();
        d.segundoApellido = u.getSegundoApellido();
        return d;
    }

    public UsuarioSalud toEntity() {
        UsuarioSalud u = new UsuarioSalud();
        u.setTipDoc(this.tipDoc);
        u.setCodDoc(this.codDoc);
        u.setNacionalidad(this.nacionalidad);
        u.setFechaNacimiento(this.fechaNacimiento);
        if (this.departamento != null) {
            try { u.setDepartamento(Departamentos.valueOf(this.departamento)); } catch (Exception e) { /* ignore */ }
        }
        u.setLocalidad(this.localidad);
        u.setDireccion(this.direccion);
        u.setNombre(this.nombre);
        u.setEmail(this.email);
        u.setTelefono(this.telefono);
        u.setSegundoNombre(this.segundoNombre);
        u.setSegundoApellido(this.segundoApellido);
        return u;
    }

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTipDoc() { return tipDoc; }
    public void setTipDoc(String tipDoc) { this.tipDoc = tipDoc; }
    public String getCodDoc() { return codDoc; }
    public void setCodDoc(String codDoc) { this.codDoc = codDoc; }
    public String getNacionalidad() { return nacionalidad; }
    public void setNacionalidad(String nacionalidad) { this.nacionalidad = nacionalidad; }
    public Date getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(Date fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }
    public String getLocalidad() { return localidad; }
    public void setLocalidad(String localidad) { this.localidad = localidad; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getSegundoNombre() { return segundoNombre; }
    public void setSegundoNombre(String segundoNombre) { this.segundoNombre = segundoNombre;}
    public String getSegundoApellido() { return segundoApellido; }
    public void setSegundoApellido(String segundoApellido) { this.segundoApellido = segundoApellido;}

}
