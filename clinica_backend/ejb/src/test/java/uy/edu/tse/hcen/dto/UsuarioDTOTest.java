package uy.edu.tse.hcen.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uy.edu.tse.hcen.model.Usuario;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UsuarioDTO Tests")
class UsuarioDTOTest {

    @Test
    void defaultConstructor_shouldWork() {
        UsuarioDTO dto = new UsuarioDTO();
        assertNotNull(dto);
        assertNull(dto.getId());
    }

    @Test
    void parameterizedConstructor_shouldSetValues() {
        UsuarioDTO dto = new UsuarioDTO(1L, "Juan", "juan@email.com");
        
        assertEquals(1L, dto.getId());
        assertEquals("Juan", dto.getNombre());
        assertEquals("juan@email.com", dto.getEmail());
    }

    @Test
    void settersAndGetters_shouldWork() {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(2L);
        dto.setNombre("María");
        dto.setEmail("maria@email.com");
        
        assertEquals(2L, dto.getId());
        assertEquals("María", dto.getNombre());
        assertEquals("maria@email.com", dto.getEmail());
    }

    static class TestUsuario extends Usuario {
        public TestUsuario() {
            super();
        }
    }

    @Test
    void fromEntity_withValidEntity_shouldCreateDTO() {
        TestUsuario usuario = new TestUsuario();
        usuario.setId(3L);
        usuario.setNombre("Pedro");
        usuario.setEmail("pedro@email.com");
        
        UsuarioDTO dto = UsuarioDTO.fromEntity(usuario);
        
        assertNotNull(dto);
        assertEquals(3L, dto.getId());
        assertEquals("Pedro", dto.getNombre());
        assertEquals("pedro@email.com", dto.getEmail());
    }

    @Test
    void fromEntity_withNull_shouldReturnNull() {
        UsuarioDTO dto = UsuarioDTO.fromEntity(null);
        assertNull(dto);
    }
}

