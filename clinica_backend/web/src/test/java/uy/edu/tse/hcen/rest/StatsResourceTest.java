package uy.edu.tse.hcen.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.tse.hcen.multitenancy.TenantContext;
import uy.edu.tse.hcen.service.StatsService;

import jakarta.ws.rs.core.Response;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsResourceTest {

    @Mock
    private StatsService statsService;

    @InjectMocks
    private StatsResource statsResource;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
    }

    @Test
    void testObtenerEstadisticas() {
        // Arrange
        String tenantId = "101";
        TenantContext.setCurrentTenant(tenantId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("profesionales", 5);
        stats.put("usuarios", 10);
        stats.put("documentos", 20);
        stats.put("consultas", 3);
        
        when(statsService.obtenerEstadisticas(tenantId)).thenReturn(stats);

        // Act
        Response response = statsResource.obtenerEstadisticas(tenantId);

        // Assert
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());
        verify(statsService).obtenerEstadisticas(tenantId);
    }

    @Test
    void testObtenerEstadisticasWithTenantMismatch() {
        // Arrange
        String tenantId = "101";
        TenantContext.setCurrentTenant("102"); // Diferente tenant

        // Act
        Response response = statsResource.obtenerEstadisticas(tenantId);

        // Assert
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        verify(statsService, never()).obtenerEstadisticas(anyString());
    }

    @Test
    void testObtenerEstadisticasWithNullTenant() {
        // Arrange
        String tenantId = "101";
        TenantContext.clear(); // Sin tenant

        // Act
        Response response = statsResource.obtenerEstadisticas(tenantId);

        // Assert
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    void testObtenerActividadReciente() {
        // Arrange
        String tenantId = "101";
        int limite = 10;
        TenantContext.setCurrentTenant(tenantId);
        
        List<Map<String, Object>> actividades = new ArrayList<>();
        Map<String, Object> actividad = new HashMap<>();
        actividad.put("tipo", "documento");
        actividad.put("texto", "Test");
        actividades.add(actividad);
        
        when(statsService.obtenerActividadReciente(tenantId, limite)).thenReturn(actividades);

        // Act
        Response response = statsResource.obtenerActividadReciente(tenantId, limite);

        // Assert
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(statsService).obtenerActividadReciente(tenantId, limite);
    }

    @Test
    void testObtenerActividadRecienteWithDefaultLimit() {
        // Arrange
        String tenantId = "101";
        TenantContext.setCurrentTenant(tenantId);
        
        when(statsService.obtenerActividadReciente(tenantId, 10)).thenReturn(new ArrayList<>());

        // Act - Usar @DefaultValue que es 10
        Response response = statsResource.obtenerActividadReciente(tenantId, 10);

        // Assert
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(statsService).obtenerActividadReciente(tenantId, 10);
    }

    @Test
    void testObtenerActividadRecienteWithLargeLimit() {
        // Arrange
        String tenantId = "101";
        int limite = 100; // Mayor que 50
        TenantContext.setCurrentTenant(tenantId);
        
        when(statsService.obtenerActividadReciente(tenantId, 50)).thenReturn(new ArrayList<>());

        // Act
        Response response = statsResource.obtenerActividadReciente(tenantId, limite);

        // Assert
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(statsService).obtenerActividadReciente(tenantId, 50); // Debe limitarse a 50
    }

    @Test
    void testObtenerActividadRecienteWithTenantMismatch() {
        // Arrange
        String tenantId = "101";
        TenantContext.setCurrentTenant("102");

        // Act
        Response response = statsResource.obtenerActividadReciente(tenantId, 10);

        // Assert
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    void testObtenerEstadisticasWithException() {
        // Arrange
        String tenantId = "101";
        TenantContext.setCurrentTenant(tenantId);
        when(statsService.obtenerEstadisticas(tenantId))
            .thenThrow(new RuntimeException("Database error"));

        // Act
        Response response = statsResource.obtenerEstadisticas(tenantId);

        // Assert
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> entity = (Map<String, Object>) response.getEntity();
        assertTrue(entity.get("error").toString().contains("Error"));
    }

    @Test
    void testObtenerActividadRecienteWithException() {
        // Arrange
        String tenantId = "101";
        TenantContext.setCurrentTenant(tenantId);
        when(statsService.obtenerActividadReciente(tenantId, 10))
            .thenThrow(new RuntimeException("Service error"));

        // Act
        Response response = statsResource.obtenerActividadReciente(tenantId, 10);

        // Assert
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> entity = (Map<String, Object>) response.getEntity();
        assertTrue(entity.get("error").toString().contains("Error"));
    }

    @Test
    void testObtenerActividadRecienteWithNullTenant() {
        // Arrange
        String tenantId = "101";
        TenantContext.clear();

        // Act
        Response response = statsResource.obtenerActividadReciente(tenantId, 10);

        // Assert
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }
}

