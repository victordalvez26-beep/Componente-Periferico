package uy.edu.tse.hcen.dto;

import uy.edu.tse.hcen.model.SolicitudAcceso;
import uy.edu.tse.hcen.model.enums.EstadoSolicitudAcceso;
import java.util.Date;

public class DTSolicitudAcceso {
    private Long id;
    private Date fechaSolicitud;
    private String estado;
    private String solicitanteId;
    private String especialidad;

    public DTSolicitudAcceso() {}

    public static DTSolicitudAcceso fromEntity(SolicitudAcceso s) {
        if (s == null) return null;
        DTSolicitudAcceso d = new DTSolicitudAcceso();
        d.id = s.getId();
        d.fechaSolicitud = s.getFechaSolicitud();
        d.estado = s.getEstado() != null ? s.getEstado().name() : null;
        d.solicitanteId = s.getSolicitanteId();
        d.especialidad = s.getEspecialidad();
        return d;
    }

    public SolicitudAcceso toEntity() {
        SolicitudAcceso s = new SolicitudAcceso();
        s.setFechaSolicitud(this.fechaSolicitud);
        if (this.estado != null) {
            try { s.setEstado(EstadoSolicitudAcceso.valueOf(this.estado)); } catch (Exception e) { /* ignore */ }
        }
        s.setSolicitanteId(this.solicitanteId);
        s.setEspecialidad(this.especialidad);
        return s;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Date getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(Date fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getSolicitanteId() { return solicitanteId; }
    public void setSolicitanteId(String solicitanteId) { this.solicitanteId = solicitanteId; }
    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }
}
