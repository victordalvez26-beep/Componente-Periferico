package uy.edu.tse.hcen.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.tse.hcen.model.NodoPeriferico;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NodoPerifericoRepositoryTest {

    @Mock
    private EntityManager em;

    @InjectMocks
    private NodoPerifericoRepository repository;

    @Test
    void testFindById() {
        Long id = 1L;
        NodoPeriferico nodo = mock(NodoPeriferico.class);
        when(nodo.getId()).thenReturn(id);
        
        when(em.find(NodoPeriferico.class, id)).thenReturn(nodo);
        
        Optional<NodoPeriferico> result = repository.findById(id);
        
        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
        verify(em).find(NodoPeriferico.class, id);
    }

    @Test
    void testFindByIdNotFound() {
        Long id = 1L;
        
        when(em.find(NodoPeriferico.class, id)).thenReturn(null);
        when(em.getReference(NodoPeriferico.class, id)).thenReturn(null);
        
        Optional<NodoPeriferico> result = repository.findById(id);
        
        assertFalse(result.isPresent());
    }

    @Test
    void testFindByIdWithGetReference() {
        Long id = 1L;
        NodoPeriferico nodo = mock(NodoPeriferico.class);
        when(nodo.getId()).thenReturn(id);
        
        when(em.find(NodoPeriferico.class, id)).thenReturn(null);
        when(em.getReference(NodoPeriferico.class, id)).thenReturn(nodo);
        
        Optional<NodoPeriferico> result = repository.findById(id);
        
        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
    }

    @Test
    void testFindByIdException() {
        Long id = 1L;
        
        when(em.find(NodoPeriferico.class, id)).thenThrow(new RuntimeException("Database error"));
        
        Optional<NodoPeriferico> result = repository.findById(id);
        
        assertFalse(result.isPresent());
    }
}

