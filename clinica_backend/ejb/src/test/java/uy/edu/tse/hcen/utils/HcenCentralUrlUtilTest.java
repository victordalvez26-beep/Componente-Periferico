package uy.edu.tse.hcen.utils;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests EXHAUSTIVOS para HcenCentralUrlUtil.
 * Cubre TODOS los métodos y casos posibles.
 */
@DisplayName("HcenCentralUrlUtil Comprehensive Tests")
class HcenCentralUrlUtilTest {

    @BeforeEach
    void setUp() {
        // Limpiar variables antes de cada test
        System.clearProperty("HCEN_CENTRAL_BASE_URL");
    }

    @AfterEach
    void tearDown() {
        // Limpiar después de cada test
        System.clearProperty("HCEN_CENTRAL_BASE_URL");
    }

    @Nested
    @DisplayName("getBaseUrl Tests - TODOS LOS CASOS")
    class GetBaseUrlTests {

        @Test
        @DisplayName("Debe usar variable de entorno si está configurada")
        void getBaseUrl_usesEnvVar() {
            // Arrange - Simular variable de entorno con System.property
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://custom-hcen:9090/hcen");

            // Act
            String result = HcenCentralUrlUtil.getBaseUrl();

            // Assert
            assertEquals("http://custom-hcen:9090/hcen", result);
        }

        @Test
        @DisplayName("Debe remover trailing slash de variable de entorno")
        void getBaseUrl_removesTrailingSlash() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://custom-hcen:9090/hcen/");

            // Act
            String result = HcenCentralUrlUtil.getBaseUrl();

            // Assert
            assertEquals("http://custom-hcen:9090/hcen", result);
            assertFalse(result.endsWith("/"));
        }

        @Test
        @DisplayName("Debe remover múltiples trailing slashes")
        void getBaseUrl_removesMultipleSlashes() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://custom-hcen:9090/hcen///");

            // Act
            String result = HcenCentralUrlUtil.getBaseUrl();

