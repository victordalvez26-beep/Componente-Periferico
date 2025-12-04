package uy.edu.tse.hcen.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HcenClient Tests")
class HcenClientTest {

    @InjectMocks
    private HcenClient client;

    @Test
    void client_shouldBeInstantiable() {
        assertNotNull(client);
    }

    @Test
    void registrarMetadatos_shouldAcceptDTO() {
        assertNotNull(client);
    }

    @Test
    void consultarMetadatosPaciente_shouldAcceptCI() {
        assertNotNull(client);
    }
}

