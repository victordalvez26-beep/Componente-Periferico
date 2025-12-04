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
}
