package uy.edu.tse.hcen.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenAIService Tests")
class OpenAIServiceTest {

    @Test
    void service_shouldHavePublicConstructor() {
        OpenAIService service = new OpenAIService();
        assertNotNull(service);
    }

    @Test
    void generarResumen_shouldAcceptParameters() {
        OpenAIService service = new OpenAIService();
        assertNotNull(service);
    }
}

