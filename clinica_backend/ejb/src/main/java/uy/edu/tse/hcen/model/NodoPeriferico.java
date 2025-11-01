package uy.edu.tse.hcen.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import uy.edu.tse.hcen.business.model.EstadoNodoPeriferico;
import uy.edu.tse.hcen.business.model.enums.Departamentos;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class NodoPeriferico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Atributo de NodoPeriferico

    @Column(nullable = false)
    private String nombre; // Atributo de NodoPeriferico

    @Column(nullable = false, unique = true)
    private String rut; // Atributo de NodoPeriferico

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Departamentos departamento; // Atributo de NodoPeriferico

    private String localidad; // Atributo de NodoPeriferico

    @Column(nullable = false)
    private String direccion; // Atributo de NodoPeriferico

    private String contacto; // Atributo de NodoPeriferico

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoNodoPeriferico estado; // Atributo de NodoPeriferico

    @OneToOne(mappedBy = "nodoPeriferico", cascade = CascadeType.ALL)
    private ConfiguracionClinica configuracion;

    protected NodoPeriferico() {}

    protected NodoPeriferico(String nombre, String rut, Departamentos departamento, String localidad, String direccion, String contacto, EstadoNodoPeriferico estado) {
        this.nombre = nombre;
        this.rut = rut;
        this.departamento = departamento;
        this.localidad = localidad;
        this.direccion = direccion;
        this.contacto = contacto;
        this.estado = estado;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getRut() { return rut; }
    public void setRut(String rut) { this.rut = rut; }

    public Departamentos getDepartamento() { return departamento; }
    public void setDepartamento(Departamentos departamento) { this.departamento = departamento; }

    public String getLocalidad() { return localidad; }
    public void setLocalidad(String localidad) { this.localidad = localidad; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getContacto() { return contacto; }
    public void setContacto(String contacto) { this.contacto = contacto; }

    public EstadoNodoPeriferico getEstado() { return estado; }
    public void setEstado(EstadoNodoPeriferico estado) { this.estado = estado; }

    public ConfiguracionClinica getConfiguracion() { return configuracion; }
    public void setConfiguracion(ConfiguracionClinica configuracion) { this.configuracion = configuracion; }
}
