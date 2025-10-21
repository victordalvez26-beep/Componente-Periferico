package uy.edu.tse.hcen.messaging;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import uy.edu.tse.hcen.model.NodoPeriferico;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class ClinicaRabbitMQProducer {

    private static final Logger logger = Logger.getLogger(ClinicaRabbitMQProducer.class.getName());
    private static final String EXCHANGE_NAME = "clinica_config_exchange";

    @Inject
    private RabbitSenderLocal sender;

    public void enviarAlta(NodoPeriferico nodo) {
        if (nodo == null || nodo.getId() == null) return;
        String routingKey = "alta.clinica";
        String payload = "{\"id_clinica\":" + nodo.getId() + "}";
        try {
            sender.sendToExchange(EXCHANGE_NAME, routingKey, payload);
            logger.info("RabbitSender used to publish alta, ID=" + nodo.getId());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "ERROR RabbitMQ via RabbitSender: Fallo al publicar el mensaje.", e);
            // Decide policy: rollback or mark as error. For now, we just log.
        }
    }
}
