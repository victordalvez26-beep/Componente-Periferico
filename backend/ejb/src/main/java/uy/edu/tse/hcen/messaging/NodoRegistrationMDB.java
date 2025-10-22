package uy.edu.tse.hcen.messaging;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.MessageListener;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uy.edu.tse.hcen.repository.NodoPerifericoRepository;
import uy.edu.tse.hcen.utils.NodoPerifericoHttpClient;
import uy.edu.tse.hcen.model.NodoPeriferico;


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
            if (message instanceof TextMessage textmessage) {
                payload = textmessage.getText();
            } else {
                logger.warning("Received non-text JMS message: " + message.getClass());
                return;
            }
            logger.log(Level.INFO, "[MDB] Received JMS message: {0}", payload);
            Map<String, Object> map = parseMessage(payload);
            procesarMensaje(map);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error procesando mensaje JMS: " + payload, ex);
            if (ex instanceof InterruptedException) {
                // restore interrupted status so higher-level handlers/threads know about it
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException(ex);
        }
    }


    private Map<String, Object> parseMessage(String message) {
        Map<String, Object> map = new java.util.HashMap<>();
        if (message == null) return map;

        try {
            Pattern actionPattern = Pattern.compile("\"action\"\\s*:\\s*\"([^\\\"]+)\"", Pattern.CASE_INSENSITIVE);
            Matcher m = actionPattern.matcher(message);
            if (m.find()) {
                map.put("action", m.group(1));
            }
        } catch (Exception e) {
            logger.log(Level.FINE, "Error parsing 'action' from message: " + message, e);
        }

        try {
            // match id_clinica as string or number: "id_clinica": "..."  or "id_clinica":  ...
            Pattern idPattern = Pattern.compile("\"id_clinica\"\\s*:\\s*\"?([^\\\"]+?)\"?(?:,|})");
            Matcher m = idPattern.matcher(message);
            if (m.find()) {
                map.put("id", m.group(1));
                return map;
            }
        } catch (Exception e) {
            logger.log(Level.FINE, "Error parsing 'id_clinica' from message: " + message, e);
        }

        try {
            Pattern idPattern2 = Pattern.compile("\"id\"\\s*:\\s*\"?([^\\\"]+?)\"?(?:,|})");
            Matcher m2 = idPattern2.matcher(message);
            if (m2.find()) {
                map.put("id", m2.group(1));
                return map;
            }
        } catch (Exception e) {
            logger.log(Level.FINE, "Error parsing 'id' from message: " + message, e);
        }

        try {
            // fallback: any quoted token or digits
            Pattern any = Pattern.compile("\"([a-fA-F0-9-]{8,})\"|([0-9]+)");
            Matcher md = any.matcher(message);
            if (md.find()) {
                if (md.group(1) != null) {
                    map.put("id", md.group(1));
                } else {
                    map.put("id", md.group(2));
                }
            }
        } catch (Exception e) {
            logger.log(Level.FINE, "Error extracting fallback id from message: " + message, e);
        }

        return map;
    }

    private void procesarMensaje(Map<String, Object> map) throws InterruptedException {
        if (map == null || !map.containsKey("id")) {
            logger.log(Level.WARNING, "Mensaje inválido o sin id: {0}", map);
            return;
        }

        String idStr = String.valueOf(map.get("id"));
        Long id = null;
        try {
            id = Long.parseLong(idStr);
        } catch (NumberFormatException ex) {
            logger.log(Level.FINE, "No se pudo parsear el id a Long: {0}", idStr);
            // leave id as null so the method logs a warning later
        }

        String action = map.containsKey("action") ? String.valueOf(map.get("action")) : "alta";

        if (id == null) {
            logger.log(Level.WARNING, "Parsed id is not numeric: {0}", map.get("id"));
            return;
        }

        var nodo = fetchNodoOrLog(id);
        if (nodo == null) {
            return;
        }

        logger.log(Level.INFO, "[MDB] Iniciando integración REST con nodo: {0} action={1}", new Object[] { nodo.getNodoPerifericoUrlBase(), action });

        performIntegrationWithRetries(nodo, action);
    }

    private uy.edu.tse.hcen.model.NodoPeriferico fetchNodoOrLog(Long id) {
        var nodo = repo.find(id);
        if (nodo == null) {
            logger.log(Level.WARNING, "Nodo no encontrado: {0}", id);
            try {
                var all = repo.findAll();
                logger.log(Level.WARNING, "Repo findAll size={0}", all == null ? 0 : all.size());
                if (all != null) {
                    for (var n : all) {
                        logger.warning(" - existing nodo id=" + n.getId() + " RUT=" + n.getRUT() + " estado=" + n.getEstado());
                    }
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Error leyendo todos los nodos desde repo for diagnostic", ex);
            }
        }
        return nodo;
    }

    private void performIntegrationWithRetries(NodoPeriferico nodo, String action) throws InterruptedException {
        int maxReintentos = 3;

        for (int intentoActual = 1; intentoActual <= maxReintentos; intentoActual++) {
            try {
                boolean ok = sendRequest(nodo, action);

                if (ok) {
                    handleSuccess(nodo, action);
                    return;
                }

                if (intentoActual < maxReintentos) {
                    Thread.sleep(2000L * intentoActual);
                } else {
                    break;
                }
            } catch (RuntimeException ex) {
                logger.log(Level.WARNING, "Intento " + intentoActual + " falló para nodo " + nodo.getId(), ex);
                if (intentoActual >= maxReintentos) {
                    throw ex; // definitive failure -> let container handle redelivery
                }
                Thread.sleep(2000L * intentoActual);
            }
        }

        throw new RuntimeException("Falló después de " + maxReintentos + " intentos para id " + nodo.getId());
    }

    private boolean sendRequest(NodoPeriferico nodo, String action) {
        if ("delete".equalsIgnoreCase(action)) {
            return httpClient.enviarBaja(nodo);
        }
        return httpClient.enviarConfiguracionInicial(nodo);
    }

    private void handleSuccess(NodoPeriferico nodo, String action) {
        if (!"delete".equalsIgnoreCase(action)) {
            nodo.setEstado(uy.edu.tse.hcen.model.enums.EstadoNodoPeriferico.ACTIVO);
            repo.update(nodo);
            logger.info("Nodo " + nodo.getId() + " activado exitosamente");
        } else {
            logger.info("Nodo " + nodo.getId() + " delete accepted by periferico");
        }
    }
}
