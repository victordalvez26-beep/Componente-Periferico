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

    public void persistWithManualTransaction(ProfesionalSalud profesional, String schema) throws Exception {
        try {
            userTransaction.begin();

            // Aplicar el schema
            Session session = em.unwrap(Session.class);
            session.doWork(connection -> {
                try (java.sql.Statement stmt = connection.createStatement()) {
                    stmt.execute("SET search_path TO " + schema + ", public");
                }
            });

            // Persistir
            em.persist(profesional);
            em.flush();

            userTransaction.commit();

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error during persist", ex);
            try {
                userTransaction.rollback();
                LOGGER.log(Level.WARNING, "UserTransaction rolled back");
            } catch (Exception rbEx) {
                LOGGER.log(Level.SEVERE, "Rollback failed", rbEx);
            }
            throw ex;
        }
    }
}
