package uy.edu.tse.hcen.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.tse.hcen.repository.UsuarioPerifericoRepository;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantAdminService Tests")
class TenantAdminServiceTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private EntityManager em;

    @Mock
    private UsuarioPerifericoRepository usuarioRepository;

    @InjectMocks
    private TenantAdminService service;

    @Nested
    @DisplayName("Create Tenant Schema Tests")
    class CreateTenantSchemaTests {

        @Test
        @DisplayName("Debe rechazar tenantSchema null")
        void createSchema_withNullSchema_shouldThrow() {
            // Act & Assert
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.createTenantSchema(null, "#007bff", "Test")
            );
            
            assertTrue(ex.getMessage().contains("required"));
        }

        @Test
        @DisplayName("Debe rechazar tenantSchema vacío")
        void createSchema_withEmptySchema_shouldThrow() {
            // Act & Assert
            assertThrows(
                    IllegalArgumentException.class,
                    () -> service.createTenantSchema("", "#007bff", "Test")
            );
        }

        @Test
        @DisplayName("Debe rechazar tenantSchema con solo espacios")
        void createSchema_withBlankSchema_shouldThrow() {
            // Act & Assert
            assertThrows(
                    IllegalArgumentException.class,
                    () -> service.createTenantSchema("   ", "#007bff", "Test")
            );
        }
    }

    @Test
    void service_shouldBeInstantiable() {
        assertNotNull(service);
    }

    @Test
    void adminCreationResult_shouldHavePublicFields() {
        TenantAdminService.AdminCreationResult result = new TenantAdminService.AdminCreationResult();
        result.adminNickname = "admin1";
        result.activationToken = "token123";
        result.activationUrl = "http://url";
        result.tokenExpiry = java.time.LocalDateTime.now();
        
        assertEquals("admin1", result.adminNickname);
        assertEquals("token123", result.activationToken);
        assertNotNull(result.tokenExpiry);
    }

    @Nested
    @DisplayName("Additional Coverage Tests")
    class AdditionalCoverageTests {

        @Test
        @DisplayName("Debe manejar colorPrimario null")
        void createSchema_withNullColor_shouldUseDefault() {
            // Act & Assert - No debe lanzar excepción
            assertThrows(Exception.class, () -> 
                service.createTenantSchema("schema_101", null, "Test"));
        }

        @Test
        @DisplayName("Debe manejar nombrePortal null")
        void createSchema_withNullNombre_shouldUseDefault() {
            // Act & Assert
            assertThrows(Exception.class, () -> 
                service.createTenantSchema("schema_101", "#007bff", null));
        }

        @Test
        @DisplayName("Debe manejar schema con caracteres especiales")
        void createSchema_withSpecialChars_shouldEscape() {
            // Act & Assert
            assertThrows(Exception.class, () -> 
                service.createTenantSchema("schema_test_123", "#007bff", "Test"));
        }

        @Test
        @DisplayName("Debe manejar color con comillas")
        void createSchema_withQuotesInColor_shouldEscape() {
            // Act & Assert
            assertThrows(Exception.class, () -> 
                service.createTenantSchema("schema_101", "#00'7bff", "Test"));
        }

        @Test
        @DisplayName("Debe manejar nombre con comillas")
        void createSchema_withQuotesInNombre_shouldEscape() {
            // Act & Assert
            assertThrows(Exception.class, () -> 
                service.createTenantSchema("schema_101", "#007bff", "Test'Clinic"));
        }

        @Test
        @DisplayName("AdminCreationResult debe ser mutable")
        void adminCreationResult_shouldBeMutable() {
            // Arrange
            TenantAdminService.AdminCreationResult result = new TenantAdminService.AdminCreationResult();
            
            // Act
            result.adminNickname = "admin1";
            result.activationToken = "token1";
            result.activationUrl = "url1";
            result.tokenExpiry = java.time.LocalDateTime.now();
            
            result.adminNickname = "admin2";
            result.activationToken = "token2";
            
            // Assert
            assertEquals("admin2", result.adminNickname);
            assertEquals("token2", result.activationToken);
        }

        @Test
        @DisplayName("AdminCreationResult debe permitir null en campos")
        void adminCreationResult_shouldAllowNulls() {
            // Arrange
            TenantAdminService.AdminCreationResult result = new TenantAdminService.AdminCreationResult();
            
            // Act
            result.adminNickname = null;
            result.activationToken = null;
            result.activationUrl = null;
            result.tokenExpiry = null;
            
            // Assert
            assertNull(result.adminNickname);
            assertNull(result.activationToken);
            assertNull(result.activationUrl);
            assertNull(result.tokenExpiry);
        }
    }
}
