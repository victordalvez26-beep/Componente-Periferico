package uy.edu.tse.hcen.model.enums;

/**
 * Enum que representa los tipos de documentos clínicos.
 * Replica los valores del backend central HCEN para consistencia.
 */
public enum TipoDocumento {

    RESUMEN_ALTA("Resumen de Alta"),
    INFORME_LABORATORIO("Informe de Laboratorio"),
    RADIOGRAFIA("Radiografía"),
    RECETA_MEDICA("Receta Médica"),
    CONSULTA_MEDICA("Consulta Médica"),
    CIRUGIA("Informe Quirúrgico"),
    ESTUDIO_IMAGENOLOGIA("Estudio de Imagenología"),
    ELECTROCARDIOGRAMA("Electrocardiograma"),
    INFORME_PATOLOGIA("Informe de Patología"),
    VACUNACION("Vacunación"),
    OTROS("Otros");

    private final String descripcion;

    TipoDocumento(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}

