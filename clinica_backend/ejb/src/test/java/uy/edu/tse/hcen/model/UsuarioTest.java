package uy.edu.tse.hcen.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Usuario Model Tests")
class UsuarioTest {

    static class TestUsuario extends Usuario {
        public TestUsuario() {
            super();
        }
    }

    @Test
    void defaultConstructor_shouldWork() {
        TestUsuario usuario = new TestUsuario();
        assertNotNull(usuario);
        assertNull(usuario.getNombre());
    }

    @Test
    void settersAndGetters_shouldWork() {
        TestUsuario usuario = new TestUsuario();
        usuario.setId(1L);
        usuario.setNombre("Juan");
        usuario.setEmail("juan@email.com");
        
        assertEquals(1L, usuario.getId());
        assertEquals("Juan", usuario.getNombre());
        assertEquals("juan@email.com", usuario.getEmail());
    }

    @Test
    void emailCanBeNull() {
        TestUsuario usuario = new TestUsuario();
        usuario.setEmail(null);
        assertNull(usuario.getEmail());
    }
}

