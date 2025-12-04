package uy.edu.tse.hcen.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.tse.hcen.repository.DocumentoClinicoRepository;
import uy.edu.tse.hcen.repository.UsuarioSaludRepository;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentoService Tests")
class DocumentoServiceTest {

    @Mock
    private DocumentoClinicoRepository documentoRepository;

    @Mock
    private UsuarioSaludRepository usuarioRepository;

    @Mock
    private HcenClient hcenClient;

    @InjectMocks
    private DocumentoService service;

    @Test
    @DisplayName("Service debe ser instanciable")
    void service_shouldBeInstantiable() {
        assertNotNull(service);
    }
}
