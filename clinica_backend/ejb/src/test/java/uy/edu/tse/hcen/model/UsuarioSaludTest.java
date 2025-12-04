package uy.edu.tse.hcen.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class UsuarioSaludTest {

    @Test
    void testDefaultConstructor() {
        UsuarioSalud usuario = new UsuarioSalud();
        assertNotNull(usuario);
        assertNull(usuario.getCi());
        assertNull(usuario.getNombre());
        assertNull(usuario.getApellido());
    }
    
    @Test
    void testSetAllFields() {
        LocalDate fechaNac = LocalDate.of(1990, 5, 15);
        UsuarioSalud usuario = new UsuarioSalud();
        usuario.setCi("12345678");
        usuario.setNombre("Maria");
        usuario.setApellido("Lopez");
        usuario.setFechaNacimiento(fechaNac);
        usuario.setDireccion("Calle 123");
        usuario.setTelefono("099111222");
        usuario.setEmail("maria@test.com");
        usuario.setDepartamento("Montevideo");
        usuario.setLocalidad("Centro");
        
        assertEquals("12345678", usuario.getCi());
        assertEquals("Maria", usuario.getNombre());
        assertEquals("Lopez", usuario.getApellido());
        assertEquals(fechaNac, usuario.getFechaNacimiento());
        assertEquals("Calle 123", usuario.getDireccion());
        assertEquals("099111222", usuario.getTelefono());
        assertEquals("maria@test.com", usuario.getEmail());
        assertEquals("Montevideo", usuario.getDepartamento());
        assertEquals("Centro", usuario.getLocalidad());
    }
    
    @Test
    void testSettersAndGetters() {
        UsuarioSalud usuario = new UsuarioSalud();
        usuario.setCi("87654321");
        usuario.setNombre("Pedro");
        usuario.setApellido("Gomez");
        usuario.setHcenUserId(100L);
        usuario.setTenantId(20L);
        
        assertEquals("87654321", usuario.getCi());
        assertEquals("Pedro", usuario.getNombre());
        assertEquals("Gomez", usuario.getApellido());
        assertEquals(100L, usuario.getHcenUserId());
        assertEquals(20L, usuario.getTenantId());
    }
    
    @Test
    void testFechaNacimiento() {
        LocalDate birthDate = LocalDate.of(1990, 5, 15);
        UsuarioSalud usuario = new UsuarioSalud();
        usuario.setFechaNacimiento(birthDate);
        
        assertEquals(birthDate, usuario.getFechaNacimiento());
    }
}

