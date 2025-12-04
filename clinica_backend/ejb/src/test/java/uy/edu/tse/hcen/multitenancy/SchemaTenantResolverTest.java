package uy.edu.tse.hcen.multitenancy;

import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests exhaustivos para SchemaTenantResolver
 */
@DisplayName("SchemaTenantResolver Tests")
class SchemaTenantResolverTest {

    private SchemaTenantResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new SchemaTenantResolver();
    }

    @AfterEach
    void tearDown() {
        // Limpiar TenantContext después de cada test
        TenantContext.clear();
    }

    @Nested
    @DisplayName("Resolve Current Tenant Identifier Tests")
    class ResolveCurrentTenantIdentifierTests {

        @Test
        @DisplayName("Debe resolver tenant ID a schema name")
        void resolveTenant_withValidTenant_shouldReturnSchemaName() {
            // Arrange
            try (MockedStatic<TenantContext> tenantContext = mockStatic(TenantContext.class)) {
                tenantContext.when(TenantContext::getCurrentTenant).thenReturn("101");

                // Act
                Object result = resolver.resolveCurrentTenantIdentifier();

                // Assert
                assertNotNull(result);
                assertEquals("schema_clinica_101", result);
            }
        }

        @Test
        @DisplayName("Debe retornar public cuando tenant es null")
        void resolveTenant_withNullTenant_shouldReturnPublic() {
            // Arrange
            try (MockedStatic<TenantContext> tenantContext = mockStatic(TenantContext.class)) {
                tenantContext.when(TenantContext::getCurrentTenant).thenReturn(null);

                // Act
                Object result = resolver.resolveCurrentTenantIdentifier();

                // Assert
                assertNotNull(result);
                assertEquals("public", result);
            }
        }

        @Test
        @DisplayName("Debe retornar public cuando tenant está vacío")
        void resolveTenant_withEmptyTenant_shouldReturnPublic() {
            // Arrange
            try (MockedStatic<TenantContext> tenantContext = mockStatic(TenantContext.class)) {
                tenantContext.when(TenantContext::getCurrentTenant).thenReturn("");

                // Act
                Object result = resolver.resolveCurrentTenantIdentifier();

                // Assert
                assertEquals("public", result);
            }
        }

        @Test
        @DisplayName("Debe retornar public cuando tenant tiene solo espacios")
        void resolveTenant_withBlankTenant_shouldReturnPublic() {
            // Arrange
            try (MockedStatic<TenantContext> tenantContext = mockStatic(TenantContext.class)) {
                tenantContext.when(TenantContext::getCurrentTenant).thenReturn("   ");

                // Act
                Object result = resolver.resolveCurrentTenantIdentifier();

                // Assert
                assertEquals("public", result);
            }
        }

        @Test
        @DisplayName("Debe resolver tenant numérico largo")
        void resolveTenant_withLongNumericTenant_shouldWork() {
            // Arrange
            try (MockedStatic<TenantContext> tenantContext = mockStatic(TenantContext.class)) {
                tenantContext.when(TenantContext::getCurrentTenant).thenReturn("999999");

                // Act
                Object result = resolver.resolveCurrentTenantIdentifier();

                // Assert
                assertEquals("schema_clinica_999999", result);
            }
        }

        @Test
        @DisplayName("Debe resolver tenant alfanumérico")
        void resolveTenant_withAlphanumericTenant_shouldWork() {
            // Arrange
            try (MockedStatic<TenantContext> tenantContext = mockStatic(TenantContext.class)) {
                tenantContext.when(TenantContext::getCurrentTenant).thenReturn("clinic_abc123");

                // Act
                Object result = resolver.resolveCurrentTenantIdentifier();

                // Assert
                assertEquals("schema_clinica_clinic_abc123", result);
            }
        }

        @Test
        @DisplayName("Debe manejar tenant con un solo caracter")
        void resolveTenant_withSingleCharTenant_shouldWork() {
            // Arrange
            try (MockedStatic<TenantContext> tenantContext = mockStatic(TenantContext.class)) {
                tenantContext.when(TenantContext::getCurrentTenant).thenReturn("1");

                // Act
                Object result = resolver.resolveCurrentTenantIdentifier();

                // Assert
                assertEquals("schema_clinica_1", result);
            }
        }
    }

    @Nested
    @DisplayName("Validate Existing Current Sessions Tests")
    class ValidateExistingCurrentSessionsTests {

        @Test
        @DisplayName("validateExistingCurrentSessions debe retornar false")
        void validateExistingCurrentSessions_shouldReturnFalse() {
            // Act & Assert
            assertFalse(resolver.validateExistingCurrentSessions());
        }

        @Test
        @DisplayName("validateExistingCurrentSessions debe ser consistente")
        void validateExistingCurrentSessions_shouldBeConsistent() {
            // Act
            boolean result1 = resolver.validateExistingCurrentSessions();
            boolean result2 = resolver.validateExistingCurrentSessions();

            // Assert
            assertEquals(result1, result2);
            assertFalse(result1);
        }
    }

    @Nested
    @DisplayName("Set Tenant Identifier Tests")
    class SetTenantIdentifierTests {

        @Test
        @DisplayName("setTenantIdentifier debe establecer tenant en contexto")
        void setTenantIdentifier_shouldSetTenantInContext() {
            // Arrange
            String tenantId = "102";

            try (MockedStatic<TenantContext> tenantContext = mockStatic(TenantContext.class)) {
                // Act
                resolver.setTenantIdentifier(tenantId);

                // Assert
                tenantContext.verify(() -> TenantContext.setCurrentTenant(tenantId));
            }
        }

        @Test
        @DisplayName("setTenantIdentifier debe manejar null")
        void setTenantIdentifier_withNull_shouldWork() {
            // Arrange
            try (MockedStatic<TenantContext> tenantContext = mockStatic(TenantContext.class)) {
                // Act
                resolver.setTenantIdentifier(null);

                // Assert
                tenantContext.verify(() -> TenantContext.setCurrentTenant(null));
            }
        }

        @Test
        @DisplayName("setTenantIdentifier debe manejar string vacío")
        void setTenantIdentifier_withEmptyString_shouldWork() {
            // Arrange
            try (MockedStatic<TenantContext> tenantContext = mockStatic(TenantContext.class)) {
                // Act
                resolver.setTenantIdentifier("");

                // Assert
                tenantContext.verify(() -> TenantContext.setCurrentTenant(""));
            }
        }

        @Test
        @DisplayName("setTenantIdentifier debe permitir cambiar tenant")
        void setTenantIdentifier_shouldAllowChangingTenant() {
            // Arrange
            try (MockedStatic<TenantContext> tenantContext = mockStatic(TenantContext.class)) {
                // Act
                resolver.setTenantIdentifier("101");
                resolver.setTenantIdentifier("102");
                resolver.setTenantIdentifier("103");

                // Assert
                tenantContext.verify(() -> TenantContext.setCurrentTenant("101"));
                tenantContext.verify(() -> TenantContext.setCurrentTenant("102"));
                tenantContext.verify(() -> TenantContext.setCurrentTenant("103"));
            }
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("setTenantIdentifier y resolve deben trabajar juntos")
        void setAndResolve_shouldWorkTogether() {
            // Arrange
            TenantContext.setCurrentTenant("105");

            // Act
            Object schemaName = resolver.resolveCurrentTenantIdentifier();

            // Assert
            assertEquals("schema_clinica_105", schemaName);
        }

        @Test
        @DisplayName("Múltiples operaciones set y resolve deben funcionar")
        void multipleSetAndResolve_shouldWork() {
            // Act & Assert
            resolver.setTenantIdentifier("101");
            TenantContext.setCurrentTenant("101");
            assertEquals("schema_clinica_101", resolver.resolveCurrentTenantIdentifier());

            resolver.setTenantIdentifier("102");
            TenantContext.setCurrentTenant("102");
            assertEquals("schema_clinica_102", resolver.resolveCurrentTenantIdentifier());

            TenantContext.clear();
            assertEquals("public", resolver.resolveCurrentTenantIdentifier());
        }

        @Test
        @DisplayName("Resolver debe crear nuevo resolver independiente")
        void multipleResolvers_shouldBeIndependent() {
            // Arrange
            SchemaTenantResolver resolver1 = new SchemaTenantResolver();
            SchemaTenantResolver resolver2 = new SchemaTenantResolver();

            // Act
            TenantContext.setCurrentTenant("201");
            Object result1 = resolver1.resolveCurrentTenantIdentifier();
            Object result2 = resolver2.resolveCurrentTenantIdentifier();

            // Assert
            assertEquals(result1, result2);
            assertEquals("schema_clinica_201", result1);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Debe manejar tenant con caracteres especiales")
        void resolveTenant_withSpecialChars_shouldWork() {
            // Arrange
            try (MockedStatic<TenantContext> tenantContext = mockStatic(TenantContext.class)) {
                tenantContext.when(TenantContext::getCurrentTenant).thenReturn("test-123_abc");

                // Act
                Object result = resolver.resolveCurrentTenantIdentifier();

                // Assert
                assertEquals("schema_clinica_test-123_abc", result);
            }
        }

        @Test
        @DisplayName("Debe manejar tenant muy largo")
        void resolveTenant_withVeryLongTenant_shouldWork() {
            // Arrange
            String longTenant = "a".repeat(100);
            try (MockedStatic<TenantContext> tenantContext = mockStatic(TenantContext.class)) {
                tenantContext.when(TenantContext::getCurrentTenant).thenReturn(longTenant);

                // Act
                Object result = resolver.resolveCurrentTenantIdentifier();

                // Assert
                assertTrue(result.toString().startsWith("schema_clinica_"));
                assertTrue(result.toString().endsWith("a"));
            }
        }

        @Test
        @DisplayName("Debe manejar múltiples espacios al inicio y final")
        void resolveTenant_withMultipleSpaces_shouldReturnPublic() {
            // Arrange
            try (MockedStatic<TenantContext> tenantContext = mockStatic(TenantContext.class)) {
                tenantContext.when(TenantContext::getCurrentTenant).thenReturn("    \t\n    ");

                // Act
                Object result = resolver.resolveCurrentTenantIdentifier();

                // Assert
                assertEquals("public", result);
            }
        }
    }
}

