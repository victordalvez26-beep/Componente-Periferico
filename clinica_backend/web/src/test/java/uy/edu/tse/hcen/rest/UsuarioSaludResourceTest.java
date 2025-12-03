package uy.edu.tse.hcen.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.tse.hcen.dto.UsuarioSaludDTO;
import uy.edu.tse.hcen.model.UsuarioSalud;
import uy.edu.tse.hcen.service.UsuarioSaludService;

import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioSaludResourceTest {

    @Mock
    private UsuarioSaludService service;

    @InjectMocks
    private UsuarioSaludResource resource;

    private UsuarioSaludDTO dto;
    private UsuarioSalud usuario;

    @BeforeEach
    void setUp() {
        dto = new UsuarioSaludDTO();
        dto.setCi("12345678");
        dto.setNombre("Juan");
        dto.setApellido("Pérez");
        dto.setFechaNacimiento(LocalDate.of(1990, 5, 15));
        dto.setEmail("juan@example.com");

        usuario = new UsuarioSalud();
        usuario.setId(1L);
        usuario.setCi("12345678");
        usuario.setNombre("Juan");
        usuario.setApellido("Pérez");
        usuario.setFechaNacimiento(LocalDate.of(1990, 5, 15));
        usuario.setEmail("juan@example.com");
    }

    @Test
    void testCrearUsuarioSalud() {
        // Arrange
        Long tenantId = 101L;
        when(service.crearUsuarioSalud(eq(tenantId), any(UsuarioSalud.class))).thenReturn(usuario);

        // Act
        Response response = resource.crearUsuarioSalud(tenantId, dto);

        // Assert
        assertNotNull(response);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());
        verify(service).crearUsuarioSalud(eq(tenantId), any(UsuarioSalud.class));
    }

    @Test
    void testCrearUsuarioSaludWithValidationError() {
        // Arrange
        Long tenantId = 101L;
        when(service.crearUsuarioSalud(eq(tenantId), any(UsuarioSalud.class)))
            .thenThrow(new IllegalArgumentException("Ya existe un paciente con CI 12345678"));

        // Act
        Response response = resource.crearUsuarioSalud(tenantId, dto);

        // Assert
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testListarUsuariosSalud() {
        // Arrange
        Long tenantId = 101L;
        List<UsuarioSalud> usuarios = Arrays.asList(usuario);
        when(service.listarUsuariosSalud(tenantId)).thenReturn(usuarios);

        // Act
        Response response = resource.listarUsuariosSalud(tenantId);

        // Assert
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());
        verify(service).listarUsuariosSalud(tenantId);
    }

    @Test
    void testObtenerUsuarioSalud() {
        // Arrange
        Long tenantId = 101L;
        Long id = 1L;
        when(service.obtenerUsuarioSalud(id, tenantId)).thenReturn(usuario);

        // Act
        Response response = resource.obtenerUsuarioSalud(tenantId, id);

        // Assert
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());
        verify(service).obtenerUsuarioSalud(id, tenantId);
    }

    @Test
    void testObtenerUsuarioSaludNotFound() {
        // Arrange
        Long tenantId = 101L;
        Long id = 999L;
        when(service.obtenerUsuarioSalud(id, tenantId)).thenReturn(null);

        // Act
        Response response = resource.obtenerUsuarioSalud(tenantId, id);

        // Assert
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void testActualizarUsuarioSalud() {
        // Arrange
        Long tenantId = 101L;
        Long id = 1L;
        when(service.actualizarUsuarioSalud(eq(tenantId), eq(id), any(UsuarioSalud.class))).thenReturn(usuario);

        // Act
        Response response = resource.actualizarUsuarioSalud(tenantId, id, dto);

        // Assert
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(service).actualizarUsuarioSalud(eq(tenantId), eq(id), any(UsuarioSalud.class));
    }

    @Test
    void testActualizarUsuarioSaludNotFound() {
        // Arrange
        Long tenantId = 101L;
        Long id = 999L;
        when(service.actualizarUsuarioSalud(eq(tenantId), eq(id), any(UsuarioSalud.class)))
            .thenThrow(new IllegalArgumentException("Usuario no encontrado"));

        // Act
        Response response = resource.actualizarUsuarioSalud(tenantId, id, dto);

        // Assert
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testCrearUsuarioSaludWithException() {
        // Arrange
        Long tenantId = 101L;
        when(service.crearUsuarioSalud(eq(tenantId), any(UsuarioSalud.class)))
            .thenThrow(new RuntimeException("Database error"));

        // Act
        Response response = resource.crearUsuarioSalud(tenantId, dto);

        // Assert
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }

    @Test
    void testListarUsuariosSaludWithException() {
        // Arrange
        Long tenantId = 101L;
        when(service.listarUsuariosSalud(tenantId))
            .thenThrow(new RuntimeException("Service error"));

        // Act
        Response response = resource.listarUsuariosSalud(tenantId);

        // Assert
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }

    @Test
    void testObtenerUsuarioSaludWithException() {
        // Arrange
        Long tenantId = 101L;
        Long id = 1L;
        when(service.obtenerUsuarioSalud(id, tenantId))
            .thenThrow(new RuntimeException("Database error"));

        // Act
        Response response = resource.obtenerUsuarioSalud(tenantId, id);

        // Assert
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }

    @Test
    void testActualizarUsuarioSaludWithException() {
        // Arrange
        Long tenantId = 101L;
        Long id = 1L;
        when(service.actualizarUsuarioSalud(eq(tenantId), eq(id), any(UsuarioSalud.class)))
            .thenThrow(new RuntimeException("Service error"));

        // Act
        Response response = resource.actualizarUsuarioSalud(tenantId, id, dto);

        // Assert
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }
}

