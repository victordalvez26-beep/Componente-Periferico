package uy.edu.tse.hcen.messaging;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.MessageListener;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import jakarta.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uy.edu.tse.hcen.repository.NodoPerifericoRepository;
import uy.edu.tse.hcen.utils.NodoPerifericoHttpClient;

@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "resourceAdapter", propertyValue = "activemq-ra"),
    @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "java:/jms/queue/nodoConfigQueue"),
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-ack")
})
public class NodoRegistrationMDB implements MessageListener {

    private static final Logger logger = Logger.getLogger(NodoRegistrationMDB.class.getName());

    @Inject
    private NodoPerifericoRepository repo;

    @Inject
    private NodoPerifericoHttpClient httpClient;

    @Override
    public void onMessage(Message message) {
        String payload = null;
        try {
            if (message instanceof TextMessage) {
                payload = ((TextMessage) message).getText();
            } else {
                logger.warning("Received non-text JMS message: " + message.getClass());
                return;
            }
            logger.info("[MDB] Received JMS message: " + payload);
            java.util.Map<String, Object> map = parseMessage(payload);
            procesarMensaje(map);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error procesando mensaje JMS: " + payload, ex);
            throw new RuntimeException(ex);
        }
    }

    private java.util.Map<String, Object> parseMessage(String message) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        if (message == null) return map;

        try {
            Pattern actionPattern = Pattern.compile("\"action\"\s*:\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
            Matcher m = actionPattern.matcher(message);
            if (m.find()) {
                map.put("action", m.group(1));
            }
        } catch (Exception ex) {}

        try {
            Pattern idPattern = Pattern.compile("\"id_clinica\"\s*:\s*(\\d+)");
            Matcher m = idPattern.matcher(message);
            if (m.find()) { map.put("id", Long.parseLong(m.group(1))); return map; }
        } catch (Exception ex) {}

        try {
            Pattern idPattern2 = Pattern.compile("\"id\"\s*:\s*(\\d+)");
            Matcher m2 = idPattern2.matcher(message);
            if (m2.find()) { map.put("id", Long.parseLong(m2.group(1))); return map; }
        } catch (Exception ex) {}

        try {
            Pattern digits = Pattern.compile("(\\d+)");
            Matcher md = digits.matcher(message);
            if (md.find()) { map.put("id", Long.parseLong(md.group(1))); }
        } catch (Exception ex) {}

        return map;
    }

    private void procesarMensaje(java.util.Map<String, Object> map) throws Exception {
        int maxReintentos = 3; int intentoActual = 0;
        if (map == null || !map.containsKey("id")) { logger.warning("Mensaje sin id: " + map); return; }
        Long id = (Long) map.get("id");
        String action = map.containsKey("action") ? String.valueOf(map.get("action")) : "alta";
        var nodo = repo.find(id);
        if (nodo == null) { logger.warning("Nodo no encontrado: " + id); return; }
        logger.info("[MDB] Iniciando integración REST con nodo: " + nodo.getNodoPerifericoUrlBase() + " action=" + action);

        while (intentoActual < maxReintentos) {
            try {
                boolean ok = httpClient.enviarConfiguracionInicial(nodo);
                if (ok) {
                    nodo.setEstado(uy.edu.tse.hcen.model.enums.EstadoNodoPeriferico.ACTIVO);
                    repo.update(nodo);
                    logger.info("Nodo " + id + " activado exitosamente");
                    return;
                } else {
                    intentoActual++; if (intentoActual >= maxReintentos) break; Thread.sleep(2000L * intentoActual);
                }
            } catch (Exception ex) {
                intentoActual++; logger.log(Level.WARNING, "Intento " + intentoActual + " falló para nodo " + id, ex);
                if (intentoActual >= maxReintentos) throw ex; Thread.sleep(2000L * intentoActual);
            }
        }
        throw new RuntimeException("Falló después de " + maxReintentos + " intentos para id " + id);
    }
}
