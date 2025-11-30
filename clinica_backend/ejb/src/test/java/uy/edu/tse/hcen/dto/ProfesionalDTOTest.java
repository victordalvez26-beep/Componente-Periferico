package uy.edu.tse.hcen.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.tse.hcen.model.enums.Especialidad;

import static org.junit.jupiter.api.Assertions.*;

class ProfesionalDTOTest {

    private ProfesionalDTO dto;

    @BeforeEach
    void setUp() {
        dto = new ProfesionalDTO();
    }

    @Test
    void testConstructor() {
        ProfesionalDTO dto = new ProfesionalDTO();
        assertNotNull(dto);
        assertNull(dto.getNombre());
        assertNull(dto.getEmail());
        assertNull(dto.getNickname());
        assertNull(dto.getEspecialidad());
        assertNull(dto.getDireccion());
        assertNull(dto.getPassword());
    }

    @Test
    void testNombre() {
        String nombre = "Dr. Juan Pérez";
        dto.setNombre(nombre);
        assertEquals(nombre, dto.getNombre());
    }

    @Test
    void testNombreNull() {
        dto.setNombre(null);
        assertNull(dto.getNombre());
    }

    @Test
    void testNombreEmpty() {
        dto.setNombre("");
        assertEquals("", dto.getNombre());
    }

    @Test
    void testEmail() {
        String email = "juan.perez@clinica.com";
        dto.setEmail(email);
        assertEquals(email, dto.getEmail());
    }

    @Test
    void testEmailNull() {
        dto.setEmail(null);
        assertNull(dto.getEmail());
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
    void testEspecialidad() {
        Especialidad especialidad = Especialidad.CARDIOLOGIA;
        dto.setEspecialidad(especialidad);
        assertEquals(especialidad, dto.getEspecialidad());
    }

    @Test
    void testEspecialidadNull() {
        dto.setEspecialidad(null);
        assertNull(dto.getEspecialidad());
    }

    @Test
    void testAllEspecialidades() {
        for (Especialidad esp : Especialidad.values()) {
            dto.setEspecialidad(esp);
            assertEquals(esp, dto.getEspecialidad());
        }
    }

    @Test
    void testDireccion() {
        String direccion = "Av. 18 de Julio 1234, Montevideo";
        dto.setDireccion(direccion);
        assertEquals(direccion, dto.getDireccion());
    }

    @Test
    void testDireccionNull() {
        dto.setDireccion(null);
        assertNull(dto.getDireccion());
    }

    @Test
    void testDireccionEmpty() {
        dto.setDireccion("");
        assertEquals("", dto.getDireccion());
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
    void testAllFields() {
        String nombre = "Dr. María García";
        String email = "maria.garcia@clinica.com";
        String nickname = "mgarcia";
        Especialidad especialidad = Especialidad.PEDIATRIA;
        String direccion = "Bvar. Artigas 5678";
        String password = "securePassword123";
        
        dto.setNombre(nombre);
        dto.setEmail(email);
        dto.setNickname(nickname);
        dto.setEspecialidad(especialidad);
        dto.setDireccion(direccion);
        dto.setPassword(password);
        
        assertEquals(nombre, dto.getNombre());
        assertEquals(email, dto.getEmail());
        assertEquals(nickname, dto.getNickname());
        assertEquals(especialidad, dto.getEspecialidad());
        assertEquals(direccion, dto.getDireccion());
        assertEquals(password, dto.getPassword());
    }

    @Test
    void testPartialFields() {
        dto.setNombre("Dr. Test");
        dto.setEspecialidad(Especialidad.MEDICINA_GENERAL);
        
        assertEquals("Dr. Test", dto.getNombre());
        assertEquals(Especialidad.MEDICINA_GENERAL, dto.getEspecialidad());
        assertNull(dto.getEmail());
        assertNull(dto.getNickname());
    }

    @Test
    void testLongStrings() {
        String longNombre = "a".repeat(500);
        String longDireccion = "b".repeat(1000);
        
        dto.setNombre(longNombre);
        dto.setDireccion(longDireccion);
        
        assertEquals(longNombre, dto.getNombre());
        assertEquals(longDireccion, dto.getDireccion());
    }

    @Test
    void testSpecialCharacters() {
        dto.setNombre("Dr. José María O'Connor");
        dto.setDireccion("Av. 18 de Julio #1234, Apt. 5B");
        dto.setEmail("jose.oconnor@example.com");
        
        assertEquals("Dr. José María O'Connor", dto.getNombre());
        assertEquals("Av. 18 de Julio #1234, Apt. 5B", dto.getDireccion());
        assertEquals("jose.oconnor@example.com", dto.getEmail());
    }
}

