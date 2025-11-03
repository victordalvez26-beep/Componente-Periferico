package uy.edu.tse.hcen.dto;

import java.time.LocalDateTime;

/**
 * DTO used for authorization and routing metadata.
 */
public class DTMetadatos {

    // Campos necesarios para la autorización y el enrutamiento
    private String tenantId; // ID del Prestador/Clínica (Para Multi-tenancy)
    private String documentoId;
    private String pacienteCI;
    private String especialidad;
    private LocalDateTime fechaCreacion;
    private String urlAcceso; // URL interna para que el Central pueda acceder al contenido completo.

    public DTMetadatos() {
    }

    // getters / setters
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getDocumentoId() { return documentoId; }
    public void setDocumentoId(String documentoId) { this.documentoId = documentoId; }

    public String getPacienteCI() { return pacienteCI; }
    public void setPacienteCI(String pacienteCI) { this.pacienteCI = pacienteCI; }

    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getUrlAcceso() { return urlAcceso; }
    public void setUrlAcceso(String urlAcceso) { this.urlAcceso = urlAcceso; }
}
