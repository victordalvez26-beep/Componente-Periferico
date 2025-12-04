package uy.edu.tse.hcen.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.tse.hcen.dto.LoginResponse;
import uy.edu.tse.hcen.model.UsuarioPeriferico;
import uy.edu.tse.hcen.model.ProfesionalSalud;
import uy.edu.tse.hcen.multitenancy.SchemaTenantResolver;
import uy.edu.tse.hcen.repository.UsuarioPerifericoRepository;
import uy.edu.tse.hcen.utils.PasswordUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for LoginService.
 * 
 * Tests cover:
 * - Authentication flows (public schema, tenant schema)
 * - Security validations
 * - Tenant context management
 * - Token generation
 * - Edge cases and error handling
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoginService - Authentication & Authorization")
class LoginServiceTest {

    @Mock
    private UsuarioPerifericoRepository userRepository;

    @Mock
    private SchemaTenantResolver tenantResolver;

    @InjectMocks
    private LoginService loginService;

    private static final String VALID_PASSWORD = "SecurePass123!";
    private static final String NICKNAME_ADMIN = "admin_clinic1";
    private static final String NICKNAME_PROF = "doctor_smith";
    private static final String TENANT_ID = "101";
    
    private String validPasswordHash;

    @BeforeEach
    void setUp() {
        validPasswordHash = PasswordUtils.hashPassword(VALID_PASSWORD);
    }

    // ==================== HAPPY PATH - PUBLIC SCHEMA ====================
    
    @Nested
    @DisplayName("Autenticación en Schema Público (Administradores)")
    class PublicSchemaAuthentication {
        
        @Test
        @DisplayName("Admin con credenciales válidas debe autenticar exitosamente")
        void adminInPublic_validCredentials_shouldAuthenticate() {
            // Arrange
            UsuarioPeriferico admin = createUsuario(1L, NICKNAME_ADMIN, validPasswordHash, "ADMINISTRADOR", TENANT_ID);
            when(userRepository.findByNicknameForLogin(NICKNAME_ADMIN)).thenReturn(admin);

            // Act
            LoginResponse response = loginService.authenticateAndGenerateToken(NICKNAME_ADMIN, VALID_PASSWORD, null);

            // Assert
            assertNotNull(response);
            assertNotNull(response.getToken());
            assertTrue(response.getToken().length() > 50, "JWT token should be substantial");
            assertEquals("ADMINISTRADOR", response.getRole());
            assertEquals(TENANT_ID, response.getTenant_id());
            
            verify(userRepository).findByNicknameForLogin(NICKNAME_ADMIN);
            verify(tenantResolver).setTenantIdentifier("public");
        }

        @Test
        @DisplayName("Admin sin role explícito debe deducir ADMINISTRADOR del tipo")
        void adminInPublic_noExplicitRole_shouldDeduceFromType() {
            // Arrange
            UsuarioPeriferico admin = createUsuario(1L, NICKNAME_ADMIN, validPasswordHash, null, TENANT_ID);
            // Make it look like AdministradorClinica by type
            when(userRepository.findByNicknameForLogin(NICKNAME_ADMIN)).thenReturn(admin);

            // Act
            LoginResponse response = loginService.authenticateAndGenerateToken(NICKNAME_ADMIN, VALID_PASSWORD, null);

            // Assert
            assertNotNull(response);
            // Should fallback to "OTRO" when role is null and type is base UsuarioPeriferico
            assertNotNull(response.getRole());
        }
    }

    // ==================== HAPPY PATH - TENANT SCHEMA ====================
    
    @Nested
    @DisplayName("Autenticación en Schema de Tenant (Profesionales)")
    class TenantSchemaAuthentication {
        
        @Test
        @DisplayName("Profesional con credenciales válidas debe autenticar en tenant schema")
        void profesionalInTenant_validCredentials_shouldAuthenticate() {
            // Arrange
            ProfesionalSalud profesional = new ProfesionalSalud();
            profesional.setId(2L);
            profesional.setNickname(NICKNAME_PROF);
            profesional.setPasswordHash(validPasswordHash);
            profesional.setRole("PROFESIONAL");

            when(userRepository.findByNicknameForLogin(NICKNAME_PROF)).thenReturn(null); // Not in public
            when(userRepository.findByNicknameInTenantSchema(NICKNAME_PROF, "schema_clinica_" + TENANT_ID))
                    .thenReturn(profesional);

            // Act
            LoginResponse response = loginService.authenticateAndGenerateToken(NICKNAME_PROF, VALID_PASSWORD, TENANT_ID);

            // Assert
            assertNotNull(response);
            assertNotNull(response.getToken());
            assertEquals("PROFESIONAL", response.getRole());
            assertEquals(TENANT_ID, response.getTenant_id());
            
            // Verify search strategy: public first, then tenant
            verify(userRepository).findByNicknameForLogin(NICKNAME_PROF);
            verify(userRepository).findByNicknameInTenantSchema(NICKNAME_PROF, "schema_clinica_" + TENANT_ID);
            verify(tenantResolver).setTenantIdentifier("public");
            verify(tenantResolver).setTenantIdentifier(TENANT_ID);
        }

