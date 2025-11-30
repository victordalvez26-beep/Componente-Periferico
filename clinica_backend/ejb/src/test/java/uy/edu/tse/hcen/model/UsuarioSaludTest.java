package uy.edu.tse.hcen.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioSaludTest {

    private UsuarioSalud usuarioSalud;

    @BeforeEach
    void setUp() {
        usuarioSalud = new UsuarioSalud();
    }

    @Test
    void testConstructor() {
        UsuarioSalud usuario = new UsuarioSalud();
        assertNotNull(usuario);
        assertNull(usuario.getId());
        assertNull(usuario.getCi());
        assertNull(usuario.getNombre());
    }

    @Test
    void testId() {
        Long id = 1L;
        usuarioSalud.setId(id);
        assertEquals(id, usuarioSalud.getId());
    }

    @Test
    void testIdNull() {
        usuarioSalud.setId(null);
        assertNull(usuarioSalud.getId());
    }

    @Test
    void testCi() {
        String ci = "12345678";
        usuarioSalud.setCi(ci);
        assertEquals(ci, usuarioSalud.getCi());
    }

    @Test
    void testCiNull() {
        usuarioSalud.setCi(null);
        assertNull(usuarioSalud.getCi());
    }

    @Test
    void testCiWithDashes() {
        String ci = "1.234.567-8";
        usuarioSalud.setCi(ci);
        assertEquals(ci, usuarioSalud.getCi());
    }

    @Test
    void testNombre() {
        String nombre = "Juan";
        usuarioSalud.setNombre(nombre);
        assertEquals(nombre, usuarioSalud.getNombre());
    }

    @Test
    void testNombreNull() {
        usuarioSalud.setNombre(null);
        assertNull(usuarioSalud.getNombre());
    }

    @Test
    void testApellido() {
        String apellido = "Pérez";
        usuarioSalud.setApellido(apellido);
        assertEquals(apellido, usuarioSalud.getApellido());
    }

    @Test
    void testApellidoNull() {
        usuarioSalud.setApellido(null);
        assertNull(usuarioSalud.getApellido());
    }

    @Test
    void testFechaNacimiento() {
        LocalDate fecha = LocalDate.of(1990, 5, 15);
        usuarioSalud.setFechaNacimiento(fecha);
        assertEquals(fecha, usuarioSalud.getFechaNacimiento());
    }

    @Test
    void testFechaNacimientoNull() {
        usuarioSalud.setFechaNacimiento(null);
        assertNull(usuarioSalud.getFechaNacimiento());
    }

    @Test
    void testDireccion() {
        String direccion = "Av. 18 de Julio 1234";
        usuarioSalud.setDireccion(direccion);
        assertEquals(direccion, usuarioSalud.getDireccion());
    }

    @Test
    void testDireccionNull() {
        usuarioSalud.setDireccion(null);
        assertNull(usuarioSalud.getDireccion());
    }

    @Test
    void testTelefono() {
        String telefono = "099123456";
        usuarioSalud.setTelefono(telefono);
        assertEquals(telefono, usuarioSalud.getTelefono());
    }

    @Test
    void testTelefonoNull() {
        usuarioSalud.setTelefono(null);
        assertNull(usuarioSalud.getTelefono());
    }

    @Test
    void testEmail() {
        String email = "juan.perez@example.com";
        usuarioSalud.setEmail(email);
        assertEquals(email, usuarioSalud.getEmail());
    }

    @Test
    void testEmailNull() {
        usuarioSalud.setEmail(null);
        assertNull(usuarioSalud.getEmail());
    }

    @Test
    void testDepartamento() {
        String departamento = "MONTEVIDEO";
        usuarioSalud.setDepartamento(departamento);
        assertEquals(departamento, usuarioSalud.getDepartamento());
    }

    @Test
    void testDepartamentoNull() {
        usuarioSalud.setDepartamento(null);
        assertNull(usuarioSalud.getDepartamento());
    }

    @Test
    void testLocalidad() {
        String localidad = "Centro";
        usuarioSalud.setLocalidad(localidad);
        assertEquals(localidad, usuarioSalud.getLocalidad());
    }

    @Test
    void testLocalidadNull() {
        usuarioSalud.setLocalidad(null);
        assertNull(usuarioSalud.getLocalidad());
    }

    @Test
    void testHcenUserId() {
        Long hcenUserId = 1000L;
        usuarioSalud.setHcenUserId(hcenUserId);
        assertEquals(hcenUserId, usuarioSalud.getHcenUserId());
    }

    @Test
    void testHcenUserIdNull() {
        usuarioSalud.setHcenUserId(null);
        assertNull(usuarioSalud.getHcenUserId());
    }

    @Test
    void testTenantId() {
        Long tenantId = 101L;
        usuarioSalud.setTenantId(tenantId);
        assertEquals(tenantId, usuarioSalud.getTenantId());
    }

    @Test
    void testTenantIdNull() {
        usuarioSalud.setTenantId(null);
        assertNull(usuarioSalud.getTenantId());
    }

    @Test
    void testFechaAlta() {
        LocalDateTime fecha = LocalDateTime.now();
        usuarioSalud.setFechaAlta(fecha);
        assertEquals(fecha, usuarioSalud.getFechaAlta());
    }

    @Test
    void testFechaAltaNull() {
        usuarioSalud.setFechaAlta(null);
        assertNull(usuarioSalud.getFechaAlta());
    }

    @Test
    void testFechaActualizacion() {
        LocalDateTime fecha = LocalDateTime.now();
        usuarioSalud.setFechaActualizacion(fecha);
        assertEquals(fecha, usuarioSalud.getFechaActualizacion());
    }

    @Test
    void testFechaActualizacionNull() {
        usuarioSalud.setFechaActualizacion(null);
        assertNull(usuarioSalud.getFechaActualizacion());
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
        Long tenantId = 101L;
        LocalDateTime fechaAlta = LocalDateTime.now();
        LocalDateTime fechaActualizacion = LocalDateTime.now();
        
        usuarioSalud.setId(id);
        usuarioSalud.setCi(ci);
        usuarioSalud.setNombre(nombre);
        usuarioSalud.setApellido(apellido);
        usuarioSalud.setFechaNacimiento(fechaNacimiento);
        usuarioSalud.setDireccion(direccion);
        usuarioSalud.setTelefono(telefono);
        usuarioSalud.setEmail(email);
        usuarioSalud.setDepartamento(departamento);
        usuarioSalud.setLocalidad(localidad);
        usuarioSalud.setHcenUserId(hcenUserId);
        usuarioSalud.setTenantId(tenantId);
        usuarioSalud.setFechaAlta(fechaAlta);
        usuarioSalud.setFechaActualizacion(fechaActualizacion);
        
        assertEquals(id, usuarioSalud.getId());
        assertEquals(ci, usuarioSalud.getCi());
        assertEquals(nombre, usuarioSalud.getNombre());
        assertEquals(apellido, usuarioSalud.getApellido());
        assertEquals(fechaNacimiento, usuarioSalud.getFechaNacimiento());
        assertEquals(direccion, usuarioSalud.getDireccion());
        assertEquals(telefono, usuarioSalud.getTelefono());
        assertEquals(email, usuarioSalud.getEmail());
        assertEquals(departamento, usuarioSalud.getDepartamento());
        assertEquals(localidad, usuarioSalud.getLocalidad());
        assertEquals(hcenUserId, usuarioSalud.getHcenUserId());
        assertEquals(tenantId, usuarioSalud.getTenantId());
        assertEquals(fechaAlta, usuarioSalud.getFechaAlta());
        assertEquals(fechaActualizacion, usuarioSalud.getFechaActualizacion());
    }

    @Test
    void testToString() {
        usuarioSalud.setId(1L);
        usuarioSalud.setCi("12345678");
        usuarioSalud.setNombre("Juan");
        usuarioSalud.setApellido("Pérez");
        usuarioSalud.setTenantId(101L);
        usuarioSalud.setHcenUserId(1000L);
        
        String toString = usuarioSalud.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("UsuarioSalud"));
        assertTrue(toString.contains("id=1"));
        assertTrue(toString.contains("ci='12345678'"));
        assertTrue(toString.contains("nombre='Juan'"));
        assertTrue(toString.contains("apellido='Pérez'"));
        assertTrue(toString.contains("tenantId=101"));
        assertTrue(toString.contains("hcenUserId=1000"));
    }

    @Test
    void testToStringWithNullFields() {
        UsuarioSalud usuario = new UsuarioSalud();
        String toString = usuario.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("UsuarioSalud"));
    }
}

