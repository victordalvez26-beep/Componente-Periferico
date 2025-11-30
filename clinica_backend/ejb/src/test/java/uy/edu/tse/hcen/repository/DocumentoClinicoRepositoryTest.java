package uy.edu.tse.hcen.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentoClinicoRepositoryTest {

    @Mock
    private MongoDatabase database;

    @Mock
    private MongoCollection<Document> collection;

    @InjectMocks
    private DocumentoClinicoRepository repository;

    @BeforeEach
    void setUp() {
        when(database.getCollection("documentos_clinicos")).thenReturn(collection);
    }

    @Test
    void testGetCollection() {
        MongoCollection<Document> result = repository.getCollection();
        assertNotNull(result);
        verify(database).getCollection("documentos_clinicos");
    }

    @Test
    void testGetCollectionPublic() {
        MongoCollection<Document> result = repository.getCollectionPublic();
        assertNotNull(result);
        verify(database).getCollection("documentos_clinicos");
    }

    @Test
    void testCrearDocumentoClinico() {
        String pacienteDoc = "12345678";
        String contenido = "Contenido del documento";
        
        Document documento = repository.crearDocumentoClinico(pacienteDoc, contenido);
        
        assertNotNull(documento);
        assertEquals(pacienteDoc, documento.getString("pacienteDoc"));
        assertEquals(contenido, documento.getString("contenido"));
        verify(collection).insertOne(any(Document.class));
    }

    @Test
    void testGuardarDocumento() {
        Document documento = new Document("test", "value");
        repository.guardarDocumento(documento);
        verify(collection).insertOne(documento);
    }

    @Test
    void testBuscarPorDocumentoPaciente() {
        String documento = "12345678";
        Document expectedDoc = new Document("pacienteDoc", documento);
        
        when(collection.find(any(Document.class))).thenReturn(mock(com.mongodb.client.FindIterable.class));
        com.mongodb.client.FindIterable<Document> findIterable = mock(com.mongodb.client.FindIterable.class);
        when(collection.find(any(Document.class))).thenReturn(findIterable);
        when(findIterable.first()).thenReturn(expectedDoc);
        
        Document result = repository.buscarPorDocumentoPaciente(documento);
        
        assertNotNull(result);
        assertEquals(documento, result.getString("pacienteDoc"));
    }

    @Test
    void testGuardarDocumentoCompleto() {
        String documentoId = "doc-123";
        String contenido = "Contenido";
        byte[] pdfBytes = "PDF content".getBytes();
        byte[] archivoAdjuntoBytes = "Adjunto".getBytes();
        String nombreArchivoAdjunto = "archivo.pdf";
        String tipoArchivoAdjunto = "application/pdf";
        String ciPaciente = "12345678";
        Long tenantId = 1L;
        String tipoDocumento = "EVALUACION";
        String descripcion = "Descripción";
        String profesionalId = "prof-1";
        String titulo = "Título";
        String autor = "Autor";
        
        Document insertedDoc = new Document();
        insertedDoc.append("_id", new ObjectId());
        when(collection.insertOne(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            doc.append("_id", new ObjectId());
            return null;
        });
        
        String result = repository.guardarDocumentoCompleto(
            documentoId, contenido, pdfBytes, archivoAdjuntoBytes, nombreArchivoAdjunto,
            tipoArchivoAdjunto, ciPaciente, tenantId, tipoDocumento, descripcion,
            profesionalId, titulo, autor
        );
        
        assertNotNull(result);
        verify(collection).insertOne(any(Document.class));
    }

    @Test
    void testGuardarDocumentoCompletoSinPdf() {
        String documentoId = "doc-123";
        String contenido = "Contenido";
        String ciPaciente = "12345678";
        Long tenantId = 1L;
        
        when(collection.insertOne(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            doc.append("_id", new ObjectId());
            return null;
        });
        
        String result = repository.guardarDocumentoCompleto(
            documentoId, contenido, null, null, null, null,
            ciPaciente, tenantId, null, null, null, null, null
        );
        
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
    void testBuscarPorIdSinTenant() {
        String mongoId = new ObjectId().toHexString();
        Document expectedDoc = new Document("_id", new ObjectId(mongoId));
        
        com.mongodb.client.FindIterable<Document> findIterable = mock(com.mongodb.client.FindIterable.class);
        when(collection.find(any(Document.class))).thenReturn(findIterable);
        when(findIterable.first()).thenReturn(expectedDoc);
        
        Document result = repository.buscarPorId(mongoId, null);
        
        assertNotNull(result);
    }

    @Test
    void testBuscarPorCiPaciente() {
        String ciPaciente = "12345678";
        Long tenantId = 1L;
        List<Document> expectedDocs = new ArrayList<>();
        expectedDocs.add(new Document("ciPaciente", ciPaciente));
        
        com.mongodb.client.FindIterable<Document> findIterable = mock(com.mongodb.client.FindIterable.class);
        when(collection.find(any(Document.class))).thenReturn(findIterable);
        when(findIterable.into(any(List.class))).thenReturn(expectedDocs);
        
        List<Document> result = repository.buscarPorCiPaciente(ciPaciente, tenantId);
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testBuscarPorDocumentoId() {
        String documentoId = "doc-123";
        Long tenantId = 1L;
        Document expectedDoc = new Document("documentoId", documentoId);
        
        com.mongodb.client.FindIterable<Document> findIterable = mock(com.mongodb.client.FindIterable.class);
        when(collection.find(any(Document.class))).thenReturn(findIterable);
        when(findIterable.first()).thenReturn(expectedDoc);
        
        Document result = repository.buscarPorDocumentoId(documentoId, tenantId);
        
        assertNotNull(result);
        assertEquals(documentoId, result.getString("documentoId"));
    }
}

