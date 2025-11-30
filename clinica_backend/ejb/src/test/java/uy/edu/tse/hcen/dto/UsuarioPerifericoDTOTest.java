package uy.edu.tse.hcen.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.tse.hcen.model.UsuarioPeriferico;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioPerifericoDTOTest {

    private UsuarioPerifericoDTO dto;

    @BeforeEach
    void setUp() {
        dto = new UsuarioPerifericoDTO();
    }

    @Test
    void testConstructor() {
        UsuarioPerifericoDTO dto = new UsuarioPerifericoDTO();
        assertNotNull(dto);
        assertNull(dto.getNickname());
        assertNull(dto.getPassword());
    }

    @Test
    void testConstructorWithParameters() {
        Long id = 1L;
        String nombre = "Juan Pérez";
        String email = "juan@example.com";
        String nickname = "jperez";
        String password = "password123";
        
        UsuarioPerifericoDTO dto = new UsuarioPerifericoDTO(id, nombre, email, nickname, password);
        
        assertEquals(id, dto.getId());
        assertEquals(nombre, dto.getNombre());
        assertEquals(email, dto.getEmail());
        assertEquals(nickname, dto.getNickname());
        assertEquals(password, dto.getPassword());
    }

    @Test
    void testNickname() {
        String nickname = "jperez";
        dto.setNickname(nickname);
        assertEquals(nickname, dto.getNickname());
    }

    @Test
    void testNicknameNull() {
        dto.setNickname(null);
        assertNull(dto.getNickname());
    }

    @Test
    void testNicknameEmpty() {
        dto.setNickname("");
        assertEquals("", dto.getNickname());
    }

    @Test
    void testPassword() {
        String password = "password123";
        dto.setPassword(password);
        assertEquals(password, dto.getPassword());
    }

    @Test
    void testPasswordNull() {
        dto.setPassword(null);
        assertNull(dto.getPassword());
    }

    @Test
    void testPasswordEmpty() {
        dto.setPassword("");
        assertEquals("", dto.getPassword());
    }

    @Test
    void testInheritance() {
        dto.setId(1L);
        dto.setNombre("Test");
        dto.setEmail("test@example.com");
        
        assertEquals(1L, dto.getId());
        assertEquals("Test", dto.getNombre());
        assertEquals("test@example.com", dto.getEmail());
    }

    @Test
    void testFromEntityWithNull() {
        UsuarioPerifericoDTO result = UsuarioPerifericoDTO.fromEntity(null);
        assertNull(result);
    }

    @Test
    void testFromEntityWithUsuarioPeriferico() {
        UsuarioPeriferico usuario = new UsuarioPeriferico();
        usuario.setId(1L);
        usuario.setNombre("Juan Pérez");
        usuario.setEmail("juan@example.com");
        usuario.setNickname("jperez");
        usuario.setPassword("hashedPassword");
        
        UsuarioPerifericoDTO result = UsuarioPerifericoDTO.fromEntity(usuario);
        
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Juan Pérez", result.getNombre());
        assertEquals("juan@example.com", result.getEmail());
        assertEquals("jperez", result.getNickname());
        assertNull(result.getPassword()); // Password no debe estar en el DTO por seguridad
    }

    @Test
    void testFromEntityWithNullFields() {
        UsuarioPeriferico usuario = new UsuarioPeriferico();
        usuario.setId(null);
        usuario.setNombre(null);
        usuario.setEmail(null);
        usuario.setNickname(null);
        
        UsuarioPerifericoDTO result = UsuarioPerifericoDTO.fromEntity(usuario);
        
        assertNotNull(result);
        assertNull(result.getId());
        assertNull(result.getNombre());
        assertNull(result.getEmail());
        assertNull(result.getNickname());
        assertNull(result.getPassword());
    }

    @Test
    void testToEntity() {
        dto.setId(1L);
        dto.setNombre("Juan Pérez");
        dto.setEmail("juan@example.com");
        dto.setNickname("jperez");
        dto.setPassword("password123");
        
        UsuarioPeriferico entity = dto.toEntity();
        
        assertNotNull(entity);
        assertEquals(1L, entity.getId());
        assertEquals("Juan Pérez", entity.getNombre());
        assertEquals("juan@example.com", entity.getEmail());
        assertEquals("jperez", entity.getNickname());
        assertNotNull(entity.getPasswordHash()); // Password debe estar hasheado
    }

    @Test
    void testToEntityWithNullId() {
        dto.setId(null);
        dto.setNombre("Test");
        dto.setEmail("test@example.com");
        dto.setNickname("test");
        dto.setPassword("password");
        
        UsuarioPeriferico entity = dto.toEntity();
        
        assertNotNull(entity);
        assertNull(entity.getId());
        assertEquals("Test", entity.getNombre());
    }

    @Test
    void testToEntityWithNullPassword() {
        dto.setId(1L);
        dto.setNombre("Test");
        dto.setEmail("test@example.com");
        dto.setNickname("test");
        dto.setPassword(null);
        
        UsuarioPeriferico entity = dto.toEntity();
        
        assertNotNull(entity);
        assertEquals("test", entity.getNickname());
        // Password hash puede ser null si no se proporcionó password
    }

    @Test
    void testToEntityPasswordHashing() {
        dto.setPassword("plainPassword");
        UsuarioPeriferico entity = dto.toEntity();
        
        assertNotNull(entity.getPasswordHash());
        assertNotEquals("plainPassword", entity.getPasswordHash());
        assertTrue(entity.checkPassword("plainPassword"));
    }

    @Test
    void testAllFields() {
        Long id = 100L;
        String nombre = "María García";
        String email = "maria@example.com";
        String nickname = "mgarcia";
        String password = "securePassword123";
        
        dto.setId(id);
        dto.setNombre(nombre);
        dto.setEmail(email);
        dto.setNickname(nickname);
        dto.setPassword(password);
        
        assertEquals(id, dto.getId());
        assertEquals(nombre, dto.getNombre());
        assertEquals(email, dto.getEmail());
        assertEquals(nickname, dto.getNickname());
        assertEquals(password, dto.getPassword());
    }

    @Test
    void testSpecialCharactersInNickname() {
        String nickname = "user_123-test";
        dto.setNickname(nickname);
        assertEquals(nickname, dto.getNickname());
    }

    @Test
    void testLongPassword() {
        String longPassword = "a".repeat(200);
        dto.setPassword(longPassword);
        assertEquals(longPassword, dto.getPassword());
    }
}

