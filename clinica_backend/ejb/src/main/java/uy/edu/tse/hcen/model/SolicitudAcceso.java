package uy.edu.tse.hcen.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.Date;
import uy.edu.tse.hcen.model.enums.EstadoSolicitudAcceso;

@Entity
public class SolicitudAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Date fechaSolicitud;

    @Enumerated(EnumType.STRING)
    private EstadoSolicitudAcceso estado;

    @Column
    private String solicitanteId;

    @Column
    private String especialidad;

    public SolicitudAcceso() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Date getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(Date fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }

    public EstadoSolicitudAcceso getEstado() { return estado; }
    public void setEstado(EstadoSolicitudAcceso estado) { this.estado = estado; }

    public String getSolicitanteId() { return solicitanteId; }
    public void setSolicitanteId(String solicitanteId) { this.solicitanteId = solicitanteId; }

    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }
}
