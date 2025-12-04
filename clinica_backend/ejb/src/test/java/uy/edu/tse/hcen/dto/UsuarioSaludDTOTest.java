package uy.edu.tse.hcen.dto;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class UsuarioSaludDTOTest {

    @Test
    void testConstructorAndGetters() {
        UsuarioSaludDTO dto = new UsuarioSaludDTO();
        dto.setCi("12345678");
        dto.setNombre("Maria");
        dto.setApellido("Garcia");
        dto.setFechaNacimiento(LocalDate.of(1990, 5, 15));
        dto.setDireccion("Calle 123");
        dto.setTelefono("099999999");
        dto.setEmail("maria@test.com");
        dto.setDepartamento("Montevideo");
        dto.setLocalidad("Centro");
        dto.setHcenUserId(100L);
        
        assertEquals("12345678", dto.getCi());
        assertEquals("Maria", dto.getNombre());
        assertEquals("Garcia", dto.getApellido());
        assertEquals(LocalDate.of(1990, 5, 15), dto.getFechaNacimiento());
        assertEquals("Calle 123", dto.getDireccion());
        assertEquals("099999999", dto.getTelefono());
        assertEquals("maria@test.com", dto.getEmail());
        assertEquals("Montevideo", dto.getDepartamento());
        assertEquals("Centro", dto.getLocalidad());
        assertEquals(100L, dto.getHcenUserId());
    }
    
    @Test
    void testWithMinimalData() {
        UsuarioSaludDTO dto = new UsuarioSaludDTO();
        dto.setCi("11111111");
        dto.setId(5L);
        
        assertEquals("11111111", dto.getCi());
        assertEquals(5L, dto.getId());
        assertNull(dto.getNombre());
        assertNull(dto.getApellido());
    }
}

