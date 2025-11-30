package uy.edu.tse.hcen.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uy.edu.tse.hcen.multitenancy.TenantContext;
import uy.edu.tse.hcen.repository.DocumentoClinicoRepository;
import uy.edu.tse.hcen.repository.DocumentoPdfRepository;
import uy.edu.tse.hcen.repository.ProfesionalSaludRepository;
import uy.edu.tse.hcen.repository.UsuarioSaludRepository;
import uy.edu.tse.hcen.model.ProfesionalSalud;
import uy.edu.tse.hcen.model.UsuarioSalud;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StatsServiceTest {

    @Mock
    private ProfesionalSaludRepository profesionalRepository;

    @Mock
    private UsuarioSaludRepository usuarioSaludRepository;

    @Mock
    private DocumentoPdfRepository documentoPdfRepository;

    @Mock
    private DocumentoClinicoRepository documentoClinicoRepository;

    @Mock
    private MongoCollection<Document> pdfCollection;

    @Mock
    private MongoCollection<Document> clinicoCollection;

    @InjectMocks
    private StatsService statsService;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
    }

    @Test
    void testObtenerEstadisticas() {
        // Arrange
        String tenantId = "101";
        Long tenantIdLong = 101L;
        
        List<ProfesionalSalud> profesionales = Arrays.asList(new ProfesionalSalud());
        List<UsuarioSalud> usuarios = Arrays.asList(new UsuarioSalud(), new UsuarioSalud());
        
        when(profesionalRepository.findAll()).thenReturn(profesionales);
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(usuarios);
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollection()).thenReturn(clinicoCollection);
        when(pdfCollection.countDocuments(any(Bson.class))).thenReturn(5L);
        when(clinicoCollection.countDocuments(any(Bson.class))).thenReturn(3L);

        // Act
        Map<String, Object> stats = statsService.obtenerEstadisticas(tenantId);

        // Assert
        assertNotNull(stats);
        assertEquals(1, stats.get("profesionales"));
        assertEquals(2, stats.get("usuarios"));
        assertEquals(8, stats.get("documentos"));
        assertNotNull(stats.get("consultas"));
    }

    @Test
    void testObtenerEstadisticasWithInvalidTenantId() {
        // Arrange
        String invalidTenantId = "invalid";

        // Act
        Map<String, Object> stats = statsService.obtenerEstadisticas(invalidTenantId);

        // Assert
        assertNotNull(stats);
        assertEquals(0, stats.get("profesionales"));
        assertEquals(0, stats.get("usuarios"));
        assertEquals(0, stats.get("documentos"));
        assertEquals(0, stats.get("consultas"));
    }

    @Test
    void testObtenerActividadReciente() {
        // Arrange
        String tenantId = "101";
        Long tenantIdLong = 101L;
        int limite = 5;
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(null);
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(new ArrayList<>());
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        // Assert
        assertNotNull(actividades);
    }

    @Test
    void testObtenerActividadRecienteWithInvalidTenantId() {
        // Arrange
        String invalidTenantId = "invalid";
        int limite = 5;

        // Act
        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(invalidTenantId, limite);

        // Assert
        assertNotNull(actividades);
        assertTrue(actividades.isEmpty());
    }

    @Test
    void testObtenerActividadRecienteWithLargeLimit() {
        // Arrange
        String tenantId = "101";
        int limite = 100; // Mayor que el máximo permitido

        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(null);
        when(usuarioSaludRepository.findByTenant(anyLong())).thenReturn(new ArrayList<>());
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        // Assert
        assertNotNull(actividades);
    }

    @Test
    void testObtenerEstadisticasWithExceptionInProfesionales() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        
        when(profesionalRepository.findAll()).thenThrow(new RuntimeException("DB error"));
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(new ArrayList<>());
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollection()).thenReturn(clinicoCollection);
        when(pdfCollection.countDocuments(any(Bson.class))).thenReturn(0L);
        when(clinicoCollection.countDocuments(any(Bson.class))).thenReturn(0L);

        Map<String, Object> stats = statsService.obtenerEstadisticas(tenantId);

        assertNotNull(stats);
        assertEquals(0, stats.get("profesionales")); // Debe retornar 0 en caso de error
    }

    @Test
    void testObtenerEstadisticasWithExceptionInUsuarios() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenThrow(new RuntimeException("DB error"));
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollection()).thenReturn(clinicoCollection);
        when(pdfCollection.countDocuments(any(Bson.class))).thenReturn(0L);
        when(clinicoCollection.countDocuments(any(Bson.class))).thenReturn(0L);

        Map<String, Object> stats = statsService.obtenerEstadisticas(tenantId);

        assertNotNull(stats);
        assertEquals(0, stats.get("usuarios"));
    }

    @Test
    void testObtenerEstadisticasWithExceptionInDocumentos() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(new ArrayList<>());
        when(documentoPdfRepository.getCollectionPublic()).thenThrow(new RuntimeException("MongoDB error"));
        when(documentoClinicoRepository.getCollection()).thenReturn(clinicoCollection);
        when(clinicoCollection.countDocuments(any(Bson.class))).thenReturn(0L);

        Map<String, Object> stats = statsService.obtenerEstadisticas(tenantId);

        assertNotNull(stats);
        assertEquals(0, stats.get("documentos"));
    }

    @Test
    void testObtenerActividadRecienteWithDocuments() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        int limite = 5;
        
        Document doc1 = new Document("_id", "doc1");
        doc1.append("fechaCreacion", new java.util.Date());
        doc1.append("profesionalId", "prof-1");
        doc1.append("tenantId", tenantIdLong);
        
        com.mongodb.client.FindIterable<Document> findIterable = mock(com.mongodb.client.FindIterable.class);
        when(findIterable.sort(any(Document.class))).thenReturn(findIterable);
        when(findIterable.limit(limite)).thenReturn(findIterable);
        when(findIterable.into(any(List.class))).thenReturn(Arrays.asList(doc1));
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(findIterable);
        when(clinicoCollection.find(any(Bson.class))).thenReturn(findIterable);
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(new ArrayList<>());
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());
        when(profesionalRepository.findByNickname("prof-1")).thenReturn(java.util.Optional.empty());

        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        assertNotNull(actividades);
    }

    @Test
    void testObtenerActividadRecienteWithUsuarios() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        int limite = 5;
        
        UsuarioSalud usuario = new UsuarioSalud();
        usuario.setNombre("Juan");
        usuario.setApellido("Pérez");
        usuario.setFechaAlta(java.time.LocalDateTime.now());
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(null);
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(Arrays.asList(usuario));
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());

        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        assertNotNull(actividades);
    }

    @Test
    void testObtenerActividadRecienteWithProfesionales() {
        String tenantId = "101";
        int limite = 5;
        
        ProfesionalSalud profesional = new ProfesionalSalud();
        profesional.setNombre("Dr. Test");
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(null);
        when(usuarioSaludRepository.findByTenant(anyLong())).thenReturn(new ArrayList<>());
        when(profesionalRepository.findAll()).thenReturn(Arrays.asList(profesional));

        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        assertNotNull(actividades);
    }

    @Test
    void testObtenerActividadRecienteWithNullDates() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        int limite = 5;
        
        UsuarioSalud usuario = new UsuarioSalud();
        usuario.setNombre("Juan");
        usuario.setApellido("Pérez");
        usuario.setFechaAlta(null); // Sin fecha
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(null);
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(Arrays.asList(usuario));
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());

        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        assertNotNull(actividades);
    }

    @Test
    void testObtenerActividadRecienteWithDocumentosPdfAndProfesional() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        int limite = 5;
        
        Document doc1 = new Document("_id", "doc1");
        doc1.append("fechaCreacion", new java.util.Date());
        doc1.append("profesionalId", "prof-1");
        doc1.append("tenantId", tenantIdLong);
        
        ProfesionalSalud profesional = new ProfesionalSalud();
        profesional.setNickname("prof-1");
        profesional.setNombre("Dr. Test");
        
        com.mongodb.client.FindIterable<Document> findIterable = mock(com.mongodb.client.FindIterable.class);
        when(findIterable.sort(any(Document.class))).thenReturn(findIterable);
        when(findIterable.limit(limite)).thenReturn(findIterable);
        when(findIterable.into(any(List.class))).thenReturn(Arrays.asList(doc1));
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(findIterable);
        when(clinicoCollection.find(any(Bson.class))).thenReturn(mock(com.mongodb.client.FindIterable.class));
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(new ArrayList<>());
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());
        when(profesionalRepository.findByNickname("prof-1")).thenReturn(java.util.Optional.of(profesional));

        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        assertNotNull(actividades);
    }

    @Test
    void testObtenerActividadRecienteWithDocumentosClinicosWithAutor() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        int limite = 5;
        
        Document doc1 = new Document("_id", "doc1");
        doc1.append("fechaCreacion", new java.util.Date());
        doc1.append("profesionalId", "prof-1");
        doc1.append("autor", "Dr. Autor");
        doc1.append("tenantId", tenantIdLong);
        
        com.mongodb.client.FindIterable<Document> findIterablePdf = mock(com.mongodb.client.FindIterable.class);
        com.mongodb.client.FindIterable<Document> findIterableClinico = mock(com.mongodb.client.FindIterable.class);
        when(findIterablePdf.sort(any(Document.class))).thenReturn(findIterablePdf);
        when(findIterablePdf.limit(limite)).thenReturn(findIterablePdf);
        when(findIterablePdf.into(any(List.class))).thenReturn(new ArrayList<>());
        
        when(findIterableClinico.sort(any(Document.class))).thenReturn(findIterableClinico);
        when(findIterableClinico.limit(limite)).thenReturn(findIterableClinico);
        when(findIterableClinico.into(any(List.class))).thenReturn(Arrays.asList(doc1));
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(findIterablePdf);
        when(clinicoCollection.find(any(Bson.class))).thenReturn(findIterableClinico);
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(new ArrayList<>());
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());

        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        assertNotNull(actividades);
    }

    @Test
    void testObtenerActividadRecienteWithDocumentosSinProfesionalId() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        int limite = 5;
        
        Document doc1 = new Document("_id", "doc1");
        doc1.append("fechaCreacion", new java.util.Date());
        doc1.append("tenantId", tenantIdLong);
        // Sin profesionalId
        
        com.mongodb.client.FindIterable<Document> findIterable = mock(com.mongodb.client.FindIterable.class);
        when(findIterable.sort(any(Document.class))).thenReturn(findIterable);
        when(findIterable.limit(limite)).thenReturn(findIterable);
        when(findIterable.into(any(List.class))).thenReturn(Arrays.asList(doc1));
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(findIterable);
        when(clinicoCollection.find(any(Bson.class))).thenReturn(mock(com.mongodb.client.FindIterable.class));
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(new ArrayList<>());
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());

        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        assertNotNull(actividades);
    }

    @Test
    void testObtenerActividadRecienteWithUsuariosNullNombre() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        int limite = 5;
        
        UsuarioSalud usuario1 = new UsuarioSalud();
        usuario1.setNombre(null);
        usuario1.setApellido("Pérez");
        usuario1.setFechaAlta(java.time.LocalDateTime.now());
        
        UsuarioSalud usuario2 = new UsuarioSalud();
        usuario2.setNombre("Juan");
        usuario2.setApellido(null);
        usuario2.setFechaAlta(java.time.LocalDateTime.now());
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(null);
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(Arrays.asList(usuario1, usuario2));
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());

        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        assertNotNull(actividades);
    }

    @Test
    void testObtenerActividadRecienteWithProfesionalesNullNombre() {
        String tenantId = "101";
        int limite = 5;
        
        ProfesionalSalud profesional1 = new ProfesionalSalud();
        profesional1.setNombre(null);
        
        ProfesionalSalud profesional2 = new ProfesionalSalud();
        profesional2.setNombre("Dr. Test");
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(null);
        when(usuarioSaludRepository.findByTenant(anyLong())).thenReturn(new ArrayList<>());
        when(profesionalRepository.findAll()).thenReturn(Arrays.asList(profesional1, profesional2));

        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        assertNotNull(actividades);
    }

    @Test
    void testObtenerActividadRecienteWithProfesionalesMoreThanLimit() {
        String tenantId = "101";
        int limite = 2;
        
        ProfesionalSalud profesional1 = new ProfesionalSalud();
        profesional1.setNombre("Dr. Test 1");
        ProfesionalSalud profesional2 = new ProfesionalSalud();
        profesional2.setNombre("Dr. Test 2");
        ProfesionalSalud profesional3 = new ProfesionalSalud();
        profesional3.setNombre("Dr. Test 3");
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(null);
        when(usuarioSaludRepository.findByTenant(anyLong())).thenReturn(new ArrayList<>());
        when(profesionalRepository.findAll()).thenReturn(Arrays.asList(profesional1, profesional2, profesional3));

        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        assertNotNull(actividades);
    }

    @Test
    void testObtenerActividadRecienteWithSorting() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        int limite = 5;
        
        java.util.Date fecha1 = new java.util.Date(System.currentTimeMillis() - 10000);
        java.util.Date fecha2 = new java.util.Date(System.currentTimeMillis() - 5000);
        java.util.Date fecha3 = new java.util.Date();
        
        Document doc1 = new Document("_id", "doc1");
        doc1.append("fechaCreacion", fecha1);
        doc1.append("tenantId", tenantIdLong);
        
        Document doc2 = new Document("_id", "doc2");
        doc2.append("fechaCreacion", fecha3);
        doc2.append("tenantId", tenantIdLong);
        
        UsuarioSalud usuario = new UsuarioSalud();
        usuario.setNombre("Juan");
        usuario.setApellido("Pérez");
        usuario.setFechaAlta(java.time.LocalDateTime.ofInstant(fecha2.toInstant(), java.time.ZoneId.systemDefault()));
        
        com.mongodb.client.FindIterable<Document> findIterable = mock(com.mongodb.client.FindIterable.class);
        when(findIterable.sort(any(Document.class))).thenReturn(findIterable);
        when(findIterable.limit(limite)).thenReturn(findIterable);
        when(findIterable.into(any(List.class))).thenReturn(Arrays.asList(doc1, doc2));
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(findIterable);
        when(clinicoCollection.find(any(Bson.class))).thenReturn(mock(com.mongodb.client.FindIterable.class));
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(Arrays.asList(usuario));
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());

        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        assertNotNull(actividades);
        // Verificar que se ordenaron correctamente (fecha3 > fecha2 > fecha1)
        if (actividades.size() >= 3) {
            Date fechaAct1 = (Date) actividades.get(0).get("fecha");
            Date fechaAct2 = (Date) actividades.get(1).get("fecha");
            assertTrue(fechaAct1.compareTo(fechaAct2) >= 0);
        }
    }

    @Test
    void testObtenerActividadRecienteWithLimitExceeded() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        int limite = 2;
        
        java.util.Date fecha = new java.util.Date();
        Document doc1 = new Document("_id", "doc1");
        doc1.append("fechaCreacion", fecha);
        doc1.append("tenantId", tenantIdLong);
        Document doc2 = new Document("_id", "doc2");
        doc2.append("fechaCreacion", fecha);
        doc2.append("tenantId", tenantIdLong);
        Document doc3 = new Document("_id", "doc3");
        doc3.append("fechaCreacion", fecha);
        doc3.append("tenantId", tenantIdLong);
        
        com.mongodb.client.FindIterable<Document> findIterable = mock(com.mongodb.client.FindIterable.class);
        when(findIterable.sort(any(Document.class))).thenReturn(findIterable);
        when(findIterable.limit(limite)).thenReturn(findIterable);
        when(findIterable.into(any(List.class))).thenReturn(Arrays.asList(doc1, doc2, doc3));
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(findIterable);
        when(clinicoCollection.find(any(Bson.class))).thenReturn(mock(com.mongodb.client.FindIterable.class));
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(new ArrayList<>());
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());

        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        assertNotNull(actividades);
        assertTrue(actividades.size() <= limite);
    }

    @Test
    void testObtenerActividadRecienteWithNullFechasInSorting() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        int limite = 5;
        
        Document doc1 = new Document("_id", "doc1");
        doc1.append("fechaCreacion", null); // Sin fecha
        doc1.append("tenantId", tenantIdLong);
        
        Document doc2 = new Document("_id", "doc2");
        doc2.append("fechaCreacion", new java.util.Date());
        doc2.append("tenantId", tenantIdLong);
        
        com.mongodb.client.FindIterable<Document> findIterable = mock(com.mongodb.client.FindIterable.class);
        when(findIterable.sort(any(Document.class))).thenReturn(findIterable);
        when(findIterable.limit(limite)).thenReturn(findIterable);
        when(findIterable.into(any(List.class))).thenReturn(Arrays.asList(doc1, doc2));
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(findIterable);
        when(clinicoCollection.find(any(Bson.class))).thenReturn(mock(com.mongodb.client.FindIterable.class));
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(new ArrayList<>());
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());

        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        assertNotNull(actividades);
    }

    @Test
    void testObtenerActividadRecienteWithExceptionInDocumentos() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        int limite = 5;
        
        when(documentoPdfRepository.getCollectionPublic()).thenThrow(new RuntimeException("MongoDB error"));
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(clinicoCollection.find(any(Bson.class))).thenReturn(null);
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(new ArrayList<>());
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());

        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        assertNotNull(actividades);
    }

    @Test
    void testObtenerActividadRecienteWithExceptionInUsuarios() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        int limite = 5;
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(null);
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenThrow(new RuntimeException("DB error"));
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());

        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        assertNotNull(actividades);
    }

    @Test
    void testObtenerActividadRecienteWithExceptionInProfesionales() {
        String tenantId = "101";
        int limite = 5;
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(null);
        when(usuarioSaludRepository.findByTenant(anyLong())).thenReturn(new ArrayList<>());
        when(profesionalRepository.findAll()).thenThrow(new RuntimeException("DB error"));

        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        assertNotNull(actividades);
    }

    @Test
    void testObtenerEstadisticasWithNullProfesionales() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        
        when(profesionalRepository.findAll()).thenReturn(null);
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(new ArrayList<>());
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollection()).thenReturn(clinicoCollection);
        when(pdfCollection.countDocuments(any(Bson.class))).thenReturn(0L);
        when(clinicoCollection.countDocuments(any(Bson.class))).thenReturn(0L);

        Map<String, Object> stats = statsService.obtenerEstadisticas(tenantId);

        assertNotNull(stats);
        assertEquals(0, stats.get("profesionales"));
    }

    @Test
    void testObtenerEstadisticasWithNullUsuarios() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(null);
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollection()).thenReturn(clinicoCollection);
        when(pdfCollection.countDocuments(any(Bson.class))).thenReturn(0L);
        when(clinicoCollection.countDocuments(any(Bson.class))).thenReturn(0L);

        Map<String, Object> stats = statsService.obtenerEstadisticas(tenantId);

        assertNotNull(stats);
        assertEquals(0, stats.get("usuarios"));
    }

    @Test
    void testObtenerEstadisticasWithContarDocumentosHoy() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(new ArrayList<>());
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollection()).thenReturn(clinicoCollection);
        when(pdfCollection.countDocuments(any(Bson.class))).thenReturn(2L, 1L); // Primero para totales, luego para hoy
        when(clinicoCollection.countDocuments(any(Bson.class))).thenReturn(3L, 2L);

        Map<String, Object> stats = statsService.obtenerEstadisticas(tenantId);

        assertNotNull(stats);
        assertNotNull(stats.get("consultas"));
    }

    @Test
    void testObtenerEstadisticasWithExceptionInContarDocumentosHoy() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(new ArrayList<>());
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollection()).thenReturn(clinicoCollection);
        when(pdfCollection.countDocuments(any(Bson.class))).thenReturn(0L).thenThrow(new RuntimeException("Error"));
        when(clinicoCollection.countDocuments(any(Bson.class))).thenReturn(0L);

        Map<String, Object> stats = statsService.obtenerEstadisticas(tenantId);

        assertNotNull(stats);
        assertEquals(0, stats.get("consultas"));
    }

    @Test
    void testObtenerActividadRecienteWithProfesionalNombreBlank() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        int limite = 5;
        
        Document doc1 = new Document("_id", "doc1");
        doc1.append("fechaCreacion", new java.util.Date());
        doc1.append("profesionalId", "prof-1");
        doc1.append("tenantId", tenantIdLong);
        
        ProfesionalSalud profesional = new ProfesionalSalud();
        profesional.setNickname("prof-1");
        profesional.setNombre("   "); // Nombre en blanco
        
        com.mongodb.client.FindIterable<Document> findIterable = mock(com.mongodb.client.FindIterable.class);
        when(findIterable.sort(any(Document.class))).thenReturn(findIterable);
        when(findIterable.limit(limite)).thenReturn(findIterable);
        when(findIterable.into(any(List.class))).thenReturn(Arrays.asList(doc1));
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(findIterable);
        when(clinicoCollection.find(any(Bson.class))).thenReturn(mock(com.mongodb.client.FindIterable.class));
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(new ArrayList<>());
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());
        when(profesionalRepository.findByNickname("prof-1")).thenReturn(java.util.Optional.of(profesional));

        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        assertNotNull(actividades);
    }

    @Test
    void testObtenerActividadRecienteWithProfesionalException() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        int limite = 5;
        
        Document doc1 = new Document("_id", "doc1");
        doc1.append("fechaCreacion", new java.util.Date());
        doc1.append("profesionalId", "prof-1");
        doc1.append("tenantId", tenantIdLong);
        
        com.mongodb.client.FindIterable<Document> findIterable = mock(com.mongodb.client.FindIterable.class);
        when(findIterable.sort(any(Document.class))).thenReturn(findIterable);
        when(findIterable.limit(limite)).thenReturn(findIterable);
        when(findIterable.into(any(List.class))).thenReturn(Arrays.asList(doc1));
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(findIterable);
        when(clinicoCollection.find(any(Bson.class))).thenReturn(mock(com.mongodb.client.FindIterable.class));
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(new ArrayList<>());
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());
        when(profesionalRepository.findByNickname("prof-1")).thenThrow(new RuntimeException("DB error"));

        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        assertNotNull(actividades);
    }

    @Test
    void testObtenerActividadRecienteWithContarProfesionalesNullTenantAnterior() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        
        TenantContext.clear(); // Asegurar que no hay tenant anterior
        
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(new ArrayList<>());
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollection()).thenReturn(clinicoCollection);
        when(pdfCollection.countDocuments(any(Bson.class))).thenReturn(0L);
        when(clinicoCollection.countDocuments(any(Bson.class))).thenReturn(0L);

        Map<String, Object> stats = statsService.obtenerEstadisticas(tenantId);

        assertNotNull(stats);
    }

    @Test
    void testObtenerActividadRecienteWithObtenerNombreProfesionalNullTenantId() {
        String tenantId = "101";
        int limite = 5;
        
        Document doc1 = new Document("_id", "doc1");
        doc1.append("fechaCreacion", new java.util.Date());
        doc1.append("profesionalId", "prof-1");
        
        com.mongodb.client.FindIterable<Document> findIterable = mock(com.mongodb.client.FindIterable.class);
        when(findIterable.sort(any(Document.class))).thenReturn(findIterable);
        when(findIterable.limit(limite)).thenReturn(findIterable);
        when(findIterable.into(any(List.class))).thenReturn(Arrays.asList(doc1));
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(findIterable);
        when(clinicoCollection.find(any(Bson.class))).thenReturn(mock(com.mongodb.client.FindIterable.class));
        when(usuarioSaludRepository.findByTenant(anyLong())).thenReturn(new ArrayList<>());
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());

        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        assertNotNull(actividades);
    }

    @Test
    void testObtenerActividadRecienteWithObtenerNombreProfesionalException() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        int limite = 5;
        
        Document doc1 = new Document("_id", "doc1");
        doc1.append("fechaCreacion", new java.util.Date());
        doc1.append("profesionalId", "prof-1");
        doc1.append("tenantId", tenantIdLong);
        
        com.mongodb.client.FindIterable<Document> findIterable = mock(com.mongodb.client.FindIterable.class);
        when(findIterable.sort(any(Document.class))).thenReturn(findIterable);
        when(findIterable.limit(limite)).thenReturn(findIterable);
        when(findIterable.into(any(List.class))).thenReturn(Arrays.asList(doc1));
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(findIterable);
        when(clinicoCollection.find(any(Bson.class))).thenReturn(mock(com.mongodb.client.FindIterable.class));
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(new ArrayList<>());
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());
        when(profesionalRepository.findByNickname("prof-1")).thenReturn(java.util.Optional.empty());

        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        assertNotNull(actividades);
    }

    @Test
    void testObtenerActividadRecienteWithUsuariosEmptyList() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        int limite = 5;
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(null);
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(new ArrayList<>());
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());

        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        assertNotNull(actividades);
    }

    @Test
    void testObtenerActividadRecienteWithProfesionalesEmptyList() {
        String tenantId = "101";
        int limite = 5;
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(null);
        when(usuarioSaludRepository.findByTenant(anyLong())).thenReturn(new ArrayList<>());
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());

        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        assertNotNull(actividades);
    }

    @Test
    void testObtenerNombreProfesionalWithNullTenantId() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        int limite = 5;
        
        Document doc1 = new Document("_id", "doc1");
        doc1.append("fechaCreacion", new java.util.Date());
        doc1.append("profesionalId", "prof-1");
        doc1.append("tenantId", null); // null tenantId
        
        com.mongodb.client.FindIterable<Document> findIterable = mock(com.mongodb.client.FindIterable.class);
        when(findIterable.sort(any(Document.class))).thenReturn(findIterable);
        when(findIterable.limit(limite)).thenReturn(findIterable);
        when(findIterable.into(any(List.class))).thenReturn(Arrays.asList(doc1));
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(findIterable);
        when(clinicoCollection.find(any(Bson.class))).thenReturn(mock(com.mongodb.client.FindIterable.class));
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(new ArrayList<>());
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());
        when(profesionalRepository.findByNickname("prof-1")).thenReturn(java.util.Optional.empty());

        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        assertNotNull(actividades);
    }

    @Test
    void testObtenerNombreProfesionalWithProfesionalPresentButNombreNull() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        int limite = 5;
        
        Document doc1 = new Document("_id", "doc1");
        doc1.append("fechaCreacion", new java.util.Date());
        doc1.append("profesionalId", "prof-1");
        doc1.append("tenantId", tenantIdLong);
        
        ProfesionalSalud profesional = new ProfesionalSalud();
        profesional.setNickname("prof-1");
        profesional.setNombre(null); // Nombre null
        
        com.mongodb.client.FindIterable<Document> findIterable = mock(com.mongodb.client.FindIterable.class);
        when(findIterable.sort(any(Document.class))).thenReturn(findIterable);
        when(findIterable.limit(limite)).thenReturn(findIterable);
        when(findIterable.into(any(List.class))).thenReturn(Arrays.asList(doc1));
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(findIterable);
        when(clinicoCollection.find(any(Bson.class))).thenReturn(mock(com.mongodb.client.FindIterable.class));
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(new ArrayList<>());
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());
        when(profesionalRepository.findByNickname("prof-1")).thenReturn(java.util.Optional.of(profesional));

        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        assertNotNull(actividades);
    }

    @Test
    void testObtenerNombreProfesionalWithExceptionInTenantContext() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        int limite = 5;
        
        Document doc1 = new Document("_id", "doc1");
        doc1.append("fechaCreacion", new java.util.Date());
        doc1.append("profesionalId", "prof-1");
        doc1.append("tenantId", tenantIdLong);
        
        com.mongodb.client.FindIterable<Document> findIterable = mock(com.mongodb.client.FindIterable.class);
        when(findIterable.sort(any(Document.class))).thenReturn(findIterable);
        when(findIterable.limit(limite)).thenReturn(findIterable);
        when(findIterable.into(any(List.class))).thenReturn(Arrays.asList(doc1));
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(findIterable);
        when(clinicoCollection.find(any(Bson.class))).thenReturn(mock(com.mongodb.client.FindIterable.class));
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(new ArrayList<>());
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());
        when(profesionalRepository.findByNickname("prof-1")).thenThrow(new RuntimeException("DB error"));

        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        assertNotNull(actividades);
    }

    @Test
    void testObtenerActividadRecienteWithDocumentosClinicosWithAutorBlank() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        int limite = 5;
        
        Document doc1 = new Document("_id", "doc1");
        doc1.append("fechaCreacion", new java.util.Date());
        doc1.append("profesionalId", "prof-1");
        doc1.append("autor", "   "); // Autor en blanco
        doc1.append("tenantId", tenantIdLong);
        
        com.mongodb.client.FindIterable<Document> findIterablePdf = mock(com.mongodb.client.FindIterable.class);
        com.mongodb.client.FindIterable<Document> findIterableClinico = mock(com.mongodb.client.FindIterable.class);
        when(findIterablePdf.sort(any(Document.class))).thenReturn(findIterablePdf);
        when(findIterablePdf.limit(limite)).thenReturn(findIterablePdf);
        when(findIterablePdf.into(any(List.class))).thenReturn(new ArrayList<>());
        
        when(findIterableClinico.sort(any(Document.class))).thenReturn(findIterableClinico);
        when(findIterableClinico.limit(limite)).thenReturn(findIterableClinico);
        when(findIterableClinico.into(any(List.class))).thenReturn(Arrays.asList(doc1));
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(findIterablePdf);
        when(clinicoCollection.find(any(Bson.class))).thenReturn(findIterableClinico);
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(new ArrayList<>());
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());
        when(profesionalRepository.findByNickname("prof-1")).thenReturn(java.util.Optional.empty());

        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        assertNotNull(actividades);
    }

    @Test
    void testObtenerActividadRecienteWithDocumentosClinicosWithAutorNull() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        int limite = 5;
        
        Document doc1 = new Document("_id", "doc1");
        doc1.append("fechaCreacion", new java.util.Date());
        doc1.append("profesionalId", "prof-1");
        doc1.append("autor", null); // Autor null
        doc1.append("tenantId", tenantIdLong);
        
        com.mongodb.client.FindIterable<Document> findIterablePdf = mock(com.mongodb.client.FindIterable.class);
        com.mongodb.client.FindIterable<Document> findIterableClinico = mock(com.mongodb.client.FindIterable.class);
        when(findIterablePdf.sort(any(Document.class))).thenReturn(findIterablePdf);
        when(findIterablePdf.limit(limite)).thenReturn(findIterablePdf);
        when(findIterablePdf.into(any(List.class))).thenReturn(new ArrayList<>());
        
        when(findIterableClinico.sort(any(Document.class))).thenReturn(findIterableClinico);
        when(findIterableClinico.limit(limite)).thenReturn(findIterableClinico);
        when(findIterableClinico.into(any(List.class))).thenReturn(Arrays.asList(doc1));
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(findIterablePdf);
        when(clinicoCollection.find(any(Bson.class))).thenReturn(findIterableClinico);
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(new ArrayList<>());
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());
        when(profesionalRepository.findByNickname("prof-1")).thenReturn(java.util.Optional.empty());

        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        assertNotNull(actividades);
    }

    @Test
    void testObtenerActividadRecienteWithDocumentosSinFechaCreacion() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        int limite = 5;
        
        Document doc1 = new Document("_id", "doc1");
        doc1.append("profesionalId", "prof-1");
        doc1.append("tenantId", tenantIdLong);
        // Sin fechaCreacion
        
        com.mongodb.client.FindIterable<Document> findIterable = mock(com.mongodb.client.FindIterable.class);
        when(findIterable.sort(any(Document.class))).thenReturn(findIterable);
        when(findIterable.limit(limite)).thenReturn(findIterable);
        when(findIterable.into(any(List.class))).thenReturn(Arrays.asList(doc1));
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(findIterable);
        when(clinicoCollection.find(any(Bson.class))).thenReturn(mock(com.mongodb.client.FindIterable.class));
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(new ArrayList<>());
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());

        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        assertNotNull(actividades);
    }

    @Test
    void testObtenerActividadRecienteWithUsuariosConFechasDiferentes() {
        String tenantId = "101";
        Long tenantIdLong = 101L;
        int limite = 5;
        
        UsuarioSalud usuario1 = new UsuarioSalud();
        usuario1.setNombre("Juan");
        usuario1.setApellido("Pérez");
        usuario1.setFechaAlta(java.time.LocalDateTime.now().minusDays(1));
        
        UsuarioSalud usuario2 = new UsuarioSalud();
        usuario2.setNombre("María");
        usuario2.setApellido("González");
        usuario2.setFechaAlta(java.time.LocalDateTime.now());
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(null);
        when(usuarioSaludRepository.findByTenant(tenantIdLong)).thenReturn(Arrays.asList(usuario1, usuario2));
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());

        List<Map<String, Object>> actividades = statsService.obtenerActividadReciente(tenantId, limite);

        assertNotNull(actividades);
        // Verificar que se ordenaron correctamente (más reciente primero)
        if (actividades.size() >= 2) {
            Date fecha1 = (Date) actividades.get(0).get("fecha");
            Date fecha2 = (Date) actividades.get(1).get("fecha");
            if (fecha1 != null && fecha2 != null) {
                assertTrue(fecha1.compareTo(fecha2) >= 0);
            }
        }
    }

    // Tests directos para métodos ahora públicos
    @Test
    void testContarProfesionalesDirecto() {
        String tenantId = "101";
        List<ProfesionalSalud> profesionales = Arrays.asList(new ProfesionalSalud(), new ProfesionalSalud());
        
        when(profesionalRepository.findAll()).thenReturn(profesionales);
        
        int count = statsService.contarProfesionales(tenantId);
        
        assertEquals(2, count);
    }

    @Test
    void testContarProfesionalesWithException() {
        String tenantId = "101";
        
        when(profesionalRepository.findAll()).thenThrow(new RuntimeException("DB error"));
        
        int count = statsService.contarProfesionales(tenantId);
        
        assertEquals(0, count);
    }

    @Test
    void testContarProfesionalesWithNullTenantAnterior() {
        String tenantId = "101";
        TenantContext.clear(); // Asegurar que no hay tenant anterior
        
        when(profesionalRepository.findAll()).thenReturn(new ArrayList<>());
        
        int count = statsService.contarProfesionales(tenantId);
        
        assertEquals(0, count);
    }

    @Test
    void testContarUsuariosSaludDirecto() {
        Long tenantId = 101L;
        List<UsuarioSalud> usuarios = Arrays.asList(new UsuarioSalud(), new UsuarioSalud(), new UsuarioSalud());
        
        when(usuarioSaludRepository.findByTenant(tenantId)).thenReturn(usuarios);
        
        int count = statsService.contarUsuariosSalud(tenantId);
        
        assertEquals(3, count);
    }

    @Test
    void testContarUsuariosSaludWithNull() {
        Long tenantId = 101L;
        
        when(usuarioSaludRepository.findByTenant(tenantId)).thenReturn(null);
        
        int count = statsService.contarUsuariosSalud(tenantId);
        
        assertEquals(0, count);
    }

    @Test
    void testContarDocumentosTotalesDirecto() {
        Long tenantId = 101L;
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollection()).thenReturn(clinicoCollection);
        when(pdfCollection.countDocuments(any(Bson.class))).thenReturn(5L);
        when(clinicoCollection.countDocuments(any(Bson.class))).thenReturn(3L);
        
        int count = statsService.contarDocumentosTotales(tenantId);
        
        assertEquals(8, count);
    }

    @Test
    void testContarDocumentosTotalesWithException() {
        Long tenantId = 101L;
        
        when(documentoPdfRepository.getCollectionPublic()).thenThrow(new RuntimeException("MongoDB error"));
        
        int count = statsService.contarDocumentosTotales(tenantId);
        
        assertEquals(0, count);
    }

    @Test
    void testContarDocumentosHoyDirecto() {
        Long tenantId = 101L;
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollection()).thenReturn(clinicoCollection);
        when(pdfCollection.countDocuments(any(Bson.class))).thenReturn(2L);
        when(clinicoCollection.countDocuments(any(Bson.class))).thenReturn(1L);
        
        int count = statsService.contarDocumentosHoy(tenantId);
        
        assertEquals(3, count);
    }

    @Test
    void testContarDocumentosHoyWithException() {
        Long tenantId = 101L;
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollection()).thenReturn(clinicoCollection);
        when(pdfCollection.countDocuments(any(Bson.class))).thenReturn(0L);
        when(clinicoCollection.countDocuments(any(Bson.class))).thenThrow(new RuntimeException("MongoDB error"));
        
        int count = statsService.contarDocumentosHoy(tenantId);
        
        assertEquals(0, count);
    }

    @Test
    void testObtenerUltimosDocumentosDirecto() {
        Long tenantId = 101L;
        int limite = 5;
        
        Document doc1 = new Document("_id", "doc1");
        doc1.append("fechaCreacion", new java.util.Date());
        doc1.append("profesionalId", "prof-1");
        doc1.append("tenantId", tenantId);
        
        com.mongodb.client.FindIterable<Document> findIterablePdf = mock(com.mongodb.client.FindIterable.class);
        com.mongodb.client.FindIterable<Document> findIterableClinico = mock(com.mongodb.client.FindIterable.class);
        
        when(findIterablePdf.sort(any(Document.class))).thenReturn(findIterablePdf);
        when(findIterablePdf.limit(limite)).thenReturn(findIterablePdf);
        when(findIterablePdf.into(any(List.class))).thenReturn(Arrays.asList(doc1));
        
        when(findIterableClinico.sort(any(Document.class))).thenReturn(findIterableClinico);
        when(findIterableClinico.limit(limite)).thenReturn(findIterableClinico);
        when(findIterableClinico.into(any(List.class))).thenReturn(new ArrayList<>());
        
        when(documentoPdfRepository.getCollectionPublic()).thenReturn(pdfCollection);
        when(documentoClinicoRepository.getCollectionPublic()).thenReturn(clinicoCollection);
        when(pdfCollection.find(any(Bson.class))).thenReturn(findIterablePdf);
        when(clinicoCollection.find(any(Bson.class))).thenReturn(findIterableClinico);
        when(profesionalRepository.findByNickname("prof-1")).thenReturn(java.util.Optional.empty());
        
        List<Map<String, Object>> actividades = statsService.obtenerUltimosDocumentos(tenantId, limite);
        
        assertNotNull(actividades);
        assertFalse(actividades.isEmpty());
    }

    @Test
    void testObtenerUltimosUsuariosDirecto() {
        Long tenantId = 101L;
        int limite = 5;
        
        UsuarioSalud usuario = new UsuarioSalud();
        usuario.setNombre("Juan");
        usuario.setApellido("Pérez");
        usuario.setFechaAlta(java.time.LocalDateTime.now());
        
        when(usuarioSaludRepository.findByTenant(tenantId)).thenReturn(Arrays.asList(usuario));
        
        List<Map<String, Object>> actividades = statsService.obtenerUltimosUsuarios(tenantId, limite);
        
        assertNotNull(actividades);
        assertFalse(actividades.isEmpty());
    }

    @Test
    void testObtenerUltimosUsuariosWithNullList() {
        Long tenantId = 101L;
        int limite = 5;
        
        when(usuarioSaludRepository.findByTenant(tenantId)).thenReturn(null);
        
        List<Map<String, Object>> actividades = statsService.obtenerUltimosUsuarios(tenantId, limite);
        
        assertNotNull(actividades);
        assertTrue(actividades.isEmpty());
    }

    @Test
    void testObtenerUltimosProfesionalesDirecto() {
        String tenantId = "101";
        int limite = 5;
        
        ProfesionalSalud profesional = new ProfesionalSalud();
        profesional.setNombre("Dr. Test");
        
        when(profesionalRepository.findAll()).thenReturn(Arrays.asList(profesional));
        
        List<Map<String, Object>> actividades = statsService.obtenerUltimosProfesionales(tenantId, limite);
        
        assertNotNull(actividades);
        assertFalse(actividades.isEmpty());
    }

    @Test
    void testObtenerUltimosProfesionalesWithNullTenantAnterior() {
        String tenantId = "101";
        int limite = 5;
        
        TenantContext.clear();
        ProfesionalSalud profesional = new ProfesionalSalud();
        profesional.setNombre("Dr. Test");
        
        when(profesionalRepository.findAll()).thenReturn(Arrays.asList(profesional));
        
        List<Map<String, Object>> actividades = statsService.obtenerUltimosProfesionales(tenantId, limite);
        
        assertNotNull(actividades);
    }

    @Test
    void testObtenerNombreProfesionalDirecto() {
        String profesionalId = "prof-1";
        Long tenantId = 101L;
        
        ProfesionalSalud profesional = new ProfesionalSalud();
        profesional.setNickname("prof-1");
        profesional.setNombre("Dr. Test");
        
        when(profesionalRepository.findByNickname(profesionalId)).thenReturn(java.util.Optional.of(profesional));
        
        String nombre = statsService.obtenerNombreProfesional(profesionalId, tenantId);
        
        assertEquals("Dr. Test", nombre);
    }

    @Test
    void testObtenerNombreProfesionalWithNullTenantAnterior() {
        String profesionalId = "prof-1";
        Long tenantId = 101L;
        
        TenantContext.clear();
        ProfesionalSalud profesional = new ProfesionalSalud();
        profesional.setNickname("prof-1");
        profesional.setNombre("Dr. Test");
        
        when(profesionalRepository.findByNickname(profesionalId)).thenReturn(java.util.Optional.of(profesional));
        
        String nombre = statsService.obtenerNombreProfesional(profesionalId, tenantId);
        
        assertEquals("Dr. Test", nombre);
    }

    @Test
    void testObtenerNombreProfesionalNotFound() {
        String profesionalId = "prof-1";
        Long tenantId = 101L;
        
        when(profesionalRepository.findByNickname(profesionalId)).thenReturn(java.util.Optional.empty());
        
        String nombre = statsService.obtenerNombreProfesional(profesionalId, tenantId);
        
        assertEquals(profesionalId, nombre);
    }

    @Test
    void testObtenerNombreProfesionalWithException() {
        String profesionalId = "prof-1";
        Long tenantId = 101L;
        
        when(profesionalRepository.findByNickname(profesionalId)).thenThrow(new RuntimeException("DB error"));
        
        String nombre = statsService.obtenerNombreProfesional(profesionalId, tenantId);
        
        assertEquals(profesionalId, nombre);
    }

    @Test
    void testCrearEstadisticasVacias() {
        Map<String, Object> stats = statsService.crearEstadisticasVacias();
        
        assertNotNull(stats);
        assertEquals(0, stats.get("profesionales"));
        assertEquals(0, stats.get("usuarios"));
        assertEquals(0, stats.get("documentos"));
        assertEquals(0, stats.get("consultas"));
    }
}

