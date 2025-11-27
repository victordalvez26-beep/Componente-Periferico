package uy.edu.tse.hcen.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.tse.hcen.model.Usuario;
import uy.edu.tse.hcen.model.UsuarioPeriferico;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioDTOTest {

    private UsuarioDTO dto;

    @BeforeEach
    void setUp() {
        dto = new UsuarioDTO();
    }

    @Test
    void testConstructor() {
        UsuarioDTO dto = new UsuarioDTO();
        assertNotNull(dto);
        assertNull(dto.getId());
        assertNull(dto.getNombre());
        assertNull(dto.getEmail());
    }

    @Test
    void testConstructorWithParameters() {
        Long id = 1L;
        String nombre = "Juan Pérez";
        String email = "juan@example.com";
        
        UsuarioDTO dto = new UsuarioDTO(id, nombre, email);
        
        assertEquals(id, dto.getId());
        assertEquals(nombre, dto.getNombre());
        assertEquals(email, dto.getEmail());
    }

    @Test
    void testId() {
        Long id = 1L;
        dto.setId(id);
        assertEquals(id, dto.getId());
    }

    @Test
    void testIdNull() {
        dto.setId(null);
        assertNull(dto.getId());
    }

    @Test
    void testIdZero() {
        dto.setId(0L);
        assertEquals(0L, dto.getId());
    }

    @Test
    void testIdNegative() {
        dto.setId(-1L);
        assertEquals(-1L, dto.getId());
    }

    @Test
    void testNombre() {
        String nombre = "Juan Pérez";
        dto.setNombre(nombre);
        assertEquals(nombre, dto.getNombre());
    }

    @Test
    void testNombreNull() {
        dto.setNombre(null);
        assertNull(dto.getNombre());
    }

    @Test
    void testNombreEmpty() {
        dto.setNombre("");
        assertEquals("", dto.getNombre());
    }

    @Test
    void testNombreWithSpaces() {
        String nombre = "  Juan Pérez  ";
        dto.setNombre(nombre);
        assertEquals(nombre, dto.getNombre());
    }

    @Test
    void testEmail() {
        String email = "juan@example.com";
        dto.setEmail(email);
        assertEquals(email, dto.getEmail());
    }

    @Test
    void testEmailNull() {
        dto.setEmail(null);
        assertNull(dto.getEmail());
    }

    @Test
    void testEmailEmpty() {
        dto.setEmail("");
        assertEquals("", dto.getEmail());
    }

    @Test
    void testEmailInvalidFormat() {
        String email = "invalid-email";
        dto.setEmail(email);
        assertEquals(email, dto.getEmail());
    }

    @Test
    void testFromEntityWithNull() {
        UsuarioDTO result = UsuarioDTO.fromEntity(null);
        assertNull(result);
    }

    @Test
    void testFromEntityWithUsuario() {
        Usuario usuario = new UsuarioPeriferico();
        usuario.setId(1L);
        usuario.setNombre("Juan Pérez");
        usuario.setEmail("juan@example.com");
        
        UsuarioDTO result = UsuarioDTO.fromEntity(usuario);
        
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Juan Pérez", result.getNombre());
        assertEquals("juan@example.com", result.getEmail());
    }

    @Test
    void testFromEntityWithNullFields() {
        Usuario usuario = new UsuarioPeriferico();
        usuario.setId(null);
        usuario.setNombre(null);
        usuario.setEmail(null);
        
        UsuarioDTO result = UsuarioDTO.fromEntity(usuario);
        
        assertNotNull(result);
        assertNull(result.getId());
        assertNull(result.getNombre());
        assertNull(result.getEmail());
    }

    @Test
    void testAllFields() {
        Long id = 100L;
        String nombre = "María García";
        String email = "maria.garcia@example.com";
        
        dto.setId(id);
        dto.setNombre(nombre);
        dto.setEmail(email);
        
        assertEquals(id, dto.getId());
        assertEquals(nombre, dto.getNombre());
        assertEquals(email, dto.getEmail());
    }

    @Test
    void testLongNames() {
        String longName = "a".repeat(500);
        dto.setNombre(longName);
        assertEquals(longName, dto.getNombre());
    }

    @Test
    void testLongEmails() {
        String longEmail = "a".repeat(200) + "@example.com";
        dto.setEmail(longEmail);
        assertEquals(longEmail, dto.getEmail());
    }
}

