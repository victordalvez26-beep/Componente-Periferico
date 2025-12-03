package uy.edu.tse.hcen.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.tse.hcen.model.UsuarioSalud;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioSaludRepositoryTest {

    @Mock
    private EntityManager em;

    @Mock
    private Query query;

    @InjectMocks
    private UsuarioSaludRepository repository;

    @Test
    void testFindByCiAndTenant() {
        String ci = "12345678";
        Long tenantId = 1L;
        Object[] row = createMockRow();
        
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(row);
        
        UsuarioSalud result = repository.findByCiAndTenant(ci, tenantId);
        
        assertNotNull(result);
        assertEquals(ci, result.getCi());
    }

    @Test
    void testFindByCiAndTenantNotFound() {
        String ci = "12345678";
        Long tenantId = 1L;
        
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenThrow(new NoResultException());
        
        UsuarioSalud result = repository.findByCiAndTenant(ci, tenantId);
        
        assertNull(result);
    }

    @Test
    void testFindByTenant() {
        Long tenantId = 1L;
        List<Object[]> rows = new ArrayList<>();
        rows.add(createMockRow());
        
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(rows);
        
        List<UsuarioSalud> result = repository.findByTenant(tenantId);
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testFindByTenantException() {
        Long tenantId = 1L;
        
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenThrow(new RuntimeException());
        
        List<UsuarioSalud> result = repository.findByTenant(tenantId);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindById() {
        Long id = 1L;
        Long tenantId = 1L;
        Object[] row = createMockRow();
        
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(row);
        
        UsuarioSalud result = repository.findById(id, tenantId);
        
        assertNotNull(result);
    }

    @Test
    void testFindByHcenUserId() {
        Long hcenUserId = 100L;
        Long tenantId = 1L;
        Object[] row = createMockRow();
        
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(row);
        
        UsuarioSalud result = repository.findByHcenUserId(hcenUserId, tenantId);
        
        assertNotNull(result);
    }

    @Test
    void testPersist() {
        UsuarioSalud usuario = new UsuarioSalud();
        usuario.setCi("12345678");
        usuario.setNombre("Juan");
        usuario.setApellido("Pérez");
        usuario.setTenantId(1L);
        
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1L);
        
        repository.persist(usuario);
        
        assertNotNull(usuario.getId());
        verify(query).getSingleResult();
    }

    @Test
    void testMerge() {
        UsuarioSalud usuario = new UsuarioSalud();
        usuario.setId(1L);
        usuario.setCi("12345678");
        usuario.setNombre("Juan");
        usuario.setApellido("Pérez");
        usuario.setTenantId(1L);
        
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(1);
        
        UsuarioSalud result = repository.merge(usuario);
        
        assertNotNull(result);
        verify(query).executeUpdate();
    }

    @Test
    void testRemove() {
        UsuarioSalud usuario = new UsuarioSalud();
        usuario.setId(1L);
        usuario.setTenantId(1L);
        
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(1);
        
        repository.remove(usuario);
        
        verify(query).executeUpdate();
    }

    private Object[] createMockRow() {
        Object[] row = new Object[14];
        row[0] = 1L; // id
        row[1] = "12345678"; // ci
        row[2] = "Juan"; // nombre
        row[3] = "Pérez"; // apellido
        row[4] = Date.valueOf(LocalDate.of(1990, 1, 1)); // fecha_nacimiento
        row[5] = "Dirección 123"; // direccion
        row[6] = "099123456"; // telefono
        row[7] = "juan@example.com"; // email
        row[8] = "MONTEVIDEO"; // departamento
        row[9] = "Centro"; // localidad
        row[10] = 100L; // hcen_user_id
        row[11] = 1L; // tenant_id
        row[12] = java.sql.Timestamp.valueOf(LocalDateTime.now()); // fecha_alta
        row[13] = null; // fecha_actualizacion
        return row;
    }
}

