package uy.edu.tse.hcen.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uy.edu.tse.hcen.exceptions.HcenUnavailableException;
import uy.edu.tse.hcen.exceptions.MetadatosSyncException;
import uy.edu.tse.hcen.multitenancy.TenantContext;
import uy.edu.tse.hcen.repository.DocumentoClinicoRepository;

@ExtendWith(MockitoExtension.class)
class DocumentoServiceTest {

    private static final String DOCUMENTO_ID = "doc-001";
    private static final String DOCUMENTO_ID_PACIENTE = "pac-123";
    private static final String TENANT_ID = "tenant-1";
    private static final String CONTENIDO = "contenido";
    private static final String MONGO_ID = "mongo-123";

    @Mock
    private DocumentoClinicoRepository repo;

    @Mock
    private HcenClient hcenClient;

    @InjectMocks
    private DocumentoService documentoService;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void crearDocumentoCompleto_quandoTodoOk_retornaRespuestaEsperada() throws Exception {
        TenantContext.setCurrentTenant(TENANT_ID);
        when(repo.guardarContenido(eq(DOCUMENTO_ID), eq(CONTENIDO)))
                .thenReturn(new Document(Map.of("_id", MONGO_ID)));

        Map<String, Object> body = new HashMap<>();
        body.put("documentoId", DOCUMENTO_ID);
        body.put("documentoIdPaciente", DOCUMENTO_ID_PACIENTE);
        body.put("contenido", CONTENIDO);

        Map<String, Object> resultado = documentoService.crearDocumentoCompleto(body);

        assertEquals(DOCUMENTO_ID, resultado.get("documentoId"));
        assertEquals(DOCUMENTO_ID_PACIENTE, resultado.get("documentoIdPaciente"));
        assertEquals("created", resultado.get("status"));
        assertTrue(resultado.get("urlAcceso").toString().contains(MONGO_ID));
        assertEquals(MONGO_ID, resultado.get("mongoId"));

        verify(repo).guardarContenido(DOCUMENTO_ID, CONTENIDO);
        verify(hcenClient).registrarMetadatosCompleto(anyMap());
    }

    @Test
    void crearDocumentoCompleto_sinTenantEnContexto_usaTenantDelBody() throws Exception {
        when(repo.guardarContenido(any(), any()))
                .thenReturn(new Document(Map.of("_id", MONGO_ID)));

        Map<String, Object> body = new HashMap<>();
        body.put("documentoIdPaciente", DOCUMENTO_ID_PACIENTE);
        body.put("contenido", CONTENIDO);
        body.put("tenantId", TENANT_ID);

        Map<String, Object> resultado = documentoService.crearDocumentoCompleto(body);

        assertEquals(TENANT_ID, TenantContext.getCurrentTenant());
        assertEquals("created", resultado.get("status"));
        verify(hcenClient).registrarMetadatosCompleto(anyMap());
    }

    @Test
    void crearDocumentoCompleto_cuandoHcenNoDisponible_lanzaMetadatosSyncException() throws Exception {
        TenantContext.setCurrentTenant(TENANT_ID);
        when(repo.guardarContenido(any(), any()))
                .thenReturn(new Document(Map.of("_id", MONGO_ID)));
        doThrow(new HcenUnavailableException("HCEN down"))
                .when(hcenClient).registrarMetadatosCompleto(anyMap());

        Map<String, Object> body = new HashMap<>();
        body.put("documentoIdPaciente", DOCUMENTO_ID_PACIENTE);
        body.put("contenido", CONTENIDO);

        MetadatosSyncException ex = assertThrows(MetadatosSyncException.class,
                () -> documentoService.crearDocumentoCompleto(body));

        assertEquals("Documento guardado localmente pero sin sincronización en HCEN", ex.getMessage());
        verify(repo).guardarContenido(any(), any());
    }
}

