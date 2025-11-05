package uy.edu.tse.hcen.dto;

import uy.edu.tse.hcen.model.HistoriaClinica;
import java.util.List;
import java.util.stream.Collectors;

public class DTHistoriaClinica {
    private Long id;
    private List<DTDocumentoClinico> documentos;

    public DTHistoriaClinica() {}

    public static DTHistoriaClinica fromEntity(HistoriaClinica h) {
        if (h == null) return null;
        DTHistoriaClinica d = new DTHistoriaClinica();
        d.id = h.getId();
        d.documentos = h.getDocumentos().stream().map(DTDocumentoClinico::fromEntity).collect(Collectors.toList());
        return d;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public List<DTDocumentoClinico> getDocumentos() { return documentos; }
    public void setDocumentos(List<DTDocumentoClinico> documentos) { this.documentos = documentos; }
}
