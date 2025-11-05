package uy.edu.tse.hcen.dto;

import uy.edu.tse.hcen.model.RegistroAcceso;
import java.util.Date;

public class DTRegistroAcceso {
    private Long id;
    private Date fecha;
    private String referencia;

    public DTRegistroAcceso() {}

    public static DTRegistroAcceso fromEntity(RegistroAcceso r) {
        if (r == null) return null;
        DTRegistroAcceso d = new DTRegistroAcceso();
        d.id = r.getId();
        d.fecha = r.getFecha();
        d.referencia = r.getReferencia();
        return d;
    }

    public RegistroAcceso toEntity() {
        RegistroAcceso r = new RegistroAcceso();
        r.setFecha(this.fecha);
        r.setReferencia(this.referencia);
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
}
