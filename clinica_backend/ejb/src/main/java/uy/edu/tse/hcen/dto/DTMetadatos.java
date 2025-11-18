package uy.edu.tse.hcen.dto;

import java.time.LocalDateTime;

/**
 * DTO used for authorization and routing metadata.
 */
public class DTMetadatos {

    // Campos necesarios para la autorización y el enrutamiento
    private String tenantId; // ID del Prestador/Clínica (Para Multi-tenancy)
    private String documentoId;
    private String documentoIdPaciente;
    private String especialidad;
    private LocalDateTime fechaCreacion;
    private String urlAcceso; // URL interna para que el Central pueda acceder al contenido completo.
    private String aaPrestador; // Identificador del Prestador (AA)
    private String emisorDocumentoOID;
    private LocalDateTime fechaRegistro;   // Fecha/hora de registro en HCEN
    private String formato;                // MIME type (ej: "application/pdf", "text/xml")

    private String autor;                 // Profesional o sistema que genera el documento
    private String titulo;                  // Título visible del documento
    private String languageCode;           // Idioma del contenido (ej: "es-UY")
    private boolean breakingTheGlass;      // Flag para emergencias
    private String hashDocumento;          // Hash para verificar integridad
    private String descripcion;            // Descripción libre o resumen
    private String tipoDocumento;          // Tipo de documento clínico (EVALUACION, INFORME, etc.)
    private String datosPatronimicos;       // Nombre completo del paciente

    public DTMetadatos() {
    }

    // getters / setters
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getDocumentoId() { return documentoId; }
    public void setDocumentoId(String documentoId) { this.documentoId = documentoId; }

    public String getDocumentoIdPaciente() { return documentoIdPaciente; }
    public void setDocumentoIdPaciente(String documentoIdPaciente) { this.documentoIdPaciente = documentoIdPaciente; }

    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getUrlAcceso() { return urlAcceso; }
    public void setUrlAcceso(String urlAcceso) { this.urlAcceso = urlAcceso; }

    public String getAaPrestador() { return aaPrestador; }
    public void setAaPrestador(String aaPrestador) { this.aaPrestador = aaPrestador; }

    public String getEmisorDocumentoOID() { return emisorDocumentoOID; }
    public void setEmisorDocumentoOID(String emisorDocumentoOID) { this.emisorDocumentoOID = emisorDocumentoOID; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public String getFormato() { return formato; }
    public void setFormato(String formato) { this.formato = formato; }

    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }

    public boolean isBreakingTheGlass() { return breakingTheGlass; }
    public void setBreakingTheGlass(boolean breakingTheGlass) { this.breakingTheGlass = breakingTheGlass; }

    public String getHashDocumento() { return hashDocumento; }
    public void setHashDocumento(String hashDocumento) { this.hashDocumento = hashDocumento; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public String getDatosPatronimicos() { return datosPatronimicos; }
    public void setDatosPatronimicos(String datosPatronimicos) { this.datosPatronimicos = datosPatronimicos; }
}
