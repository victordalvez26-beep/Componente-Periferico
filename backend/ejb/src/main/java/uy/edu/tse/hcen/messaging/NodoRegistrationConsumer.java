package uy.edu.tse.hcen.messaging;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import jakarta.inject.Inject;
import jakarta.ejb.Singleton;
import uy.edu.tse.hcen.repository.NodoPerifericoRepository;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import uy.edu.tse.hcen.utils.NodoPerifericoHttpClient;

@Singleton
public class NodoRegistrationConsumer {
    
    private static final Logger logger = Logger.getLogger(NodoRegistrationConsumer.class.getName());
    private static final String QUEUE_NAME = "nodo_config_queue";
    private static final String EXCHANGE_NAME = "clinica_config_exchange";

    @Inject
    private NodoPerifericoRepository repo; 
    
    @Inject
    private NodoPerifericoHttpClient httpClient;

    public void startConsuming() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(RabbitConfig.host());
            factory.setPort(RabbitConfig.port());
            String u = RabbitConfig.user();
            String p = RabbitConfig.pass();
            // Security: require credentials explicitely
            if (u == null || u.isEmpty()) throw new IllegalStateException("RABBITMQ_USER no configurado");
            if (p == null || p.isEmpty()) throw new IllegalStateException("RABBITMQ_PASSWORD no configurado");
            factory.setUsername(u);
            factory.setPassword(p);

            // Recovery and heartbeat
            factory.setAutomaticRecoveryEnabled(true);
            factory.setNetworkRecoveryInterval(5000);
            factory.setRequestedHeartbeat(30);

            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            // Declare exchange and queue with DLX args
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC, true);
            java.util.Map<String, Object> args = new java.util.HashMap<>();
            args.put("x-dead-letter-exchange", "clinica_dlx");
            args.put("x-dead-letter-routing-key", "failed.alta");
            // Optional TTL: messages expire after 24h in the queue
            args.put("x-message-ttl", 86400000);

            channel.queueDeclare(QUEUE_NAME, true, false, false, args);
            channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "alta.clinica");

            // Declare DLX and DLQ
            channel.exchangeDeclare("clinica_dlx", BuiltinExchangeType.DIRECT, true);
            channel.queueDeclare("nodo_dlq", true, false, false, null);
            channel.queueBind("nodo_dlq", "clinica_dlx", "failed.alta");

            // Prefetch: process one message at a time
            channel.basicQos(1);

            logger.info(" [*] Esperando mensajes de RabbitMQ. Para salir, presione CTRL+C");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                logger.info(" [x] Recibido: '" + message + "'");
                boolean procesadoExitosamente = false;
                try {
                    procesarMensaje(message);
                    procesadoExitosamente = true;
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error procesando mensaje", e);
                    try {
                        // nack -> no requeue so message goes to DLQ
                        channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
                    } catch (Exception nackEx) {
                        logger.log(Level.SEVERE, "Error enviando NACK", nackEx);
                    }
                    return;
                }

                if (procesadoExitosamente) {
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                }
            };

            channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {});

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Fallo al iniciar el consumidor de RabbitMQ. Verifique el servidor.", e);
        }
    }

    @jakarta.annotation.PostConstruct
    private void init() {

        startConsuming();
    }
    
    private void procesarMensaje(String payload) {
        int maxReintentos = 3;
        int intentoActual = 0;
        try {
            Long id = Long.parseLong(payload.replaceAll("[^0-9]", ""));
            var nodo = repo.find(id);

            if (nodo == null) {
                logger.warning("Nodo no encontrado: " + id);
                return; // ACK - invalid message
            }

            logger.info("Iniciando integración REST con nodo: " + nodo.getNodoPerifericoUrlBase());

            while (intentoActual < maxReintentos) {
                try {
                    boolean ok = httpClient.enviarConfiguracionInicial(nodo);
                    if (ok) {
                        nodo.setEstado(uy.edu.tse.hcen.model.enums.EstadoNodoPeriferico.ACTIVO);
                        repo.update(nodo);
                        logger.info("Nodo " + id + " activado exitosamente");
                        return; // success
                    } else {
                        intentoActual++;
                        if (intentoActual >= maxReintentos) break;
                        Thread.sleep(2000L * intentoActual); // backoff
                    }
                } catch (Exception ex) {
                    intentoActual++;
                    logger.log(Level.WARNING, "Intento " + intentoActual + " falló para nodo " + id, ex);
                    if (intentoActual >= maxReintentos) {
                        throw ex; // definitive failure
                    }
                    Thread.sleep(2000L * intentoActual);
                }
            }

            // If we reach here, it failed after retries
            throw new RuntimeException("Falló después de " + maxReintentos + " intentos");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error irrecuperable: " + payload, e);
            // send to DLQ manually if needed (the consumer handler will also NACK)
            try {
                enviarADLQ(payload, e.getMessage());
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Fallo al enviar a DLQ", ex);
            }
        }
    }

    private void enviarADLQ(String payload, String reason) {
        try {
            com.rabbitmq.client.ConnectionFactory cf = new com.rabbitmq.client.ConnectionFactory();
            cf.setHost(RabbitConfig.host());
            cf.setPort(RabbitConfig.port());
            cf.setAutomaticRecoveryEnabled(true);
            cf.setNetworkRecoveryInterval(5000);
            cf.setRequestedHeartbeat(30);
            String u = RabbitConfig.user();
            String p = RabbitConfig.pass();
            if (u != null) cf.setUsername(u);
            if (p != null) cf.setPassword(p);
            try (Connection conn = cf.newConnection(); Channel ch = conn.createChannel()) {
                ch.exchangeDeclare("clinica_dlx", "direct", true);
                ch.queueDeclare("nodo_dlq", true, false, false, null);
                ch.basicPublish("clinica_dlx", "failed.alta", com.rabbitmq.client.MessageProperties.PERSISTENT_TEXT_PLAIN, payload.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "No se pudo enviar el mensaje a DLQ", e);
        }
    }
}