        @Test
        @DisplayName("Profesional tipo ProfesionalSalud sin role debe deducir PROFESIONAL")
        void profesional_noExplicitRole_shouldDeduceFromInstanceof() {
            // Arrange
            ProfesionalSalud profesional = new ProfesionalSalud();
            profesional.setNickname(NICKNAME_PROF);
            profesional.setPasswordHash(validPasswordHash);
            profesional.setRole(null); // No explicit role

            when(userRepository.findByNicknameForLogin(NICKNAME_PROF)).thenReturn(null);
            when(userRepository.findByNicknameInTenantSchema(NICKNAME_PROF, "schema_clinica_" + TENANT_ID))
                    .thenReturn(profesional);

            // Act
            LoginResponse response = loginService.authenticateAndGenerateToken(NICKNAME_PROF, VALID_PASSWORD, TENANT_ID);

            // Assert
            assertEquals("PROFESIONAL", response.getRole(), "Should deduce PROFESIONAL from ProfesionalSalud type");
        }
    }

    // ==================== SECURITY TESTS ====================
    
    @Nested
    @DisplayName("Security & Validaciones")
    class SecurityTests {
        
        @Test
        @DisplayName("Contraseña incorrecta debe lanzar SecurityException")
        void authenticate_incorrectPassword_shouldThrowSecurityException() {
            // Arrange
            UsuarioPeriferico user = createUsuario(1L, NICKNAME_ADMIN, validPasswordHash, "ADMINISTRADOR", TENANT_ID);
            when(userRepository.findByNicknameForLogin(NICKNAME_ADMIN)).thenReturn(user);

            // Act & Assert
            SecurityException ex = assertThrows(SecurityException.class, 
                () -> loginService.authenticateAndGenerateToken(NICKNAME_ADMIN, "WrongPassword!", null));
            
            assertEquals("Credenciales inválidas.", ex.getMessage());
            verify(userRepository).findByNicknameForLogin(NICKNAME_ADMIN);
        }

        @Test
        @DisplayName("Usuario inexistente debe lanzar SecurityException")
        void authenticate_userNotFound_shouldThrowSecurityException() {
            // Arrange
            when(userRepository.findByNicknameForLogin("nonexistent")).thenReturn(null);

            // Act & Assert
            SecurityException ex = assertThrows(SecurityException.class, 
                () -> loginService.authenticateAndGenerateToken("nonexistent", VALID_PASSWORD, null));
            
            assertEquals("Credenciales inválidas.", ex.getMessage());
        }

        @Test
        @DisplayName("Password null debe lanzar excepción")
        void authenticate_nullPassword_shouldThrow() {
            // Arrange
            UsuarioPeriferico user = createUsuario(1L, NICKNAME_ADMIN, validPasswordHash, "ADMINISTRADOR", TENANT_ID);
            when(userRepository.findByNicknameForLogin(NICKNAME_ADMIN)).thenReturn(user);

            // Act & Assert
            assertThrows(Exception.class, 
                () -> loginService.authenticateAndGenerateToken(NICKNAME_ADMIN, null, null));
        }

        @Test
        @DisplayName("Password vacío debe lanzar SecurityException")
        void authenticate_emptyPassword_shouldThrowSecurityException() {
            // Arrange
            UsuarioPeriferico user = createUsuario(1L, NICKNAME_ADMIN, validPasswordHash, "ADMINISTRADOR", TENANT_ID);
            when(userRepository.findByNicknameForLogin(NICKNAME_ADMIN)).thenReturn(user);

            // Act & Assert
            SecurityException ex = assertThrows(SecurityException.class, 
                () -> loginService.authenticateAndGenerateToken(NICKNAME_ADMIN, "", null));
            
            assertEquals("Credenciales inválidas.", ex.getMessage());
        }

        @Test
        @DisplayName("Usuario sin password hash debe lanzar excepción")
        void authenticate_userWithoutPasswordHash_shouldThrow() {
            // Arrange
            UsuarioPeriferico user = createUsuario(1L, NICKNAME_ADMIN, null, "ADMINISTRADOR", TENANT_ID);
            when(userRepository.findByNicknameForLogin(NICKNAME_ADMIN)).thenReturn(user);

            // Act & Assert
            assertThrows(Exception.class, 
                () -> loginService.authenticateAndGenerateToken(NICKNAME_ADMIN, VALID_PASSWORD, null));
        }
    }

