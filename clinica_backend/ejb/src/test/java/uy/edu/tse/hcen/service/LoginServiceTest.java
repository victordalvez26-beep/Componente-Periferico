package uy.edu.tse.hcen.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.tse.hcen.dto.LoginResponse;
import uy.edu.tse.hcen.model.AdministradorClinica;
import uy.edu.tse.hcen.model.ProfesionalSalud;
import uy.edu.tse.hcen.model.UsuarioPeriferico;
import uy.edu.tse.hcen.multitenancy.SchemaTenantResolver;
import uy.edu.tse.hcen.multitenancy.TenantContext;
import uy.edu.tse.hcen.repository.UsuarioPerifericoRepository;
import uy.edu.tse.hcen.utils.PasswordUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private UsuarioPerifericoRepository userRepository;

    @Mock
    private SchemaTenantResolver tenantResolver;

    @InjectMocks
    private LoginService loginService;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
    }

    @Test
    void testAuthenticateAndGenerateTokenWithAdmin() throws SecurityException {
        // Arrange
        String nickname = "admin";
        String password = "password123";
        String tenantId = "101";
        
        UsuarioPeriferico admin = new UsuarioPeriferico();
        admin.setId(1L);
        admin.setNickname(nickname);
        admin.setPassword(password);
        admin.setTenantId(tenantId);
        admin.setRole("ADMINISTRADOR");
        
        doNothing().when(tenantResolver).setTenantIdentifier("public");
        when(userRepository.findByNicknameForLogin(nickname)).thenReturn(admin);
        
        // Act
        LoginResponse response = loginService.authenticateAndGenerateToken(nickname, password, tenantId);
        
        // Assert
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("ADMINISTRADOR", response.getRole());
        assertEquals(tenantId, response.getTenant_id());
        
        verify(userRepository).findByNicknameForLogin(nickname);
    }

    @Test
    void testAuthenticateAndGenerateTokenWithProfesional() throws SecurityException {
        // Arrange
        String nickname = "prof1";
        String password = "password123";
        String tenantId = "101";
        String schemaName = "schema_clinica_101";
        
        UsuarioPeriferico profesional = new ProfesionalSalud();
        profesional.setId(2L);
        profesional.setNickname(nickname);
        profesional.setPassword(password);
        profesional.setRole("PROFESIONAL");
        
        doNothing().when(tenantResolver).setTenantIdentifier("public");
        when(userRepository.findByNicknameForLogin(nickname)).thenReturn(null);
        when(userRepository.findByNicknameInTenantSchema(nickname, schemaName)).thenReturn(profesional);
        doNothing().when(tenantResolver).setTenantIdentifier(tenantId);
        
        // Act
        LoginResponse response = loginService.authenticateAndGenerateToken(nickname, password, tenantId);
        
        // Assert
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("PROFESIONAL", response.getRole());
        assertEquals(tenantId, response.getTenant_id());
        
        verify(userRepository).findByNicknameForLogin(nickname);
        verify(userRepository).findByNicknameInTenantSchema(nickname, schemaName);
    }

    @Test
    void testAuthenticateAndGenerateTokenWithInvalidCredentials() {
        // Arrange
        String nickname = "invalid";
        String password = "wrong";
        String tenantId = "101";
        
        doNothing().when(tenantResolver).setTenantIdentifier("public");
        when(userRepository.findByNicknameForLogin(nickname)).thenReturn(null);
        when(userRepository.findByNicknameInTenantSchema(anyString(), anyString())).thenReturn(null);
        
        // Act & Assert
        assertThrows(SecurityException.class, () -> {
            loginService.authenticateAndGenerateToken(nickname, password, tenantId);
        });
    }

    @Test
    void testAuthenticateAndGenerateTokenWithWrongPassword() {
        // Arrange
        String nickname = "admin";
        String correctPassword = "password123";
        String wrongPassword = "wrongpassword";
        String tenantId = "101";
        
        UsuarioPeriferico admin = new UsuarioPeriferico();
        admin.setId(1L);
        admin.setNickname(nickname);
        admin.setPassword(correctPassword);
        admin.setTenantId(tenantId);
        
        doNothing().when(tenantResolver).setTenantIdentifier("public");
        when(userRepository.findByNicknameForLogin(nickname)).thenReturn(admin);
        
        // Act & Assert
        assertThrows(SecurityException.class, () -> {
            loginService.authenticateAndGenerateToken(nickname, wrongPassword, tenantId);
        });
    }

    @Test
    void testAuthenticateAndGenerateTokenWithNullNickname() {
        // Act & Assert
        assertThrows(SecurityException.class, () -> {
            loginService.authenticateAndGenerateToken(null, "password", "101");
        });
    }

    @Test
    void testAuthenticateAndGenerateTokenWithNullTenantId() throws SecurityException {
        // Arrange
        String nickname = "admin";
        String password = "password123";
        
        UsuarioPeriferico admin = new UsuarioPeriferico();
        admin.setId(1L);
        admin.setNickname(nickname);
        admin.setPassword(password);
        admin.setTenantId("101");
        
        doNothing().when(tenantResolver).setTenantIdentifier("public");
        when(userRepository.findByNicknameForLogin(nickname)).thenReturn(admin);
        
        // Act
        LoginResponse response = loginService.authenticateAndGenerateToken(nickname, password, null);
        
        // Assert
        assertNotNull(response);
        assertNotNull(response.getToken());
    }

    @Test
    void testAuthenticateAndGenerateTokenWithProfesionalRoleFromInstance() throws SecurityException {
        // Arrange
        String nickname = "prof1";
        String password = "password123";
        String tenantId = "101";
        String schemaName = "schema_clinica_101";
        
        ProfesionalSalud profesional = new ProfesionalSalud();
        profesional.setId(2L);
        profesional.setNickname(nickname);
        profesional.setPassword(password);
        profesional.setRole(null); // Sin role explícito
        
        doNothing().when(tenantResolver).setTenantIdentifier("public");
        when(userRepository.findByNicknameForLogin(nickname)).thenReturn(null);
        when(userRepository.findByNicknameInTenantSchema(nickname, schemaName)).thenReturn(profesional);
        doNothing().when(tenantResolver).setTenantIdentifier(tenantId);
        
        // Act
        LoginResponse response = loginService.authenticateAndGenerateToken(nickname, password, tenantId);
        
        // Assert
        assertNotNull(response);
        assertEquals("PROFESIONAL", response.getRole());
    }

    @Test
    void testAuthenticateAndGenerateTokenWithAdministradorClinicaInstance() throws SecurityException {
        // Arrange
        String nickname = "admin";
        String password = "password123";
        String tenantId = "101";
        
        AdministradorClinica admin = mock(AdministradorClinica.class);
        when(admin.getId()).thenReturn(1L);
        when(admin.getNickname()).thenReturn(nickname);
        when(admin.getPasswordHash()).thenReturn(PasswordUtils.hashPassword(password));
        when(admin.getRole()).thenReturn(null); // Sin role explícito
        when(admin.getTenantId()).thenReturn(tenantId);
        when(admin.checkPassword(password)).thenReturn(true);
        
        doNothing().when(tenantResolver).setTenantIdentifier("public");
        when(userRepository.findByNicknameForLogin(nickname)).thenReturn(admin);
        
        // Act
        LoginResponse response = loginService.authenticateAndGenerateToken(nickname, password, tenantId);
        
        // Assert
        assertNotNull(response);
        assertEquals("ADMINISTRADOR", response.getRole());
    }

    @Test
    void testAuthenticateAndGenerateTokenWithBlankTenantId() throws SecurityException {
        // Arrange
        String nickname = "admin";
        String password = "password123";
        String tenantId = "";
        
        UsuarioPeriferico admin = new UsuarioPeriferico();
        admin.setId(1L);
        admin.setNickname(nickname);
        admin.setPassword(password);
        admin.setTenantId("101");
        
        doNothing().when(tenantResolver).setTenantIdentifier("public");
        when(userRepository.findByNicknameForLogin(nickname)).thenReturn(admin);
        
        // Act
        LoginResponse response = loginService.authenticateAndGenerateToken(nickname, password, tenantId);
        
        // Assert
        assertNotNull(response);
        assertNotNull(response.getToken());
    }

    @Test
    void testAuthenticateAndGenerateTokenWithOtherRole() throws SecurityException {
        // Arrange
        String nickname = "user";
        String password = "password123";
        String tenantId = "101";
        
        UsuarioPeriferico user = mock(UsuarioPeriferico.class);
        when(user.getId()).thenReturn(1L);
        when(user.getNickname()).thenReturn(nickname);
        when(user.getPasswordHash()).thenReturn(PasswordUtils.hashPassword(password));
        when(user.getRole()).thenReturn(null);
        when(user.getTenantId()).thenReturn(tenantId);
        when(user.checkPassword(password)).thenReturn(true);
        // No es ProfesionalSalud ni AdministradorClinica
        
        doNothing().when(tenantResolver).setTenantIdentifier("public");
        when(userRepository.findByNicknameForLogin(nickname)).thenReturn(user);
        
        // Act
        LoginResponse response = loginService.authenticateAndGenerateToken(nickname, password, tenantId);
        
        // Assert
        assertNotNull(response);
        assertEquals("OTRO", response.getRole());
    }

    @Test
    void testAuthenticateAndGenerateTokenWithBlankRole() throws SecurityException {
        // Arrange
        String nickname = "prof1";
        String password = "password123";
        String tenantId = "101";
        String schemaName = "schema_clinica_101";
        
        ProfesionalSalud profesional = new ProfesionalSalud();
        profesional.setId(2L);
        profesional.setNickname(nickname);
        profesional.setPassword(password);
        profesional.setRole(""); // Role vacío
        
        doNothing().when(tenantResolver).setTenantIdentifier("public");
        when(userRepository.findByNicknameForLogin(nickname)).thenReturn(null);
        when(userRepository.findByNicknameInTenantSchema(nickname, schemaName)).thenReturn(profesional);
        doNothing().when(tenantResolver).setTenantIdentifier(tenantId);
        
        // Act
        LoginResponse response = loginService.authenticateAndGenerateToken(nickname, password, tenantId);
        
        // Assert
        assertNotNull(response);
        assertEquals("PROFESIONAL", response.getRole()); // Debe deducirse del tipo
    }
}
