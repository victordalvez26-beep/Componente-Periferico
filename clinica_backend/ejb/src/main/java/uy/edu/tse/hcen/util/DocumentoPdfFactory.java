package uy.edu.tse.hcen.util;

import org.bson.Document;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Helper para generar PDFs on-demand a partir de documentos clínicos almacenados en MongoDB.
 */
public final class DocumentoPdfFactory {

    private static final Logger LOGGER = Logger.getLogger(DocumentoPdfFactory.class.getName());

    private DocumentoPdfFactory() {
    }

    /**
     * Construye un PDF usando el contenido y metadatos guardados en MongoDB.
     *
     * @param documento Registro de MongoDB con campos como contenido, titulo, autor, etc.
     * @return Bytes del PDF generado
     * @throws IOException Si falla la generación del PDF
     */
    public static byte[] generarDesdeDocumento(Document documento) throws IOException {
        if (documento == null) {
            throw new IllegalArgumentException("Documento requerido para generar PDF");
        }

        String contenido = documento.getString("contenido");
        if (contenido == null || contenido.isBlank()) {
            throw new IllegalArgumentException("El documento no contiene texto para generar el PDF");
        }

        String titulo = documento.getString("titulo");
        if (titulo == null || titulo.isBlank()) {
            titulo = documento.getString("tipoDocumento");
        }
        if (titulo == null || titulo.isBlank()) {
            titulo = "Documento Clínico";
        }

        String autor = documento.getString("autor");
        if (autor == null || autor.isBlank()) {
            autor = documento.getString("profesionalId");
        }
        if (autor == null || autor.isBlank()) {
            autor = "HCEN";
        }

        String ciPaciente = documento.getString("ciPaciente");
        LOGGER.fine(String.format("Generando PDF on-demand. Título: %s, Autor: %s, Paciente: %s",
                titulo, autor, ciPaciente));

        return PdfGenerator.textoAPdf(contenido, titulo, autor, ciPaciente);
    }
}

