package uy.edu.tse.hcen.util;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilidad para generar documentos PDF a partir de texto.
 */
public class PdfGenerator {

    private static final Logger LOGGER = Logger.getLogger(PdfGenerator.class.getName());

    /**
     * Convierte un texto a PDF.
     * 
     * @param contenido Texto a convertir
     * @param titulo Título del documento (opcional)
     * @param autor Autor del documento (opcional)
     * @param pacienteCI CI del paciente (opcional)
     * @return Array de bytes con el contenido del PDF
     * @throws IOException Si hay un error al generar el PDF
     */
    public static byte[] textoAPdf(String contenido, String titulo, String autor, String pacienteCI) throws IOException {
        if (contenido == null || contenido.isBlank()) {
            throw new IllegalArgumentException("El contenido no puede estar vacío");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Encabezado con información del documento
            if (titulo != null && !titulo.isBlank()) {
                Paragraph tituloPar = new Paragraph(titulo)
                        .setFontSize(18)
                        .setBold()
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(20);
                document.add(tituloPar);
            }

            // Información del documento
            Paragraph info = new Paragraph();
            if (pacienteCI != null && !pacienteCI.isBlank()) {
                info.add(new Text("Paciente CI: " + pacienteCI + "\n"));
            }
            if (autor != null && !autor.isBlank()) {
                info.add(new Text("Autor: " + autor + "\n"));
            }
            info.add(new Text("Fecha: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "\n"));
            info.setFontSize(10)
                    .setMarginBottom(15)
                    .setTextAlignment(TextAlignment.LEFT);
            document.add(info);

            // Línea separadora
            document.add(new Paragraph("─────────────────────────────────────────────────────────")
                    .setFontSize(8)
                    .setMarginBottom(15));

            // Contenido del documento
            // Dividir el contenido en párrafos (por líneas)
            String[] lineas = contenido.split("\n");
            for (String linea : lineas) {
                if (linea != null && !linea.trim().isEmpty()) {
                    Paragraph parrafo = new Paragraph(linea)
                            .setFontSize(12)
                            .setMarginBottom(8);
                    document.add(parrafo);
                } else {
                    // Línea en blanco
                    document.add(new Paragraph(" ").setMarginBottom(8));
                }
            }

            // Pie de página
            document.add(new Paragraph("\n─────────────────────────────────────────────────────────")
                    .setFontSize(8)
                    .setMarginTop(20));
            Paragraph pie = new Paragraph("Documento generado automáticamente por HCEN")
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(5);
            document.add(pie);

            document.close();
            pdf.close();

            byte[] pdfBytes = baos.toByteArray();
            LOGGER.log(Level.INFO, "PDF generado exitosamente. Tamaño: {0} bytes", pdfBytes.length);
            return pdfBytes;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al generar PDF", e);
            throw new IOException("Error al generar PDF: " + e.getMessage(), e);
        } finally {
            try {
                baos.close();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error al cerrar ByteArrayOutputStream", e);
            }
        }
    }

    /**
     * Convierte un texto simple a PDF sin metadatos adicionales.
     * 
     * @param contenido Texto a convertir
     * @return Array de bytes con el contenido del PDF
     * @throws IOException Si hay un error al generar el PDF
     */
    public static byte[] textoAPdf(String contenido) throws IOException {
        return textoAPdf(contenido, null, null, null);
    }
}

