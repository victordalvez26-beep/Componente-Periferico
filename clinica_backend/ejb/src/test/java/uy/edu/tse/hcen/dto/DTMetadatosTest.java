package uy.edu.tse.hcen.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DTMetadatosTest {

    private DTMetadatos metadatos;

    @BeforeEach
    void setUp() {
        metadatos = new DTMetadatos();
    }

    @Test
    void testConstructor() {
        assertNotNull(metadatos);
        assertNull(metadatos.getTenantId());
        assertNull(metadatos.getDocumentoId());
    }

    @Test
    void testTenantId() {
        String tenantId = "tenant-123";
        metadatos.setTenantId(tenantId);
        assertEquals(tenantId, metadatos.getTenantId());
    }

    @Test
    void testTenantIdNull() {
        metadatos.setTenantId(null);
        assertNull(metadatos.getTenantId());
    }

    @Test
    void testDocumentoId() {
        String documentoId = "doc-456";
        metadatos.setDocumentoId(documentoId);
        assertEquals(documentoId, metadatos.getDocumentoId());
    }

    @Test
    void testDocumentoIdPaciente() {
        String documentoIdPaciente = "paciente-789";
        metadatos.setDocumentoIdPaciente(documentoIdPaciente);
        assertEquals(documentoIdPaciente, metadatos.getDocumentoIdPaciente());
    }

    @Test
    void testEspecialidad() {
        String especialidad = "CARDIOLOGIA";
        metadatos.setEspecialidad(especialidad);
        assertEquals(especialidad, metadatos.getEspecialidad());
    }

    @Test
    void testFechaCreacion() {
        LocalDateTime fecha = LocalDateTime.now();
        metadatos.setFechaCreacion(fecha);
        assertEquals(fecha, metadatos.getFechaCreacion());
    }

    @Test
    void testFechaCreacionNull() {
        metadatos.setFechaCreacion(null);
        assertNull(metadatos.getFechaCreacion());
    }

    @Test
    void testUrlAcceso() {
        String url = "https://example.com/documento/123";
        metadatos.setUrlAcceso(url);
        assertEquals(url, metadatos.getUrlAcceso());
    }

    @Test
    void testAaPrestador() {
        String aaPrestador = "AA123456";
        metadatos.setAaPrestador(aaPrestador);
        assertEquals(aaPrestador, metadatos.getAaPrestador());
    }

    @Test
    void testEmisorDocumentoOID() {
        String oid = "1.2.840.113549.1.1.1";
        metadatos.setEmisorDocumentoOID(oid);
        assertEquals(oid, metadatos.getEmisorDocumentoOID());
    }

    @Test
    void testFechaRegistro() {
        LocalDateTime fecha = LocalDateTime.of(2024, 1, 15, 10, 30);
        metadatos.setFechaRegistro(fecha);
        assertEquals(fecha, metadatos.getFechaRegistro());
    }

    @Test
    void testFormato() {
        String formato = "application/pdf";
        metadatos.setFormato(formato);
        assertEquals(formato, metadatos.getFormato());
    }

    @Test
    void testFormatoXml() {
        String formato = "text/xml";
        metadatos.setFormato(formato);
        assertEquals(formato, metadatos.getFormato());
    }

    @Test
    void testAutor() {
        String autor = "Dr. Juan Pérez";
        metadatos.setAutor(autor);
        assertEquals(autor, metadatos.getAutor());
    }

    @Test
    void testTitulo() {
        String titulo = "Informe de Consulta";
        metadatos.setTitulo(titulo);
        assertEquals(titulo, metadatos.getTitulo());
    }

    @Test
    void testLanguageCode() {
        String languageCode = "es-UY";
        metadatos.setLanguageCode(languageCode);
        assertEquals(languageCode, metadatos.getLanguageCode());
    }

    @Test
    void testBreakingTheGlass() {
        metadatos.setBreakingTheGlass(true);
        assertTrue(metadatos.isBreakingTheGlass());
        
        metadatos.setBreakingTheGlass(false);
        assertFalse(metadatos.isBreakingTheGlass());
    }

    @Test
    void testHashDocumento() {
        String hash = "a1b2c3d4e5f6";
        metadatos.setHashDocumento(hash);
        assertEquals(hash, metadatos.getHashDocumento());
    }

    @Test
    void testDescripcion() {
        String descripcion = "Descripción del documento clínico";
        metadatos.setDescripcion(descripcion);
        assertEquals(descripcion, metadatos.getDescripcion());
    }

    @Test
    void testTipoDocumento() {
        String tipo = "EVALUACION";
        metadatos.setTipoDocumento(tipo);
        assertEquals(tipo, metadatos.getTipoDocumento());
    }

    @Test
    void testDatosPatronimicos() {
        String datos = "Juan Pérez García";
        metadatos.setDatosPatronimicos(datos);
        assertEquals(datos, metadatos.getDatosPatronimicos());
    }

    @Test
    void testAllFields() {
        LocalDateTime now = LocalDateTime.now();
        
        metadatos.setTenantId("tenant-123");
        metadatos.setDocumentoId("doc-456");
        metadatos.setDocumentoIdPaciente("paciente-789");
        metadatos.setEspecialidad("CARDIOLOGIA");
        metadatos.setFechaCreacion(now);
        metadatos.setUrlAcceso("https://example.com/doc");
        metadatos.setAaPrestador("AA123");
        metadatos.setEmisorDocumentoOID("1.2.3.4");
        metadatos.setFechaRegistro(now);
        metadatos.setFormato("application/pdf");
        metadatos.setAutor("Dr. Test");
        metadatos.setTitulo("Test Document");
        metadatos.setLanguageCode("es-UY");
        metadatos.setBreakingTheGlass(true);
        metadatos.setHashDocumento("hash123");
        metadatos.setDescripcion("Test description");
        metadatos.setTipoDocumento("INFORME");
        metadatos.setDatosPatronimicos("Test Patient");

        assertEquals("tenant-123", metadatos.getTenantId());
        assertEquals("doc-456", metadatos.getDocumentoId());
        assertEquals("paciente-789", metadatos.getDocumentoIdPaciente());
        assertEquals("CARDIOLOGIA", metadatos.getEspecialidad());
        assertEquals(now, metadatos.getFechaCreacion());
        assertEquals("https://example.com/doc", metadatos.getUrlAcceso());
        assertEquals("AA123", metadatos.getAaPrestador());
        assertEquals("1.2.3.4", metadatos.getEmisorDocumentoOID());
        assertEquals(now, metadatos.getFechaRegistro());
        assertEquals("application/pdf", metadatos.getFormato());
        assertEquals("Dr. Test", metadatos.getAutor());
        assertEquals("Test Document", metadatos.getTitulo());
        assertEquals("es-UY", metadatos.getLanguageCode());
        assertTrue(metadatos.isBreakingTheGlass());
        assertEquals("hash123", metadatos.getHashDocumento());
        assertEquals("Test description", metadatos.getDescripcion());
        assertEquals("INFORME", metadatos.getTipoDocumento());
        assertEquals("Test Patient", metadatos.getDatosPatronimicos());
    }

    @Test
    void testEmptyStrings() {
        metadatos.setTenantId("");
        metadatos.setDocumentoId("");
        metadatos.setAutor("");
        
        assertEquals("", metadatos.getTenantId());
        assertEquals("", metadatos.getDocumentoId());
        assertEquals("", metadatos.getAutor());
    }

    @Test
    void testLongStrings() {
        String longString = "a".repeat(1000);
        metadatos.setDescripcion(longString);
        assertEquals(longString, metadatos.getDescripcion());
    }
}