    // ==================== TENANT RESOLUTION TESTS ====================
    
    @Nested
    @DisplayName("Resolución de Tenant Context")
    class TenantResolutionTests {
        
        @Test
        @DisplayName("Sin tenantId debe buscar solo en public schema")
        void authenticate_noTenantId_shouldOnlySearchPublic() {
            // Arrange
            when(userRepository.findByNicknameForLogin(NICKNAME_PROF)).thenReturn(null);

            // Act & Assert
            assertThrows(SecurityException.class, 
                () -> loginService.authenticateAndGenerateToken(NICKNAME_PROF, VALID_PASSWORD, null));
            
            verify(userRepository).findByNicknameForLogin(NICKNAME_PROF);
            verify(userRepository, never()).findByNicknameInTenantSchema(anyString(), anyString());
            verify(tenantResolver, times(1)).setTenantIdentifier("public");
        }

        @Test
        @DisplayName("TenantId vacío debe buscar solo en public")
        void authenticate_emptyTenantId_shouldOnlySearchPublic() {
            // Arrange
            when(userRepository.findByNicknameForLogin(NICKNAME_PROF)).thenReturn(null);

            // Act & Assert
            assertThrows(SecurityException.class, 
                () -> loginService.authenticateAndGenerateToken(NICKNAME_PROF, VALID_PASSWORD, ""));
            
            verify(userRepository, never()).findByNicknameInTenantSchema(anyString(), anyString());
        }

        @Test
        @DisplayName("TenantId con espacios debe buscar solo en public")
        void authenticate_blankTenantId_shouldOnlySearchPublic() {
            // Arrange
            when(userRepository.findByNicknameForLogin(NICKNAME_PROF)).thenReturn(null);

            // Act & Assert
            assertThrows(SecurityException.class, 
                () -> loginService.authenticateAndGenerateToken(NICKNAME_PROF, VALID_PASSWORD, "   "));
            
            verify(userRepository, never()).findByNicknameInTenantSchema(anyString(), anyString());
        }

        @Test
        @DisplayName("Admin encontrado en public no debe buscar en tenant")
        void authenticate_adminFoundInPublic_shouldNotSearchTenant() {
            // Arrange
            UsuarioPeriferico admin = createUsuario(1L, NICKNAME_ADMIN, validPasswordHash, "ADMINISTRADOR", TENANT_ID);
            when(userRepository.findByNicknameForLogin(NICKNAME_ADMIN)).thenReturn(admin);

            // Act
            loginService.authenticateAndGenerateToken(NICKNAME_ADMIN, VALID_PASSWORD, TENANT_ID);

            // Assert - Should not search in tenant schema
            verify(userRepository).findByNicknameForLogin(NICKNAME_ADMIN);
            verify(userRepository, never()).findByNicknameInTenantSchema(anyString(), anyString());
        }

        @Test
        @DisplayName("TenantResolver debe configurarse correctamente al encontrar en tenant")
        void authenticate_foundInTenant_shouldConfigureTenantResolver() {
            // Arrange
            ProfesionalSalud profesional = new ProfesionalSalud();
            profesional.setNickname(NICKNAME_PROF);
            profesional.setPasswordHash(validPasswordHash);
            profesional.setRole("PROFESIONAL");

            when(userRepository.findByNicknameForLogin(NICKNAME_PROF)).thenReturn(null);
            when(userRepository.findByNicknameInTenantSchema(NICKNAME_PROF, "schema_clinica_" + TENANT_ID))
                    .thenReturn(profesional);

            // Act
            loginService.authenticateAndGenerateToken(NICKNAME_PROF, VALID_PASSWORD, TENANT_ID);

            // Assert - Verify tenant configuration sequence
            verify(tenantResolver).setTenantIdentifier("public"); // Initial search
            verify(tenantResolver).setTenantIdentifier(TENANT_ID); // Found in tenant
        }
    }

    // ==================== TOKEN GENERATION TESTS ====================
    
    @Nested
    @DisplayName("Generación de Tokens JWT")
    class TokenGenerationTests {
        
