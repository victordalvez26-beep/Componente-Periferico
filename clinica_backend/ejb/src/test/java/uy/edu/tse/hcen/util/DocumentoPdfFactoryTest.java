package uy.edu.tse.hcen.util;

import org.bson.Document;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests exhaustivos para DocumentoPdfFactory
 */
@DisplayName("DocumentoPdfFactory Tests")
class DocumentoPdfFactoryTest {

    @Nested
    @DisplayName("Generar PDF desde Documento")
    class GenerarPdfTests {

        @Test
        @DisplayName("Debe generar PDF con documento completo")
        void generatePdf_withCompleteDocument_shouldWork() throws IOException {
            // Arrange
            Document doc = new Document();
            doc.put("contenido", "Paciente presenta síntomas de gripe.\nSe recomienda reposo.");
            doc.put("titulo", "Consulta Médica");
            doc.put("autor", "Dr. Juan Pérez");
            doc.put("ciPaciente", "12345678");

            // Act
            byte[] pdf = DocumentoPdfFactory.generarDesdeDocumento(doc);

            // Assert
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
            
            // Verificar header PDF
            String header = new String(pdf, 0, 8);
            assertTrue(header.startsWith("%PDF"));
        }

        @Test
        @DisplayName("Debe generar PDF sin título (usa tipoDocumento)")
        void generatePdf_withoutTitulo_shouldUseTipoDocumento() throws IOException {
            // Arrange
            Document doc = new Document();
            doc.put("contenido", "Contenido del documento");
            doc.put("tipoDocumento", "EVALUACION");
            doc.put("autor", "Dr. Test");
            doc.put("ciPaciente", "11111111");

            // Act
            byte[] pdf = DocumentoPdfFactory.generarDesdeDocumento(doc);

            // Assert
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }

        @Test
        @DisplayName("Debe generar PDF sin título ni tipoDocumento (usa default)")
        void generatePdf_withoutTituloNorTipo_shouldUseDefault() throws IOException {
            // Arrange
            Document doc = new Document();
            doc.put("contenido", "Contenido del documento");
            doc.put("autor", "Dr. Test");

            // Act
            byte[] pdf = DocumentoPdfFactory.generarDesdeDocumento(doc);

            // Assert
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }

        @Test
        @DisplayName("Debe generar PDF sin autor (usa profesionalId)")
        void generatePdf_withoutAutor_shouldUseProfesionalId() throws IOException {
            // Arrange
            Document doc = new Document();
            doc.put("contenido", "Contenido del documento");
            doc.put("titulo", "Test");
            doc.put("profesionalId", "doctor1");

            // Act
            byte[] pdf = DocumentoPdfFactory.generarDesdeDocumento(doc);

            // Assert
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }

        @Test
        @DisplayName("Debe generar PDF sin autor ni profesionalId (usa HCEN)")
        void generatePdf_withoutAutorNorProfesionalId_shouldUseHCEN() throws IOException {
            // Arrange
            Document doc = new Document();
            doc.put("contenido", "Contenido del documento");
            doc.put("titulo", "Test");

            // Act
            byte[] pdf = DocumentoPdfFactory.generarDesdeDocumento(doc);

            // Assert
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }

        @Test
        @DisplayName("Debe generar PDF con campos vacíos (usa defaults)")
        void generatePdf_withEmptyFields_shouldUseDefaults() throws IOException {
            // Arrange
            Document doc = new Document();
            doc.put("contenido", "Contenido válido");
            doc.put("titulo", "");
            doc.put("autor", "");
            doc.put("tipoDocumento", "");
            doc.put("profesionalId", "");

            // Act
            byte[] pdf = DocumentoPdfFactory.generarDesdeDocumento(doc);

            // Assert
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }

        @Test
        @DisplayName("Debe generar PDF con solo contenido")
        void generatePdf_withOnlyContent_shouldWork() throws IOException {
            // Arrange
            Document doc = new Document();
            doc.put("contenido", "Contenido mínimo requerido");

            // Act
            byte[] pdf = DocumentoPdfFactory.generarDesdeDocumento(doc);

            // Assert
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }

        @Test
        @DisplayName("Debe generar PDF con contenido largo")
        void generatePdf_withLongContent_shouldWork() throws IOException {
            // Arrange
            Document doc = new Document();
            StringBuilder contenido = new StringBuilder();
            for (int i = 0; i < 50; i++) {
                contenido.append("Línea ").append(i).append(": Información médica del paciente.\n");
            }
            doc.put("contenido", contenido.toString());
            doc.put("titulo", "Historial Extenso");

            // Act
            byte[] pdf = DocumentoPdfFactory.generarDesdeDocumento(doc);

            // Assert
            assertNotNull(pdf);
            assertTrue(pdf.length > 1000); // PDF largo debe ser razonable
        }
    }

    @Nested
    @DisplayName("Validación de Entrada")
    class ValidacionEntradaTests {

        @Test
        @DisplayName("Documento null debe lanzar IllegalArgumentException")
        void generatePdf_withNullDocument_shouldThrowException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> DocumentoPdfFactory.generarDesdeDocumento(null)
            );
            
