package uy.edu.tse.hcen.dto;

import uy.edu.tse.hcen.model.UsuarioSalud;
import java.time.LocalDate;
import java.time.ZoneId;
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
    private String apellido;
    private String email;
    private String telefono;
    private String segundoNombre;
    private String segundoApellido;


    public DTUsuarioSalud() {}

    public static DTUsuarioSalud fromEntity(UsuarioSalud u) {
        if (u == null) return null;
        DTUsuarioSalud d = new DTUsuarioSalud();
        d.id = u.getId();
        d.tipDoc = null; // No existe en el modelo
        d.codDoc = u.getCi(); // Usar CI como código de documento
        d.nacionalidad = null; // No existe en el modelo
        if (u.getFechaNacimiento() != null) {
            d.fechaNacimiento = Date.from(u.getFechaNacimiento().atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
        d.departamento = u.getDepartamento(); // Ya es String en el modelo
        d.localidad = u.getLocalidad();
        d.direccion = u.getDireccion();
        d.nombre = u.getNombre();
        d.apellido = u.getApellido();
        d.email = u.getEmail();
        d.telefono = u.getTelefono();
        d.segundoNombre = null; // No existe en el modelo
        d.segundoApellido = null; // No existe en el modelo
        return d;
    }

    public UsuarioSalud toEntity() {
        UsuarioSalud u = new UsuarioSalud();
        if (this.codDoc != null) {
            u.setCi(this.codDoc); // Usar CI como código de documento
        }
        if (this.fechaNacimiento != null) {
            u.setFechaNacimiento(this.fechaNacimiento.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        }
        u.setDepartamento(this.departamento);
        u.setLocalidad(this.localidad);
        u.setDireccion(this.direccion);
        u.setNombre(this.nombre);
        if (this.apellido != null) {
            u.setApellido(this.apellido);
        }
        u.setEmail(this.email);
        u.setTelefono(this.telefono);
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
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getSegundoNombre() { return segundoNombre; }
    public void setSegundoNombre(String segundoNombre) { this.segundoNombre = segundoNombre;}
    public String getSegundoApellido() { return segundoApellido; }
    public void setSegundoApellido(String segundoApellido) { this.segundoApellido = segundoApellido;}

}
