package uy.edu.tse.hcen.service;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.annotation.Resource;
import jakarta.transaction.UserTransaction;
import java.util.logging.Level;
import java.util.logging.Logger;
import uy.edu.tse.hcen.model.ProfesionalSalud;
import org.hibernate.Session;
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class ProfesionalPersistenceHelper {

    private static final Logger LOGGER = Logger.getLogger(ProfesionalPersistenceHelper.class.getName());

    @PersistenceContext(unitName = "hcenPersistenceUnit")
    private EntityManager em;

    @Resource
    private UserTransaction userTransaction;

    /**
     * Persiste un profesional de salud en un schema específico usando transacción manual.
     * 
     * @param profesional Profesional a persistir
     * @param schema Nombre del schema donde persistir
     * @throws jakarta.persistence.PersistenceException si hay error de persistencia
     * @throws jakarta.transaction.RollbackException si el rollback falla
     */
    public void persistWithManualTransaction(ProfesionalSalud profesional, String schema) {
        try {
            userTransaction.begin();

            // Aplicar el schema
            Session session = em.unwrap(Session.class);
            session.doWork(connection -> {
                try (java.sql.Statement stmt = connection.createStatement()) {
                    stmt.execute("SET search_path TO " + schema + ", public");
                } catch (java.sql.SQLException e) {
                    throw new jakarta.persistence.PersistenceException("Error estableciendo search_path", e);
                }
            });

            // Persistir
            em.persist(profesional);
            em.flush();

            userTransaction.commit();

        } catch (RuntimeException ex) {
            LOGGER.log(Level.SEVERE, String.format("Error durante persistencia de profesional en schema %s: %s", 
                schema, ex.getMessage()), ex);
            rollbackTransaction();
            throw ex;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, String.format("Error durante persistencia de profesional en schema %s: %s", 
                schema, ex.getMessage()), ex);
            rollbackTransaction();
            throw new jakarta.persistence.PersistenceException(
                String.format("Error persistiendo profesional en schema %s", schema), ex);
        }
    }
    
    /**
     * Realiza rollback de la transacción si está activa.
     * Este método maneja silenciosamente errores de rollback para evitar ocultar la excepción original.
     */
    private void rollbackTransaction() {
        try {
            int status = userTransaction.getStatus();
            if (status != jakarta.transaction.Status.STATUS_NO_TRANSACTION 
                    && status != jakarta.transaction.Status.STATUS_COMMITTED
                    && status != jakarta.transaction.Status.STATUS_ROLLEDBACK) {
                userTransaction.rollback();
                LOGGER.log(Level.WARNING, "UserTransaction rolled back exitosamente");
            }
        } catch (Exception rbEx) {
            // Loggear pero no re-lanzar para no ocultar la excepción original
            LOGGER.log(Level.SEVERE, 
                String.format("Error crítico durante rollback: %s", rbEx.getMessage()), rbEx);
        }
    }
}
