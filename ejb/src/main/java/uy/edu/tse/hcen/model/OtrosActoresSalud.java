package uy.edu.tse.hcen.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import uy.edu.tse.hcen.model.enums.ActorSalud;
import uy.edu.tse.hcen.model.enums.Departamentos;
import uy.edu.tse.hcen.model.enums.EstadoNodoPeriferico;

@Entity(name = "OAS")
public class OtrosActoresSalud extends NodoPeriferico {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActorSalud tipo; // Atributo de OAS

    protected OtrosActoresSalud() { super(); }

    public OtrosActoresSalud(String nombre, String rut, Departamentos departamento, ContactInfo contactInfo, EstadoNodoPeriferico estado, ActorSalud tipo) {
        super(nombre, rut, departamento, contactInfo.getLocalidad(), contactInfo.getDireccion(), contactInfo.getContacto(), estado);
        this.tipo = tipo;
    }

    public ActorSalud getTipo() { return tipo; }
    public void setTipo(ActorSalud tipo) { this.tipo = tipo; }

    
    public static class ContactInfo {
        private final String localidad;
        private final String direccion;
        private final String contacto;

        public ContactInfo(String localidad, String direccion, String contacto) {
            this.localidad = localidad;
            this.direccion = direccion;
            this.contacto = contacto;
        }

        public String getLocalidad() { return localidad; }
        public String getDireccion() { return direccion; }
        public String getContacto() { return contacto; }
    }

}
