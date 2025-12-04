package uy.edu.tse.hcen.repository;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests EXHAUSTIVOS para DocumentoClinicoRepository con mocks MongoDB.
 * Cubre TODOS los métodos y casos posibles.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentoClinicoRepository Comprehensive Tests")
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentoClinicoRepositoryTest {

    @Mock
    private MongoDatabase database;

    @Mock
    private MongoCollection<Document> collection;

    @Mock
    private FindIterable<Document> findIterable;

    @Mock
    private MongoCursor<Document> cursor;

    @InjectMocks
    private DocumentoClinicoRepository repository;

    private static final Long TENANT_ID = 101L;
    private static final String CI_PACIENTE = "12345678";
    private static final byte[] VALID_PDF_BYTES = "%PDF-1.4\ntest content".getBytes();

    @BeforeEach
    void setUp() {
        lenient().when(database.getCollection("documentos_clinicos")).thenReturn(collection);
    }

    @Nested
    @DisplayName("getCollection Tests")
    class GetCollectionTests {

        @Test
        @DisplayName("Debe retornar colección de MongoDB")
        void getCollection_returnsCollection() {
            // Act
            MongoCollection<Document> result = repository.getCollection();

            // Assert
            assertNotNull(result);
            assertEquals(collection, result);
            verify(database).getCollection("documentos_clinicos");
        }

        @Test
        @DisplayName("getCollectionPublic debe retornar misma colección")
        void getCollectionPublic_returnsCollection() {
            // Act
            MongoCollection<Document> result = repository.getCollectionPublic();

            // Assert
            assertNotNull(result);
            assertEquals(collection, result);
        }
    }

    @Nested
    @DisplayName("crearDocumentoClinico Tests - TODOS LOS CASOS")
    class CrearDocumentoClinicoTests {

        @Test
        @DisplayName("Debe crear documento con pacienteDoc y contenido")
        void crearDocumento_success() {
            // Arrange
            ObjectId objectId = new ObjectId();
            doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId);
                return null;
            }).when(collection).insertOne(any(Document.class));

            // Act
            Document result = repository.crearDocumentoClinico("12345678", "Contenido de prueba");

            // Assert
            assertNotNull(result);
            assertEquals("12345678", result.getString("pacienteDoc"));
            assertEquals("Contenido de prueba", result.getString("contenido"));
            verify(collection).insertOne(any(Document.class));
        }

        @Test
        @DisplayName("Debe crear múltiples documentos")
        void crearDocumento_multiple_success() {
            // Arrange
            ObjectId objectId = new ObjectId();
            doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId);
                return null;
            }).when(collection).insertOne(any(Document.class));

            // Act
            Document doc1 = repository.crearDocumentoClinico("11111111", "Contenido 1");
            Document doc2 = repository.crearDocumentoClinico("22222222", "Contenido 2");
            Document doc3 = repository.crearDocumentoClinico("33333333", "Contenido 3");

            // Assert
            assertNotNull(doc1);
            assertNotNull(doc2);
            assertNotNull(doc3);
            verify(collection, times(3)).insertOne(any(Document.class));
        }

        @Test
        @DisplayName("Debe manejar contenido largo")
        void crearDocumento_contenidoLargo_success() {
            // Arrange
            String contenidoLargo = "Contenido muy largo. ".repeat(1000);
            ObjectId objectId = new ObjectId();
            doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId);
                return null;
            }).when(collection).insertOne(any(Document.class));

            // Act
            Document result = repository.crearDocumentoClinico("12345678", contenidoLargo);

            // Assert
            assertNotNull(result);
            assertEquals(contenidoLargo, result.getString("contenido"));
        }

        @Test
        @DisplayName("Debe manejar contenido con caracteres especiales")
        void crearDocumento_caracteresEspeciales_success() {
            // Arrange
            ObjectId objectId = new ObjectId();
            doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId);
                return null;
            }).when(collection).insertOne(any(Document.class));

            // Act
            Document result = repository.crearDocumentoClinico(
                    "12345678",
                    "Paciente José María O'Brien - Diagnóstico: úlcera gástrica");

            // Assert
            assertNotNull(result);
            assertTrue(result.getString("contenido").contains("José María"));
        }
    }

    @Nested
    @DisplayName("guardarDocumento Tests")
    class GuardarDocumentoTests {

        @Test
        @DisplayName("Debe guardar documento en MongoDB")
        void guardarDocumento_success() {
            // Arrange
            Document doc = new Document("test", "value");

            // Act
            repository.guardarDocumento(doc);

            // Assert
            verify(collection).insertOne(doc);
        }

        @Test
        @DisplayName("Debe guardar múltiples documentos")
        void guardarDocumento_multiple_success() {
            // Arrange
            Document doc1 = new Document("doc", "1");
            Document doc2 = new Document("doc", "2");
            Document doc3 = new Document("doc", "3");

            // Act
            repository.guardarDocumento(doc1);
            repository.guardarDocumento(doc2);
            repository.guardarDocumento(doc3);

            // Assert
            verify(collection, times(3)).insertOne(any(Document.class));
        }
    }

    @Nested
    @DisplayName("buscarPorDocumentoPaciente Tests - TODOS LOS CASOS")
    class BuscarPorDocumentoPacienteTests {

        @Test
        @DisplayName("Debe buscar documento por pacienteDoc")
        void buscarPorDocumentoPaciente_found_returnsDocument() {
            // Arrange
            Document doc = new Document("pacienteDoc", "12345678");
            when(collection.find(any(Document.class))).thenReturn(findIterable);
            when(findIterable.first()).thenReturn(doc);

            // Act
            Document result = repository.buscarPorDocumentoPaciente("12345678");

            // Assert
            assertNotNull(result);
            assertEquals("12345678", result.getString("pacienteDoc"));
            verify(collection).find(any(Document.class));
        }

        @Test
        @DisplayName("Debe retornar null si no encuentra")
        void buscarPorDocumentoPaciente_notFound_returnsNull() {
            // Arrange
            when(collection.find(any(Document.class))).thenReturn(findIterable);
            when(findIterable.first()).thenReturn(null);

            // Act
            Document result = repository.buscarPorDocumentoPaciente("99999999");

            // Assert
            assertNull(result);
        }

        @Test
        @DisplayName("Debe buscar múltiples documentos consecutivos")
        void buscarPorDocumentoPaciente_multiple_success() {
            // Arrange
            Document doc1 = new Document("pacienteDoc", "11111111");
            Document doc2 = new Document("pacienteDoc", "22222222");
            
            when(collection.find(any(Document.class))).thenReturn(findIterable);
            when(findIterable.first())
                    .thenReturn(doc1)
                    .thenReturn(doc2);

            // Act
            Document result1 = repository.buscarPorDocumentoPaciente("11111111");
            Document result2 = repository.buscarPorDocumentoPaciente("22222222");

            // Assert
            assertNotNull(result1);
            assertNotNull(result2);
        }
    }

    @Nested
    @DisplayName("guardarDocumentoCompleto Tests - TODOS LOS CASOS")
    class GuardarDocumentoCompletoTests {

        @Test
        @DisplayName("Debe guardar documento completo con todos los parámetros")
        void guardarCompleto_allParams_success() {
            // Arrange
            ObjectId objectId = new ObjectId();
            doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId);
                return null;
            }).when(collection).insertOne(any(Document.class));

            // Act
            String result = repository.guardarDocumentoCompleto(
                    "doc-uuid-123",
                    "Contenido del documento",
                    VALID_PDF_BYTES,
                    "archivo adjunto".getBytes(),
                    "adjunto.pdf",
                    "application/pdf",
                    CI_PACIENTE,
                    TENANT_ID,
                    "EVALUACION",
                    "Descripción",
                    "doctor1",
                    "Título del documento",
                    "Dr. Juan Pérez");

            // Assert
            assertNotNull(result);
            assertEquals(objectId.toHexString(), result);
            verify(collection).insertOne(argThat(doc ->
                    doc.getString("documentoId").equals("doc-uuid-123") &&
                    doc.getString("contenido").equals("Contenido del documento") &&
                    doc.getString("ciPaciente").equals(CI_PACIENTE) &&
                    doc.getLong("tenantId").equals(TENANT_ID) &&
                    doc.getString("tipoDocumento").equals("EVALUACION") &&
                    doc.getString("descripcion").equals("Descripción") &&
                    doc.getString("profesionalId").equals("doctor1") &&
                    doc.getString("titulo").equals("Título del documento") &&
                    doc.getString("autor").equals("Dr. Juan Pérez")
            ));
        }

        @Test
        @DisplayName("Debe guardar sin pdfBytes")
        void guardarCompleto_sinPdfBytes_success() {
            // Arrange
            ObjectId objectId = new ObjectId();
            doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId);
                return null;
            }).when(collection).insertOne(any(Document.class));

            // Act
            String result = repository.guardarDocumentoCompleto(
                    "doc-uuid-123",
                    "Contenido",
                    null,
                    null,
                    null,
                    null,
                    CI_PACIENTE,
                    TENANT_ID,
                    "EVALUACION",
                    "Desc",
                    "doctor1",
                    "Título",
                    "Autor");

            // Assert
            assertNotNull(result);
            verify(collection).insertOne(argThat(doc ->
                    !doc.containsKey("pdfBytes")
            ));
        }

        @Test
        @DisplayName("Debe guardar sin archivo adjunto")
        void guardarCompleto_sinArchivoAdjunto_success() {
            // Arrange
            ObjectId objectId = new ObjectId();
            doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId);
                return null;
            }).when(collection).insertOne(any(Document.class));

            // Act
            String result = repository.guardarDocumentoCompleto(
                    "doc-uuid-123",
                    "Contenido",
                    VALID_PDF_BYTES,
                    null,
                    null,
                    null,
                    CI_PACIENTE,
                    TENANT_ID,
                    "EVALUACION",
                    "Desc",
                    "doctor1",
                    null,
                    null);

            // Assert
            assertNotNull(result);
            verify(collection).insertOne(argThat(doc ->
                    !doc.containsKey("archivoAdjunto")
            ));
        }

        @Test
        @DisplayName("Debe incluir archivo adjunto con nombre y tipo")
        void guardarCompleto_conArchivoAdjunto_success() {
            // Arrange
            ObjectId objectId = new ObjectId();
            doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId);
                return null;
            }).when(collection).insertOne(any(Document.class));

            // Act
            String result = repository.guardarDocumentoCompleto(
                    "doc-uuid-123",
                    "Contenido",
                    VALID_PDF_BYTES,
                    "contenido adjunto".getBytes(),
                    "archivo.txt",
                    "text/plain",
                    CI_PACIENTE,
                    TENANT_ID,
                    "EVALUACION",
                    "Desc",
                    "doctor1",
                    "Título",
                    "Autor");

            // Assert
            assertNotNull(result);
            verify(collection).insertOne(argThat(doc ->
                    doc.containsKey("archivoAdjunto") &&
                    doc.getString("nombreArchivoAdjunto").equals("archivo.txt") &&
                    doc.getString("tipoArchivoAdjunto").equals("text/plain")
            ));
        }

        @Test
        @DisplayName("Debe guardar sin campos opcionales")
        void guardarCompleto_sinOpcionales_success() {
            // Arrange
            ObjectId objectId = new ObjectId();
            doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId);
                return null;
            }).when(collection).insertOne(any(Document.class));

            // Act
            String result = repository.guardarDocumentoCompleto(
                    "doc-uuid-123",
                    "Contenido",
                    null,
                    null,
                    null,
                    null,
                    CI_PACIENTE,
                    TENANT_ID,
                    null,
                    null,
                    null,
                    null,
                    null);

            // Assert
            assertNotNull(result);
            verify(collection).insertOne(argThat(doc ->
                    !doc.containsKey("tipoDocumento") &&
                    !doc.containsKey("descripcion") &&
                    !doc.containsKey("profesionalId") &&
                    !doc.containsKey("titulo") &&
                    !doc.containsKey("autor")
            ));
        }

        @Test
        @DisplayName("Debe incluir fechaCreacion con zona Uruguay")
        void guardarCompleto_includesFechaCreacion() {
            // Arrange
            ObjectId objectId = new ObjectId();
            doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId);
                return null;
            }).when(collection).insertOne(any(Document.class));

            // Act
            repository.guardarDocumentoCompleto(
                    "doc-uuid-123",
                    "Contenido",
                    VALID_PDF_BYTES,
                    null,
                    null,
                    null,
                    CI_PACIENTE,
                    TENANT_ID,
                    "EVALUACION",
                    "Desc",
                    "doctor1",
                    "Título",
                    "Autor");

            // Assert
            verify(collection).insertOne(argThat(doc ->
                    doc.getDate("fechaCreacion") != null
            ));
        }

        @Test
        @DisplayName("Debe incluir contentType application/pdf")
        void guardarCompleto_includesContentType() {
            // Arrange
            ObjectId objectId = new ObjectId();
            doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId);
                return null;
            }).when(collection).insertOne(any(Document.class));

            // Act
            repository.guardarDocumentoCompleto(
                    "doc-uuid-123",
                    "Contenido",
                    VALID_PDF_BYTES,
                    null,
                    null,
                    null,
                    CI_PACIENTE,
                    TENANT_ID,
                    "EVALUACION",
                    "Desc",
                    "doctor1",
                    "Título",
                    "Autor");

            // Assert
            verify(collection).insertOne(argThat(doc ->
                    doc.getString("contentType").equals("application/pdf")
            ));
        }

        @Test
        @DisplayName("Debe manejar pdfBytes vacío (no agrega campo)")
        void guardarCompleto_pdfBytesVacio_noAgregaCampo() {
            // Arrange
            ObjectId objectId = new ObjectId();
            doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId);
                return null;
            }).when(collection).insertOne(any(Document.class));

            // Act
            repository.guardarDocumentoCompleto(
                    "doc-uuid-123",
                    "Contenido",
                    new byte[0],
                    null,
                    null,
                    null,
                    CI_PACIENTE,
                    TENANT_ID,
                    "EVALUACION",
                    "Desc",
                    "doctor1",
                    null,
                    null);

            // Assert
            verify(collection).insertOne(argThat(doc ->
                    !doc.containsKey("pdfBytes")
            ));
        }

        @Test
        @DisplayName("Debe manejar archivoAdjunto vacío (no agrega campo)")
        void guardarCompleto_archivoVacio_noAgregaCampo() {
            // Arrange
            ObjectId objectId = new ObjectId();
            doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId);
                return null;
            }).when(collection).insertOne(any(Document.class));

            // Act
            repository.guardarDocumentoCompleto(
                    "doc-uuid-123",
                    "Contenido",
                    VALID_PDF_BYTES,
                    new byte[0],
                    "archivo.txt",
                    "text/plain",
                    CI_PACIENTE,
                    TENANT_ID,
                    "EVALUACION",
                    "Desc",
                    "doctor1",
                    null,
                    null);

            // Assert
            verify(collection).insertOne(argThat(doc ->
                    !doc.containsKey("archivoAdjunto")
            ));
        }
    }

    @Nested
    @DisplayName("buscarPorId Tests - TODOS LOS CASOS")
    class BuscarPorIdTests {

        @Test
        @DisplayName("Debe buscar por ID con tenantId")
        void buscarPorId_conTenantId_success() {
            // Arrange
            ObjectId objectId = new ObjectId();
            Document doc = new Document("_id", objectId).append("tenantId", TENANT_ID);
            
            when(collection.find(any(Document.class))).thenReturn(findIterable);
            when(findIterable.first()).thenReturn(doc);

            // Act
            Document result = repository.buscarPorId(objectId.toHexString(), TENANT_ID);

            // Assert
            assertNotNull(result);
            verify(collection).find(any(Document.class));
        }

        @Test
        @DisplayName("Debe buscar por ID sin tenantId")
        void buscarPorId_sinTenantId_success() {
            // Arrange
            ObjectId objectId = new ObjectId();
            Document doc = new Document("_id", objectId);
            
            when(collection.find(any(Document.class))).thenReturn(findIterable);
            when(findIterable.first()).thenReturn(doc);

            // Act
            Document result = repository.buscarPorId(objectId.toHexString(), null);

            // Assert
            assertNotNull(result);
        }

        @Test
        @DisplayName("Debe retornar null con ID inválido")
        void buscarPorId_invalidId_returnsNull() {
            // Act
            Document result = repository.buscarPorId("invalid-id", TENANT_ID);

            // Assert
            assertNull(result);
            verify(collection, never()).find(any(Document.class));
        }

        @Test
        @DisplayName("Debe retornar null si no encuentra")
        void buscarPorId_notFound_returnsNull() {
            // Arrange
            ObjectId objectId = new ObjectId();
            when(collection.find(any(Document.class))).thenReturn(findIterable);
            when(findIterable.first()).thenReturn(null);

            // Act
            Document result = repository.buscarPorId(objectId.toHexString(), TENANT_ID);

            // Assert
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("buscarPorCiPaciente Tests - TODOS LOS CASOS")
    class BuscarPorCiPacienteTests {

        @Test
        @DisplayName("Debe buscar documentos por CI y tenantId")
        void buscarPorCi_success() {
            // Arrange - buscarPorCiPaciente usa .into(new ArrayList<>())
            java.util.List<Document> docs = java.util.List.of(
                    new Document("ciPaciente", CI_PACIENTE),
                    new Document("ciPaciente", CI_PACIENTE)
            );
            
            when(collection.find(any(Document.class))).thenReturn(findIterable);
            when(findIterable.into(any())).thenReturn(docs);

            // Act
            List<Document> result = repository.buscarPorCiPaciente(CI_PACIENTE, TENANT_ID);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            verify(findIterable).into(any(java.util.ArrayList.class));
        }

        @Test
        @DisplayName("Debe retornar lista vacía si no hay documentos")
        void buscarPorCi_empty_returnsEmptyList() {
            // Arrange
            when(collection.find(any(Document.class))).thenReturn(findIterable);
            when(findIterable.into(any())).thenReturn(new java.util.ArrayList<>());

            // Act
            List<Document> result = repository.buscarPorCiPaciente(CI_PACIENTE, TENANT_ID);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Debe buscar con tenantId null (sin filtrar)")
        void buscarPorCi_sinTenantId_success() {
            // Arrange
            java.util.List<Document> docs = java.util.List.of(
                    new Document("ciPaciente", CI_PACIENTE).append("tenantId", 101L),
                    new Document("ciPaciente", CI_PACIENTE).append("tenantId", 102L)
            );
            
            when(collection.find(any(Document.class))).thenReturn(findIterable);
            when(findIterable.into(any())).thenReturn(docs);

            // Act
            List<Document> result = repository.buscarPorCiPaciente(CI_PACIENTE, null);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("Debe buscar con diferentes CIs")
        void buscarPorCi_differentCis_success() {
            // Arrange
            when(collection.find(any(Document.class))).thenReturn(findIterable);
            when(findIterable.iterator()).thenReturn(cursor);
            when(cursor.hasNext()).thenReturn(false);

            // Act
            repository.buscarPorCiPaciente("11111111", TENANT_ID);
            repository.buscarPorCiPaciente("22222222", TENANT_ID);
            repository.buscarPorCiPaciente("33333333", TENANT_ID);

            // Assert
            verify(collection, times(3)).find(any(Document.class));
        }
    }

    @Nested
    @DisplayName("buscarPorDocumentoId Tests - TODOS LOS CASOS")
    class BuscarPorDocumentoIdTests {

        @Test
        @DisplayName("Debe buscar por documentoId y tenantId")
        void buscarPorDocumentoId_found_returnsDocument() {
            // Arrange
            Document doc = new Document("documentoId", "doc-uuid-123");
            when(collection.find(any(Document.class))).thenReturn(findIterable);
            when(findIterable.first()).thenReturn(doc);

            // Act
            Document result = repository.buscarPorDocumentoId("doc-uuid-123", TENANT_ID);

            // Assert
            assertNotNull(result);
            assertEquals("doc-uuid-123", result.getString("documentoId"));
        }

        @Test
        @DisplayName("Debe retornar null si no encuentra")
        void buscarPorDocumentoId_notFound_returnsNull() {
            // Arrange
            when(collection.find(any(Document.class))).thenReturn(findIterable);
            when(findIterable.first()).thenReturn(null);

            // Act
            Document result = repository.buscarPorDocumentoId("doc-not-found", TENANT_ID);

            // Assert
            assertNull(result);
        }

        @Test
        @DisplayName("Debe buscar sin tenantId si es null")
        void buscarPorDocumentoId_sinTenantId_success() {
            // Arrange
            Document doc = new Document("documentoId", "doc-uuid-123");
            when(collection.find(any(Document.class))).thenReturn(findIterable);
            when(findIterable.first()).thenReturn(doc);

            // Act
            Document result = repository.buscarPorDocumentoId("doc-uuid-123", null);

            // Assert
            assertNotNull(result);
        }

        @Test
        @DisplayName("Debe buscar múltiples documentoIds")
        void buscarPorDocumentoId_multiple_success() {
            // Arrange
            Document doc1 = new Document("documentoId", "doc1");
            Document doc2 = new Document("documentoId", "doc2");
            
            when(collection.find(any(Document.class))).thenReturn(findIterable);
            when(findIterable.first())
                    .thenReturn(doc1)
                    .thenReturn(doc2);

            // Act
            Document result1 = repository.buscarPorDocumentoId("doc1", TENANT_ID);
            Document result2 = repository.buscarPorDocumentoId("doc2", TENANT_ID);

            // Assert
            assertNotNull(result1);
            assertNotNull(result2);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Constructor sin parámetros debe ser válido")
        void constructor_noArgs_valid() {
            // Act
            DocumentoClinicoRepository repo = new DocumentoClinicoRepository();

            // Assert
            assertNotNull(repo);
        }

        @Test
        @DisplayName("Debe manejar contenido muy largo")
        void guardarCompleto_contenidoLargo_success() {
            // Arrange
            String contenidoLargo = "Línea de historia clínica. ".repeat(10000);
            ObjectId objectId = new ObjectId();
            doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId);
                return null;
            }).when(collection).insertOne(any(Document.class));

            // Act
            String result = repository.guardarDocumentoCompleto(
                    "doc-uuid-123",
                    contenidoLargo,
                    VALID_PDF_BYTES,
                    null,
                    null,
                    null,
                    CI_PACIENTE,
                    TENANT_ID,
                    "EVALUACION",
                    "Desc",
                    "doctor1",
                    null,
                    null);

            // Assert
            assertNotNull(result);
        }

        @Test
        @DisplayName("Debe manejar archivo adjunto grande")
        void guardarCompleto_archivoGrande_success() {
            // Arrange
            byte[] archivoGrande = new byte[1024 * 1024 * 2]; // 2MB
            ObjectId objectId = new ObjectId();
            doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId);
                return null;
            }).when(collection).insertOne(any(Document.class));

            // Act
            String result = repository.guardarDocumentoCompleto(
                    "doc-uuid-123",
                    "Contenido",
                    VALID_PDF_BYTES,
                    archivoGrande,
                    "archivo-grande.zip",
                    "application/zip",
                    CI_PACIENTE,
                    TENANT_ID,
                    "EVALUACION",
                    "Desc",
                    "doctor1",
                    null,
                    null);

            // Assert
            assertNotNull(result);
        }

        @Test
        @DisplayName("Debe manejar caracteres especiales en todos los campos")
        void guardarCompleto_caracteresEspeciales_success() {
            // Arrange
            ObjectId objectId = new ObjectId();
            doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId);
                return null;
            }).when(collection).insertOne(any(Document.class));

            // Act
            String result = repository.guardarDocumentoCompleto(
                    "doc-uuid-123",
                    "Paciente José María O'Brien",
                    VALID_PDF_BYTES,
                    null,
                    null,
                    null,
                    "1.234.567-8",
                    TENANT_ID,
                    "EVALUACIÓN",
                    "Descripción con ñ y á",
                    "doctor1",
                    "Título Médico",
                    "Dr. José González");

            // Assert
            assertNotNull(result);
        }

        @Test
        @DisplayName("Debe manejar tenantId muy grande")
        void guardarCompleto_tenantIdGrande_success() {
            // Arrange
            ObjectId objectId = new ObjectId();
            doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId);
                return null;
            }).when(collection).insertOne(any(Document.class));

            // Act
            String result = repository.guardarDocumentoCompleto(
                    "doc-uuid-123",
                    "Contenido",
                    VALID_PDF_BYTES,
                    null,
                    null,
                    null,
                    CI_PACIENTE,
                    999999999L,
                    "EVALUACION",
                    "Desc",
                    "doctor1",
                    null,
                    null);

            // Assert
            assertNotNull(result);
        }
    }
}

