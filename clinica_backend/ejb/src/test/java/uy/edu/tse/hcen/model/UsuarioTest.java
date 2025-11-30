package uy.edu.tse.hcen.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioTest {

    private UsuarioPeriferico usuario;

    @BeforeEach
    void setUp() {
        // Usamos UsuarioPeriferico porque Usuario es abstracta
        usuario = new UsuarioPeriferico();
    }

    @Test
    void testConstructor() {
        UsuarioPeriferico usuario = new UsuarioPeriferico();
        assertNotNull(usuario);
        assertNull(usuario.getId());
        assertNull(usuario.getNombre());
        assertNull(usuario.getEmail());
    }

    @Test
    void testId() {
        Long id = 1L;
        usuario.setId(id);
        assertEquals(id, usuario.getId());
    }

    @Test
    void testIdNull() {
        usuario.setId(null);
        assertNull(usuario.getId());
    }

    @Test
    void testIdZero() {
        usuario.setId(0L);
        assertEquals(0L, usuario.getId());
    }

    @Test
    void testNombre() {
        String nombre = "Juan Pérez";
        usuario.setNombre(nombre);
        assertEquals(nombre, usuario.getNombre());
    }

    @Test
    void testNombreNull() {
        usuario.setNombre(null);
        assertNull(usuario.getNombre());
    }

    @Test
    void testNombreEmpty() {
        usuario.setNombre("");
        assertEquals("", usuario.getNombre());
    }

    @Test
    void testEmail() {
        String email = "juan@example.com";
        usuario.setEmail(email);
        assertEquals(email, usuario.getEmail());
    }

    @Test
    void testEmailNull() {
        usuario.setEmail(null);
        assertNull(usuario.getEmail());
    }

    @Test
    void testEmailEmpty() {
        usuario.setEmail("");
        assertEquals("", usuario.getEmail());
    }

    @Test
    void testAllFields() {
        Long id = 100L;
        String nombre = "María García";
        String email = "maria@example.com";
        
        usuario.setId(id);
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        
        assertEquals(id, usuario.getId());
        assertEquals(nombre, usuario.getNombre());
        assertEquals(email, usuario.getEmail());
    }

    @Test
    void testLongStrings() {
        String longNombre = "a".repeat(500);
        String longEmail = "a".repeat(200) + "@example.com";
        
        usuario.setNombre(longNombre);
        usuario.setEmail(longEmail);
        
        assertEquals(longNombre, usuario.getNombre());
        assertEquals(longEmail, usuario.getEmail());
    }

    @Test
    void testSpecialCharacters() {
        usuario.setNombre("José María O'Connor");
        usuario.setEmail("jose.oconnor@example.com");
        
        assertEquals("José María O'Connor", usuario.getNombre());
        assertEquals("jose.oconnor@example.com", usuario.getEmail());
    }
}

