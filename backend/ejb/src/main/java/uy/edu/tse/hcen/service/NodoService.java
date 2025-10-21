package uy.edu.tse.hcen.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import uy.edu.tse.hcen.messaging.RabbitSenderLocal;
import uy.edu.tse.hcen.model.NodoPeriferico;
import uy.edu.tse.hcen.repository.NodoPerifericoRepository;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class NodoService {

    private static final Logger logger = Logger.getLogger(NodoService.class.getName());

    @Inject
    private NodoPerifericoRepository repo;

    @Inject
    private RabbitSenderLocal sender;

    /**
     * Crea el nodo en BD y publica el mensaje de alta de forma transaccional.
     * Política: si la publicación falla, marcamos estado ERROR_MENSAJERIA y lanzamos
     * una RuntimeException para provocar rollback (configurable si quieres otra política).
     */
    @Transactional
    public NodoPeriferico createAndNotify(NodoPeriferico nodo) {
        // Persistir (repo.create usa EntityManager y quedará en la misma tx)
        NodoPeriferico created = repo.create(nodo);

        // Preparar payload
        String payload = "{\"id_clinica\":" + created.getId() + "}";

        try {
            sender.sendToExchange("clinica_config_exchange", "alta.clinica", payload);
            logger.info("Published alta for nodo id=" + created.getId());
            return created;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to publish alta message for nodo id=" + created.getId(), e);
            // marcar estado de nodo y forzar rollback
            created.setEstado(uy.edu.tse.hcen.model.enums.EstadoNodoPeriferico.ERROR_MENSAJERIA);
            repo.update(created);
            // lanzar excepción para rollback de la transacción
            throw new RuntimeException("Failed to publish message to RabbitMQ", e);
        }
    }
}
