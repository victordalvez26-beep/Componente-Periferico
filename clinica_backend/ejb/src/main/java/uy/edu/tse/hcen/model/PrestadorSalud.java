package uy.edu.tse.hcen.model;

import jakarta.persistence.Entity;

@Entity
public class PrestadorSalud extends NodoPeriferico {

    protected PrestadorSalud() { super(); }

    public PrestadorSalud(String nombre, String rut, uy.edu.tse.hcen.business.model.enums.Departamentos departamento, String localidad, String direccion, String contacto, uy.edu.tse.hcen.business.model.EstadoNodoPeriferico estado) {
        super(nombre, rut, departamento, localidad, direccion, contacto, estado);
    }
}
