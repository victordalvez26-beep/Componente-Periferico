package uy.edu.tse.hcen.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.annotation.Resource;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.Status;
import uy.edu.tse.hcen.messaging.RabbitSenderLocal;
import uy.edu.tse.hcen.model.NodoPeriferico;
import uy.edu.tse.hcen.repository.NodoPerifericoRepository;
import uy.edu.tse.hcen.model.enums.EstadoNodoPeriferico;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class NodoService {

    private static final Logger logger = Logger.getLogger(NodoService.class.getName());
    private static final String EXCHANGE_NAME = "clinica_config_exchange";

    @Inject
    private NodoPerifericoRepository repo;

    @Inject
    private RabbitSenderLocal sender;

    @Resource
    private jakarta.transaction.TransactionSynchronizationRegistry txRegistry;

    @Transactional
    public NodoPeriferico createAndNotify(NodoPeriferico nodo) {
        // Validar datos antes de persistir
        if (nodo.getRUT() == null || nodo.getNombre() == null) {
            throw new IllegalArgumentException("RUT y nombre son obligatorios");
        }
        
        NodoPeriferico created = repo.create(nodo);
        final String payload = buildPayload(created.getId(), "alta");

        registerSyncPublication(created.getId(), "alta.clinica", payload);
        return created;
    }

    @Transactional
    public NodoPeriferico updateAndNotify(NodoPeriferico nodo) {
        NodoPeriferico updated = repo.update(nodo);
        final String payload = buildPayload(updated.getId(), "update");
        registerSyncPublication(updated.getId(), "update.clinica", payload);
        return updated;
    }

    @Transactional
    public void deleteAndNotify(Long id) {
        repo.delete(id);
        final String payload = buildPayload(id, "delete");
        registerSyncPublication(id, "delete.clinica", payload);
    }

    public void publishForId(Long id, String action) {
        NodoPeriferico existing = repo.find(id);
        if (existing == null) {
            throw new IllegalArgumentException("Nodo no encontrado: " + id);
        }
        
        String routing = determineRoutingKey(action);
        String payload = buildPayload(id, action);
        
        try {
            sender.sendToExchange(EXCHANGE_NAME, routing, payload);
            logger.info("Published " + routing + " for nodo id=" + id);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to publish for nodo id=" + id, e);
            throw new RuntimeException("Failed to publish message", e);
        }
    }

    // --- Métodos auxiliares ---
    
    private void registerSyncPublication(Long nodoId, String routingKey, String payload) {
        try {
            txRegistry.registerInterposedSynchronization(new jakarta.transaction.Synchronization() {
                @Override
                public void beforeCompletion() {}

                @Override
                public void afterCompletion(int status) {
                    if (status == Status.STATUS_COMMITTED) {
                        try {
                            sender.sendToExchange(EXCHANGE_NAME, routingKey, payload);
                            logger.info("Published " + routingKey + " for nodo id=" + nodoId);
                        } catch (Exception e) {
                            logger.log(Level.SEVERE, "Failed to publish after commit", e);
                            markMessageError(nodoId);
                        }
                    }
                }
            });
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not register tx sync", e);
            // Fallback inmediato
            try {
                sender.sendToExchange(EXCHANGE_NAME, routingKey, payload);
            } catch (Exception ex) {
                markMessageError(nodoId);
                throw new RuntimeException("Failed to publish message", ex);
            }
        }
    }

    private void markMessageError(Long nodoId) {
        try {
            repo.updateEstadoInNewTx(nodoId, EstadoNodoPeriferico.ERROR_MENSAJERIA);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Failed to mark ERROR_MENSAJERIA", ex);
        }
    }

    private String buildPayload(Long clinicaId, String action) {
        StringBuilder sb = new StringBuilder("{\"id_clinica\":").append(clinicaId);
        if (action != null && !action.equals("alta")) {
            sb.append(",\"action\":\"").append(action).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private String determineRoutingKey(String action) {
        if (action == null || action.equalsIgnoreCase("alta")) return "alta.clinica";
        if (action.equalsIgnoreCase("update")) return "update.clinica";
        if (action.equalsIgnoreCase("delete")) return "delete.clinica";
        return "alta.clinica";
    }
}
