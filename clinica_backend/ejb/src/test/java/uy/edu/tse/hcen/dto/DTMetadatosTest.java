package uy.edu.tse.hcen.dto;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class DTMetadatosTest {

    @Test
    void testConstructorAndGetters() {
        DTMetadatos metadata = new DTMetadatos();
        metadata.setDocumentoIdPaciente("12345678");
        metadata.setDatosPatronimicos("Juan Perez");
        metadata.setTipoDocumento("Historia Clínica");
        metadata.setEspecialidad("Cardiología");
        metadata.setUrlAcceso("http://hospital.com/doc/123");
        metadata.setTenantId("10");
        
        assertEquals("12345678", metadata.getDocumentoIdPaciente());
        assertEquals("Juan Perez", metadata.getDatosPatronimicos());
        assertEquals("Historia Clínica", metadata.getTipoDocumento());
        assertEquals("Cardiología", metadata.getEspecialidad());
        assertEquals("http://hospital.com/doc/123", metadata.getUrlAcceso());
        assertEquals("10", metadata.getTenantId());
    }
    
    @Test
    void testWithDates() {
        DTMetadatos metadata = new DTMetadatos();
        LocalDateTime now = LocalDateTime.now();
        metadata.setFechaCreacion(now);
        metadata.setFechaRegistro(now);
        
        assertEquals(now, metadata.getFechaCreacion());
        assertEquals(now, metadata.getFechaRegistro());
    }
    
    @Test
    void testDefaultConstructor() {
        DTMetadatos metadata = new DTMetadatos();
        assertNull(metadata.getDocumentoIdPaciente());
        assertNull(metadata.getTipoDocumento());
        assertNull(metadata.getUrlAcceso());
    }
    
    @Test
    void testWithAllFields() {
        DTMetadatos metadata = new DTMetadatos();
        metadata.setDocumentoId("DOC123");
        metadata.setAutor("Dr. Lopez");
        metadata.setTitulo("Consulta médica");
        metadata.setFormato("application/pdf");
        metadata.setBreakingTheGlass(true);
        metadata.setHashDocumento("abc123hash");
        metadata.setDescripcion("Consulta de control");
        
        assertEquals("DOC123", metadata.getDocumentoId());
        assertEquals("Dr. Lopez", metadata.getAutor());
        assertEquals("Consulta médica", metadata.getTitulo());
        assertEquals("application/pdf", metadata.getFormato());
        assertTrue(metadata.isBreakingTheGlass());
        assertEquals("abc123hash", metadata.getHashDocumento());
        assertEquals("Consulta de control", metadata.getDescripcion());
    }
}

