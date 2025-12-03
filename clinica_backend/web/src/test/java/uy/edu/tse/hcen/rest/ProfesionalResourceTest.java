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
        TenantContext.setCurrentTenant("101");
        
        // Inyectar mocks usando reflection
        try {
            java.lang.reflect.Field field = ProfesionalResource.class.getDeclaredField("politicasAccesoClient");
            field.setAccessible(true);
            field.set(resource, politicasAccesoClient);
            
            field = ProfesionalResource.class.getDeclaredField("securityContext");
            field.setAccessible(true);
            field.set(resource, securityContext);
        } catch (Exception e) {
            throw new RuntimeException("Error setting up mocks: " + e.getMessage(), e);
        }
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
void testVerificarPermisoWithException_Fixed() {
    // Arrange
    final String PROFESIONAL_ID = "prof1";
    final String PACIENTE_CI = "12345678";
    final String TENANT_ID = "101";
    
    TenantContext.setCurrentTenant(TENANT_ID);

    // STUBBING CORREGIDO: Usamos isNull() para el tercer argumento (tipoDoc)
    when(politicasAccesoClient.verificarPermiso(
        eq(PROFESIONAL_ID),   // Argumento 1: profesionalId
        eq(PACIENTE_CI),      // Argumento 2: pacienteCI
        isNull(String.class), // Argumento 3: tipoDoc (null)
        eq(TENANT_ID)         // Argumento 4: tenantId
    )).thenThrow(new RuntimeException("Service error: Conexión fallida"));

    // Act
    // Usamos los mismos parámetros que el stubbing
    Response response = resource.verificarPermiso(PROFESIONAL_ID, PACIENTE_CI, null);

    // Assert
    assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus(),
                 "Debe retornar 500 INTERNAL_SERVER_ERROR cuando el cliente lanza una excepción.");
    
    // Verificamos que la llamada al cliente se hizo correctamente (opcional)
    verify(politicasAccesoClient, times(1)).verificarPermiso(
        eq(PROFESIONAL_ID), eq(PACIENTE_CI), isNull(), eq(TENANT_ID)
    );
    
    @SuppressWarnings("unchecked")
    Map<String, Object> entity = (Map<String, Object>) response.getEntity();
    assertNotNull(entity, "El cuerpo de la respuesta no debe ser nulo.");
    assertTrue(entity.get("error").toString().contains("Error"),
               "El mensaje de error debe contener 'Error'.");
}


}

