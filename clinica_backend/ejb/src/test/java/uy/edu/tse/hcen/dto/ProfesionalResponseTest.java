package uy.edu.tse.hcen.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.tse.hcen.model.ProfesionalSalud;
import uy.edu.tse.hcen.model.enums.Especialidad;

import static org.junit.jupiter.api.Assertions.*;

class ProfesionalResponseTest {

    private ProfesionalResponse response;

    @BeforeEach
    void setUp() {
        response = new ProfesionalResponse();
    }

    @Test
    void testConstructor() {
        ProfesionalResponse response = new ProfesionalResponse();
        assertNotNull(response);
        assertNull(response.getId());
        assertNull(response.getNombre());
        assertNull(response.getEmail());
        assertNull(response.getNickname());
        assertNull(response.getEspecialidad());
        assertNull(response.getDireccion());
    }

    @Test
    void testId() {
        Long id = 1L;
        response.setId(id);
        assertEquals(id, response.getId());
    }

    @Test
    void testIdNull() {
        response.setId(null);
        assertNull(response.getId());
    }

    @Test
    void testNombre() {
        String nombre = "Dr. Juan Pérez";
        response.setNombre(nombre);
        assertEquals(nombre, response.getNombre());
    }

    @Test
    void testNombreNull() {
        response.setNombre(null);
        assertNull(response.getNombre());
    }

    @Test
    void testEmail() {
        String email = "juan.perez@clinica.com";
        response.setEmail(email);
        assertEquals(email, response.getEmail());
    }

    @Test
    void testEmailNull() {
        response.setEmail(null);
        assertNull(response.getEmail());
    }

    @Test
    void testNickname() {
        String nickname = "jperez";
        response.setNickname(nickname);
        assertEquals(nickname, response.getNickname());
    }

    @Test
    void testNicknameNull() {
        response.setNickname(null);
        assertNull(response.getNickname());
    }

    @Test
    void testEspecialidad() {
        String especialidad = "CARDIOLOGIA";
        response.setEspecialidad(especialidad);
        assertEquals(especialidad, response.getEspecialidad());
    }

    @Test
    void testEspecialidadNull() {
        response.setEspecialidad(null);
        assertNull(response.getEspecialidad());
    }

    @Test
    void testDireccion() {
        String direccion = "Av. 18 de Julio 1234";
        response.setDireccion(direccion);
        assertEquals(direccion, response.getDireccion());
    }

    @Test
    void testDireccionNull() {
        response.setDireccion(null);
        assertNull(response.getDireccion());
    }

    @Test
    void testFromEntityWithNull() {
        // El método fromEntity no maneja null, lanza NullPointerException
        assertThrows(NullPointerException.class, () -> {
            ProfesionalResponse.fromEntity(null);
        });
    }

    @Test
    void testFromEntityWithProfesionalSalud() {
        ProfesionalSalud profesional = new ProfesionalSalud();
        profesional.setId(1L);
        profesional.setNombre("Dr. Juan Pérez");
        profesional.setEmail("juan.perez@clinica.com");
        profesional.setNickname("jperez");
        profesional.setEspecialidad(Especialidad.CARDIOLOGIA);
        profesional.setDireccion("Av. 18 de Julio 1234");
        
        ProfesionalResponse result = ProfesionalResponse.fromEntity(profesional);
        
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Dr. Juan Pérez", result.getNombre());
        assertEquals("juan.perez@clinica.com", result.getEmail());
        assertEquals("jperez", result.getNickname());
        assertEquals("CARDIOLOGIA", result.getEspecialidad());
        assertEquals("Av. 18 de Julio 1234", result.getDireccion());
    }

    @Test
    void testFromEntityWithNullEspecialidad() {
        ProfesionalSalud profesional = new ProfesionalSalud();
        profesional.setId(1L);
        profesional.setNombre("Dr. Test");
        profesional.setEspecialidad(null);
        
        ProfesionalResponse result = ProfesionalResponse.fromEntity(profesional);
        
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Dr. Test", result.getNombre());
        assertNull(result.getEspecialidad());
    }

    @Test
    void testFromEntityWithAllEspecialidades() {
        for (Especialidad esp : Especialidad.values()) {
            ProfesionalSalud profesional = new ProfesionalSalud();
            profesional.setId(1L);
            profesional.setNombre("Dr. Test");
            profesional.setEspecialidad(esp);
            
            ProfesionalResponse result = ProfesionalResponse.fromEntity(profesional);
            
            assertNotNull(result);
            assertEquals(esp.name(), result.getEspecialidad());
        }
    }

    @Test
    void testFromEntityWithNullFields() {
        ProfesionalSalud profesional = new ProfesionalSalud();
        profesional.setId(null);
        profesional.setNombre(null);
        profesional.setEmail(null);
        profesional.setNickname(null);
        profesional.setEspecialidad(null);
        profesional.setDireccion(null);
        
        ProfesionalResponse result = ProfesionalResponse.fromEntity(profesional);
        
        assertNotNull(result);
        assertNull(result.getId());
        assertNull(result.getNombre());
        assertNull(result.getEmail());
        assertNull(result.getNickname());
        assertNull(result.getEspecialidad());
        assertNull(result.getDireccion());
    }

    @Test
    void testAllFields() {
        Long id = 100L;
        String nombre = "Dr. María García";
        String email = "maria.garcia@clinica.com";
        String nickname = "mgarcia";
        String especialidad = "PEDIATRIA";
        String direccion = "Bvar. Artigas 5678";
        
        response.setId(id);
        response.setNombre(nombre);
        response.setEmail(email);
        response.setNickname(nickname);
        response.setEspecialidad(especialidad);
        response.setDireccion(direccion);
        
        assertEquals(id, response.getId());
        assertEquals(nombre, response.getNombre());
        assertEquals(email, response.getEmail());
        assertEquals(nickname, response.getNickname());
        assertEquals(especialidad, response.getEspecialidad());
        assertEquals(direccion, response.getDireccion());
    }

    @Test
    void testLongStrings() {
        String longNombre = "a".repeat(500);
        String longDireccion = "b".repeat(1000);
        
        response.setNombre(longNombre);
        response.setDireccion(longDireccion);
        
        assertEquals(longNombre, response.getNombre());
        assertEquals(longDireccion, response.getDireccion());
    }
}

