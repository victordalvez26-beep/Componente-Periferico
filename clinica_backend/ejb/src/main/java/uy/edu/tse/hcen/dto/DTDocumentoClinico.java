package uy.edu.tse.hcen.dto;

import uy.edu.tse.hcen.model.DocumentoClinico;
import uy.edu.tse.hcen.model.enums.TipoDocumentoClinico;
import java.util.Date;

public class DTDocumentoClinico {
    private Long id;
    private String titulo;
    private String tipo;
    private String formato;
    private Date fechaCreacion;
    private String urlAcceso;
    private String metadatos;

    public DTDocumentoClinico() {}

    public static DTDocumentoClinico fromEntity(DocumentoClinico d) {
        if (d == null) return null;
        DTDocumentoClinico r = new DTDocumentoClinico();
        r.id = d.getId();
        r.titulo = d.getTitulo();
        r.tipo = d.getTipo() != null ? d.getTipo().name() : null;
        r.formato = d.getFormato();
        r.fechaCreacion = d.getFechaCreacion();
        r.urlAcceso = d.getUrlAcceso();
        r.metadatos = d.getMetadatos();
        return r;
    }

    public DocumentoClinico toEntity() {
        DocumentoClinico d = new DocumentoClinico();
        d.setTitulo(this.titulo);
        if (this.tipo != null) {
            try { d.setTipo(TipoDocumentoClinico.valueOf(this.tipo)); } catch (Exception e) { /* ignore invalid */ }
        }
        d.setFormato(this.formato);
        d.setFechaCreacion(this.fechaCreacion);
        d.setUrlAcceso(this.urlAcceso);
        d.setMetadatos(this.metadatos);
        return d;
    }

    // getters / setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getFormato() { return formato; }
    public void setFormato(String formato) { this.formato = formato; }
    public Date getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Date fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public String getUrlAcceso() { return urlAcceso; }
    public void setUrlAcceso(String urlAcceso) { this.urlAcceso = urlAcceso; }
    public String getMetadatos() { return metadatos; }
    public void setMetadatos(String metadatos) { this.metadatos = metadatos; }
}