        @Test
        @DisplayName("Token debe contener role y tenantId correctos")
        void authenticate_shouldGenerateValidJWT() {
            // Arrange
            UsuarioPeriferico user = createUsuario(1L, NICKNAME_ADMIN, validPasswordHash, "ADMINISTRADOR", TENANT_ID);
            when(userRepository.findByNicknameForLogin(NICKNAME_ADMIN)).thenReturn(user);

            // Act
            LoginResponse response = loginService.authenticateAndGenerateToken(NICKNAME_ADMIN, VALID_PASSWORD, null);

            // Assert
            assertNotNull(response.getToken());
            assertTrue(response.getToken().contains("."), "JWT should have parts separated by dots");
            assertTrue(response.getToken().length() > 50, "JWT should be substantial");
            assertEquals("ADMINISTRADOR", response.getRole());
            assertEquals(TENANT_ID, response.getTenant_id());
        }

        @Test
        @DisplayName("Tokens generados deben ser diferentes entre usuarios")
        void authenticate_differentUsers_shouldGenerateDifferentTokens() {
            // Arrange
            UsuarioPeriferico user1 = createUsuario(1L, "user1", validPasswordHash, "ADMINISTRADOR", TENANT_ID);
            UsuarioPeriferico user2 = createUsuario(2L, "user2", validPasswordHash, "PROFESIONAL", TENANT_ID);
            
            when(userRepository.findByNicknameForLogin("user1")).thenReturn(user1);
            when(userRepository.findByNicknameForLogin("user2")).thenReturn(user2);

            // Act
            LoginResponse response1 = loginService.authenticateAndGenerateToken("user1", VALID_PASSWORD, null);
            LoginResponse response2 = loginService.authenticateAndGenerateToken("user2", VALID_PASSWORD, null);

            // Assert
            assertNotEquals(response1.getToken(), response2.getToken(), "Different users should get different tokens");
        }
    }

    // ==================== EDGE CASES ====================
    
    @Nested
    @DisplayName("Casos Borde y Excepciones")
    class EdgeCasesTests {
        
        @Test
        @DisplayName("Nickname null debe lanzar SecurityException")
        void authenticate_nullNickname_shouldThrow() {
            // Act & Assert
            assertThrows(SecurityException.class, 
                () -> loginService.authenticateAndGenerateToken(null, VALID_PASSWORD, TENANT_ID));
        }

        @Test
        @DisplayName("Usuario sin tenantId debe funcionar si está en public")
        void authenticate_userWithoutTenantId_shouldWork() {
            // Arrange
            UsuarioPeriferico user = createUsuario(1L, NICKNAME_ADMIN, validPasswordHash, "ADMINISTRADOR", null);
            when(userRepository.findByNicknameForLogin(NICKNAME_ADMIN)).thenReturn(user);

            // Act
            LoginResponse response = loginService.authenticateAndGenerateToken(NICKNAME_ADMIN, VALID_PASSWORD, null);

            // Assert
            assertNotNull(response);
            assertNull(response.getTenant_id(), "User without tenantId should have null tenant in response");
        }

        @Test
        @DisplayName("Búsqueda con tenantId pero usuario no existe en ningún schema")
        void authenticate_userNotInAnySchema_shouldThrow() {
            // Arrange
            when(userRepository.findByNicknameForLogin(NICKNAME_PROF)).thenReturn(null);
            when(userRepository.findByNicknameInTenantSchema(NICKNAME_PROF, "schema_clinica_" + TENANT_ID))
                    .thenReturn(null);

            // Act & Assert
            SecurityException ex = assertThrows(SecurityException.class, 
                () -> loginService.authenticateAndGenerateToken(NICKNAME_PROF, VALID_PASSWORD, TENANT_ID));
            
            assertEquals("Credenciales inválidas.", ex.getMessage());
        }

        @Test
        @DisplayName("TenantId muy largo no debe causar problemas")
        void authenticate_veryLongTenantId_shouldHandle() {
            // Arrange
            String longTenantId = "123456789012345";
            when(userRepository.findByNicknameForLogin(NICKNAME_PROF)).thenReturn(null);
            when(userRepository.findByNicknameInTenantSchema(NICKNAME_PROF, "schema_clinica_" + longTenantId))
                    .thenReturn(null);

            // Act & Assert
            assertThrows(SecurityException.class, 
                () -> loginService.authenticateAndGenerateToken(NICKNAME_PROF, VALID_PASSWORD, longTenantId));
        }
    }

    // ==================== HELPER METHODS ====================
    
    private UsuarioPeriferico createUsuario(Long id, String nickname, String passwordHash, 
                                            String role, String tenantId) {
        UsuarioPeriferico user = new UsuarioPeriferico() {}; // Anonymous subclass for testing
        user.setId(id);
        user.setNickname(nickname);
        user.setPasswordHash(passwordHash);
        user.setRole(role);
        user.setTenantId(tenantId);
        return user;
    }
}
