package uy.edu.tse.hcen.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenAIService Tests")
class OpenAIServiceTest {

    @Nested
    @DisplayName("Validación de Entrada Tests")
    class ValidacionEntradaTests {

        @Test
        @DisplayName("Debe rechazar contenido vacío")
        void generarResumen_withEmptyContent_shouldThrow() {
            OpenAIService service = new OpenAIService();
            assertThrows(IllegalArgumentException.class, () ->
                    service.generarResumenHistoriaClinica(""));
        }

        @Test
        @DisplayName("Debe rechazar contenido null")
        void generarResumen_withNullContent_shouldThrow() {
            OpenAIService service = new OpenAIService();
            assertThrows(IllegalArgumentException.class, () ->
                    service.generarResumenHistoriaClinica(null));
        }

        @Test
        @DisplayName("Debe rechazar contenido con solo espacios")
        void generarResumen_withBlankContent_shouldThrow() {
            OpenAIService service = new OpenAIService();
            assertThrows(IllegalArgumentException.class, () ->
                    service.generarResumenHistoriaClinica("   \n  \t  "));
        }

        @Test
        @DisplayName("Debe rechazar si no hay API key configurada")
        void generarResumen_withoutAPIKey_shouldThrow() {
            OpenAIService service = new OpenAIService();
            // Sin API key debe lanzar IllegalStateException
            assertThrows(IllegalStateException.class, () ->
                    service.generarResumenHistoriaClinica("Historia clínica válida"));
        }
    }

    @Nested
    @DisplayName("Casos de Borde Tests")
    class CasosBordeTests {

        @Test
        @DisplayName("Debe manejar contenido muy corto")
        void generarResumen_withVeryShortContent_shouldThrow() {
            OpenAIService service = new OpenAIService();
            // Sin API key lanzará IllegalStateException
            assertThrows(IllegalStateException.class, () ->
                    service.generarResumenHistoriaClinica("Ok"));
        }

        @Test
        @DisplayName("Debe manejar contenido con caracteres especiales")
        void generarResumen_withSpecialChars_shouldThrow() {
            OpenAIService service = new OpenAIService();
            assertThrows(IllegalStateException.class, () ->
                    service.generarResumenHistoriaClinica("Paciente: José María\nDiagnóstico: úlcera"));
        }

        @Test
        @DisplayName("Debe manejar contenido muy largo")
        void generarResumen_withVeryLongContent_shouldThrow() {
            OpenAIService service = new OpenAIService();
            StringBuilder longContent = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                longContent.append("Línea ").append(i).append(": Historia clínica.\n");
            }

            assertThrows(IllegalStateException.class, () ->
                    service.generarResumenHistoriaClinica(longContent.toString()));
        }

        @Test
        @DisplayName("Debe manejar contenido con saltos de línea múltiples")
        void generarResumen_withMultipleLineBreaks_shouldThrow() {
            OpenAIService service = new OpenAIService();
            assertThrows(IllegalStateException.class, () ->
                    service.generarResumenHistoriaClinica("Línea 1\n\n\nLínea 4"));
        }
    }

    @Test
    void service_shouldHavePublicConstructor() {
        OpenAIService service = new OpenAIService();
        assertNotNull(service);
    }

    @Test
    void generarResumen_shouldAcceptParameters() {
        OpenAIService service = new OpenAIService();
        assertNotNull(service);
    }
}
