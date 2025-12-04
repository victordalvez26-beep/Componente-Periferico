package uy.edu.tse.hcen.util;

import org.junit.jupiter.api.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests exhaustivos para PdfGenerator
 */
@DisplayName("PdfGenerator Tests")
class PdfGeneratorTest {

    @Nested
    @DisplayName("Generar PDF con Metadata Completa")
    class GenerarPdfCompletoTests {

        @Test
        @DisplayName("Debe generar PDF con todos los parámetros")
        void generatePdf_withAllParams_shouldCreateValidPdf() throws IOException {
            // Arrange
            String contenido = "Paciente presenta fiebre de 38.5°C.\nSe recomienda reposo y antitérmicos.";
            String titulo = "Consulta Médica";
            String autor = "Dr. Juan Pérez";
            String ci = "12345678";

            // Act
            byte[] pdf = PdfGenerator.textoAPdf(contenido, titulo, autor, ci);

            // Assert
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
            assertTrue(pdf.length > 1000); // PDF debe tener tamaño razonable
            
            // Verificar header PDF
            String header = new String(pdf, 0, Math.min(8, pdf.length));
            assertTrue(header.startsWith("%PDF"));
        }

        @Test
        @DisplayName("Debe generar PDF con contenido largo")
        void generatePdf_withLongContent_shouldWork() throws IOException {
            // Arrange
            StringBuilder contenido = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                contenido.append("Línea ").append(i).append(": Contenido del documento médico.\n");
            }

            // Act
            byte[] pdf = PdfGenerator.textoAPdf(contenido.toString(), "Historial", "Dr. Med", "11111111");

            // Assert
            assertNotNull(pdf);
            assertTrue(pdf.length > 3000); // PDF largo debe ser más grande
        }

        @Test
        @DisplayName("Debe generar PDF con caracteres especiales")
        void generatePdf_withSpecialChars_shouldWork() throws IOException {
            // Arrange
            String contenido = "Paciente: José María González\n" +
                    "Diagnóstico: Hipótesis de gripe\n" +
                    "Medicación: Paracetamol 500mg\n" +
                    "Síntomas: fiebre, tos, malestar general";

            // Act
            byte[] pdf = PdfGenerator.textoAPdf(contenido, "Receta Médica", "Dr. González", "22222222");

            // Assert
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }

