package uy.edu.tse.hcen.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.logging.Logger;
import uy.edu.tse.hcen.messaging.RabbitSenderLocal;

/**
 * Producer for clinic-related messages. Uses RabbitSenderLocal to publish reliable messages
 * to the broker. Messages are sent to exchange 'hcen.clinic' with routing keys like 'clinica.alta'.
 */
@Stateless
public class ClinicaProducer {
    private static final Logger LOG = Logger.getLogger(ClinicaProducer.class.getName());

    @Inject
    private RabbitSenderLocal sender;

    /**
     * Enqueue a clinic creation (alta) request. Payload must be a JSON string.
     * This method relies on RabbitSender's guarantees (durable exchange, persistent messages,
     * publisher confirms). If the broker is unavailable the call may throw and should be retried by caller.
     */
    public void enqueueAltaClinica(String jsonPayload) {
        try {
            String exchange = "hcen.clinic";
            String routingKey = "clinica.alta";
            sender.sendToExchange(exchange, routingKey, jsonPayload);
            LOG.info("Enqueued alta clinica message to exchange " + exchange + " routingKey=" + routingKey);
        } catch (Exception ex) {
            LOG.severe("Failed to enqueue alta clinica: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }
}
