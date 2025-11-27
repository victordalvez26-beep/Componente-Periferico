package uy.edu.tse.hcen.rest.admin;

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
import uy.edu.tse.hcen.service.TenantAdminService;

import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminTenantResourceTest {

    @Mock
    private TenantAdminService tenantAdminService;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private AdminTenantResource resource;

    @BeforeEach
    void setUp() {
        try {
            java.lang.reflect.Field field = AdminTenantResource.class.getDeclaredField("tenantAdminService");
            field.setAccessible(true);
            field.set(resource, tenantAdminService);
        } catch (Exception e) {
            fail("Error setting up mocks: " + e.getMessage());
        }
    }

    @Test
    void testCreateTenantSuccess() throws SQLException {
        AdminTenantResource.TenantCreateRequest req = new AdminTenantResource.TenantCreateRequest();
        req.tenantId = "123";
        req.nombrePortal = "Clínica Test";
        req.colorPrimario = "#007bff";
        
        when(securityContext.isUserInRole("ADMINISTRADOR")).thenReturn(true);
        doNothing().when(tenantAdminService).createTenantSchema(anyString(), anyString(), anyString());
        
        Response response = resource.createTenant(req, securityContext);
        
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    void testCreateTenantNoAuth() {
        AdminTenantResource.TenantCreateRequest req = new AdminTenantResource.TenantCreateRequest();
        req.tenantId = "123";
        
        when(securityContext.isUserInRole("ADMINISTRADOR")).thenReturn(false);
        
        Response response = resource.createTenant(req, securityContext);
        
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    void testCreateTenantNullRequest() {
        when(securityContext.isUserInRole("ADMINISTRADOR")).thenReturn(true);
        
        Response response = resource.createTenant(null, securityContext);
        
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testCreateTenantMissingTenantId() {
        AdminTenantResource.TenantCreateRequest req = new AdminTenantResource.TenantCreateRequest();
        when(securityContext.isUserInRole("ADMINISTRADOR")).thenReturn(true);
        
        Response response = resource.createTenant(req, securityContext);
        
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testListTenantsSuccess() throws SQLException {
        when(securityContext.isUserInRole("ADMINISTRADOR")).thenReturn(true);
        List<Map<String, Object>> tenants = new ArrayList<>();
        when(tenantAdminService.listTenants()).thenReturn(tenants);
        
        Response response = resource.listTenants(securityContext);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void testListTenantsNoAuth() {
        when(securityContext.isUserInRole("ADMINISTRADOR")).thenReturn(false);
        
        Response response = resource.listTenants(securityContext);
        
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }
}

