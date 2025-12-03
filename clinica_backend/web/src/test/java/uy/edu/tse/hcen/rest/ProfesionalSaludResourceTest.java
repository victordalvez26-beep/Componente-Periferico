package uy.edu.tse.hcen.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.tse.hcen.dto.ProfesionalDTO;
import uy.edu.tse.hcen.model.ProfesionalSalud;
import uy.edu.tse.hcen.model.enums.Especialidad;
import uy.edu.tse.hcen.service.ProfesionalSaludService;

import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfesionalSaludResourceTest {

    @Mock
    private ProfesionalSaludService profesionalService;

    @InjectMocks
    private ProfesionalSaludResource resource;

    private ProfesionalDTO dto;
    private ProfesionalSalud profesional;

    @BeforeEach
    void setUp() {
        dto = new ProfesionalDTO();
        dto.setNombre("Dr. Juan Pérez");
        dto.setEmail("juan@example.com");
        dto.setNickname("jperez");
        dto.setEspecialidad(Especialidad.CARDIOLOGIA);
        dto.setDireccion("Av. 18 de Julio 1234");
        dto.setPassword("password123");

        profesional = new ProfesionalSalud();
        profesional.setId(1L);
        profesional.setNombre("Dr. Juan Pérez");
        profesional.setEmail("juan@example.com");
        profesional.setNickname("jperez");
        profesional.setEspecialidad(Especialidad.CARDIOLOGIA);
    }

    @Test
    void testListAll() {
        // Arrange
        List<ProfesionalSalud> profesionales = Arrays.asList(profesional);
        when(profesionalService.findAllInCurrentTenant()).thenReturn(profesionales);

        // Act
        Response response = resource.listAll();

        // Assert
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());
        verify(profesionalService).findAllInCurrentTenant();
    }

    @Test
    void testGetById() {
        // Arrange
        Long id = 1L;
        when(profesionalService.findById(id)).thenReturn(Optional.of(profesional));

        // Act
        Response response = resource.getById(id);

        // Assert
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());
        verify(profesionalService).findById(id);
    }

    @Test
    void testGetByIdNotFound() {
        // Arrange
        Long id = 999L;
        when(profesionalService.findById(id)).thenReturn(Optional.empty());

        // Act
        Response response = resource.getById(id);

        // Assert
        assertNotNull(response);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void testCreateSuccess() {
        // Arrange
        when(profesionalService.create(dto)).thenReturn(profesional);

        // Act
        Response response = resource.create(dto);

        // Assert
        assertNotNull(response);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());
        verify(profesionalService).create(dto);
    }

    @Test
    void testCreateWithNullNickname() {
        // Arrange
        dto.setNickname(null);

        // Act
        Response response = resource.create(dto);

        // Assert
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        verify(profesionalService, never()).create(any());
    }

    @Test
    void testCreateWithBlankNickname() {
        // Arrange
        dto.setNickname("   ");

        // Act
        Response response = resource.create(dto);

        // Assert
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testCreateWithNullNombre() {
        // Arrange
        dto.setNombre(null);

        // Act
        Response response = resource.create(dto);

        // Assert
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testCreateWithInvalidEmail() {
        // Arrange
        dto.setEmail("invalid-email");

        // Act
        Response response = resource.create(dto);

        // Assert
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testCreateWithNullEmail() {
        // Arrange
        dto.setEmail(null);

        // Act
        Response response = resource.create(dto);

        // Assert
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testUpdateSuccess() {
        // Arrange
        Long id = 1L;
        when(profesionalService.update(id, dto)).thenReturn(profesional);

        // Act
        Response response = resource.update(id, dto);

        // Assert
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(profesionalService).update(id, dto);
    }

    @Test
    void testUpdateNotFound() {
        // Arrange
        Long id = 999L;
        when(profesionalService.update(id, dto))
            .thenThrow(new IllegalArgumentException("Profesional no encontrado"));

        // Act
        Response response = resource.update(id, dto);

        // Assert
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void testDeleteSuccess() {
        // Arrange
        Long id = 1L;
        doNothing().when(profesionalService).delete(id);

        // Act
        Response response = resource.delete(id);

        // Assert
        assertNotNull(response);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        verify(profesionalService).delete(id);
    }

    @Test
    void testDeleteNotFound() {
        // Arrange
        Long id = 999L;
        doThrow(new IllegalArgumentException("Profesional no encontrado"))
            .when(profesionalService).delete(id);

        // Act
        Response response = resource.delete(id);

        // Assert
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void testCreateWithNullDto() {
        // Act
        Response response = resource.create(null);

        // Assert
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
}

