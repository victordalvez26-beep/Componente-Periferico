package uy.edu.tse.hcen.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.tse.hcen.model.ProfesionalSalud;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProfesionalSaludRepository.
 * 
 * @author Senior Test Engineer
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProfesionalSaludRepository Tests")
class ProfesionalSaludRepositoryTest {

    @Mock
    private EntityManager em;

    @Mock
    private TypedQuery<ProfesionalSalud> query;

    @InjectMocks
    private ProfesionalSaludRepository repository;

    private ProfesionalSalud createTestProfesional() {
        ProfesionalSalud prof = new ProfesionalSalud();
        prof.setId(1L);
        prof.setNickname("doctor1");
        prof.setNombre("Dr. Juan Pérez");
        prof.setEmail("doctor@clinic.com");
        return prof;
    }

    // ==================== FIND ALL TESTS ====================

    @Nested
    @DisplayName("FindAll Tests")
    class FindAllTests {

        @Test
        @DisplayName("FindAll con profesionales debe retornar lista")
        void findAll_withProfessionals_shouldReturnList() {
            // Arrange
            List<ProfesionalSalud> profesionales = Arrays.asList(
                    createTestProfesional(),
                    createTestProfesional()
            );
            
            when(em.createQuery(anyString(), eq(ProfesionalSalud.class))).thenReturn(query);
            when(query.getResultList()).thenReturn(profesionales);

            // Act
            List<ProfesionalSalud> result = repository.findAll();

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            
            verify(em).createQuery(anyString(), eq(ProfesionalSalud.class));
        }

        @Test
        @DisplayName("FindAll sin profesionales debe retornar lista vacía")
        void findAll_empty_shouldReturnEmptyList() {
            // Arrange
            when(em.createQuery(anyString(), eq(ProfesionalSalud.class))).thenReturn(query);
            when(query.getResultList()).thenReturn(Collections.emptyList());

            // Act
            List<ProfesionalSalud> result = repository.findAll();

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==================== FIND BY ID TESTS ====================

    @Nested
    @DisplayName("FindById Tests")
    class FindByIdTests {

        @Test
        @DisplayName("FindById existente debe retornar Optional")
        void findById_existing_shouldReturn() {
            // Arrange
            ProfesionalSalud prof = createTestProfesional();
            when(em.find(ProfesionalSalud.class, 1L)).thenReturn(prof);

            // Act
            Optional<ProfesionalSalud> result = repository.findById(1L);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(1L, result.get().getId());
        }

        @Test
        @DisplayName("FindById inexistente debe retornar vacío")
        void findById_notFound_shouldReturnEmpty() {
            // Arrange
            when(em.find(ProfesionalSalud.class, 999L)).thenReturn(null);

            // Act
            Optional<ProfesionalSalud> result = repository.findById(999L);

            // Assert
            assertFalse(result.isPresent());
        }
    }

    // ==================== FIND BY NICKNAME TESTS ====================

    @Nested
    @DisplayName("FindByNickname Tests")
    class FindByNicknameTests {

        @Test
        @DisplayName("FindByNickname existente debe retornar Optional")
        void findByNickname_existing_shouldReturn() {
            // Arrange
            ProfesionalSalud prof = createTestProfesional();
            
            when(em.createQuery(anyString(), eq(ProfesionalSalud.class))).thenReturn(query);
            when(query.setParameter("nick", "doctor1")).thenReturn(query);
            when(query.setMaxResults(1)).thenReturn(query);
            when(query.getResultList()).thenReturn(List.of(prof));

            // Act
            Optional<ProfesionalSalud> result = repository.findByNickname("doctor1");

            // Assert
            assertTrue(result.isPresent());
            assertEquals("doctor1", result.get().getNickname());
        }

        @Test
        @DisplayName("FindByNickname inexistente debe retornar vacío")
        void findByNickname_notFound_shouldReturnEmpty() {
            // Arrange
            when(em.createQuery(anyString(), eq(ProfesionalSalud.class))).thenReturn(query);
            when(query.setParameter("nick", "nonexistent")).thenReturn(query);
            when(query.setMaxResults(1)).thenReturn(query);
            when(query.getResultList()).thenReturn(Collections.emptyList());

            // Act
            Optional<ProfesionalSalud> result = repository.findByNickname("nonexistent");

            // Assert
            assertFalse(result.isPresent());
        }
    }

    // ==================== FIND BY EMAIL TESTS ====================

    @Nested
    @DisplayName("FindByEmail Tests")
    class FindByEmailTests {

        @Test
        @DisplayName("FindByEmail existente debe retornar Optional")
        void findByEmail_existing_shouldReturn() {
            // Arrange
            ProfesionalSalud prof = createTestProfesional();
            
            when(em.createQuery(anyString(), eq(ProfesionalSalud.class))).thenReturn(query);
            when(query.setParameter("email", "doctor@clinic.com")).thenReturn(query);
            when(query.setMaxResults(1)).thenReturn(query);
            when(query.getResultList()).thenReturn(List.of(prof));

            // Act
            Optional<ProfesionalSalud> result = repository.findByEmail("doctor@clinic.com");

            // Assert
            assertTrue(result.isPresent());
            assertEquals("doctor@clinic.com", result.get().getEmail());
        }

        @Test
        @DisplayName("FindByEmail inexistente debe retornar vacío")
        void findByEmail_notFound_shouldReturnEmpty() {
            // Arrange
            when(em.createQuery(anyString(), eq(ProfesionalSalud.class))).thenReturn(query);
            when(query.setParameter("email", "nonexistent@email.com")).thenReturn(query);
            when(query.setMaxResults(1)).thenReturn(query);
            when(query.getResultList()).thenReturn(Collections.emptyList());

            // Act
            Optional<ProfesionalSalud> result = repository.findByEmail("nonexistent@email.com");

            // Assert
            assertFalse(result.isPresent());
        }
    }

    // ==================== SAVE TESTS ====================

    @Nested
    @DisplayName("Save Tests")
    class SaveTests {

        @Test
        @DisplayName("Save con ID null debe hacer persist")
        void save_newEntity_shouldPersist() {
            // Arrange
            ProfesionalSalud prof = new ProfesionalSalud();
            prof.setId(null);
            prof.setNickname("newdoctor");
            
            doNothing().when(em).persist(prof);

            // Act
            ProfesionalSalud result = repository.save(prof);

            // Assert
            assertEquals(prof, result);
            verify(em).persist(prof);
            verify(em, never()).merge(any());
        }

        @Test
        @DisplayName("Save con ID existente debe hacer merge")
        void save_existingEntity_shouldMerge() {
            // Arrange
            ProfesionalSalud prof = createTestProfesional();
            
            when(em.merge(prof)).thenReturn(prof);

            // Act
            ProfesionalSalud result = repository.save(prof);

            // Assert
            assertEquals(prof, result);
            verify(em).merge(prof);
            verify(em, never()).persist(any());
        }
    }

    // ==================== DELETE TESTS ====================

    @Nested
    @DisplayName("Delete Tests")
    class DeleteTests {

        @Test
        @DisplayName("Delete debe remover profesional")
        void delete_shouldRemove() {
            // Arrange
            ProfesionalSalud prof = createTestProfesional();
            
            when(em.merge(prof)).thenReturn(prof);
            doNothing().when(em).remove(prof);

            // Act
            repository.delete(prof);

            // Assert
            verify(em).merge(prof);
            verify(em).remove(prof);
        }
    }
}

