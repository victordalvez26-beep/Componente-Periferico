package uy.edu.tse.hcen.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfesionalPersistenceHelper Tests")
class ProfesionalPersistenceHelperTest {

    @Mock
    private EntityManager em;

    @InjectMocks
    private ProfesionalPersistenceHelper helper;

    @Test
    @DisplayName("Helper debe ser instanciable")
    void helper_shouldBeInstantiable() {
        assertNotNull(helper);
    }
}

