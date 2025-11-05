package uy.edu.tse.hcen.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Entity
public class HistoriaClinica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    
    // One HistoriaClinica owns many DocumentoClinico entities
    @OneToMany(mappedBy = "historia", orphanRemoval = true)
    private List<DocumentoClinico> documentos = new ArrayList<>();

    public HistoriaClinica() { }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public List<DocumentoClinico> getDocumentos() { return documentos; }
    public void setDocumentos(List<DocumentoClinico> documentos) { this.documentos = documentos; }

    public void addDocumento(DocumentoClinico d) {
        documentos.add(d);
        d.setHistoria(this);
    }

    public void removeDocumento(DocumentoClinico d) {
        documentos.remove(d);
        d.setHistoria(null);
    }
}
