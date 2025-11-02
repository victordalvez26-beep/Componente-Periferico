package uy.edu.tse.hcen.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Column;

@Entity
public class ConfiguracionClinica {

    @Id
    @Column(name = "nodo_periferico_id")
    private Long id; // Mismo ID que el NodoPeriferico asociado (mapeado a nodo_periferico_id)

    @OneToOne
    @MapsId
    @JoinColumn(name = "nodo_periferico_id")
    private NodoPeriferico nodoPeriferico;

    @Column(name = "logourl")
    private String logoUrl; // Atributo de ConfiguracionClinica (mapeado a logourl)

    @Column(name = "colorprincipal")
    private String colorPrincipal; // Atributo de ConfiguracionClinica (mapeado a colorprincipal)

    @Column(name = "habilitado")
    private boolean habilitado; // Atributo de ConfiguracionClinica (mapeado a habilitado)

    public ConfiguracionClinica() {}

    public ConfiguracionClinica(NodoPeriferico nodoPeriferico, String logoUrl, String colorPrincipal, boolean habilitado) {
        this.nodoPeriferico = nodoPeriferico;
        this.logoUrl = logoUrl;
        this.colorPrincipal = colorPrincipal;
        this.habilitado = habilitado;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public NodoPeriferico getNodoPeriferico() { return nodoPeriferico; }
    public void setNodoPeriferico(NodoPeriferico nodoPeriferico) { this.nodoPeriferico = nodoPeriferico; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getColorPrincipal() { return colorPrincipal; }
    public void setColorPrincipal(String colorPrincipal) { this.colorPrincipal = colorPrincipal; }

    public boolean isHabilitado() { return habilitado; }
    public void setHabilitado(boolean habilitado) { this.habilitado = habilitado; }
}
