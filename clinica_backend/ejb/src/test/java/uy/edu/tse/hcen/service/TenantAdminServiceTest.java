package uy.edu.tse.hcen.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.tse.hcen.repository.UsuarioPerifericoRepository;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantAdminService Tests")
class TenantAdminServiceTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private EntityManager em;

    @Mock
    private UsuarioPerifericoRepository usuarioRepository;

    @InjectMocks
    private TenantAdminService service;

    @Test
    void service_shouldBeInstantiable() {
        assertNotNull(service);
    }

    @Test
    void adminCreationResult_shouldHavePublicFields() {
        TenantAdminService.AdminCreationResult result = new TenantAdminService.AdminCreationResult();
        result.adminNickname = "admin1";
        result.activationToken = "token123";
        result.activationUrl = "http://url";
        result.tokenExpiry = java.time.LocalDateTime.now();
        
        assertEquals("admin1", result.adminNickname);
        assertEquals("token123", result.activationToken);
        assertNotNull(result.tokenExpiry);
    }
}

