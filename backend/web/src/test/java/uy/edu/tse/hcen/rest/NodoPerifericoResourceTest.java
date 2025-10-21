package uy.edu.tse.hcen.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uy.edu.tse.hcen.model.NodoPeriferico;
import uy.edu.tse.hcen.repository.NodoPerifericoRepository;
import uy.edu.tse.hcen.rest.dto.NodoPerifericoDTO;
import uy.edu.tse.hcen.rest.dto.NodoPerifericoConverter;

import jakarta.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class NodoPerifericoResourceTest {

    @Mock
    private NodoPerifericoRepository repo;

    private NodoPerifericoResource resource;

    @BeforeEach
    public void setup() throws Exception {
        MockitoAnnotations.openMocks(this);
        resource = new NodoPerifericoResource();
        // inject mock repo via reflection
        java.lang.reflect.Field f = NodoPerifericoResource.class.getDeclaredField("repo");
        f.setAccessible(true);
        f.set(resource, repo);
    }

    @Test
    public void create_valid_shouldCallRepoAndReturnCreated() {
        NodoPerifericoDTO dto = new NodoPerifericoDTO();
        dto.setNombre("Centro A");
        dto.setRUT("12345678-9");
        dto.setDepartamento("MONTEVIDEO");
        dto.setEstado("ACTIVO");

        NodoPeriferico saved = NodoPerifericoConverter.toEntity(dto);
        saved.setId(10L);

        when(repo.create(any())).thenReturn(saved);

        Response resp = resource.create(dto);

        assertEquals(201, resp.getStatus());
        NodoPerifericoDTO returned = (NodoPerifericoDTO) resp.getEntity();
        assertNotNull(returned);
        assertEquals(10L, returned.getId());

        ArgumentCaptor<NodoPeriferico> captor = ArgumentCaptor.forClass(NodoPeriferico.class);
        verify(repo, times(1)).create(captor.capture());
        assertEquals(dto.getNombre(), captor.getValue().getNombre());
    }

    @Test
    public void create_invalidEnum_shouldReturnBadRequest() {
        NodoPerifericoDTO dto = new NodoPerifericoDTO();
        dto.setNombre("Centro B");
        dto.setRUT("87654321-0");
        dto.setDepartamento("NO_SU_DEPARTAMENTO");
        dto.setEstado("ACTIVO");

        Response resp = resource.create(dto);
        assertEquals(400, resp.getStatus());
        String msg = (String) resp.getEntity();
        assertTrue(msg.contains("Invalid departamento value"));
    }
}
