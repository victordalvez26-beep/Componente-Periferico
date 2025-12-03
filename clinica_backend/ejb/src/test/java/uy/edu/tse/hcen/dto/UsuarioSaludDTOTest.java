package uy.edu.tse.hcen.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioSaludDTOTest {

    private UsuarioSaludDTO dto;

    @BeforeEach
    void setUp() {
        dto = new UsuarioSaludDTO();
    }

    @Test
    void testConstructor() {
        UsuarioSaludDTO dto = new UsuarioSaludDTO();
        assertNotNull(dto);
        assertNull(dto.getId());
        assertNull(dto.getCi());
        assertNull(dto.getNombre());
    }

    @Test
    void testId() {
        Long id = 1L;
        dto.setId(id);
        assertEquals(id, dto.getId());
    }

    @Test
    void testIdNull() {
        dto.setId(null);
        assertNull(dto.getId());
    }

    @Test
    void testCi() {
        String ci = "12345678";
        dto.setCi(ci);
        assertEquals(ci, dto.getCi());
    }

    @Test
    void testCiNull() {
        dto.setCi(null);
        assertNull(dto.getCi());
    }

    @Test
    void testCiWithDashes() {
        String ci = "1.234.567-8";
        dto.setCi(ci);
        assertEquals(ci, dto.getCi());
    }

    @Test
    void testNombre() {
        String nombre = "Juan";
        dto.setNombre(nombre);
        assertEquals(nombre, dto.getNombre());
    }

    @Test
    void testNombreNull() {
        dto.setNombre(null);
        assertNull(dto.getNombre());
    }

    @Test
    void testApellido() {
        String apellido = "Pérez";
        dto.setApellido(apellido);
        assertEquals(apellido, dto.getApellido());
    }

    @Test
    void testApellidoNull() {
        dto.setApellido(null);
        assertNull(dto.getApellido());
    }

    @Test
    void testFechaNacimiento() {
        LocalDate fecha = LocalDate.of(1990, 5, 15);
        dto.setFechaNacimiento(fecha);
        assertEquals(fecha, dto.getFechaNacimiento());
    }

    @Test
    void testFechaNacimientoNull() {
        dto.setFechaNacimiento(null);
        assertNull(dto.getFechaNacimiento());
    }

    @Test
    void testFechaNacimientoPast() {
        LocalDate fecha = LocalDate.of(1950, 1, 1);
        dto.setFechaNacimiento(fecha);
        assertEquals(fecha, dto.getFechaNacimiento());
    }

    @Test
    void testFechaNacimientoFuture() {
        LocalDate fecha = LocalDate.of(2050, 12, 31);
        dto.setFechaNacimiento(fecha);
        assertEquals(fecha, dto.getFechaNacimiento());
    }

    @Test
    void testDireccion() {
        String direccion = "Av. 18 de Julio 1234";
        dto.setDireccion(direccion);
        assertEquals(direccion, dto.getDireccion());
    }

    @Test
    void testDireccionNull() {
        dto.setDireccion(null);
        assertNull(dto.getDireccion());
    }

    @Test
    void testTelefono() {
        String telefono = "099123456";
        dto.setTelefono(telefono);
        assertEquals(telefono, dto.getTelefono());
    }

    @Test
    void testTelefonoNull() {
        dto.setTelefono(null);
        assertNull(dto.getTelefono());
    }

    @Test
    void testTelefonoWithFormatting() {
        String telefono = "+598 99 123 456";
        dto.setTelefono(telefono);
        assertEquals(telefono, dto.getTelefono());
    }

    @Test
    void testEmail() {
        String email = "juan.perez@example.com";
        dto.setEmail(email);
        assertEquals(email, dto.getEmail());
    }

    @Test
    void testEmailNull() {
        dto.setEmail(null);
        assertNull(dto.getEmail());
    }

    @Test
    void testDepartamento() {
        String departamento = "MONTEVIDEO";
        dto.setDepartamento(departamento);
        assertEquals(departamento, dto.getDepartamento());
    }

    @Test
    void testDepartamentoNull() {
        dto.setDepartamento(null);
        assertNull(dto.getDepartamento());
    }

    @Test
    void testLocalidad() {
        String localidad = "Centro";
        dto.setLocalidad(localidad);
        assertEquals(localidad, dto.getLocalidad());
    }

    @Test
    void testLocalidadNull() {
        dto.setLocalidad(null);
        assertNull(dto.getLocalidad());
    }

    @Test
    void testHcenUserId() {
        Long hcenUserId = 1000L;
        dto.setHcenUserId(hcenUserId);
        assertEquals(hcenUserId, dto.getHcenUserId());
    }

    @Test
    void testHcenUserIdNull() {
        dto.setHcenUserId(null);
        assertNull(dto.getHcenUserId());
    }

    @Test
    void testAllFields() {
        Long id = 1L;
        String ci = "12345678";
        String nombre = "Juan";
        String apellido = "Pérez";
        LocalDate fechaNacimiento = LocalDate.of(1990, 5, 15);
        String direccion = "Av. 18 de Julio 1234";
        String telefono = "099123456";
        String email = "juan.perez@example.com";
        String departamento = "MONTEVIDEO";
        String localidad = "Centro";
        Long hcenUserId = 1000L;
        
        dto.setId(id);
        dto.setCi(ci);
        dto.setNombre(nombre);
        dto.setApellido(apellido);
        dto.setFechaNacimiento(fechaNacimiento);
        dto.setDireccion(direccion);
        dto.setTelefono(telefono);
        dto.setEmail(email);
        dto.setDepartamento(departamento);
        dto.setLocalidad(localidad);
        dto.setHcenUserId(hcenUserId);
        
        assertEquals(id, dto.getId());
        assertEquals(ci, dto.getCi());
        assertEquals(nombre, dto.getNombre());
        assertEquals(apellido, dto.getApellido());
        assertEquals(fechaNacimiento, dto.getFechaNacimiento());
        assertEquals(direccion, dto.getDireccion());
        assertEquals(telefono, dto.getTelefono());
        assertEquals(email, dto.getEmail());
        assertEquals(departamento, dto.getDepartamento());
        assertEquals(localidad, dto.getLocalidad());
        assertEquals(hcenUserId, dto.getHcenUserId());
    }

    @Test
    void testPartialFields() {
        dto.setCi("12345678");
        dto.setNombre("Juan");
        
        assertEquals("12345678", dto.getCi());
        assertEquals("Juan", dto.getNombre());
        assertNull(dto.getApellido());
        assertNull(dto.getEmail());
    }

    @Test
    void testEmptyStrings() {
        dto.setCi("");
        dto.setNombre("");
        dto.setApellido("");
        
        assertEquals("", dto.getCi());
        assertEquals("", dto.getNombre());
        assertEquals("", dto.getApellido());
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
}

