package uy.edu.tse.hcen.resource;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uy.edu.tse.hcen.dto.ProfesionalDTO;
import uy.edu.tse.hcen.model.ProfesionalSalud;
import uy.edu.tse.hcen.model.enums.Especialidad;
import uy.edu.tse.hcen.service.ProfesionalSaludService;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfesionalResourceTest {

    @Mock
    private ProfesionalSaludService profesionalService;

    @InjectMocks
    private ProfesionalResource resource;

    private ProfesionalDTO dto;
    private ProfesionalSalud profesional;

    @BeforeEach
    void setUp() {
        dto = new ProfesionalDTO();
        dto.setNombre("Dr. Juan");
        dto.setEmail("juan@example.com");
        dto.setNickname("juan");
        dto.setEspecialidad(Especialidad.CARDIOLOGIA);
        dto.setDireccion("Av. 18 de Julio 1234");
        dto.setPassword("password123");

        profesional = new ProfesionalSalud();
        profesional.setId(1L);
        profesional.setNombre("Dr. Juan");
        profesional.setEmail("juan@example.com");
        profesional.setNickname("juan");
        profesional.setEspecialidad(Especialidad.CARDIOLOGIA);
    }

    @Test
    void testCreateProfesionalSuccess() {
        // Arrange
        when(profesionalService.create(any(ProfesionalDTO.class))).thenReturn(profesional);

        // Act
        Response response = resource.createProfesional(dto);

        // Assert
        assertNotNull(response);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());
        verify(profesionalService).create(any(ProfesionalDTO.class));
    }

    @Test
    void testCreateProfesionalWithIllegalArgumentException() {
        // Arrange
        when(profesionalService.create(any(ProfesionalDTO.class)))
            .thenThrow(new IllegalArgumentException("Nickname ya existe"));

        // Act
        Response response = resource.createProfesional(dto);

        // Assert
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> entity = (java.util.Map<String, Object>) response.getEntity();
        assertTrue(entity.get("error").toString().contains("Nickname"));
    }

    @Test
    void testCreateProfesionalWithSecurityException() {
        // Arrange
        when(profesionalService.create(any(ProfesionalDTO.class)))
            .thenThrow(new SecurityException("No tiene permisos"));

        // Act
        Response response = resource.createProfesional(dto);

        // Assert
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> entity = (java.util.Map<String, Object>) response.getEntity();
        assertTrue(entity.get("error").toString().contains("permisos"));
    }

    @Test
    void testGetAllProfesionales() {
        // Arrange
        List<ProfesionalSalud> profesionales = Arrays.asList(profesional);
        when(profesionalService.findAllInCurrentTenant()).thenReturn(profesionales);

        // Act
        List<ProfesionalSalud> result = resource.getAllProfesionales();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(profesionalService).findAllInCurrentTenant();
    }

    @Test
    void testGetAllProfesionalesEmpty() {
        // Arrange
        when(profesionalService.findAllInCurrentTenant()).thenReturn(Arrays.asList());

        // Act
        List<ProfesionalSalud> result = resource.getAllProfesionales();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testUpdateProfesionalSuccess() {
        // Arrange
        Long id = 1L;
        when(profesionalService.update(eq(id), any(ProfesionalDTO.class))).thenReturn(profesional);

        // Act
        Response response = resource.updateProfesional(id, dto);

        // Assert
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());
        verify(profesionalService).update(eq(id), any(ProfesionalDTO.class));
    }

    @Test
    void testUpdateProfesionalNotFound() {
        // Arrange
        Long id = 999L;
        when(profesionalService.update(eq(id), any(ProfesionalDTO.class)))
            .thenThrow(new IllegalArgumentException("Profesional no encontrado"));

        // Act
        Response response = resource.updateProfesional(id, dto);

        // Assert
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> entity = (java.util.Map<String, Object>) response.getEntity();
        assertTrue(entity.get("error").toString().contains("encontrado"));
    }

    @Test
    void testDeleteProfesionalSuccess() {
        // Arrange
        Long id = 1L;
        doNothing().when(profesionalService).delete(id);

        // Act
        Response response = resource.deleteProfesional(id);

        // Assert
        assertNotNull(response);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        verify(profesionalService).delete(id);
    }

    @Test
    void testDeleteProfesionalNotFound() {
        // Arrange
        Long id = 999L;
        doThrow(new IllegalArgumentException("Profesional no encontrado"))
            .when(profesionalService).delete(id);

        // Act
        Response response = resource.deleteProfesional(id);

        // Assert
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> entity = (java.util.Map<String, Object>) response.getEntity();
        assertTrue(entity.get("error").toString().contains("encontrado"));
    }
}

