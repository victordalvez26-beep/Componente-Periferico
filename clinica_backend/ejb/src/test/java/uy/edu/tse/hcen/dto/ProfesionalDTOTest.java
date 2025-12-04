package uy.edu.tse.hcen.dto;

import org.junit.jupiter.api.Test;
import uy.edu.tse.hcen.model.enums.Especialidad;
import static org.junit.jupiter.api.Assertions.*;

class ProfesionalDTOTest {

    @Test
    void testConstructorAndGetters() {
        ProfesionalDTO dto = new ProfesionalDTO();
        dto.setNombre("Juan");
        dto.setEmail("juan@test.com");
        dto.setNickname("drjuan");
        dto.setEspecialidad(Especialidad.MEDICINA_GENERAL);
        dto.setDireccion("Calle 123");
        dto.setPassword("pass123");
        
        assertEquals("Juan", dto.getNombre());
        assertEquals("juan@test.com", dto.getEmail());
        assertEquals("drjuan", dto.getNickname());
        assertEquals(Especialidad.MEDICINA_GENERAL, dto.getEspecialidad());
        assertEquals("Calle 123", dto.getDireccion());
        assertEquals("pass123", dto.getPassword());
    }
    
    @Test
    void testWithNullValues() {
        ProfesionalDTO dto = new ProfesionalDTO();
        assertNull(dto.getNombre());
        assertNull(dto.getEmail());
        assertNull(dto.getNickname());
        assertNull(dto.getEspecialidad());
        assertNull(dto.getDireccion());
        assertNull(dto.getPassword());
    }
}

