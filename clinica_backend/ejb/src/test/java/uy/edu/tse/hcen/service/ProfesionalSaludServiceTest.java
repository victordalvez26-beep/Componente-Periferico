package uy.edu.tse.hcen.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uy.edu.tse.hcen.context.TenantContext;
import uy.edu.tse.hcen.dto.ProfesionalDTO;
import uy.edu.tse.hcen.model.ProfesionalSalud;
import uy.edu.tse.hcen.model.enums.Especialidad;
import uy.edu.tse.hcen.repository.NodoPerifericoRepository;
import uy.edu.tse.hcen.repository.ProfesionalSaludRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfesionalSaludServiceTest {

    @Mock
    private ProfesionalSaludRepository profesionalRepository;

    @Mock
    private TenantContext tenantContext;

    @Mock
    private NodoPerifericoRepository nodoRepository;

    @Mock
    private ProfesionalPersistenceHelper persistenceHelper;

    @InjectMocks
    private ProfesionalSaludService profesionalService;

    private ProfesionalDTO dto;
    private ProfesionalSalud profesional;

    @BeforeEach
    void setUp() {
        dto = new ProfesionalDTO();
        dto.setNombre("Dr. Juan Pérez");
        dto.setEmail("juan@example.com");
        dto.setNickname("jperez");
        dto.setEspecialidad(Especialidad.CARDIOLOGIA);
        dto.setDireccion("Av. 18 de Julio 1234");
        dto.setPassword("password123");

        profesional = new ProfesionalSalud();
        profesional.setId(1L);
        profesional.setNombre("Dr. Juan Pérez");
        profesional.setEmail("juan@example.com");
        profesional.setNickname("jperez");
        profesional.setEspecialidad(Especialidad.CARDIOLOGIA);
        profesional.setDireccion("Av. 18 de Julio 1234");
    }

    @Test
    void testCreateWithAdminRole() throws Exception {
        // Arrange
        when(tenantContext.getRole()).thenReturn("ADMINISTRADOR");
        when(tenantContext.getTenantId()).thenReturn("101");
        doNothing().when(persistenceHelper).persistWithManualTransaction(any(ProfesionalSalud.class), anyString());

        // Act
        ProfesionalSalud result = profesionalService.create(dto);

        // Assert
        assertNotNull(result);
        assertEquals(dto.getNombre(), result.getNombre());
        assertEquals(dto.getEmail(), result.getEmail());
        assertEquals(dto.getNickname(), result.getNickname());
        verify(persistenceHelper).persistWithManualTransaction(any(ProfesionalSalud.class), eq("schema_clinica_101"));
    }

    @Test
    void testCreateWithNonAdminRole() {
        // Arrange
        when(tenantContext.getRole()).thenReturn("PROFESIONAL");

        // Act & Assert
        assertThrows(SecurityException.class, () -> {
            profesionalService.create(dto);
        });
    }

    @Test
    void testCreateWithNullRole() {
        // Arrange
        when(tenantContext.getRole()).thenReturn(null);

        // Act & Assert
        assertThrows(SecurityException.class, () -> {
            profesionalService.create(dto);
        });
    }

    @Test
    void testFindAllInCurrentTenant() {
        // Arrange
        List<ProfesionalSalud> profesionales = Arrays.asList(profesional);
        when(profesionalRepository.findAll()).thenReturn(profesionales);

        // Act
        List<ProfesionalSalud> result = profesionalService.findAllInCurrentTenant();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(profesionalRepository).findAll();
    }

    @Test
    void testUpdate() {
        // Arrange
        Long id = 1L;
        ProfesionalDTO updateDto = new ProfesionalDTO();
        updateDto.setNombre("Dr. Juan Pérez Actualizado");
        updateDto.setEmail("juan.actualizado@example.com");

        when(profesionalRepository.findById(id)).thenReturn(Optional.of(profesional));
        when(profesionalRepository.findByNickname(anyString())).thenReturn(Optional.empty());
        when(profesionalRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(profesionalRepository.save(any(ProfesionalSalud.class))).thenReturn(profesional);

        // Act
        ProfesionalSalud result = profesionalService.update(id, updateDto);

        // Assert
        assertNotNull(result);
        verify(profesionalRepository).findById(id);
        verify(profesionalRepository).save(any(ProfesionalSalud.class));
    }

    @Test
    void testUpdateWithNonExistentId() {
        // Arrange
        Long id = 999L;
        when(profesionalRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            profesionalService.update(id, dto);
        });
    }

    @Test
    void testUpdateWithDuplicateNickname() {
        // Arrange
        Long id = 1L;
        ProfesionalDTO updateDto = new ProfesionalDTO();
        updateDto.setNickname("duplicate");

        ProfesionalSalud existing = new ProfesionalSalud();
        existing.setId(2L);
        existing.setNickname("duplicate");

        when(profesionalRepository.findById(id)).thenReturn(Optional.of(profesional));
        when(profesionalRepository.findByNickname("duplicate")).thenReturn(Optional.of(existing));

        // Act & Assert
        // El método lanza RuntimeException cuando JAX-RS no está disponible en tests
        assertThrows(RuntimeException.class, () -> {
            profesionalService.update(id, updateDto);
        });
    }

    @Test
    void testFindById() {
        // Arrange
        Long id = 1L;
        when(profesionalRepository.findById(id)).thenReturn(Optional.of(profesional));

        // Act
        Optional<ProfesionalSalud> result = profesionalService.findById(id);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
        verify(profesionalRepository).findById(id);
    }

    @Test
    void testFindByIdNotFound() {
        // Arrange
        Long id = 999L;
        when(profesionalRepository.findById(id)).thenReturn(Optional.empty());

        // Act
        Optional<ProfesionalSalud> result = profesionalService.findById(id);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testDelete() {
        // Arrange
        Long id = 1L;
        when(profesionalRepository.findById(id)).thenReturn(Optional.of(profesional));
        doNothing().when(profesionalRepository).delete(any(ProfesionalSalud.class));

        // Act
        profesionalService.delete(id);

        // Assert
        verify(profesionalRepository).findById(id);
        verify(profesionalRepository).delete(profesional);
    }

    @Test
    void testDeleteWithNonExistentId() {
        // Arrange
        Long id = 999L;
        when(profesionalRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            profesionalService.delete(id);
        });
    }

    @Test
    void testUpdateWithPassword() {
        // Arrange
        Long id = 1L;
        ProfesionalDTO updateDto = new ProfesionalDTO();
        updateDto.setPassword("newPassword123");

        when(profesionalRepository.findById(id)).thenReturn(Optional.of(profesional));
        when(profesionalRepository.findByNickname(anyString())).thenReturn(Optional.empty());
        when(profesionalRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(profesionalRepository.save(any(ProfesionalSalud.class))).thenReturn(profesional);

        // Act
        ProfesionalSalud result = profesionalService.update(id, updateDto);

        // Assert
        assertNotNull(result);
        verify(profesionalRepository).save(any(ProfesionalSalud.class));
    }

    @Test
    void testUpdateWithBlankPassword() {
        // Arrange
        Long id = 1L;
        ProfesionalDTO updateDto = new ProfesionalDTO();
        updateDto.setPassword("   "); // Blank password

        when(profesionalRepository.findById(id)).thenReturn(Optional.of(profesional));
        when(profesionalRepository.findByNickname(anyString())).thenReturn(Optional.empty());
        when(profesionalRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(profesionalRepository.save(any(ProfesionalSalud.class))).thenReturn(profesional);

        // Act
        ProfesionalSalud result = profesionalService.update(id, updateDto);

        // Assert
        assertNotNull(result);
        // Password should not be updated if blank
    }

    @Test
    void testCreateWithNullTenantId() throws Exception {
        // Arrange
        when(tenantContext.getRole()).thenReturn("ADMINISTRADOR");
        when(tenantContext.getTenantId()).thenReturn(null);
        doNothing().when(persistenceHelper).persistWithManualTransaction(any(ProfesionalSalud.class), eq("public"));

        // Act
        ProfesionalSalud result = profesionalService.create(dto);

        // Assert
        assertNotNull(result);
        verify(persistenceHelper).persistWithManualTransaction(any(ProfesionalSalud.class), eq("public"));
    }

    @Test
    void testCreateWithBlankTenantId() throws Exception {
        // Arrange
        when(tenantContext.getRole()).thenReturn("ADMINISTRADOR");
        when(tenantContext.getTenantId()).thenReturn("");
        doNothing().when(persistenceHelper).persistWithManualTransaction(any(ProfesionalSalud.class), eq("public"));

        // Act
        ProfesionalSalud result = profesionalService.create(dto);

        // Assert
        assertNotNull(result);
        verify(persistenceHelper).persistWithManualTransaction(any(ProfesionalSalud.class), eq("public"));
    }

    @Test
    void testCreateWithInvalidTenantIdFormat() throws Exception {
        // Arrange
        when(tenantContext.getRole()).thenReturn("ADMINISTRADOR");
        when(tenantContext.getTenantId()).thenReturn("invalid");
        doNothing().when(persistenceHelper).persistWithManualTransaction(any(ProfesionalSalud.class), eq("schema_clinica_invalid"));

        // Act
        ProfesionalSalud result = profesionalService.create(dto);

        // Assert
        assertNotNull(result);
        verify(persistenceHelper).persistWithManualTransaction(any(ProfesionalSalud.class), eq("schema_clinica_invalid"));
    }

    @Test
    void testCreateWithPersistenceException() throws Exception {
        // Arrange
        when(tenantContext.getRole()).thenReturn("ADMINISTRADOR");
        when(tenantContext.getTenantId()).thenReturn("101");
        doThrow(new RuntimeException("DB error")).when(persistenceHelper).persistWithManualTransaction(any(ProfesionalSalud.class), anyString());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            profesionalService.create(dto);
        });
    }

    @Test
    void testUpdateWithDuplicateEmail() {
        // Arrange
        Long id = 1L;
        ProfesionalDTO updateDto = new ProfesionalDTO();
        updateDto.setEmail("duplicate@example.com");

        ProfesionalSalud existing = new ProfesionalSalud();
        existing.setId(2L);
        existing.setEmail("duplicate@example.com");

        when(profesionalRepository.findById(id)).thenReturn(Optional.of(profesional));
        when(profesionalRepository.findByNickname(anyString())).thenReturn(Optional.empty());
        when(profesionalRepository.findByEmail("duplicate@example.com")).thenReturn(Optional.of(existing));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            profesionalService.update(id, updateDto);
        });
    }

    @Test
    void testUpdateWithSameNickname() {
        // Arrange
        Long id = 1L;
        ProfesionalDTO updateDto = new ProfesionalDTO();
        updateDto.setNickname("jperez"); // Mismo nickname

        when(profesionalRepository.findById(id)).thenReturn(Optional.of(profesional));
        when(profesionalRepository.save(any(ProfesionalSalud.class))).thenReturn(profesional);

        // Act
        ProfesionalSalud result = profesionalService.update(id, updateDto);

        // Assert
        assertNotNull(result);
        // No debe verificar duplicados si es el mismo nickname
        verify(profesionalRepository, never()).findByNickname("jperez");
    }

    @Test
    void testUpdateWithSameEmail() {
        // Arrange
        Long id = 1L;
        ProfesionalDTO updateDto = new ProfesionalDTO();
        updateDto.setEmail("juan@example.com"); // Mismo email

        when(profesionalRepository.findById(id)).thenReturn(Optional.of(profesional));
        when(profesionalRepository.save(any(ProfesionalSalud.class))).thenReturn(profesional);

        // Act
        ProfesionalSalud result = profesionalService.update(id, updateDto);

        // Assert
        assertNotNull(result);
        // No debe verificar duplicados si es el mismo email
        verify(profesionalRepository, never()).findByEmail("juan@example.com");
    }

    @Test
    void testUpdateWithNullFields() {
        // Arrange
        Long id = 1L;
        ProfesionalDTO updateDto = new ProfesionalDTO();
        // Todos los campos son null

        when(profesionalRepository.findById(id)).thenReturn(Optional.of(profesional));
        when(profesionalRepository.save(any(ProfesionalSalud.class))).thenReturn(profesional);

        // Act
        ProfesionalSalud result = profesionalService.update(id, updateDto);

        // Assert
        assertNotNull(result);
        verify(profesionalRepository).save(any(ProfesionalSalud.class));
    }

    @Test
    void testFindAllInCurrentTenantEmpty() {
        // Arrange
        when(profesionalRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        List<ProfesionalSalud> result = profesionalService.findAllInCurrentTenant();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(profesionalRepository).findAll();
    }
}

