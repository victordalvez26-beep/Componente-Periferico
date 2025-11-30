package uy.edu.tse.hcen.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.tse.hcen.model.enums.Departamentos;
import uy.edu.tse.hcen.model.enums.Especialidad;

import static org.junit.jupiter.api.Assertions.*;

class ProfesionalSaludTest {

    private ProfesionalSalud profesional;

    @BeforeEach
    void setUp() {
        profesional = new ProfesionalSalud();
    }

    @Test
    void testConstructor() {
        ProfesionalSalud profesional = new ProfesionalSalud();
        assertNotNull(profesional);
        assertNull(profesional.getEspecialidad());
        assertNull(profesional.getDepartamento());
        assertNull(profesional.getDireccion());
    }

    @Test
    void testConstructorWithParameters() {
        String nombre = "Dr. Juan Pérez";
        String email = "juan@example.com";
        String nickname = "jperez";
        String password = "password123";
        Especialidad especialidad = Especialidad.CARDIOLOGIA;
        Departamentos departamento = Departamentos.MONTEVIDEO;
        
        ProfesionalSalud profesional = new ProfesionalSalud(
            nombre, email, nickname, password, especialidad, departamento
        );
        
        assertEquals(nombre, profesional.getNombre());
        assertEquals(email, profesional.getEmail());
        assertEquals(nickname, profesional.getNickname());
        assertEquals(especialidad, profesional.getEspecialidad());
        assertEquals(departamento, profesional.getDepartamento());
        assertNotNull(profesional.getPasswordHash());
        assertTrue(profesional.checkPassword(password));
    }

    @Test
    void testConstructorWithNullPassword() {
        ProfesionalSalud profesional = new ProfesionalSalud(
            "Dr. Test", "test@example.com", "test", null,
            Especialidad.MEDICINA_GENERAL, Departamentos.MONTEVIDEO
        );
        
        assertNotNull(profesional);
        assertEquals("Dr. Test", profesional.getNombre());
    }

    @Test
    void testEspecialidad() {
        Especialidad especialidad = Especialidad.CARDIOLOGIA;
        profesional.setEspecialidad(especialidad);
        assertEquals(especialidad, profesional.getEspecialidad());
    }

    @Test
    void testEspecialidadNull() {
        profesional.setEspecialidad(null);
        assertNull(profesional.getEspecialidad());
    }

    @Test
    void testAllEspecialidades() {
        for (Especialidad esp : Especialidad.values()) {
            profesional.setEspecialidad(esp);
            assertEquals(esp, profesional.getEspecialidad());
        }
    }

    @Test
    void testDepartamento() {
        Departamentos departamento = Departamentos.MONTEVIDEO;
        profesional.setDepartamento(departamento);
        assertEquals(departamento, profesional.getDepartamento());
    }

    @Test
    void testDepartamentoNull() {
        profesional.setDepartamento(null);
        assertNull(profesional.getDepartamento());
    }

    @Test
    void testAllDepartamentos() {
        for (Departamentos dept : Departamentos.values()) {
            profesional.setDepartamento(dept);
            assertEquals(dept, profesional.getDepartamento());
        }
    }

    @Test
    void testDireccion() {
        String direccion = "Av. 18 de Julio 1234";
        profesional.setDireccion(direccion);
        assertEquals(direccion, profesional.getDireccion());
    }

    @Test
    void testDireccionNull() {
        profesional.setDireccion(null);
        assertNull(profesional.getDireccion());
    }

    @Test
    void testDireccionEmpty() {
        profesional.setDireccion("");
        assertEquals("", profesional.getDireccion());
    }

    @Test
    void testTrabajaEn() {
        NodoPeriferico nodo = new PrestadorSalud();
        nodo.setId(1L);
        profesional.setTrabajaEn(nodo);
        
        // Verificar que se puede establecer la relación
        assertNotNull(profesional.getTenantNodeId());
        assertEquals(1L, profesional.getTenantNodeId());
    }

    @Test
    void testTrabajaEnNull() {
        profesional.setTrabajaEn(null);
        assertNull(profesional.getTenantNodeId());
    }

    @Test
    void testGetTenantNodeId() {
        NodoPeriferico nodo = new PrestadorSalud();
        nodo.setId(101L);
        profesional.setTrabajaEn(nodo);
        
        assertEquals(101L, profesional.getTenantNodeId());
    }

    @Test
    void testGetTenantNodeIdNull() {
        profesional.setTrabajaEn(null);
        assertNull(profesional.getTenantNodeId());
    }

    @Test
    void testInheritance() {
        profesional.setId(1L);
        profesional.setNombre("Dr. Test");
        profesional.setEmail("test@example.com");
        profesional.setNickname("test");
        profesional.setPassword("password123");
        
        assertEquals(1L, profesional.getId());
        assertEquals("Dr. Test", profesional.getNombre());
        assertEquals("test@example.com", profesional.getEmail());
        assertEquals("test", profesional.getNickname());
        assertTrue(profesional.checkPassword("password123"));
    }

    @Test
    void testAllFields() {
        Long id = 1L;
        String nombre = "Dr. María García";
        String email = "maria@example.com";
        String nickname = "mgarcia";
        String password = "password123";
        Especialidad especialidad = Especialidad.PEDIATRIA;
        Departamentos departamento = Departamentos.CANELONES;
        String direccion = "Bvar. Artigas 5678";
        
        profesional.setId(id);
        profesional.setNombre(nombre);
        profesional.setEmail(email);
        profesional.setNickname(nickname);
        profesional.setPassword(password);
        profesional.setEspecialidad(especialidad);
        profesional.setDepartamento(departamento);
        profesional.setDireccion(direccion);
        
        assertEquals(id, profesional.getId());
        assertEquals(nombre, profesional.getNombre());
        assertEquals(email, profesional.getEmail());
        assertEquals(nickname, profesional.getNickname());
        assertEquals(especialidad, profesional.getEspecialidad());
        assertEquals(departamento, profesional.getDepartamento());
        assertEquals(direccion, profesional.getDireccion());
        assertTrue(profesional.checkPassword(password));
    }

    @Test
    void testLongStrings() {
        String longNombre = "a".repeat(500);
        String longDireccion = "b".repeat(1000);
        
        profesional.setNombre(longNombre);
        profesional.setDireccion(longDireccion);
        
        assertEquals(longNombre, profesional.getNombre());
        assertEquals(longDireccion, profesional.getDireccion());
    }
}

