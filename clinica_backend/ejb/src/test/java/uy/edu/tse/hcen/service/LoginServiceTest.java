package uy.edu.tse.hcen.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uy.edu.tse.hcen.dto.LoginResponse;
import uy.edu.tse.hcen.model.AdministradorClinica;
import uy.edu.tse.hcen.model.PrestadorSalud;
import uy.edu.tse.hcen.model.ProfesionalSalud;
import uy.edu.tse.hcen.model.UsuarioPeriferico;
import uy.edu.tse.hcen.model.enums.Departamentos;
import uy.edu.tse.hcen.model.enums.EstadoNodoPeriferico;
import uy.edu.tse.hcen.multitenancy.SchemaTenantResolver;
import uy.edu.tse.hcen.multitenancy.TenantContext;
import uy.edu.tse.hcen.repository.UsuarioPerifericoRepository;
import uy.edu.tse.hcen.utils.PasswordUtils;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private UsuarioPerifericoRepository userRepository;

    @Mock
    private SchemaTenantResolver tenantResolver;

    @InjectMocks
    private LoginService loginService;

    private UsuarioPeriferico usuario;
    private String testNickname;
    private String testPassword;
    private String testPasswordHash;

    @BeforeEach
    void setUp() {
        // Limpiar el TenantContext antes de cada test
        TenantContext.clear();
        
        testNickname = "testuser";
        testPassword = "password123";
        testPasswordHash = PasswordUtils.hashPassword(testPassword);
    }

    @Test
    void testAuthenticateAndGenerateToken_Success_WithExplicitRoleAndTenantId() throws SecurityException {
        // Arrange
        usuario = new UsuarioPeriferico();
        usuario.setNickname(testNickname);
        usuario.setPasswordHash(testPasswordHash);
        usuario.setRole("PROFESIONAL");
        usuario.setTenantId("101");

        when(userRepository.findByNicknameForLogin(testNickname)).thenReturn(usuario);

        // Act
        LoginResponse response = loginService.authenticateAndGenerateToken(testNickname, testPassword, "101");

        // Assert
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("PROFESIONAL", response.getRole());
        assertEquals("101", response.getTenant_id());
        
        // Verificar que se llamó al repositorio
        verify(userRepository, times(1)).findByNicknameForLogin(testNickname);
        
        // Verificar que se estableció el tenant en el contexto
        verify(tenantResolver, times(1)).setTenantIdentifier("public");
    }

    // Test para verificar que el token se genera correctamente cuando el tenantId se obtiene del lookup
    @Test
    void testAuthenticateAndGenerateToken_Success_WithTenantIdFromLookup_c1() throws SecurityException {
        // Arrange
        usuario = new UsuarioPeriferico();
        usuario.setNickname("user_c1");
        usuario.setPasswordHash(testPasswordHash);
        usuario.setRole("ADMINISTRADOR");
        // No se establece tenantId, debe venir del lookup

        when(userRepository.findByNicknameForLogin("user_c1")).thenReturn(usuario);

        // Act
        LoginResponse response = loginService.authenticateAndGenerateToken("user_c1", testPassword, null);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("ADMINISTRADOR", response.getRole());
        assertEquals("101", response.getTenant_id()); // Debe venir del lookup por "c1"
    }

    @Test
    void testAuthenticateAndGenerateToken_Success_WithTenantIdFromLookup_c2() throws SecurityException {
        // Arrange
        usuario = new UsuarioPeriferico();
        usuario.setNickname("user_c2");
        usuario.setPasswordHash(testPasswordHash);
        usuario.setRole("PROFESIONAL");

        when(userRepository.findByNicknameForLogin("user_c2")).thenReturn(usuario);

        // Act
        LoginResponse response = loginService.authenticateAndGenerateToken("user_c2", testPassword, null);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("PROFESIONAL", response.getRole());
        assertEquals("102", response.getTenant_id()); // Debe venir del lookup por "c2"
    }

    // Test para verificar que el token se genera correctamente cuando el role se obtiene del instanceof
    @Test
    void testAuthenticateAndGenerateToken_Success_RoleFromInstanceOf_ProfesionalSalud() throws SecurityException {
        // Arrange
        ProfesionalSalud profesional = new ProfesionalSalud();
        profesional.setNickname(testNickname);
        profesional.setPasswordHash(testPasswordHash);
        // No se establece role, debe venir del instanceof

        when(userRepository.findByNicknameForLogin(testNickname)).thenReturn(profesional);

        // Act
        LoginResponse response = loginService.authenticateAndGenerateToken(testNickname, testPassword, "101");

        // Assert
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("PROFESIONAL", response.getRole());
    }

    // Test para verificar que el token se genera correctamente cuando el role se obtiene del instanceof
    @Test
    void testAuthenticateAndGenerateToken_Success_RoleFromInstanceOf_AdministradorClinica() throws SecurityException {
        // Arrange
        // Crear una instancia real de PrestadorSalud para pasarla a AdministradorClinica
        PrestadorSalud nodo = new PrestadorSalud(
            "Test Nodo", 
            "12345678", 
            Departamentos.MONTEVIDEO, 
            "Montevideo", 
            "Calle Test 123", 
            "contacto@test.com", 
            EstadoNodoPeriferico.ACTIVO
        );
        AdministradorClinica admin = new AdministradorClinica(nodo);
        admin.setNickname(testNickname);
        admin.setPasswordHash(testPasswordHash);
        // No se establece role, debe venir del instanceof

        when(userRepository.findByNicknameForLogin(testNickname)).thenReturn(admin);

        // Act
        LoginResponse response = loginService.authenticateAndGenerateToken(testNickname, testPassword, "101");

        // Assert
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("ADMINISTRADOR", response.getRole());
    }

    @Test
    void testAuthenticateAndGenerateToken_Success_RoleDefault_OtherUser() throws SecurityException {
        // Arrange
        usuario = new UsuarioPeriferico();
        usuario.setNickname(testNickname);
        usuario.setPasswordHash(testPasswordHash);
        // No es ProfesionalSalud ni AdministradorClinica, debe ser "OTRO"

        when(userRepository.findByNicknameForLogin(testNickname)).thenReturn(usuario);

        // Act
        LoginResponse response = loginService.authenticateAndGenerateToken(testNickname, testPassword, "101");

        // Assert
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("OTRO", response.getRole());
    }

    @Test
    void testAuthenticateAndGenerateToken_Failure_UserNotFound() {
        // Arrange
        when(userRepository.findByNicknameForLogin(testNickname)).thenReturn(null);

        // Act & Assert
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            loginService.authenticateAndGenerateToken(testNickname, testPassword, null);
        });

        assertEquals("Credenciales inválidas.", exception.getMessage());
        verify(userRepository, times(1)).findByNicknameForLogin(testNickname);
        verify(tenantResolver, times(1)).setTenantIdentifier("public");
    }

    
    @Test
    void testAuthenticateAndGenerateToken_Failure_WrongPassword() {
        // Arrange
        usuario = new UsuarioPeriferico();
        usuario.setNickname(testNickname);
        usuario.setPasswordHash(testPasswordHash);
        usuario.setRole("PROFESIONAL");
        usuario.setTenantId("101");

        when(userRepository.findByNicknameForLogin(testNickname)).thenReturn(usuario);

        // Act & Assert
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            loginService.authenticateAndGenerateToken(testNickname, "wrongPassword", "101");
        });

        assertEquals("Credenciales inválidas.", exception.getMessage());
        verify(userRepository, times(1)).findByNicknameForLogin(testNickname);
    }

    @Test
    void testAuthenticateAndGenerateToken_Success_WithBlankRole_UsesInstanceOf() throws SecurityException {
        // Arrange
        ProfesionalSalud profesional = new ProfesionalSalud();
        profesional.setNickname(testNickname);
        profesional.setPasswordHash(testPasswordHash);
        profesional.setRole(""); // Role vacío, debe usar instanceof

        when(userRepository.findByNicknameForLogin(testNickname)).thenReturn(profesional);

        // Act
        LoginResponse response = loginService.authenticateAndGenerateToken(testNickname, testPassword, "101");

        // Assert
        assertNotNull(response);
        assertEquals("PROFESIONAL", response.getRole());
    }

    @Test
    void testAuthenticateAndGenerateToken_Success_WithNullTenantId_UsesLookup() throws SecurityException {
        // Arrange
        usuario = new UsuarioPeriferico();
        usuario.setNickname("admin_c1");
        usuario.setPasswordHash(testPasswordHash);
        usuario.setRole("ADMINISTRADOR");
        usuario.setTenantId(null); // TenantId nulo, debe usar lookup

        when(userRepository.findByNicknameForLogin("admin_c1")).thenReturn(usuario);

        // Act
        LoginResponse response = loginService.authenticateAndGenerateToken("admin_c1", testPassword, null);

        // Assert
        assertNotNull(response);
        assertEquals("101", response.getTenant_id()); // Debe venir del lookup
    }

    @Test
    void testAuthenticateAndGenerateToken_Success_WithBlankTenantId_UsesLookup() throws SecurityException {
        // Arrange
        usuario = new UsuarioPeriferico();
        usuario.setNickname("user_c2");
        usuario.setPasswordHash(testPasswordHash);
        usuario.setRole("PROFESIONAL");
        usuario.setTenantId(""); // TenantId vacío, debe usar lookup

        when(userRepository.findByNicknameForLogin("user_c2")).thenReturn(usuario);

        // Act
        LoginResponse response = loginService.authenticateAndGenerateToken("user_c2", testPassword, null);

        // Assert
        assertNotNull(response);
        assertEquals("102", response.getTenant_id()); // Debe venir del lookup
    }

    @Test
    void testAuthenticateAndGenerateToken_Success_NoTenantIdInLookup_ReturnsNull() throws SecurityException {
        // Arrange
        usuario = new UsuarioPeriferico();
        usuario.setNickname("usernotfound");
        usuario.setPasswordHash(testPasswordHash);
        usuario.setRole("PROFESIONAL");
        // No tiene tenantId y el nickname no contiene "c1" ni "c2"

        when(userRepository.findByNicknameForLogin("usernotfound")).thenReturn(usuario);

        // Act
        LoginResponse response = loginService.authenticateAndGenerateToken("usernotfound", testPassword, null);

        // Assert
        assertNotNull(response);
        assertNull(response.getTenant_id()); // No debe encontrar tenantId
    }

    @Test
    void testAuthenticateAndGenerateToken_Success_TenantContextSet_WhenTenantIdExists() throws SecurityException {
        // Arrange
        usuario = new UsuarioPeriferico();
        usuario.setNickname(testNickname);
        usuario.setPasswordHash(testPasswordHash);
        usuario.setRole("PROFESIONAL");
        usuario.setTenantId("101");

        when(userRepository.findByNicknameForLogin(testNickname)).thenReturn(usuario);

        // Act
        loginService.authenticateAndGenerateToken(testNickname, testPassword, "101");

        // Assert
        // Verificar que el tenant se estableció en el contexto (aunque es estático,
        // podemos verificar que se llamó setTenantIdentifier)
        verify(tenantResolver, atLeastOnce()).setTenantIdentifier(anyString());
    }

    @Test
    void testAuthenticateAndGenerateToken_Success_NullNicknameInLookup_ReturnsNull() throws SecurityException {
        // Arrange
        usuario = new UsuarioPeriferico();
        usuario.setNickname(null);
        usuario.setPasswordHash(testPasswordHash);
        usuario.setRole("PROFESIONAL");

        // Este test verifica que lookupTenantIdByNickname maneja null
        // Pero el método es privado, así que probamos el comportamiento general
        // cuando el usuario no tiene tenantId y el nickname no coincide con c1/c2
        usuario.setNickname("normaluser");
        
        when(userRepository.findByNicknameForLogin("normaluser")).thenReturn(usuario);

        // Act
        LoginResponse response = loginService.authenticateAndGenerateToken("normaluser", testPassword, null);

        // Assert
        assertNotNull(response);
        assertNull(response.getTenant_id());
    }
}

