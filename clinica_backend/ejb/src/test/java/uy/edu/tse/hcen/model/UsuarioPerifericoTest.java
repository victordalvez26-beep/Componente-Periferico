package uy.edu.tse.hcen.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UsuarioPerifericoTest {

    // Clase concreta para testear la clase abstracta
    static class TestUsuarioPeriferico extends UsuarioPeriferico {
        public TestUsuarioPeriferico() {
            super();
        }
    }
    
    @Test
    void testDefaultConstructor() {
        TestUsuarioPeriferico usuario = new TestUsuarioPeriferico();
        assertNotNull(usuario);
        assertNull(usuario.getNombre());
        assertNull(usuario.getNickname());
    }
    
    @Test
    void testBasicSettersAndGetters() {
        TestUsuarioPeriferico usuario = new TestUsuarioPeriferico();
        usuario.setNickname("admin");
        usuario.setNombre("Administrator");
        usuario.setEmail("admin@clinic.com");
        
        assertEquals("admin", usuario.getNickname());
        assertEquals("Administrator", usuario.getNombre());
        assertEquals("admin@clinic.com", usuario.getEmail());
    }
    
    @Test
    void testPasswordHashing() {
        TestUsuarioPeriferico usuario = new TestUsuarioPeriferico();
        String plainPassword = "myPassword123";
        usuario.setPassword(plainPassword);
        
        assertNotNull(usuario.getPasswordHash());
        assertNotEquals(plainPassword, usuario.getPasswordHash()); // Should be hashed
        assertTrue(usuario.checkPassword(plainPassword));
        assertFalse(usuario.checkPassword("wrongPassword"));
    }
    
    @Test
    void testRoleManagement() {
        TestUsuarioPeriferico usuario = new TestUsuarioPeriferico();
        usuario.setRole("ADMINISTRADOR");
        assertEquals("ADMINISTRADOR", usuario.getRole());
    }
    
    @Test
    void testTenantId() {
        TestUsuarioPeriferico usuario = new TestUsuarioPeriferico();
        usuario.setTenantId("25");
        assertEquals("25", usuario.getTenantId());
    }
}

