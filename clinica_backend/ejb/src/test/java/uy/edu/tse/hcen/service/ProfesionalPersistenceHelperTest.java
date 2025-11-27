package uy.edu.tse.hcen.service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.UserTransaction;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.tse.hcen.model.ProfesionalSalud;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfesionalPersistenceHelperTest {

    @Mock
    private EntityManager em;

    @Mock
    private UserTransaction userTransaction;

    @Mock
    private Session session;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @InjectMocks
    private ProfesionalPersistenceHelper helper;

    @Test
    void testPersistWithManualTransaction() throws Exception {
        ProfesionalSalud profesional = new ProfesionalSalud();
        profesional.setId(1L);
        String schema = "schema_clinica_1";

        when(em.unwrap(Session.class)).thenReturn(session);
        doAnswer(invocation -> {
            // Simular doWork
            return null;
        }).when(session).doWork(any());

        helper.persistWithManualTransaction(profesional, schema);

        verify(userTransaction).begin();
        verify(em).persist(profesional);
        verify(em).flush();
        verify(userTransaction).commit();
    }

    @Test
    void testPersistWithManualTransactionRollback() throws Exception {
        ProfesionalSalud profesional = new ProfesionalSalud();
        String schema = "schema_clinica_1";

        when(em.unwrap(Session.class)).thenReturn(session);
        doAnswer(invocation -> null).when(session).doWork(any());
        doThrow(new RuntimeException("Database error")).when(em).persist(any());

        assertThrows(RuntimeException.class, () -> {
            helper.persistWithManualTransaction(profesional, schema);
        });

        verify(userTransaction).begin();
        verify(userTransaction).rollback();
    }

    @Test
    void testPersistWithManualTransactionRollbackException() throws Exception {
        ProfesionalSalud profesional = new ProfesionalSalud();
        String schema = "schema_clinica_1";

        when(em.unwrap(Session.class)).thenReturn(session);
        doAnswer(invocation -> null).when(session).doWork(any());
        doThrow(new RuntimeException("Database error")).when(em).persist(any());
        doThrow(new Exception("Rollback failed")).when(userTransaction).rollback();

        assertThrows(RuntimeException.class, () -> {
            helper.persistWithManualTransaction(profesional, schema);
        });

        verify(userTransaction).begin();
        verify(userTransaction).rollback();
    }

    @Test
    void testPersistWithManualTransactionWithPublicSchema() throws Exception {
        ProfesionalSalud profesional = new ProfesionalSalud();
        profesional.setId(1L);
        String schema = "public";

        when(em.unwrap(Session.class)).thenReturn(session);
        doAnswer(invocation -> null).when(session).doWork(any());

        helper.persistWithManualTransaction(profesional, schema);

        verify(userTransaction).begin();
        verify(em).persist(profesional);
        verify(em).flush();
        verify(userTransaction).commit();
    }

    @Test
    void testPersistWithManualTransactionWithExceptionInDoWork() throws Exception {
        ProfesionalSalud profesional = new ProfesionalSalud();
        String schema = "schema_clinica_1";

        when(em.unwrap(Session.class)).thenReturn(session);
        doThrow(new RuntimeException("Schema error")).when(session).doWork(any());

        assertThrows(RuntimeException.class, () -> {
            helper.persistWithManualTransaction(profesional, schema);
        });

        verify(userTransaction).begin();
        verify(userTransaction).rollback();
        verify(em, never()).persist(any());
    }

    @Test
    void testPersistWithManualTransactionWithExceptionInFlush() throws Exception {
        ProfesionalSalud profesional = new ProfesionalSalud();
        String schema = "schema_clinica_1";

        when(em.unwrap(Session.class)).thenReturn(session);
        doAnswer(invocation -> null).when(session).doWork(any());
        doNothing().when(em).persist(any());
        doThrow(new RuntimeException("Flush error")).when(em).flush();

        assertThrows(RuntimeException.class, () -> {
            helper.persistWithManualTransaction(profesional, schema);
        });

        verify(userTransaction).begin();
        verify(em).persist(profesional);
        verify(em).flush();
        verify(userTransaction).rollback();
        verify(userTransaction, never()).commit();
    }
}

