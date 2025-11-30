package uy.edu.tse.hcen.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.tse.hcen.model.ProfesionalSalud;
import uy.edu.tse.hcen.model.enums.Especialidad;
import uy.edu.tse.hcen.multitenancy.TenantContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfesionalSaludRepositoryTest {

    @Mock
    private EntityManager em;

    @Mock
    private TypedQuery<ProfesionalSalud> query;

    @InjectMocks
    private ProfesionalSaludRepository repository;

    private ProfesionalSalud profesional;

    @BeforeEach
    void setUp() throws Exception {
        TenantContext.clear();
        
        profesional = new ProfesionalSalud();
        profesional.setId(1L);
        profesional.setNombre("Dr. Juan Pérez");
        profesional.setEmail("juan@example.com");
        profesional.setNickname("jperez");
        profesional.setEspecialidad(Especialidad.CARDIOLOGIA);
        
        // Inyectar EntityManager mock usando reflection
        java.lang.reflect.Field emField = ProfesionalSaludRepository.class.getDeclaredField("em");
        emField.setAccessible(true);
        emField.set(repository, em);
    }

    @Test
    void testFindAll() {
        // Arrange
        List<ProfesionalSalud> profesionales = Arrays.asList(profesional);
        TenantContext.setCurrentTenant("101");
        
        when(em.createQuery(eq("SELECT p FROM ProfesionalSalud p"), eq(ProfesionalSalud.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(profesionales);

        // Act
        List<ProfesionalSalud> result = repository.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(profesional.getId(), result.get(0).getId());
        verify(em, times(1)).createQuery(eq("SELECT p FROM ProfesionalSalud p"), eq(ProfesionalSalud.class));
        verify(query, times(1)).getResultList();
    }

    @Test
    void testFindAllWithNullTenant() {
        // Arrange
        List<ProfesionalSalud> profesionales = Arrays.asList(profesional);
        TenantContext.clear();
        
        when(em.createQuery(eq("SELECT p FROM ProfesionalSalud p"), eq(ProfesionalSalud.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(profesionales);

        // Act
        List<ProfesionalSalud> result = repository.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(em, times(1)).createQuery(eq("SELECT p FROM ProfesionalSalud p"), eq(ProfesionalSalud.class));
        verify(query, times(1)).getResultList();
    }

    @Test
    void testFindAllWithBlankTenant() {
        // Arrange
        List<ProfesionalSalud> profesionales = Arrays.asList(profesional);
        TenantContext.setCurrentTenant("");
        
        when(em.createQuery(eq("SELECT p FROM ProfesionalSalud p"), eq(ProfesionalSalud.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(profesionales);

        // Act
        List<ProfesionalSalud> result = repository.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(em, times(1)).createQuery(eq("SELECT p FROM ProfesionalSalud p"), eq(ProfesionalSalud.class));
        verify(query, times(1)).getResultList();
    }

    @Test
    void testFindById() {
        // Arrange
        Long id = 1L;
        when(em.find(ProfesionalSalud.class, id)).thenReturn(profesional);

        // Act
        Optional<ProfesionalSalud> result = repository.findById(id);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
        verify(em).find(ProfesionalSalud.class, id);
    }

    @Test
    void testFindByIdNotFound() {
        // Arrange
        Long id = 999L;
        when(em.find(ProfesionalSalud.class, id)).thenReturn(null);

        // Act
        Optional<ProfesionalSalud> result = repository.findById(id);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testFindByNickname() {
        // Arrange
        String nickname = "jperez";
        List<ProfesionalSalud> profesionales = Arrays.asList(profesional);
        
        when(em.createQuery(eq("SELECT p FROM ProfesionalSalud p WHERE p.nickname = :nick"), eq(ProfesionalSalud.class))).thenReturn(query);
        when(query.setParameter("nick", nickname)).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
        when(query.getResultList()).thenReturn(profesionales);

        // Act
        Optional<ProfesionalSalud> result = repository.findByNickname(nickname);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(nickname, result.get().getNickname());
        verify(em, times(1)).createQuery(eq("SELECT p FROM ProfesionalSalud p WHERE p.nickname = :nick"), eq(ProfesionalSalud.class));
        verify(query, times(1)).setParameter("nick", nickname);
        verify(query, times(1)).setMaxResults(1);
        verify(query, times(1)).getResultList();
    }

    @Test
    void testFindByNicknameNotFound() {
        // Arrange
        String nickname = "notfound";
        
        when(em.createQuery(anyString(), eq(ProfesionalSalud.class))).thenReturn(query);
        when(query.setParameter("nick", nickname)).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList());

        // Act
        Optional<ProfesionalSalud> result = repository.findByNickname(nickname);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testFindByEmail() {
        // Arrange
        String email = "juan@example.com";
        List<ProfesionalSalud> profesionales = Arrays.asList(profesional);
        
        when(em.createQuery(eq("SELECT p FROM ProfesionalSalud p WHERE p.email = :email"), eq(ProfesionalSalud.class))).thenReturn(query);
        when(query.setParameter("email", email)).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
        when(query.getResultList()).thenReturn(profesionales);

        // Act
        Optional<ProfesionalSalud> result = repository.findByEmail(email);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(email, result.get().getEmail());
        verify(em, times(1)).createQuery(eq("SELECT p FROM ProfesionalSalud p WHERE p.email = :email"), eq(ProfesionalSalud.class));
        verify(query, times(1)).setParameter("email", email);
        verify(query, times(1)).setMaxResults(1);
        verify(query, times(1)).getResultList();
    }

    @Test
    void testFindByEmailNotFound() {
        // Arrange
        String email = "notfound@example.com";
        
        when(em.createQuery(anyString(), eq(ProfesionalSalud.class))).thenReturn(query);
        when(query.setParameter("email", email)).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList());

        // Act
        Optional<ProfesionalSalud> result = repository.findByEmail(email);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testSaveNew() {
        // Arrange
        ProfesionalSalud newProfesional = new ProfesionalSalud();
        newProfesional.setNombre("Dr. Nuevo");
        newProfesional.setId(null); // New entity
        doNothing().when(em).persist(any(ProfesionalSalud.class));

        // Act
        ProfesionalSalud result = repository.save(newProfesional);

        // Assert
        assertNotNull(result);
        verify(em).persist(newProfesional);
        verify(em, never()).merge(any(ProfesionalSalud.class));
    }

    @Test
    void testSaveExisting() {
        // Arrange
        profesional.setId(1L); // Existing entity
        when(em.merge(profesional)).thenReturn(profesional);

        // Act
        ProfesionalSalud result = repository.save(profesional);

        // Assert
        assertNotNull(result);
        verify(em, never()).persist(any(ProfesionalSalud.class));
        verify(em).merge(profesional);
    }

    @Test
    void testDelete() {
        // Arrange
        when(em.merge(profesional)).thenReturn(profesional);
        doNothing().when(em).remove(any(ProfesionalSalud.class));

        // Act
        repository.delete(profesional);

        // Assert
        verify(em).merge(profesional);
        verify(em).remove(any(ProfesionalSalud.class));
    }

    @Test
    void testFindAllWithEmptyList() {
        // Arrange
        TenantContext.setCurrentTenant("101");
        
        when(em.createQuery(eq("SELECT p FROM ProfesionalSalud p"), eq(ProfesionalSalud.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList());

        // Act
        List<ProfesionalSalud> result = repository.findAll();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(em, times(1)).createQuery(eq("SELECT p FROM ProfesionalSalud p"), eq(ProfesionalSalud.class));
        verify(query, times(1)).getResultList();
    }

    @Test
    void testFindByNicknameWithEmptyList() {
        // Arrange
        String nickname = "notfound";
        
        when(em.createQuery(eq("SELECT p FROM ProfesionalSalud p WHERE p.nickname = :nick"), eq(ProfesionalSalud.class))).thenReturn(query);
        when(query.setParameter("nick", nickname)).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
        when(query.getResultList()).thenReturn(java.util.Collections.emptyList());

        // Act
        Optional<ProfesionalSalud> result = repository.findByNickname(nickname);

        // Assert
        assertFalse(result.isPresent());
        verify(em, times(1)).createQuery(eq("SELECT p FROM ProfesionalSalud p WHERE p.nickname = :nick"), eq(ProfesionalSalud.class));
        verify(query, times(1)).setParameter("nick", nickname);
        verify(query, times(1)).setMaxResults(1);
        verify(query, times(1)).getResultList();
    }

    @Test
    void testFindByEmailWithEmptyList() {
        // Arrange
        String email = "notfound@example.com";
        
        when(em.createQuery(eq("SELECT p FROM ProfesionalSalud p WHERE p.email = :email"), eq(ProfesionalSalud.class))).thenReturn(query);
        when(query.setParameter("email", email)).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
        when(query.getResultList()).thenReturn(java.util.Collections.emptyList());

        // Act
        Optional<ProfesionalSalud> result = repository.findByEmail(email);

        // Assert
        assertFalse(result.isPresent());
        verify(em, times(1)).createQuery(eq("SELECT p FROM ProfesionalSalud p WHERE p.email = :email"), eq(ProfesionalSalud.class));
        verify(query, times(1)).setParameter("email", email);
        verify(query, times(1)).setMaxResults(1);
        verify(query, times(1)).getResultList();
    }

    @Test
    void testFindByIdWithNull() {
        // Arrange
        Long id = 1L;
        when(em.find(ProfesionalSalud.class, id)).thenReturn(null);

        // Act
        Optional<ProfesionalSalud> result = repository.findById(id);

        // Assert
        assertFalse(result.isPresent());
        verify(em, times(1)).find(ProfesionalSalud.class, id);
    }

    @Test
    void testSaveNewWithNullId() {
        // Arrange
        ProfesionalSalud newProfesional = new ProfesionalSalud();
        newProfesional.setNombre("Dr. Nuevo");
        newProfesional.setId(null);
        doNothing().when(em).persist(any(ProfesionalSalud.class));

        // Act
        ProfesionalSalud result = repository.save(newProfesional);

        // Assert
        assertNotNull(result);
        assertEquals(newProfesional, result);
        verify(em, times(1)).persist(newProfesional);
        verify(em, never()).merge(any(ProfesionalSalud.class));
    }

    @Test
    void testSaveExistingWithId() {
        // Arrange
        ProfesionalSalud existingProfesional = new ProfesionalSalud();
        existingProfesional.setId(100L);
        existingProfesional.setNombre("Dr. Existente");
        when(em.merge(existingProfesional)).thenReturn(existingProfesional);

        // Act
        ProfesionalSalud result = repository.save(existingProfesional);

        // Assert
        assertNotNull(result);
        assertEquals(existingProfesional, result);
        verify(em, never()).persist(any(ProfesionalSalud.class));
        verify(em, times(1)).merge(existingProfesional);
    }

    @Test
    void testDeleteWithMerge() {
        // Arrange
        ProfesionalSalud profesionalToDelete = new ProfesionalSalud();
        profesionalToDelete.setId(1L);
        ProfesionalSalud mergedProfesional = new ProfesionalSalud();
        mergedProfesional.setId(1L);
        
        when(em.merge(profesionalToDelete)).thenReturn(mergedProfesional);
        doNothing().when(em).remove(mergedProfesional);

        // Act
        repository.delete(profesionalToDelete);

        // Assert
        verify(em, times(1)).merge(profesionalToDelete);
        verify(em, times(1)).remove(mergedProfesional);
    }
}

