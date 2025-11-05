package uy.edu.tse.hcen.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.util.Date;
import uy.edu.tse.hcen.model.enums.TipoDocumentoClinico;

@Entity
public class DocumentoClinico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;

    @Enumerated(EnumType.STRING)
    private TipoDocumentoClinico tipo;

    private String formato;

    private Date fechaCreacion;

    private String urlAcceso;

    @Column(length = 2000)
    private String metadatos; //VER

    @ManyToOne(fetch = FetchType.LAZY)
    private HistoriaClinica historia;

    public DocumentoClinico() { }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public TipoDocumentoClinico getTipo() { return tipo; }
    public void setTipo(TipoDocumentoClinico tipo) { this.tipo = tipo; }

    public String getFormato() { return formato; }
    public void setFormato(String formato) { this.formato = formato; }

    public Date getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Date fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getUrlAcceso() { return urlAcceso; }
    public void setUrlAcceso(String urlAcceso) { this.urlAcceso = urlAcceso; }

    public String getMetadatos() { return metadatos; }
    public void setMetadatos(String metadatos) { this.metadatos = metadatos; }

    public HistoriaClinica getHistoria() { return historia; }
    public void setHistoria(HistoriaClinica historia) { this.historia = historia; }
}
