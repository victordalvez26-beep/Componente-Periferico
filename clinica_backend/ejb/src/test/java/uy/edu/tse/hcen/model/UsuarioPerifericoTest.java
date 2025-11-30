package uy.edu.tse.hcen.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioPerifericoTest {

    private UsuarioPeriferico usuario;

    @BeforeEach
    void setUp() {
        usuario = new UsuarioPeriferico();
    }

    @Test
    void testConstructor() {
        UsuarioPeriferico usuario = new UsuarioPeriferico();
        assertNotNull(usuario);
        assertNull(usuario.getNickname());
        assertNull(usuario.getPasswordHash());
        assertNull(usuario.getTenantId());
        assertNull(usuario.getRole());
    }

    @Test
    void testNickname() {
        String nickname = "jperez";
        usuario.setNickname(nickname);
        assertEquals(nickname, usuario.getNickname());
    }

    @Test
    void testNicknameNull() {
        usuario.setNickname(null);
        assertNull(usuario.getNickname());
    }

    @Test
    void testNicknameEmpty() {
        usuario.setNickname("");
        assertEquals("", usuario.getNickname());
    }

    @Test
    void testSetPassword() {
        String password = "password123";
        usuario.setPassword(password);
        
        assertNotNull(usuario.getPasswordHash());
        assertNotEquals(password, usuario.getPasswordHash());
        assertTrue(usuario.checkPassword(password));
    }

    @Test
    void testSetPasswordNull() {
        // BCrypt lanza IllegalArgumentException, no NullPointerException
        assertThrows(IllegalArgumentException.class, () -> {
            usuario.setPassword(null);
        });
    }

    @Test
    void testSetPasswordEmpty() {
        usuario.setPassword("");
        assertNotNull(usuario.getPasswordHash());
        assertTrue(usuario.checkPassword(""));
    }

    @Test
    void testSetPasswordHash() {
        String hash = "$2a$10$testHash";
        usuario.setPasswordHash(hash);
        assertEquals(hash, usuario.getPasswordHash());
    }

    @Test
    void testSetPasswordHashNull() {
        usuario.setPasswordHash(null);
        assertNull(usuario.getPasswordHash());
    }

    @Test
    void testCheckPassword() {
        String password = "testPassword123";
        usuario.setPassword(password);
        
        assertTrue(usuario.checkPassword(password));
        assertFalse(usuario.checkPassword("wrongPassword"));
    }

    @Test
    void testCheckPasswordWithNull() {
        usuario.setPassword("password");
        
        // BCrypt lanza IllegalArgumentException, no NullPointerException
        assertThrows(IllegalArgumentException.class, () -> {
            usuario.checkPassword(null);
        });
    }

    @Test
    void testCheckPasswordWithNullHash() {
        usuario.setPasswordHash(null);
        
        // Si el hash es null, checkPassword retorna false, no lanza excepción
        assertFalse(usuario.checkPassword("password"));
    }

    @Test
    void testTenantId() {
        String tenantId = "tenant-123";
        usuario.setTenantId(tenantId);
        assertEquals(tenantId, usuario.getTenantId());
    }

    @Test
    void testTenantIdNull() {
        usuario.setTenantId(null);
        assertNull(usuario.getTenantId());
    }

    @Test
    void testRole() {
        String role = "PROFESIONAL";
        usuario.setRole(role);
        assertEquals(role, usuario.getRole());
    }

    @Test
    void testRoleNull() {
        usuario.setRole(null);
        assertNull(usuario.getRole());
    }

    @Test
    void testInheritance() {
        usuario.setId(1L);
        usuario.setNombre("Test");
        usuario.setEmail("test@example.com");
        
        assertEquals(1L, usuario.getId());
        assertEquals("Test", usuario.getNombre());
        assertEquals("test@example.com", usuario.getEmail());
    }

    @Test
    void testAllFields() {
        Long id = 1L;
        String nombre = "Juan Pérez";
        String email = "juan@example.com";
        String nickname = "jperez";
        String password = "password123";
        String tenantId = "tenant-123";
        String role = "PROFESIONAL";
        
        usuario.setId(id);
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        usuario.setNickname(nickname);
        usuario.setPassword(password);
        usuario.setTenantId(tenantId);
        usuario.setRole(role);
        
        assertEquals(id, usuario.getId());
        assertEquals(nombre, usuario.getNombre());
        assertEquals(email, usuario.getEmail());
        assertEquals(nickname, usuario.getNickname());
        assertNotNull(usuario.getPasswordHash());
        assertEquals(tenantId, usuario.getTenantId());
        assertEquals(role, usuario.getRole());
        assertTrue(usuario.checkPassword(password));
    }

    @Test
    void testPasswordHashing() {
        String password = "plainPassword";
        usuario.setPassword(password);
        
        String hash = usuario.getPasswordHash();
        assertNotNull(hash);
        assertNotEquals(password, hash);
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$"));
    }

    @Test
    void testMultiplePasswordChanges() {
        usuario.setPassword("password1");
        String hash1 = usuario.getPasswordHash();
        assertTrue(usuario.checkPassword("password1"));
        
        usuario.setPassword("password2");
        String hash2 = usuario.getPasswordHash();
        assertNotEquals(hash1, hash2);
        assertTrue(usuario.checkPassword("password2"));
        assertFalse(usuario.checkPassword("password1"));
    }

    @Test
    void testSpecialCharactersInNickname() {
        String nickname = "user_123-test";
        usuario.setNickname(nickname);
        assertEquals(nickname, usuario.getNickname());
    }

    @Test
    void testLongPassword() {
        String longPassword = "a".repeat(200);
        usuario.setPassword(longPassword);
        assertTrue(usuario.checkPassword(longPassword));
    }
}

