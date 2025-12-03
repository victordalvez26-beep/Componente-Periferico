package uy.edu.tse.hcen.dto;

import java.io.Serializable;

public class ConfiguracionPortalDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String colorPrimario;
    private String colorSecundario;
    private String logoUrl;
    private String nombrePortal;

    public String getColorPrimario() {
        return colorPrimario;
    }
    public void setColorPrimario(String colorPrimario) {
        this.colorPrimario = colorPrimario;
    }
    public String getColorSecundario() {
        return colorSecundario;
    }
    public void setColorSecundario(String colorSecundario) {
        this.colorSecundario = colorSecundario;
    }
    public String getLogoUrl() {
        return logoUrl;
    }
    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }
    public String getNombrePortal() {
        return nombrePortal;
    }
    public void setNombrePortal(String nombrePortal) {
        this.nombrePortal = nombrePortal;
    }

}