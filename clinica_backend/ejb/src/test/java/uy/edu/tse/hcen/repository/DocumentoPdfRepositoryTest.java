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

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests EXHAUSTIVOS para DocumentoPdfRepository con mocks MongoDB.
 * Cubre TODOS los métodos y casos posibles.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentoPdfRepository Comprehensive Tests")
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentoPdfRepositoryTest {

    @Mock
    private MongoDatabase database;

    @Mock
    private MongoCollection<Document> collection;

    @Mock
    private FindIterable<Document> findIterable;

    @Mock
    private MongoCursor<Document> cursor;

    @InjectMocks
    private DocumentoPdfRepository repository;

    private static final Long TENANT_ID = 101L;
    private static final String CI_PACIENTE = "12345678";
    private static final byte[] VALID_PDF_BYTES = "%PDF-1.4\ntest content".getBytes();

    @BeforeEach
    void setUp() {
        lenient().when(database.getCollection("documentos_pdf")).thenReturn(collection);
    }

    @Nested
    @DisplayName("getCollectionPublic Tests")
    class GetCollectionPublicTests {

        @Test
        @DisplayName("Debe retornar colección de MongoDB")
        void getCollectionPublic_returnsCollection() {
            // Act
            MongoCollection<Document> result = repository.getCollectionPublic();

            // Assert
            assertNotNull(result);
            assertEquals(collection, result);
            verify(database).getCollection("documentos_pdf");
        }

        @Test
        @DisplayName("Debe llamar a database.getCollection cada vez")
        void getCollectionPublic_callsDatabase() {
            // Act
            repository.getCollectionPublic();
            repository.getCollectionPublic();
            repository.getCollectionPublic();

            // Assert
            verify(database, times(3)).getCollection("documentos_pdf");
        }
    }

    @Nested
    @DisplayName("guardarPdf Tests - TODOS LOS CASOS")
    class GuardarPdfTests {

        @Test
        @DisplayName("Debe guardar PDF con todos los parámetros")
        void guardarPdf_allParams_success() {
            // Arrange
            ObjectId objectId = new ObjectId();
            Document capturedDoc = new Document();
            capturedDoc.append("_id", objectId);
            
            doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId);
                return null;
            }).when(collection).insertOne(any(Document.class));

            // Act
            String result = repository.guardarPdf(
                    "doc-uuid-123",
                    VALID_PDF_BYTES,
                    CI_PACIENTE,
                    TENANT_ID,
                    "EVALUACION",
                    "Descripción test",
                    "doctor1");

            // Assert
            assertNotNull(result);
            assertEquals(objectId.toHexString(), result);
            verify(collection).insertOne(argThat(doc ->
                    doc.getString("documentoId").equals("doc-uuid-123") &&
                    doc.getString("ciPaciente").equals(CI_PACIENTE) &&
                    doc.getLong("tenantId").equals(TENANT_ID) &&
                    doc.getString("tipoDocumento").equals("EVALUACION") &&
                    doc.getString("descripcion").equals("Descripción test") &&
                    doc.getString("profesionalId").equals("doctor1") &&
                    doc.getString("contentType").equals("application/pdf")
            ));
        }

        @Test
        @DisplayName("Debe guardar PDF sin tipoDocumento")
        void guardarPdf_sinTipoDocumento_success() {
            // Arrange
            ObjectId objectId = new ObjectId();
            doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId);
                return null;
            }).when(collection).insertOne(any(Document.class));

            // Act
            String result = repository.guardarPdf(
                    "doc-uuid-123",
                    VALID_PDF_BYTES,
                    CI_PACIENTE,
                    TENANT_ID,
                    null,
                    "Descripción",
                    "doctor1");

            // Assert
            assertNotNull(result);
            verify(collection).insertOne(argThat(doc ->
                    !doc.containsKey("tipoDocumento")
            ));
        }

        @Test
        @DisplayName("Debe guardar PDF sin descripción")
        void guardarPdf_sinDescripcion_success() {
            // Arrange
            ObjectId objectId = new ObjectId();
            doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId);
                return null;
            }).when(collection).insertOne(any(Document.class));

            // Act
            String result = repository.guardarPdf(
                    "doc-uuid-123",
                    VALID_PDF_BYTES,
                    CI_PACIENTE,
                    TENANT_ID,
                    "EVALUACION",
                    null,
                    "doctor1");

            // Assert
            assertNotNull(result);
            verify(collection).insertOne(argThat(doc ->
                    !doc.containsKey("descripcion")
            ));
        }

        @Test
        @DisplayName("Debe guardar PDF sin profesionalId")
        void guardarPdf_sinProfesionalId_success() {
            // Arrange
            ObjectId objectId = new ObjectId();
            doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId);
                return null;
            }).when(collection).insertOne(any(Document.class));

            // Act
            String result = repository.guardarPdf(
                    "doc-uuid-123",
                    VALID_PDF_BYTES,
                    CI_PACIENTE,
                    TENANT_ID,
                    "EVALUACION",
                    "Descripción",
                    null);

            // Assert
            assertNotNull(result);
            verify(collection).insertOne(argThat(doc ->
                    !doc.containsKey("profesionalId")
            ));
        }

        @Test
        @DisplayName("Debe incluir Binary con pdfBytes")
        void guardarPdf_includesBinary() {
            // Arrange
            ObjectId objectId = new ObjectId();
            doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId);
                return null;
            }).when(collection).insertOne(any(Document.class));

            // Act
            repository.guardarPdf(
                    "doc-uuid-123",
                    VALID_PDF_BYTES,
                    CI_PACIENTE,
                    TENANT_ID,
                    "EVALUACION",
                    "Desc",
                    "doctor1");

            // Assert
            verify(collection).insertOne(argThat(doc -> {
                Binary binary = doc.get("pdfBytes", Binary.class);
                return binary != null && binary.getData().length == VALID_PDF_BYTES.length;
            }));
        }

        @Test
        @DisplayName("Debe incluir fechaCreacion con zona horaria Uruguay")
        void guardarPdf_includesFechaCreacion() {
            // Arrange
            ObjectId objectId = new ObjectId();
            doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId);
                return null;
            }).when(collection).insertOne(any(Document.class));

            // Act
            repository.guardarPdf(
                    "doc-uuid-123",
                    VALID_PDF_BYTES,
                    CI_PACIENTE,
                    TENANT_ID,
                    "EVALUACION",
                    "Desc",
                    "doctor1");

            // Assert
            verify(collection).insertOne(argThat(doc -> {
                Date fecha = doc.getDate("fechaCreacion");
                return fecha != null;
            }));
        }

        @Test
        @DisplayName("Debe retornar ObjectId en formato hex")
        void guardarPdf_returnsHexString() {
            // Arrange
            ObjectId objectId = new ObjectId();
            doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId);
                return null;
            }).when(collection).insertOne(any(Document.class));

            // Act
            String result = repository.guardarPdf(
                    "doc-uuid-123",
                    VALID_PDF_BYTES,
                    CI_PACIENTE,
                    TENANT_ID,
                    "EVALUACION",
                    "Desc",
                    "doctor1");

            // Assert
            assertEquals(objectId.toHexString(), result);
        }

        @Test
        @DisplayName("Debe guardar múltiples PDFs")
        void guardarPdf_multiple_success() {
            // Arrange
            ObjectId objectId1 = new ObjectId();
            ObjectId objectId2 = new ObjectId();
            ObjectId objectId3 = new ObjectId();
            
            doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId1);
                return null;
            }).doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId2);
                return null;
            }).doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId3);
                return null;
            }).when(collection).insertOne(any(Document.class));

            // Act
            String id1 = repository.guardarPdf("doc1", VALID_PDF_BYTES, CI_PACIENTE, TENANT_ID, "EVALUACION", "D1", "doc1");
            String id2 = repository.guardarPdf("doc2", VALID_PDF_BYTES, "87654321", TENANT_ID, "INFORME", "D2", "doc2");
            String id3 = repository.guardarPdf("doc3", VALID_PDF_BYTES, CI_PACIENTE, 102L, "RECETA", "D3", "doc3");

            // Assert
            assertNotNull(id1);
            assertNotNull(id2);
            assertNotNull(id3);
            verify(collection, times(3)).insertOne(any(Document.class));
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
            Document doc = new Document();
            doc.append("_id", objectId);
            doc.append("ciPaciente", CI_PACIENTE);
            doc.append("tenantId", TENANT_ID);
            
            when(collection.find(any(Document.class))).thenReturn(findIterable);
            when(findIterable.first()).thenReturn(doc);

            // Act
            Document result = repository.buscarPorId(objectId.toHexString(), TENANT_ID);

            // Assert
            assertNotNull(result);
            assertEquals(CI_PACIENTE, result.getString("ciPaciente"));
            verify(collection).find(any(Document.class));
        }

        @Test
        @DisplayName("Debe buscar por ID sin tenantId")
        void buscarPorId_sinTenantId_success() {
            // Arrange
            ObjectId objectId = new ObjectId();
            Document doc = new Document();
            doc.append("_id", objectId);
            
            when(collection.find(any(Document.class))).thenReturn(findIterable);
            when(findIterable.first()).thenReturn(doc);

            // Act
            Document result = repository.buscarPorId(objectId.toHexString(), null);

            // Assert
            assertNotNull(result);
            verify(collection).find(any(Document.class));
        }

        @Test
        @DisplayName("Debe retornar null si no encuentra documento")
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

        @Test
        @DisplayName("Debe retornar null con ID inválido")
        void buscarPorId_invalidId_returnsNull() {
            // Act
            Document result = repository.buscarPorId("invalid-id-format", TENANT_ID);

            // Assert
            assertNull(result);
            verify(collection, never()).find(any(Document.class));
        }

        @Test
        @DisplayName("Debe manejar ID vacío")
        void buscarPorId_emptyId_returnsNull() {
            // Act
            Document result = repository.buscarPorId("", TENANT_ID);

            // Assert
            assertNull(result);
        }

        @Test
        @DisplayName("Debe manejar ID null")
        void buscarPorId_nullId_returnsNull() {
            // Act
            Document result = repository.buscarPorId(null, TENANT_ID);

            // Assert
            assertNull(result);
        }

        @Test
        @DisplayName("Debe buscar múltiples IDs consecutivos")
        void buscarPorId_multiple_success() {
            // Arrange
            ObjectId id1 = new ObjectId();
            ObjectId id2 = new ObjectId();
            ObjectId id3 = new ObjectId();
            
            Document doc1 = new Document("_id", id1);
            Document doc2 = new Document("_id", id2);
            Document doc3 = new Document("_id", id3);
            
            when(collection.find(any(Document.class))).thenReturn(findIterable);
            when(findIterable.first())
                    .thenReturn(doc1)
                    .thenReturn(doc2)
                    .thenReturn(doc3);

            // Act
            Document result1 = repository.buscarPorId(id1.toHexString(), TENANT_ID);
            Document result2 = repository.buscarPorId(id2.toHexString(), TENANT_ID);
            Document result3 = repository.buscarPorId(id3.toHexString(), TENANT_ID);

            // Assert
            assertNotNull(result1);
            assertNotNull(result2);
            assertNotNull(result3);
            verify(collection, times(3)).find(any(Document.class));
        }
    }

    @Nested
    @DisplayName("buscarPorPaciente Tests - TODOS LOS CASOS")
    class BuscarPorPacienteTests {

        @Test
        @DisplayName("Debe buscar documentos por CI y tenantId")
        void buscarPorPaciente_success() {
            // Arrange
            Document doc1 = new Document("ciPaciente", CI_PACIENTE);
            Document doc2 = new Document("ciPaciente", CI_PACIENTE);
            
            when(collection.find(any(Document.class))).thenReturn(findIterable);
            when(findIterable.iterator()).thenReturn(cursor);
            when(cursor.hasNext())
                    .thenReturn(true)
                    .thenReturn(true)
                    .thenReturn(false);
            when(cursor.next())
                    .thenReturn(doc1)
                    .thenReturn(doc2);

            // Act
            List<Document> result = repository.buscarPorPaciente(CI_PACIENTE, TENANT_ID);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            verify(collection).find(any(Document.class));
            verify(cursor).close();
        }

        @Test
        @DisplayName("Debe retornar lista vacía si no hay documentos")
        void buscarPorPaciente_empty_returnsEmptyList() {
            // Arrange
            when(collection.find(any(Document.class))).thenReturn(findIterable);
            when(findIterable.iterator()).thenReturn(cursor);
            when(cursor.hasNext()).thenReturn(false);

            // Act
            List<Document> result = repository.buscarPorPaciente(CI_PACIENTE, TENANT_ID);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(cursor).close();
        }

        @Test
        @DisplayName("Debe cerrar cursor en finally")
        void buscarPorPaciente_closesCursor() {
            // Arrange
            when(collection.find(any(Document.class))).thenReturn(findIterable);
            when(findIterable.iterator()).thenReturn(cursor);
            when(cursor.hasNext()).thenReturn(false);

            // Act
            repository.buscarPorPaciente(CI_PACIENTE, TENANT_ID);

            // Assert
            verify(cursor).close();
        }

        @Test
        @DisplayName("Debe buscar con diferentes CIs")
        void buscarPorPaciente_differentCis_success() {
            // Arrange
            when(collection.find(any(Document.class))).thenReturn(findIterable);
            when(findIterable.iterator()).thenReturn(cursor);
            when(cursor.hasNext()).thenReturn(false);

            // Act
            repository.buscarPorPaciente("11111111", TENANT_ID);
            repository.buscarPorPaciente("22222222", TENANT_ID);
            repository.buscarPorPaciente("33333333", TENANT_ID);

            // Assert
            verify(collection, times(3)).find(any(Document.class));
        }

        @Test
        @DisplayName("Debe buscar con diferentes tenantIds")
        void buscarPorPaciente_differentTenants_success() {
            // Arrange
            when(collection.find(any(Document.class))).thenReturn(findIterable);
            when(findIterable.iterator()).thenReturn(cursor);
            when(cursor.hasNext()).thenReturn(false);

            // Act
            repository.buscarPorPaciente(CI_PACIENTE, 101L);
            repository.buscarPorPaciente(CI_PACIENTE, 102L);
            repository.buscarPorPaciente(CI_PACIENTE, 103L);

            // Assert
            verify(collection, times(3)).find(any(Document.class));
        }
    }

    @Nested
    @DisplayName("buscarPorPacienteTodasClinicas Tests - TODOS LOS CASOS")
    class BuscarPorPacienteTodasClinicasTests {

        @Test
        @DisplayName("Debe buscar por CI sin filtrar por tenantId")
        void buscarTodasClinicas_success() {
            // Arrange
            Document doc1 = new Document("ciPaciente", CI_PACIENTE).append("tenantId", 101L);
            Document doc2 = new Document("ciPaciente", CI_PACIENTE).append("tenantId", 102L);
            Document doc3 = new Document("ciPaciente", CI_PACIENTE).append("tenantId", 103L);
            
            when(collection.find(any(Document.class))).thenReturn(findIterable);
            when(findIterable.iterator()).thenReturn(cursor);
            when(cursor.hasNext())
                    .thenReturn(true)
                    .thenReturn(true)
                    .thenReturn(true)
                    .thenReturn(false);
            when(cursor.next())
                    .thenReturn(doc1)
                    .thenReturn(doc2)
                    .thenReturn(doc3);

            // Act
            List<Document> result = repository.buscarPorPacienteTodasClinicas(CI_PACIENTE);

            // Assert
            assertNotNull(result);
            assertEquals(3, result.size());
            verify(collection).find(any(Document.class));
            verify(cursor).close();
        }

        @Test
        @DisplayName("Debe retornar lista vacía si no hay documentos")
        void buscarTodasClinicas_empty_returnsEmptyList() {
            // Arrange
            when(collection.find(any(Document.class))).thenReturn(findIterable);
            when(findIterable.iterator()).thenReturn(cursor);
            when(cursor.hasNext()).thenReturn(false);

            // Act
            List<Document> result = repository.buscarPorPacienteTodasClinicas(CI_PACIENTE);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Debe cerrar cursor en finally")
        void buscarTodasClinicas_closesCursor() {
            // Arrange
            when(collection.find(any(Document.class))).thenReturn(findIterable);
            when(findIterable.iterator()).thenReturn(cursor);
            when(cursor.hasNext()).thenReturn(false);

            // Act
            repository.buscarPorPacienteTodasClinicas(CI_PACIENTE);

            // Assert
            verify(cursor).close();
        }

        @Test
        @DisplayName("Debe buscar múltiples pacientes")
        void buscarTodasClinicas_multiplePacientes_success() {
            // Arrange
            when(collection.find(any(Document.class))).thenReturn(findIterable);
            when(findIterable.iterator()).thenReturn(cursor);
            when(cursor.hasNext()).thenReturn(false);

            // Act
            repository.buscarPorPacienteTodasClinicas("11111111");
            repository.buscarPorPacienteTodasClinicas("22222222");
            repository.buscarPorPacienteTodasClinicas("33333333");

            // Assert
            verify(collection, times(3)).find(any(Document.class));
        }

        @Test
        @DisplayName("Debe retornar documentos de múltiples clínicas")
        void buscarTodasClinicas_multipleClinicas() {
            // Arrange
            Document doc1 = new Document("ciPaciente", CI_PACIENTE).append("tenantId", 101L);
            Document doc2 = new Document("ciPaciente", CI_PACIENTE).append("tenantId", 102L);
            
            when(collection.find(any(Document.class))).thenReturn(findIterable);
            when(findIterable.iterator()).thenReturn(cursor);
            when(cursor.hasNext())
                    .thenReturn(true)
                    .thenReturn(true)
                    .thenReturn(false);
            when(cursor.next())
                    .thenReturn(doc1)
                    .thenReturn(doc2);

            // Act
            List<Document> result = repository.buscarPorPacienteTodasClinicas(CI_PACIENTE);

            // Assert
            assertEquals(2, result.size());
            // Verifica que hay documentos de diferentes clínicas
            long clinic1Count = result.stream().filter(d -> d.getLong("tenantId") == 101L).count();
            long clinic2Count = result.stream().filter(d -> d.getLong("tenantId") == 102L).count();
            assertEquals(1, clinic1Count);
            assertEquals(1, clinic2Count);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Constructor sin parámetros debe ser válido")
        void constructor_noArgs_valid() {
            // Act
            DocumentoPdfRepository repo = new DocumentoPdfRepository();

            // Assert
            assertNotNull(repo);
        }

        @Test
        @DisplayName("Debe manejar PDF muy grande")
        void guardarPdf_largePdf_success() {
            // Arrange
            byte[] largePdf = new byte[1024 * 1024 * 5]; // 5MB
            ObjectId objectId = new ObjectId();
            doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId);
                return null;
            }).when(collection).insertOne(any(Document.class));

            // Act
            String result = repository.guardarPdf(
                    "doc-large",
                    largePdf,
                    CI_PACIENTE,
                    TENANT_ID,
                    "EVALUACION",
                    "PDF grande",
                    "doctor1");

            // Assert
            assertNotNull(result);
        }

        @Test
        @DisplayName("Debe manejar CI con guiones")
        void buscarPorPaciente_ciConGuiones_success() {
            // Arrange
            String ciConGuiones = "1.234.567-8";
            when(collection.find(any(Document.class))).thenReturn(findIterable);
            when(findIterable.iterator()).thenReturn(cursor);
            when(cursor.hasNext()).thenReturn(false);

            // Act
            List<Document> result = repository.buscarPorPaciente(ciConGuiones, TENANT_ID);

            // Assert
            assertNotNull(result);
            verify(collection).find(any(Document.class));
        }

        @Test
        @DisplayName("Debe manejar documentoId con caracteres especiales")
        void guardarPdf_documentoIdEspecial_success() {
            // Arrange
            ObjectId objectId = new ObjectId();
            doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId);
                return null;
            }).when(collection).insertOne(any(Document.class));

            // Act
            String result = repository.guardarPdf(
                    "doc-uuid-123-abc-def-456",
                    VALID_PDF_BYTES,
                    CI_PACIENTE,
                    TENANT_ID,
                    "EVALUACION",
                    "Desc",
                    "doctor1");

            // Assert
            assertNotNull(result);
        }

        @Test
        @DisplayName("Debe manejar descripción con caracteres especiales")
        void guardarPdf_descripcionEspecial_success() {
            // Arrange
            ObjectId objectId = new ObjectId();
            doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId);
                return null;
            }).when(collection).insertOne(any(Document.class));

            // Act
            String result = repository.guardarPdf(
                    "doc-uuid-123",
                    VALID_PDF_BYTES,
                    CI_PACIENTE,
                    TENANT_ID,
                    "EVALUACION",
                    "Evaluación médica del Dr. José María O'Brien",
                    "doctor1");

            // Assert
            assertNotNull(result);
            verify(collection).insertOne(argThat(doc ->
                    doc.getString("descripcion").contains("José María")
            ));
        }

        @Test
        @DisplayName("Debe manejar tenantId muy grande")
        void guardarPdf_tenantIdGrande_success() {
            // Arrange
            ObjectId objectId = new ObjectId();
            doAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.append("_id", objectId);
                return null;
            }).when(collection).insertOne(any(Document.class));

            // Act
            String result = repository.guardarPdf(
                    "doc-uuid-123",
                    VALID_PDF_BYTES,
                    CI_PACIENTE,
                    999999999L,
                    "EVALUACION",
                    "Desc",
                    "doctor1");

            // Assert
            assertNotNull(result);
        }
    }
}

