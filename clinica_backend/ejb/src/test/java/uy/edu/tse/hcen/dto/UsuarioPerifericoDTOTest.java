package uy.edu.tse.hcen.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uy.edu.tse.hcen.model.UsuarioPeriferico;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UsuarioPerifericoDTO Tests")
class UsuarioPerifericoDTOTest {

    @Test
    void defaultConstructor_shouldWork() {
        UsuarioPerifericoDTO dto = new UsuarioPerifericoDTO();
        assertNotNull(dto);
        assertNull(dto.getNickname());
    }

    @Test
    void parameterizedConstructor_shouldSetValues() {
        UsuarioPerifericoDTO dto = new UsuarioPerifericoDTO(
                1L, "Juan", "juan@email.com", "juan123", "password");
        
        assertEquals(1L, dto.getId());
        assertEquals("Juan", dto.getNombre());
        assertEquals("juan@email.com", dto.getEmail());
        assertEquals("juan123", dto.getNickname());
        assertEquals("password", dto.getPassword());
    }

    @Test
    void settersAndGetters_shouldWork() {
        UsuarioPerifericoDTO dto = new UsuarioPerifericoDTO();
        dto.setNickname("user1");
        dto.setPassword("pass123");
        
        assertEquals("user1", dto.getNickname());
        assertEquals("pass123", dto.getPassword());
    }

    @Test
    void fromEntity_withValidEntity_shouldCreateDTO() {
        UsuarioPeriferico usuario = new UsuarioPeriferico();
        usuario.setId(1L);
        usuario.setNombre("Test");
        usuario.setEmail("test@email.com");
        usuario.setNickname("test123");
        
        UsuarioPerifericoDTO dto = UsuarioPerifericoDTO.fromEntity(usuario);
        
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("test123", dto.getNickname());
        assertNull(dto.getPassword()); // Password no se expone
    }

    @Test
    void fromEntity_withNull_shouldReturnNull() {
        UsuarioPerifericoDTO dto = UsuarioPerifericoDTO.fromEntity(null);
        assertNull(dto);
    }

    @Test
    void toEntity_shouldCreateEntity() {
        UsuarioPerifericoDTO dto = new UsuarioPerifericoDTO(
                1L, "Juan", "juan@email.com", "juan123", "password");
        
        UsuarioPeriferico entity = dto.toEntity();
        
        assertNotNull(entity);
        assertEquals(1L, entity.getId());
        assertEquals("Juan", entity.getNombre());
        assertEquals("juan123", entity.getNickname());
        assertNotNull(entity.getPasswordHash()); // Password fue hasheado
    }

    @Test
    void toEntity_withoutPassword_shouldNotSetHash() {
        UsuarioPerifericoDTO dto = new UsuarioPerifericoDTO();
        dto.setNickname("user1");
        dto.setPassword(null);
        
        UsuarioPeriferico entity = dto.toEntity();
        
        assertNotNull(entity);
        assertEquals("user1", entity.getNickname());
    }
}