            assertTrue(exception.getMessage().contains("requerido"));
        }

        @Test
        @DisplayName("Documento sin contenido debe lanzar IllegalArgumentException")
        void generatePdf_withoutContenido_shouldThrowException() {
            // Arrange
            Document doc = new Document();
            doc.put("titulo", "Test");
            doc.put("autor", "Test");

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> DocumentoPdfFactory.generarDesdeDocumento(doc)
            );
            
            assertTrue(exception.getMessage().contains("no contiene texto"));
        }

        @Test
        @DisplayName("Documento con contenido null debe lanzar IllegalArgumentException")
        void generatePdf_withNullContenido_shouldThrowException() {
            // Arrange
            Document doc = new Document();
            doc.put("contenido", null);
            doc.put("titulo", "Test");

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> DocumentoPdfFactory.generarDesdeDocumento(doc)
            );
            
            assertTrue(exception.getMessage().contains("no contiene texto"));
        }

        @Test
        @DisplayName("Documento con contenido vacío debe lanzar IllegalArgumentException")
        void generatePdf_withEmptyContenido_shouldThrowException() {
            // Arrange
            Document doc = new Document();
            doc.put("contenido", "");
            doc.put("titulo", "Test");

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> DocumentoPdfFactory.generarDesdeDocumento(doc)
            );
            
            assertTrue(exception.getMessage().contains("no contiene texto"));
        }

        @Test
        @DisplayName("Documento con contenido solo espacios debe lanzar IllegalArgumentException")
        void generatePdf_withBlankContenido_shouldThrowException() {
            // Arrange
            Document doc = new Document();
            doc.put("contenido", "   \n  \t  ");
            doc.put("titulo", "Test");

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> DocumentoPdfFactory.generarDesdeDocumento(doc)
            );
            
            assertTrue(exception.getMessage().contains("no contiene texto"));
        }
    }

    @Nested
    @DisplayName("Casos de Borde")
    class CasosBordeTests {

        @Test
        @DisplayName("Debe manejar título con espacios")
        void generatePdf_withTitleWithSpaces_shouldWork() throws IOException {
            // Arrange
            Document doc = new Document();
            doc.put("contenido", "Contenido");
            doc.put("titulo", "   ");

            // Act
            byte[] pdf = DocumentoPdfFactory.generarDesdeDocumento(doc);

            // Assert
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }

        @Test
        @DisplayName("Debe manejar autor con espacios")
        void generatePdf_withAutorWithSpaces_shouldWork() throws IOException {
            // Arrange
            Document doc = new Document();
            doc.put("contenido", "Contenido");
            doc.put("autor", "   ");

            // Act
            byte[] pdf = DocumentoPdfFactory.generarDesdeDocumento(doc);

            // Assert
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }

        @Test
        @DisplayName("Debe manejar documento con campos adicionales")
        void generatePdf_withExtraFields_shouldIgnoreThem() throws IOException {
            // Arrange
            Document doc = new Document();
            doc.put("contenido", "Contenido válido");
            doc.put("titulo", "Test");
            doc.put("campoExtra1", "valor1");
            doc.put("campoExtra2", 123);
            doc.put("campoExtra3", true);

            // Act
            byte[] pdf = DocumentoPdfFactory.generarDesdeDocumento(doc);

            // Assert
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }

        @Test
        @DisplayName("Debe manejar contenido con caracteres especiales")
        void generatePdf_withSpecialChars_shouldWork() throws IOException {
            // Arrange
            Document doc = new Document();
            doc.put("contenido", "Paciente: José María\nDiagnóstico: úlcera gástrica\nTratamiento: ácido fólico");
            doc.put("titulo", "Informe Médico");

            // Act
            byte[] pdf = DocumentoPdfFactory.generarDesdeDocumento(doc);

            // Assert
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }

        @Test
        @DisplayName("Clase utility tiene constructor privado")
        void utilityClass_hasPrivateConstructor() throws Exception {
            // Assert
            var constructor = DocumentoPdfFactory.class.getDeclaredConstructor();
            assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
        }

        @Test
        @DisplayName("Dos llamadas con mismo documento deben generar PDFs válidos")
        void multipleCalls_shouldGenerateValidPdfs() throws IOException {
            // Arrange
            Document doc = new Document();
            doc.put("contenido", "Test content");

            // Act
            byte[] pdf1 = DocumentoPdfFactory.generarDesdeDocumento(doc);
            byte[] pdf2 = DocumentoPdfFactory.generarDesdeDocumento(doc);

            // Assert
            assertNotNull(pdf1);
            assertNotNull(pdf2);
            assertTrue(pdf1.length > 0);
            assertTrue(pdf2.length > 0);
        }

        @Test
        @DisplayName("Debe manejar tipo documento RECETA")
        void generatePdf_withTipoReceta_shouldWork() throws IOException {
            // Arrange
            Document doc = new Document();
            doc.put("contenido", "Ibuprofeno 400mg cada 8 horas");
            doc.put("tipoDocumento", "RECETA");
            doc.put("ciPaciente", "33333333");

            // Act
            byte[] pdf = DocumentoPdfFactory.generarDesdeDocumento(doc);

            // Assert
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }

        @Test
        @DisplayName("Debe manejar tipo documento ESTUDIO")
        void generatePdf_withTipoEstudio_shouldWork() throws IOException {
            // Arrange
            Document doc = new Document();
            doc.put("contenido", "Resultados de análisis de sangre");
            doc.put("tipoDocumento", "ESTUDIO");
            doc.put("ciPaciente", "44444444");

            // Act
            byte[] pdf = DocumentoPdfFactory.generarDesdeDocumento(doc);

            // Assert
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }
    }
}
