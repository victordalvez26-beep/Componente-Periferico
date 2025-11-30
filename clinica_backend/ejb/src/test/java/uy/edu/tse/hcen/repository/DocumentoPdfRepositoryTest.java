package uy.edu.tse.hcen.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings("unchecked")
class DocumentoPdfRepositoryTest {

    @Mock
    private MongoDatabase database;

    @Mock
    private MongoCollection<Document> collection;

    @InjectMocks
    private DocumentoPdfRepository repository;

    @BeforeEach
    void setUp() {
        when(database.getCollection("documentos_pdf")).thenReturn(collection);
    }

    @Test
    void testGetCollectionPublic() {
        MongoCollection<Document> result = repository.getCollectionPublic();
        assertNotNull(result);
        verify(database).getCollection("documentos_pdf");
    }

    @Test
    void testGuardarPdf() {
        String documentoId = "doc-123";
        byte[] pdfBytes = "PDF content".getBytes();
        String ciPaciente = "12345678";
        Long tenantId = 1L;
        String tipoDocumento = "EVALUACION";
        String descripcion = "Descripción";
        String profesionalId = "prof-1";
        
        when(collection.insertOne(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            doc.append("_id", new ObjectId());
            return null;
        });
        
        String result = repository.guardarPdf(documentoId, pdfBytes, ciPaciente, tenantId,
                tipoDocumento, descripcion, profesionalId);
        
        assertNotNull(result);
        verify(collection).insertOne(any(Document.class));
    }

    @Test
    void testGuardarPdfSinMetadata() {
        String documentoId = "doc-123";
        byte[] pdfBytes = "PDF content".getBytes();
        String ciPaciente = "12345678";
        Long tenantId = 1L;
        
        when(collection.insertOne(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            doc.append("_id", new ObjectId());
            return null;
        });
        
        String result = repository.guardarPdf(documentoId, pdfBytes, ciPaciente, tenantId,
                null, null, null);
        
        assertNotNull(result);
        verify(collection).insertOne(any(Document.class));
    }

    @Test
    void testBuscarPorId() {
        String mongoId = new ObjectId().toHexString();
        Long tenantId = 1L;
        Document expectedDoc = new Document("_id", new ObjectId(mongoId));
        
        com.mongodb.client.FindIterable<Document> findIterable = mock(com.mongodb.client.FindIterable.class);
        when(collection.find(any(Document.class))).thenReturn(findIterable);
        when(findIterable.first()).thenReturn(expectedDoc);
        
        Document result = repository.buscarPorId(mongoId, tenantId);
        
        assertNotNull(result);
    }

    @Test
    void testBuscarPorIdInvalidId() {
        String invalidId = "invalid";
        Long tenantId = 1L;
        
        Document result = repository.buscarPorId(invalidId, tenantId);
        
        assertNull(result);
    }

    @Test
    void testBuscarPorPaciente() {
        String ciPaciente = "12345678";
        Long tenantId = 1L;
        
        com.mongodb.client.FindIterable<Document> findIterable = mock(com.mongodb.client.FindIterable.class);
        com.mongodb.client.MongoCursor<Document> cursor = mock(com.mongodb.client.MongoCursor.class);
        when(collection.find(any(Document.class))).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(false);
        
        List<Document> result = repository.buscarPorPaciente(ciPaciente, tenantId);
        
        assertNotNull(result);
        verify(cursor).close();
    }

    @Test
    void testBuscarPorPacienteTodasClinicas() {
        String ciPaciente = "12345678";
        
        com.mongodb.client.FindIterable<Document> findIterable = mock(com.mongodb.client.FindIterable.class);
        com.mongodb.client.MongoCursor<Document> cursor = mock(com.mongodb.client.MongoCursor.class);
        when(collection.find(any(Document.class))).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(false);
        
        List<Document> result = repository.buscarPorPacienteTodasClinicas(ciPaciente);
        
        assertNotNull(result);
        verify(cursor).close();
    }
}

