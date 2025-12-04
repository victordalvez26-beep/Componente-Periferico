package uy.edu.tse.hcen.dto;

import org.junit.jupiter.api.Test;
import uy.edu.tse.hcen.model.ProfesionalSalud;
import uy.edu.tse.hcen.model.enums.Especialidad;
import static org.junit.jupiter.api.Assertions.*;

class ProfesionalResponseTest {

    @Test
    void testDefaultConstructor() {
        ProfesionalResponse response = new ProfesionalResponse();
        assertNotNull(response);
        assertNull(response.getId());
    }
    
    @Test
    void testSettersAndGetters() {
        ProfesionalResponse response = new ProfesionalResponse();
        response.setId(5L);
        response.setNombre("Maria Lopez");
        response.setEmail("maria@clinic.com");
        response.setNickname("drmaria");
        response.setEspecialidad("PEDIATRIA");
        response.setDireccion("Av. Italia 123");
        
        assertEquals(5L, response.getId());
        assertEquals("Maria Lopez", response.getNombre());
        assertEquals("maria@clinic.com", response.getEmail());
        assertEquals("drmaria", response.getNickname());
        assertEquals("PEDIATRIA", response.getEspecialidad());
        assertEquals("Av. Italia 123", response.getDireccion());
    }
    
    @Test
    void testFromEntity() {
        ProfesionalSalud profesional = new ProfesionalSalud();
        profesional.setId(10L);
        profesional.setNombre("Juan");
        profesional.setEmail("juan@test.com");
        profesional.setNickname("drjuan");
        profesional.setEspecialidad(Especialidad.CARDIOLOGIA);
        profesional.setDireccion("Calle 456");
        
        ProfesionalResponse response = ProfesionalResponse.fromEntity(profesional);
        
        assertEquals(10L, response.getId());
        assertEquals("Juan", response.getNombre());
        assertEquals("juan@test.com", response.getEmail());
        assertEquals("drjuan", response.getNickname());
        assertEquals("CARDIOLOGIA", response.getEspecialidad());
        assertEquals("Calle 456", response.getDireccion());
    }
}

