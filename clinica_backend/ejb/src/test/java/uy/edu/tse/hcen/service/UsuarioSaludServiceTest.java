package uy.edu.tse.hcen.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.tse.hcen.client.HcenUsuarioSaludClient;
import uy.edu.tse.hcen.model.UsuarioSalud;
import uy.edu.tse.hcen.multitenancy.TenantContext;
import uy.edu.tse.hcen.repository.UsuarioSaludRepository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioSaludServiceTest {

    @Mock
    private UsuarioSaludRepository repository;

    @Mock
    private HcenUsuarioSaludClient hcenClient;

    @InjectMocks
    private UsuarioSaludService service;

    private UsuarioSalud usuario;
    private Long tenantId;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        tenantId = 101L;

        usuario = new UsuarioSalud();
        usuario.setCi("12345678");
        usuario.setNombre("Juan");
        usuario.setApellido("Pérez");
        usuario.setFechaNacimiento(LocalDate.of(1990, 5, 15));
        usuario.setEmail("juan@example.com");
    }

    @Test
    void testCrearUsuarioSalud() {
        // Arrange
        when(repository.findByCiAndTenant(usuario.getCi(), tenantId)).thenReturn(null);
        doAnswer(invocation -> {
            UsuarioSalud u = invocation.getArgument(0);
            u.setId(1L);
            return null;
        }).when(repository).persist(any(UsuarioSalud.class));
        
        HcenUsuarioSaludClient.HcenUserResponse hcenResponse = 
            new HcenUsuarioSaludClient.HcenUserResponse(1000L, "Usuario registrado");
        when(hcenClient.registrarUsuarioEnHcen(eq(tenantId), any(UsuarioSalud.class))).thenReturn(hcenResponse);

        // Act
        UsuarioSalud result = service.crearUsuarioSalud(tenantId, usuario);

        // Assert
        assertNotNull(result);
        assertEquals(tenantId, result.getTenantId());
        assertNotNull(result.getFechaAlta());
        verify(repository).findByCiAndTenant(usuario.getCi(), tenantId);
        verify(repository).persist(any(UsuarioSalud.class));
    }

    @Test
    void testCrearUsuarioSaludWithExistingCi() {
        // Arrange
        UsuarioSalud existing = new UsuarioSalud();
        existing.setId(1L);
        existing.setCi("12345678");
        when(repository.findByCiAndTenant(usuario.getCi(), tenantId)).thenReturn(existing);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            service.crearUsuarioSalud(tenantId, usuario);
        });
    }

    @Test
    void testActualizarUsuarioSalud() {
        // Arrange
        Long id = 1L;
        UsuarioSalud existing = new UsuarioSalud();
        existing.setId(id);
        existing.setCi("12345678");
        existing.setNombre("Juan Original");
        
        UsuarioSalud datosActualizados = new UsuarioSalud();
        datosActualizados.setNombre("Juan Actualizado");
        datosActualizados.setApellido("Pérez Actualizado");
        
        when(repository.findById(id, tenantId)).thenReturn(existing);
        when(repository.merge(any(UsuarioSalud.class))).thenReturn(existing);
        when(hcenClient.registrarUsuarioEnHcen(eq(tenantId), any(UsuarioSalud.class)))
            .thenReturn(new HcenUsuarioSaludClient.HcenUserResponse(1L, "Usuario actualizado"));

        // Act
        UsuarioSalud result = service.actualizarUsuarioSalud(tenantId, id, datosActualizados);

        // Assert
        assertNotNull(result);
        verify(repository).findById(id, tenantId);
        verify(repository).merge(any(UsuarioSalud.class));
    }

    @Test
    void testActualizarUsuarioSaludNotFound() {
        // Arrange
        Long id = 999L;
        when(repository.findById(id, tenantId)).thenReturn(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            service.actualizarUsuarioSalud(tenantId, id, usuario);
        });
    }

    @Test
    void testListarUsuariosSalud() {
        // Arrange
        List<UsuarioSalud> usuarios = Arrays.asList(usuario);
        when(repository.findByTenant(tenantId)).thenReturn(usuarios);

        // Act
        List<UsuarioSalud> result = service.listarUsuariosSalud(tenantId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository).findByTenant(tenantId);
    }

    @Test
    void testObtenerUsuarioSalud() {
        // Arrange
        Long id = 1L;
        usuario.setId(id);
        when(repository.findById(id, tenantId)).thenReturn(usuario);

        // Act
        UsuarioSalud result = service.obtenerUsuarioSalud(id, tenantId);

        // Assert
        assertNotNull(result);
        assertEquals(id, result.getId());
        verify(repository).findById(id, tenantId);
    }

    @Test
    void testBuscarPorCi() {
        // Arrange
        String ci = "12345678";
        when(repository.findByCiAndTenant(ci, tenantId)).thenReturn(usuario);

        // Act
        UsuarioSalud result = service.buscarPorCi(ci, tenantId);

        // Assert
        assertNotNull(result);
        assertEquals(ci, result.getCi());
        verify(repository).findByCiAndTenant(ci, tenantId);
    }

    @Test
    void testCrearUsuarioSaludWithHcenError() {
        // Arrange
        when(repository.findByCiAndTenant(usuario.getCi(), tenantId)).thenReturn(null);
        doAnswer(invocation -> {
            UsuarioSalud u = invocation.getArgument(0);
            u.setId(1L);
            return null;
        }).when(repository).persist(any(UsuarioSalud.class));
        
        when(hcenClient.registrarUsuarioEnHcen(eq(tenantId), any(UsuarioSalud.class)))
            .thenThrow(new RuntimeException("HCEN no disponible"));

        // Act
        UsuarioSalud result = service.crearUsuarioSalud(tenantId, usuario);

        // Assert
        assertNotNull(result);
        // El usuario debe crearse localmente aunque falle HCEN
        verify(repository).persist(any(UsuarioSalud.class));
    }

    @Test
    void testCrearUsuarioSaludWithHcenNullUserId() {
        // Arrange
        when(repository.findByCiAndTenant(usuario.getCi(), tenantId)).thenReturn(null);
        doAnswer(invocation -> {
            UsuarioSalud u = invocation.getArgument(0);
            u.setId(1L);
            return null;
        }).when(repository).persist(any(UsuarioSalud.class));
        
        HcenUsuarioSaludClient.HcenUserResponse hcenResponse = 
            new HcenUsuarioSaludClient.HcenUserResponse(null, "Error en HCEN");
        when(hcenClient.registrarUsuarioEnHcen(eq(tenantId), any(UsuarioSalud.class))).thenReturn(hcenResponse);

        // Act
        UsuarioSalud result = service.crearUsuarioSalud(tenantId, usuario);

        // Assert
        assertNotNull(result);
        assertNull(result.getHcenUserId());
        verify(repository).persist(any(UsuarioSalud.class));
        verify(repository, never()).merge(any(UsuarioSalud.class));
    }

    @Test
    void testCrearUsuarioSaludWithTenantContextMismatch() {
        // Arrange
        TenantContext.setCurrentTenant("999"); // Diferente al tenantId
        when(repository.findByCiAndTenant(usuario.getCi(), tenantId)).thenReturn(null);
        doAnswer(invocation -> {
            UsuarioSalud u = invocation.getArgument(0);
            u.setId(1L);
            return null;
        }).when(repository).persist(any(UsuarioSalud.class));
        
        HcenUsuarioSaludClient.HcenUserResponse hcenResponse = 
            new HcenUsuarioSaludClient.HcenUserResponse(1000L, "Usuario registrado");
        when(hcenClient.registrarUsuarioEnHcen(eq(tenantId), any(UsuarioSalud.class))).thenReturn(hcenResponse);

        // Act
        UsuarioSalud result = service.crearUsuarioSalud(tenantId, usuario);

        // Assert
        assertNotNull(result);
        assertEquals(tenantId, result.getTenantId());
        verify(repository).persist(any(UsuarioSalud.class));
    }

    @Test
    void testActualizarUsuarioSaludWithHcenError() {
        // Arrange
        Long id = 1L;
        UsuarioSalud existing = new UsuarioSalud();
        existing.setId(id);
        existing.setCi("12345678");
        
        UsuarioSalud datosActualizados = new UsuarioSalud();
        datosActualizados.setNombre("Juan Actualizado");
        
        when(repository.findById(id, tenantId)).thenReturn(existing);
        when(repository.merge(any(UsuarioSalud.class))).thenReturn(existing);
        when(hcenClient.registrarUsuarioEnHcen(eq(tenantId), any(UsuarioSalud.class)))
            .thenThrow(new RuntimeException("HCEN error"));

        // Act
        UsuarioSalud result = service.actualizarUsuarioSalud(tenantId, id, datosActualizados);

        // Assert
        assertNotNull(result);
        verify(repository).merge(any(UsuarioSalud.class));
        // Debe continuar aunque falle HCEN
    }

    @Test
    void testListarUsuariosSaludEmpty() {
        // Arrange
        when(repository.findByTenant(tenantId)).thenReturn(Arrays.asList());

        // Act
        List<UsuarioSalud> result = service.listarUsuariosSalud(tenantId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository).findByTenant(tenantId);
    }

    @Test
    void testBuscarPorCiNotFound() {
        // Arrange
        String ci = "99999999";
        when(repository.findByCiAndTenant(ci, tenantId)).thenReturn(null);

        // Act
        UsuarioSalud result = service.buscarPorCi(ci, tenantId);

        // Assert
        assertNull(result);
        verify(repository).findByCiAndTenant(ci, tenantId);
    }
}