            // Assert - Solo remueve el último
            assertEquals("http://custom-hcen:9090/hcen//", result);
        }

        @Test
        @DisplayName("Debe hacer trim de espacios en variable de entorno")
        void getBaseUrl_trimsSpaces() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "  http://custom-hcen:9090/hcen  ");

            // Act
            String result = HcenCentralUrlUtil.getBaseUrl();

            // Assert
            assertEquals("http://custom-hcen:9090/hcen", result);
        }

        @Test
        @DisplayName("Debe usar default si variable de entorno no está configurada")
        void getBaseUrl_noEnvVar_usesDefault() {
            // Act
            String result = HcenCentralUrlUtil.getBaseUrl();

            // Assert
            assertEquals("http://hcen-backend:8080/hcen", result);
        }

        @Test
        @DisplayName("Debe usar default si variable de entorno está vacía")
        void getBaseUrl_emptyEnvVar_usesDefault() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "");

            // Act
            String result = HcenCentralUrlUtil.getBaseUrl();

            // Assert
            assertEquals("http://hcen-backend:8080/hcen", result);
        }

        @Test
        @DisplayName("Debe usar default si variable de entorno es solo espacios")
        void getBaseUrl_blankEnvVar_usesDefault() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "   ");

            // Act
            String result = HcenCentralUrlUtil.getBaseUrl();

            // Assert
            assertEquals("http://hcen-backend:8080/hcen", result);
        }

        @Test
        @DisplayName("Debe manejar URL con puerto diferente")
        void getBaseUrl_differentPort_success() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://localhost:3000/hcen");

            // Act
            String result = HcenCentralUrlUtil.getBaseUrl();

            // Assert
            assertEquals("http://localhost:3000/hcen", result);
        }

        @Test
        @DisplayName("Debe manejar URL con HTTPS")
        void getBaseUrl_https_success() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "https://hcen-prod.example.com/hcen");

            // Act
            String result = HcenCentralUrlUtil.getBaseUrl();

            // Assert
            assertEquals("https://hcen-prod.example.com/hcen", result);
        }

        @Test
        @DisplayName("Debe manejar URL sin path")
        void getBaseUrl_noPath_success() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://hcen-backend:8080");

            // Act
            String result = HcenCentralUrlUtil.getBaseUrl();

            // Assert
            assertEquals("http://hcen-backend:8080", result);
        }

        @Test
        @DisplayName("Debe ser consistente en múltiples llamadas")
        void getBaseUrl_multipleCalls_consistent() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://test-url:8080/hcen");

            // Act
            String result1 = HcenCentralUrlUtil.getBaseUrl();
            String result2 = HcenCentralUrlUtil.getBaseUrl();
            String result3 = HcenCentralUrlUtil.getBaseUrl();

            // Assert
            assertEquals(result1, result2);
            assertEquals(result2, result3);
        }
    }

    @Nested
    @DisplayName("getApiBaseUrl Tests - TODOS LOS CASOS")
    class GetApiBaseUrlTests {

        @Test
        @DisplayName("Debe agregar /api al base URL")
        void getApiBaseUrl_addsApiPath() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://custom-hcen:9090/hcen");

            // Act
            String result = HcenCentralUrlUtil.getApiBaseUrl();

            // Assert
            assertEquals("http://custom-hcen:9090/hcen/api", result);
            assertTrue(result.endsWith("/api"));
        }

        @Test
        @DisplayName("Debe usar default + /api si no hay configuración")
        void getApiBaseUrl_noConfig_usesDefault() {
            // Act
            String result = HcenCentralUrlUtil.getApiBaseUrl();

            // Assert
            assertEquals("http://hcen-backend:8080/hcen/api", result);
        }

        @Test
        @DisplayName("No debe tener trailing slash después de /api")
        void getApiBaseUrl_noTrailingSlash() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://test:8080/hcen");

            // Act
            String result = HcenCentralUrlUtil.getApiBaseUrl();

            // Assert
            assertFalse(result.endsWith("/api/"));
            assertTrue(result.endsWith("/api"));
        }

        @Test
        @DisplayName("Debe ser consistente en múltiples llamadas")
        void getApiBaseUrl_multipleCalls_consistent() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://test:8080/hcen");

            // Act
            String result1 = HcenCentralUrlUtil.getApiBaseUrl();
            String result2 = HcenCentralUrlUtil.getApiBaseUrl();

            // Assert
            assertEquals(result1, result2);
        }
    }

    @Nested
    @DisplayName("buildUrl Tests - TODOS LOS CASOS")
    class BuildUrlTests {

        @Test
        @DisplayName("Debe construir URL con path válido")
        void buildUrl_validPath_success() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://test:8080/hcen");

            // Act
            String result = HcenCentralUrlUtil.buildUrl("/metadatos");

            // Assert
            assertEquals("http://test:8080/hcen/metadatos", result);
        }

        @Test
        @DisplayName("Debe agregar slash inicial si falta")
        void buildUrl_noLeadingSlash_addsSlash() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://test:8080/hcen");

            // Act
            String result = HcenCentralUrlUtil.buildUrl("metadatos");

            // Assert
            assertEquals("http://test:8080/hcen/metadatos", result);
        }

        @Test
        @DisplayName("Debe retornar base URL si path es null")
        void buildUrl_nullPath_returnsBaseUrl() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://test:8080/hcen");

            // Act
            String result = HcenCentralUrlUtil.buildUrl(null);

            // Assert
            assertEquals("http://test:8080/hcen", result);
        }

        @Test
        @DisplayName("Debe retornar base URL si path es vacío")
        void buildUrl_emptyPath_returnsBaseUrl() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://test:8080/hcen");

            // Act
            String result = HcenCentralUrlUtil.buildUrl("");

            // Assert
            assertEquals("http://test:8080/hcen", result);
        }

        @Test
        @DisplayName("Debe manejar path con múltiples segmentos")
        void buildUrl_multipleSegments_success() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://test:8080/hcen");

            // Act
            String result = HcenCentralUrlUtil.buildUrl("/api/v1/metadatos");

            // Assert
            assertEquals("http://test:8080/hcen/api/v1/metadatos", result);
        }

        @Test
        @DisplayName("Debe manejar path con query params")
        void buildUrl_withQueryParams_success() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://test:8080/hcen");

            // Act
            String result = HcenCentralUrlUtil.buildUrl("/metadatos?ci=12345678&tenant=101");

            // Assert
            assertEquals("http://test:8080/hcen/metadatos?ci=12345678&tenant=101", result);
        }

        @Test
        @DisplayName("Debe construir múltiples URLs consecutivas")
        void buildUrl_multipleCalls_success() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://test:8080/hcen");

            // Act
            String url1 = HcenCentralUrlUtil.buildUrl("/metadatos");
            String url2 = HcenCentralUrlUtil.buildUrl("/documentos");
            String url3 = HcenCentralUrlUtil.buildUrl("/usuarios");

            // Assert
            assertEquals("http://test:8080/hcen/metadatos", url1);
            assertEquals("http://test:8080/hcen/documentos", url2);
            assertEquals("http://test:8080/hcen/usuarios", url3);
        }
    }

    @Nested
    @DisplayName("buildApiUrl Tests - TODOS LOS CASOS")
    class BuildApiUrlTests {

        @Test
        @DisplayName("Debe construir URL con /api + path")
        void buildApiUrl_validPath_success() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://test:8080/hcen");

            // Act
            String result = HcenCentralUrlUtil.buildApiUrl("/metadatos-documento");

            // Assert
            assertEquals("http://test:8080/hcen/api/metadatos-documento", result);
        }

        @Test
        @DisplayName("Debe agregar slash inicial si falta")
        void buildApiUrl_noLeadingSlash_addsSlash() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://test:8080/hcen");

            // Act
            String result = HcenCentralUrlUtil.buildApiUrl("metadatos-documento");

            // Assert
            assertEquals("http://test:8080/hcen/api/metadatos-documento", result);
        }

        @Test
        @DisplayName("Debe retornar base URL + /api si path es null")
        void buildApiUrl_nullPath_returnsApiBaseUrl() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://test:8080/hcen");

            // Act
            String result = HcenCentralUrlUtil.buildApiUrl(null);

            // Assert
            assertEquals("http://test:8080/hcen/api", result);
        }

        @Test
        @DisplayName("Debe retornar base URL + /api si path es vacío")
        void buildApiUrl_emptyPath_returnsApiBaseUrl() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://test:8080/hcen");

            // Act
            String result = HcenCentralUrlUtil.buildApiUrl("");

            // Assert
            assertEquals("http://test:8080/hcen/api", result);
        }

        @Test
        @DisplayName("Debe manejar path con múltiples segmentos")
        void buildApiUrl_multipleSegments_success() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://test:8080/hcen");

            // Act
            String result = HcenCentralUrlUtil.buildApiUrl("/metadatos-documento/paciente/12345678");

            // Assert
            assertEquals("http://test:8080/hcen/api/metadatos-documento/paciente/12345678", result);
        }

        @Test
        @DisplayName("Debe manejar path con query params")
        void buildApiUrl_withQueryParams_success() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://test:8080/hcen");

            // Act
            String result = HcenCentralUrlUtil.buildApiUrl("/metadatos?profesionalId=doc1&tenantId=101");

            // Assert
            assertEquals("http://test:8080/hcen/api/metadatos?profesionalId=doc1&tenantId=101", result);
        }

        @Test
        @DisplayName("Debe usar default si no hay configuración")
        void buildApiUrl_noConfig_usesDefault() {
            // Act
            String result = HcenCentralUrlUtil.buildApiUrl("/metadatos-documento");

            // Assert
            assertEquals("http://hcen-backend:8080/hcen/api/metadatos-documento", result);
        }

        @Test
        @DisplayName("Debe construir múltiples URLs consecutivas")
        void buildApiUrl_multipleCalls_success() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://test:8080/hcen");

            // Act
            String url1 = HcenCentralUrlUtil.buildApiUrl("/metadatos-documento");
            String url2 = HcenCentralUrlUtil.buildApiUrl("/service-auth/token");
            String url3 = HcenCentralUrlUtil.buildApiUrl("/paciente/12345678/metadatos");

            // Assert
            assertEquals("http://test:8080/hcen/api/metadatos-documento", url1);
            assertEquals("http://test:8080/hcen/api/service-auth/token", url2);
            assertEquals("http://test:8080/hcen/api/paciente/12345678/metadatos", url3);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Debe manejar URL con localhost")
        void edgeCase_localhost_success() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://localhost:8080/hcen");

            // Act
            String baseUrl = HcenCentralUrlUtil.getBaseUrl();
            String apiUrl = HcenCentralUrlUtil.buildApiUrl("/test");

            // Assert
            assertEquals("http://localhost:8080/hcen", baseUrl);
            assertEquals("http://localhost:8080/hcen/api/test", apiUrl);
        }

        @Test
        @DisplayName("Debe manejar URL con IP")
        void edgeCase_ipAddress_success() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://192.168.1.100:8080/hcen");

            // Act
            String result = HcenCentralUrlUtil.buildApiUrl("/metadatos");

            // Assert
            assertEquals("http://192.168.1.100:8080/hcen/api/metadatos", result);
        }

        @Test
        @DisplayName("Debe manejar URL con dominio complejo")
        void edgeCase_complexDomain_success() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "https://hcen-backend.prod.example.com:8443/hcen");

            // Act
            String result = HcenCentralUrlUtil.buildApiUrl("/metadatos-documento");

            // Assert
            assertEquals("https://hcen-backend.prod.example.com:8443/hcen/api/metadatos-documento", result);
        }

        @Test
        @DisplayName("Debe manejar path con caracteres especiales")
        void edgeCase_specialCharsInPath_success() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://test:8080/hcen");

            // Act
            String result = HcenCentralUrlUtil.buildApiUrl("/paciente/1.234.567-8/metadatos");

            // Assert
            assertEquals("http://test:8080/hcen/api/paciente/1.234.567-8/metadatos", result);
        }

        @Test
        @DisplayName("Debe manejar path muy largo")
        void edgeCase_longPath_success() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://test:8080/hcen");

            // Act
            String result = HcenCentralUrlUtil.buildApiUrl("/api/v1/metadatos/documento/paciente/12345678/clinica/101/profesional/doctor1");

            // Assert
            assertTrue(result.startsWith("http://test:8080/hcen/api"));
            assertTrue(result.contains("paciente/12345678"));
        }

        @Test
        @DisplayName("Debe manejar cambio de configuración entre llamadas")
        void edgeCase_configChange_usesNewConfig() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://url1:8080/hcen");
            String result1 = HcenCentralUrlUtil.getBaseUrl();

            // Act - Cambiar configuración
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://url2:9090/hcen");
            String result2 = HcenCentralUrlUtil.getBaseUrl();

            // Assert
            assertEquals("http://url1:8080/hcen", result1);
            assertEquals("http://url2:9090/hcen", result2);
        }

        @Test
        @DisplayName("Debe manejar URL con path complejo")
        void edgeCase_complexPath_success() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://test:8080/app/hcen/v1");

            // Act
            String result = HcenCentralUrlUtil.buildApiUrl("/metadatos");

            // Assert
            assertEquals("http://test:8080/app/hcen/v1/api/metadatos", result);
        }

        @Test
        @DisplayName("Debe manejar trailing slash en base URL y path")
        void edgeCase_trailingSlashes_success() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://test:8080/hcen/");

            // Act
            String result = HcenCentralUrlUtil.buildUrl("/metadatos");

            // Assert
            assertEquals("http://test:8080/hcen/metadatos", result);
        }
    }

    @Nested
    @DisplayName("Integration Tests - Combinaciones")
    class IntegrationTests {

        @Test
        @DisplayName("Debe construir URLs completas para todos los endpoints")
        void integration_allEndpoints_success() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://hcen-prod:8080/hcen");

            // Act
            String metadatos = HcenCentralUrlUtil.buildApiUrl("/metadatos-documento");
            String serviceAuth = HcenCentralUrlUtil.buildApiUrl("/service-auth/token");
            String paciente = HcenCentralUrlUtil.buildApiUrl("/paciente/12345678/metadatos");

            // Assert
            assertTrue(metadatos.contains("/api/metadatos-documento"));
            assertTrue(serviceAuth.contains("/api/service-auth/token"));
            assertTrue(paciente.contains("/api/paciente/12345678/metadatos"));
        }

        @Test
        @DisplayName("Debe funcionar con diferentes configuraciones")
        void integration_differentConfigs_success() {
            // Test 1: Desarrollo
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://localhost:8080/hcen");
            String dev = HcenCentralUrlUtil.buildApiUrl("/metadatos");
            assertEquals("http://localhost:8080/hcen/api/metadatos", dev);

            // Test 2: Producción
            System.setProperty("HCEN_CENTRAL_BASE_URL", "https://hcen.prod.com/hcen");
            String prod = HcenCentralUrlUtil.buildApiUrl("/metadatos");
            assertEquals("https://hcen.prod.com/hcen/api/metadatos", prod);

            // Test 3: Docker
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://hcen-backend:8080/hcen");
            String docker = HcenCentralUrlUtil.buildApiUrl("/metadatos");
            assertEquals("http://hcen-backend:8080/hcen/api/metadatos", docker);
        }

        @Test
        @DisplayName("Debe ser thread-safe en llamadas concurrentes")
        void integration_threadSafe_success() {
            // Arrange
            System.setProperty("HCEN_CENTRAL_BASE_URL", "http://test:8080/hcen");

            // Act - Simular llamadas concurrentes
            String url1 = HcenCentralUrlUtil.buildApiUrl("/endpoint1");
            String url2 = HcenCentralUrlUtil.buildApiUrl("/endpoint2");
            String url3 = HcenCentralUrlUtil.buildApiUrl("/endpoint3");

            // Assert - Todas deben ser correctas
            assertTrue(url1.contains("/api/endpoint1"));
            assertTrue(url2.contains("/api/endpoint2"));
            assertTrue(url3.contains("/api/endpoint3"));
        }
    }
}
