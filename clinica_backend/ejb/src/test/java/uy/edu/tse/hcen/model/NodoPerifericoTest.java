package uy.edu.tse.hcen.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.tse.hcen.model.enums.Departamentos;
import uy.edu.tse.hcen.model.enums.EstadoNodoPeriferico;

import static org.junit.jupiter.api.Assertions.*;

class NodoPerifericoTest {

    private PrestadorSalud nodo;

    @BeforeEach
    void setUp() {
        nodo = new PrestadorSalud();
    }

    @Test
    void testConstructor() {
        PrestadorSalud nodo = new PrestadorSalud();
        assertNotNull(nodo);
        assertNull(nodo.getId());
        assertNull(nodo.getNombre());
        assertNull(nodo.getRut());
    }

    @Test
    void testConstructorWithParameters() {
        String nombre = "Clínica San José";
        String rut = "123456789012";
        Departamentos departamento = Departamentos.MONTEVIDEO;
        String localidad = "Centro";
        String direccion = "Av. 18 de Julio 1234";
        String contacto = "contacto@clinica.com";
        EstadoNodoPeriferico estado = EstadoNodoPeriferico.ACTIVO;
        
        PrestadorSalud nodo = new PrestadorSalud(
            nombre, rut, departamento, localidad, direccion, contacto, estado
        );
        
        assertEquals(nombre, nodo.getNombre());
        assertEquals(rut, nodo.getRut());
        assertEquals(departamento, nodo.getDepartamento());
        assertEquals(localidad, nodo.getLocalidad());
        assertEquals(direccion, nodo.getDireccion());
        assertEquals(contacto, nodo.getContacto());
        assertEquals(estado, nodo.getEstado());
    }

    @Test
    void testId() {
        Long id = 1L;
        nodo.setId(id);
        assertEquals(id, nodo.getId());
    }

    @Test
    void testIdNull() {
        nodo.setId(null);
        assertNull(nodo.getId());
    }

    @Test
    void testNombre() {
        String nombre = "Clínica Test";
        nodo.setNombre(nombre);
        assertEquals(nombre, nodo.getNombre());
    }

    @Test
    void testNombreNull() {
        nodo.setNombre(null);
        assertNull(nodo.getNombre());
    }

    @Test
    void testRut() {
        String rut = "123456789012";
        nodo.setRut(rut);
        assertEquals(rut, nodo.getRut());
    }

    @Test
    void testRutNull() {
        nodo.setRut(null);
        assertNull(nodo.getRut());
    }

    @Test
    void testDepartamento() {
        Departamentos departamento = Departamentos.MONTEVIDEO;
        nodo.setDepartamento(departamento);
        assertEquals(departamento, nodo.getDepartamento());
    }

    @Test
    void testDepartamentoNull() {
        nodo.setDepartamento(null);
        assertNull(nodo.getDepartamento());
    }

    @Test
    void testLocalidad() {
        String localidad = "Centro";
        nodo.setLocalidad(localidad);
        assertEquals(localidad, nodo.getLocalidad());
    }

    @Test
    void testLocalidadNull() {
        nodo.setLocalidad(null);
        assertNull(nodo.getLocalidad());
    }

    @Test
    void testDireccion() {
        String direccion = "Av. 18 de Julio 1234";
        nodo.setDireccion(direccion);
        assertEquals(direccion, nodo.getDireccion());
    }

    @Test
    void testDireccionNull() {
        nodo.setDireccion(null);
        assertNull(nodo.getDireccion());
    }

    @Test
    void testContacto() {
        String contacto = "contacto@clinica.com";
        nodo.setContacto(contacto);
        assertEquals(contacto, nodo.getContacto());
    }

    @Test
    void testContactoNull() {
        nodo.setContacto(null);
        assertNull(nodo.getContacto());
    }

    @Test
    void testEstado() {
        EstadoNodoPeriferico estado = EstadoNodoPeriferico.ACTIVO;
        nodo.setEstado(estado);
        assertEquals(estado, nodo.getEstado());
    }

    @Test
    void testEstadoNull() {
        nodo.setEstado(null);
        assertNull(nodo.getEstado());
    }

    @Test
    void testAllEstados() {
        for (EstadoNodoPeriferico estado : EstadoNodoPeriferico.values()) {
            nodo.setEstado(estado);
            assertEquals(estado, nodo.getEstado());
        }
    }

    @Test
    void testConfiguracion() {
        ConfiguracionClinica config = new ConfiguracionClinica();
        config.setLogoUrl("http://example.com/logo.png");
        nodo.setConfiguracion(config);
        
        assertEquals(config, nodo.getConfiguracion());
    }

    @Test
    void testConfiguracionNull() {
        nodo.setConfiguracion(null);
        assertNull(nodo.getConfiguracion());
    }

    @Test
    void testAllFields() {
        Long id = 1L;
        String nombre = "Clínica Test";
        String rut = "123456789012";
        Departamentos departamento = Departamentos.CANELONES;
        String localidad = "Ciudad de la Costa";
        String direccion = "Av. Principal 567";
        String contacto = "info@clinica.com";
        EstadoNodoPeriferico estado = EstadoNodoPeriferico.ACTIVO;
        
        nodo.setId(id);
        nodo.setNombre(nombre);
        nodo.setRut(rut);
        nodo.setDepartamento(departamento);
        nodo.setLocalidad(localidad);
        nodo.setDireccion(direccion);
        nodo.setContacto(contacto);
        nodo.setEstado(estado);
        
        assertEquals(id, nodo.getId());
        assertEquals(nombre, nodo.getNombre());
        assertEquals(rut, nodo.getRut());
        assertEquals(departamento, nodo.getDepartamento());
        assertEquals(localidad, nodo.getLocalidad());
        assertEquals(direccion, nodo.getDireccion());
        assertEquals(contacto, nodo.getContacto());
        assertEquals(estado, nodo.getEstado());
    }
}

