package uy.edu.tse.hcen.model;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "portal_configuracion")
public class PortalConfiguracion implements Serializable {

    // Se recomienda usar un ID fijo (e.g., 1L) ya que solo habrá una fila.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 

    // Colores
    @Column(name = "color_primario", length = 7) // Formato HEX: #RRGGBB
    private String colorPrimario = "#007bff";

    @Column(name = "color_secundario", length = 7)
    private String colorSecundario = "#6c757d";

    // Logo y favicon
    @Column(name = "logo_url", length = 512)
    private String logoUrl; // URL del logo alojado en CDN o sistema de archivos

    @Column(name = "nombre_portal", length = 100)
    private String nombrePortal; // Nombre corto de la clínica para el título

    public PortalConfiguracion() {
        // Constructor privado para evitar la instanciación directa
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
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