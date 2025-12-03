package uy.edu.tse.hcen.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdministradorClinicaTest {

    private AdministradorClinica admin;
    private NodoPeriferico nodo;

    @BeforeEach
    void setUp() {
        admin = new AdministradorClinica();
        nodo = new PrestadorSalud();
        nodo.setId(1L);
    }

    @Test
    void testConstructor() {
        AdministradorClinica admin = new AdministradorClinica();
        assertNotNull(admin);
        assertNull(admin.getAdministra());
    }

    @Test
    void testConstructorWithNodo() {
        AdministradorClinica admin = new AdministradorClinica(nodo);
        assertEquals(nodo, admin.getAdministra());
    }

    @Test
    void testAdministra() {
        admin.setAdministra(nodo);
        assertEquals(nodo, admin.getAdministra());
    }

    @Test
    void testAdministraNull() {
        admin.setAdministra(null);
        assertNull(admin.getAdministra());
    }

    @Test
    void testGetTenantNodeId() {
        nodo.setId(101L);
        admin.setAdministra(nodo);
        assertEquals(101L, admin.getTenantNodeId());
    }

    @Test
    void testGetTenantNodeIdNull() {
        admin.setAdministra(null);
        assertNull(admin.getTenantNodeId());
    }

    @Test
    void testInheritance() {
        admin.setId(1L);
        admin.setNombre("Admin Test");
        admin.setEmail("admin@example.com");
        admin.setNickname("admin");
        admin.setPassword("password123");
        
        assertEquals(1L, admin.getId());
        assertEquals("Admin Test", admin.getNombre());
        assertEquals("admin@example.com", admin.getEmail());
        assertEquals("admin", admin.getNickname());
        assertTrue(admin.checkPassword("password123"));
    }

    @Test
    void testAllFields() {
        Long id = 1L;
        String nombre = "Administrador";
        String email = "admin@clinica.com";
        String nickname = "admin";
        String password = "admin123";
        
        admin.setId(id);
        admin.setNombre(nombre);
        admin.setEmail(email);
        admin.setNickname(nickname);
        admin.setPassword(password);
        admin.setAdministra(nodo);
        
        assertEquals(id, admin.getId());
        assertEquals(nombre, admin.getNombre());
        assertEquals(email, admin.getEmail());
        assertEquals(nickname, admin.getNickname());
        assertEquals(nodo, admin.getAdministra());
        assertTrue(admin.checkPassword(password));
    }
}