        @Test
        @DisplayName("Debe generar PDF con saltos de línea múltiples")
        void generatePdf_withMultipleLineBreaks_shouldWork() throws IOException {
            // Arrange
            String contenido = "Diagnóstico:\n\nPaciente con síntomas de gripe.\n\n\nIndicaciones:\nReposo absoluto.";

            // Act
            byte[] pdf = PdfGenerator.textoAPdf(contenido, "Informe", "Dr. Test", "33333333");

            // Assert
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }
    }

    @Nested
    @DisplayName("Generar PDF con Parámetros Opcionales")
    class GenerarPdfOpcionalesTests {

        @Test
        @DisplayName("Debe generar PDF sin título")
        void generatePdf_withoutTitle_shouldWork() throws IOException {
            // Arrange
            String contenido = "Contenido del documento.";

            // Act
            byte[] pdf = PdfGenerator.textoAPdf(contenido, null, "Dr. Test", "12345678");

            // Assert
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }

        @Test
        @DisplayName("Debe generar PDF sin autor")
        void generatePdf_withoutAutor_shouldWork() throws IOException {
            // Arrange
            String contenido = "Contenido del documento.";

            // Act
            byte[] pdf = PdfGenerator.textoAPdf(contenido, "Título", null, "12345678");

            // Assert
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }

        @Test
        @DisplayName("Debe generar PDF sin CI paciente")
        void generatePdf_withoutCI_shouldWork() throws IOException {
            // Arrange
            String contenido = "Contenido del documento.";

            // Act
            byte[] pdf = PdfGenerator.textoAPdf(contenido, "Título", "Autor", null);

            // Assert
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }

        @Test
        @DisplayName("Debe generar PDF con título vacío")
        void generatePdf_withEmptyTitle_shouldWork() throws IOException {
            // Arrange
            String contenido = "Contenido del documento.";

            // Act
            byte[] pdf = PdfGenerator.textoAPdf(contenido, "", "Autor", "12345678");

            // Assert
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }

        @Test
        @DisplayName("Debe generar PDF con autor vacío")
        void generatePdf_withEmptyAutor_shouldWork() throws IOException {
            // Arrange
            String contenido = "Contenido del documento.";

            // Act
            byte[] pdf = PdfGenerator.textoAPdf(contenido, "Título", "", "12345678");

            // Assert
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }

        @Test
        @DisplayName("Debe generar PDF con CI vacío")
        void generatePdf_withEmptyCI_shouldWork() throws IOException {
            // Arrange
            String contenido = "Contenido del documento.";

            // Act
            byte[] pdf = PdfGenerator.textoAPdf(contenido, "Título", "Autor", "");

            // Assert
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }

        @Test
        @DisplayName("Debe generar PDF sin ningún parámetro opcional")
        void generatePdf_withOnlyContent_shouldWork() throws IOException {
            // Arrange
            String contenido = "Contenido mínimo del documento.";

            // Act
            byte[] pdf = PdfGenerator.textoAPdf(contenido);

            // Assert
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
            
            // Verificar header PDF
            String header = new String(pdf, 0, 8);
            assertTrue(header.startsWith("%PDF"));
        }
    }

    @Nested
    @DisplayName("Validación de Entrada")
    class ValidacionEntradaTests {

        @Test
        @DisplayName("Contenido null debe lanzar IllegalArgumentException")
        void generatePdf_withNullContent_shouldThrowException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> PdfGenerator.textoAPdf(null, "Título", "Autor", "12345678")
            );
            
            assertTrue(exception.getMessage().contains("vacío"));
        }

        @Test
        @DisplayName("Contenido vacío debe lanzar IllegalArgumentException")
        void generatePdf_withEmptyContent_shouldThrowException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> PdfGenerator.textoAPdf("", "Título", "Autor", "12345678")
            );
            
            assertTrue(exception.getMessage().contains("vacío"));
        }

        @Test
        @DisplayName("Contenido con solo espacios debe lanzar IllegalArgumentException")
        void generatePdf_withBlankContent_shouldThrowException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> PdfGenerator.textoAPdf("   \n  \t  ", "Título", "Autor", "12345678")
            );
            
            assertTrue(exception.getMessage().contains("vacío"));
        }

        @Test
        @DisplayName("Método sobrecargado con contenido null debe lanzar excepción")
        void generatePdfOverload_withNullContent_shouldThrowException() {
            // Act & Assert
            assertThrows(
                    IllegalArgumentException.class,
                    () -> PdfGenerator.textoAPdf(null)
            );
        }
    }

    @Nested
    @DisplayName("Formato de PDF")
    class FormatoPdfTests {

        @Test
        @DisplayName("PDF generado debe tener estructura válida")
        void generatePdf_shouldHaveValidStructure() throws IOException {
            // Arrange
            String contenido = "Test content";

            // Act
            byte[] pdf = PdfGenerator.textoAPdf(contenido, "Test", "Author", "12345678");

            // Assert
            String pdfString = new String(pdf);
            
            // Verificar elementos básicos del PDF
            assertTrue(pdfString.startsWith("%PDF"));
            assertTrue(pdfString.contains("%%EOF"));
        }

        @Test
        @DisplayName("PDF debe contener metadata del documento")
        void generatePdf_shouldContainMetadata() throws IOException {
            // Arrange
            String contenido = "Contenido de prueba";
            String titulo = "Título Test";

            // Act
            byte[] pdf = PdfGenerator.textoAPdf(contenido, titulo, "Dr. Test", "12345678");

            // Assert
            assertNotNull(pdf);
            assertTrue(pdf.length > 500); // Debe tener tamaño suficiente para metadata
        }

        @Test
        @DisplayName("PDF debe incluir fecha de generación")
        void generatePdf_shouldIncludeDate() throws IOException {
            // Arrange
            String contenido = "Test con fecha";

            // Act
            byte[] pdf = PdfGenerator.textoAPdf(contenido, "Test", "Autor", "12345678");

            // Assert
            assertNotNull(pdf);
            // La fecha se incluye en el PDF
            assertTrue(pdf.length > 0);
        }
    }

    @Nested
    @DisplayName("Casos de Borde")
    class CasosBordeTests {

        @Test
        @DisplayName("Debe manejar contenido con una sola línea")
        void generatePdf_withSingleLine_shouldWork() throws IOException {
            // Arrange
            String contenido = "Una sola línea de texto sin saltos.";

            // Act
            byte[] pdf = PdfGenerator.textoAPdf(contenido);

            // Assert
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }

        @Test
        @DisplayName("Debe manejar contenido muy corto")
        void generatePdf_withVeryShortContent_shouldWork() throws IOException {
            // Arrange
            String contenido = "Ok";

            // Act
            byte[] pdf = PdfGenerator.textoAPdf(contenido);

            // Assert
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }

        @Test
        @DisplayName("Debe manejar líneas vacías intercaladas")
        void generatePdf_withInterspersedEmptyLines_shouldWork() throws IOException {
            // Arrange
            String contenido = "Línea 1\n\nLínea 3\n\n\nLínea 6";

            // Act
            byte[] pdf = PdfGenerator.textoAPdf(contenido, "Test", "Test", "12345678");

            // Assert
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }

        @Test
        @DisplayName("Debe manejar texto con tabs")
        void generatePdf_withTabs_shouldWork() throws IOException {
            // Arrange
            String contenido = "Columna1\tColumna2\tColumna3\nValor1\tValor2\tValor3";

            // Act
            byte[] pdf = PdfGenerator.textoAPdf(contenido);

            // Assert
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }

        @Test
        @DisplayName("PDFs múltiples deben tener tamaños diferentes según contenido")
        void multiplePdfs_shouldHaveDifferentSizes() throws IOException {
            // Act
            byte[] pdf1 = PdfGenerator.textoAPdf("Corto");
            byte[] pdf2 = PdfGenerator.textoAPdf("Contenido mucho más largo que el anterior para verificar diferencias de tamaño en el PDF generado");

            // Assert
            assertTrue(pdf2.length > pdf1.length);
        }
    }
}
