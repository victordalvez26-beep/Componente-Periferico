package uy.edu.tse.hcen.rest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uy.edu.tse.hcen.client.PoliticasAccesoClient;
import uy.edu.tse.hcen.multitenancy.TenantContext;

import java.security.Principal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfesionalResourceTest {

    @Mock
    private PoliticasAccesoClient politicasAccesoClient;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Principal principal;

    @InjectMocks
    private ProfesionalResource resource;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
    }

    @Test
    void testVerificarPermisoSuccess() {
        // Arrange
        String profesionalId = "prof1";
        String pacienteCI = "12345678";
        String tipoDoc = "PDF";
        TenantContext.setCurrentTenant("101");

        when(politicasAccesoClient.verificarPermiso(eq(profesionalId), eq(pacienteCI), eq(tipoDoc), eq("101")))
            .thenReturn(true);

        // Act
        Response response = resource.verificarPermiso(profesionalId, pacienteCI, tipoDoc);

        // Assert
        assertEquals(200, response.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> entity = (Map<String, Object>) response.getEntity();
        assertTrue((Boolean) entity.get("tienePermiso"));
        verify(politicasAccesoClient).verificarPermiso(profesionalId, pacienteCI, tipoDoc, "101");
    }

    @Test
    void testVerificarPermisoDenied() {
        // Arrange
        String profesionalId = "prof1";
        String pacienteCI = "12345678";
        TenantContext.setCurrentTenant("101");

        when(politicasAccesoClient.verificarPermiso(eq(profesionalId), eq(pacienteCI), isNull(), eq("101")))
            .thenReturn(false);

        // Act
        Response response = resource.verificarPermiso(profesionalId, pacienteCI, null);

        // Assert
        assertEquals(200, response.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> entity = (Map<String, Object>) response.getEntity();
        assertFalse((Boolean) entity.get("tienePermiso"));
    }

    @Test
    void testVerificarPermisoWithNullTenant() {
        // Arrange
        TenantContext.clear();

        // Act
        Response response = resource.verificarPermiso("prof1", "12345678", null);

        // Assert
        assertEquals(400, response.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> entity = (Map<String, Object>) response.getEntity();
        assertTrue(entity.get("error").toString().contains("Tenant"));
    }

    @Test
    void testVerificarPermisoWithBlankTenant() {
        // Arrange
        TenantContext.setCurrentTenant("");

        // Act
        Response response = resource.verificarPermiso("prof1", "12345678", null);

        // Assert
        assertEquals(400, response.getStatus());
    }

    @Test
    void testVerificarPermisoWithNullPacienteCI() {
        // Arrange
        TenantContext.setCurrentTenant("101");

        // Act
        Response response = resource.verificarPermiso("prof1", null, null);

        // Assert
        assertEquals(400, response.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> entity = (Map<String, Object>) response.getEntity();
        assertTrue(entity.get("error").toString().contains("pacienteCI"));
    }

    @Test
    void testVerificarPermisoWithBlankPacienteCI() {
        // Arrange
        TenantContext.setCurrentTenant("101");

        // Act
        Response response = resource.verificarPermiso("prof1", "", null);

        // Assert
        assertEquals(400, response.getStatus());
    }

    @Test
    void testVerificarPermisoWithNullProfesionalIdButPrincipal() {
        // Arrange
        TenantContext.setCurrentTenant("101");
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("prof1");

        when(politicasAccesoClient.verificarPermiso(eq("prof1"), eq("12345678"), isNull(), eq("101")))
            .thenReturn(true);

        // Act
        Response response = resource.verificarPermiso(null, "12345678", null);

        // Assert
        assertEquals(200, response.getStatus());
        verify(politicasAccesoClient).verificarPermiso("prof1", "12345678", null, "101");
    }

    @Test
    void testVerificarPermisoWithNullProfesionalIdAndNoPrincipal() {
        // Arrange
        TenantContext.setCurrentTenant("101");
        when(securityContext.getUserPrincipal()).thenReturn(null);

        // Act
        Response response = resource.verificarPermiso(null, "12345678", null);

        // Assert
        assertEquals(400, response.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> entity = (Map<String, Object>) response.getEntity();
        assertTrue(entity.get("error").toString().contains("profesionalId"));
    }

    @Test
    void testVerificarPermisoWithException() {
        // Arrange
        TenantContext.setCurrentTenant("101");
        when(politicasAccesoClient.verificarPermiso(anyString(), anyString(), anyString(), anyString()))
            .thenThrow(new RuntimeException("Service error"));

        // Act
        Response response = resource.verificarPermiso("prof1", "12345678", null);

        // Assert
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> entity = (Map<String, Object>) response.getEntity();
        assertTrue(entity.get("error").toString().contains("Error"));
    }
}

