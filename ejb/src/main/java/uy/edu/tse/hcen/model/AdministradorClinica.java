package uy.edu.tse.hcen.model;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;

/**
 * Entidad para el Administrador de Clínica.
 */
@Entity
public class AdministradorClinica extends UsuarioPeriferico {

    // Relación con el NodoPeriferico que administra
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nodo_periferico_id")
    private NodoPeriferico administra;

    protected AdministradorClinica() {
        super();
    }

    public AdministradorClinica(NodoPeriferico administra) {
        this.administra = administra;
    }

    public NodoPeriferico getAdministra() {
        return administra;
    }

    public void setAdministra(NodoPeriferico administra) {
        this.administra = administra;
    }

    /**
     * Obtiene el ID del NodoPeriferico asociado (tenant ID numérico).
     */
    public Long getTenantNodeId() {
        return this.administra != null ? this.administra.getId() : null;
    }
}
