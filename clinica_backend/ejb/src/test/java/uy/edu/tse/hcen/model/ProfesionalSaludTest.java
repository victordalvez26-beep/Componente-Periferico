package uy.edu.tse.hcen.model;

import org.junit.jupiter.api.Test;
import uy.edu.tse.hcen.model.enums.Departamentos;
import uy.edu.tse.hcen.model.enums.Especialidad;
import static org.junit.jupiter.api.Assertions.*;

class ProfesionalSaludTest {

    @Test
    void testDefaultConstructor() {
        ProfesionalSalud profesional = new ProfesionalSalud();
        assertNotNull(profesional);
        assertNull(profesional.getEspecialidad());
        assertNull(profesional.getDepartamento());
    }
    
    @Test
    void testSetAllFields() {
        ProfesionalSalud profesional = new ProfesionalSalud();
        profesional.setNombre("Juan");
        profesional.setEmail("juan@test.com");
        profesional.setNickname("drjuan");
        profesional.setPassword("password123");
        profesional.setEspecialidad(Especialidad.CARDIOLOGIA);
        profesional.setDepartamento(Departamentos.MONTEVIDEO);
        
        assertEquals("Juan", profesional.getNombre());
        assertEquals("juan@test.com", profesional.getEmail());
        assertEquals("drjuan", profesional.getNickname());
        assertEquals(Especialidad.CARDIOLOGIA, profesional.getEspecialidad());
        assertEquals(Departamentos.MONTEVIDEO, profesional.getDepartamento());
    }
    
    @Test
    void testSettersAndGetters() {
        ProfesionalSalud profesional = new ProfesionalSalud();
        profesional.setEspecialidad(Especialidad.PEDIATRIA);
        profesional.setDepartamento(Departamentos.CANELONES);
        profesional.setDireccion("Av. Italia 1234");
        
        assertEquals(Especialidad.PEDIATRIA, profesional.getEspecialidad());
        assertEquals(Departamentos.CANELONES, profesional.getDepartamento());
        assertEquals("Av. Italia 1234", profesional.getDireccion());
    }
    
    @Test
    void testDireccion() {
        ProfesionalSalud profesional = new ProfesionalSalud();
        profesional.setDireccion("Calle Test 123");
        assertEquals("Calle Test 123", profesional.getDireccion());
    }
    
    @Test
    void testPasswordIsHashed() {
        ProfesionalSalud profesional = new ProfesionalSalud();
        String plainPassword = "mySecurePass";
        profesional.setPassword(plainPassword);
        
        assertNotNull(profesional.getPasswordHash());
        assertNotEquals(plainPassword, profesional.getPasswordHash());
        assertTrue(profesional.checkPassword(plainPassword));
    }
}

