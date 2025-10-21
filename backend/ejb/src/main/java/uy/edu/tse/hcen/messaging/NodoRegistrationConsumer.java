package uy.edu.tse.hcen.messaging;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
// use fully-qualified RabbitMQ ConnectionFactory where needed to avoid collision with JMS ConnectionFactory
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import jakarta.inject.Inject;
import jakarta.ejb.Singleton;
import jakarta.annotation.Resource;
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

    @Resource(lookup = "java:/ConnectionFactory")
    private jakarta.jms.ConnectionFactory jmsConnectionFactory;

    @Resource(lookup = "java:/jms/queue/nodoConfigQueue")
    private jakarta.jms.Queue jmsQueue;

    @jakarta.annotation.PostConstruct
    private void init() {
        startConsuming();
    }

    public void startConsuming() {
        try {
            com.rabbitmq.client.ConnectionFactory factory = new com.rabbitmq.client.ConnectionFactory();
            factory.setHost(RabbitConfig.host());
            factory.setPort(RabbitConfig.port());
            String u = RabbitConfig.user();
            String p = RabbitConfig.pass();
            if (u == null || u.isEmpty()) throw new IllegalStateException("RABBITMQ_USER no configurado");
            if (p == null || p.isEmpty()) throw new IllegalStateException("RABBITMQ_PASSWORD no configurado");
            factory.setUsername(u);
            factory.setPassword(p);

            factory.setAutomaticRecoveryEnabled(true);
            factory.setNetworkRecoveryInterval(5000);
            factory.setRequestedHeartbeat(30);

            com.rabbitmq.client.Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC, true);
            java.util.Map<String, Object> args = new java.util.HashMap<>();
            args.put("x-dead-letter-exchange", "clinica_dlx");
            args.put("x-dead-letter-routing-key", "failed.alta");
            args.put("x-message-ttl", 86400000);

            channel.queueDeclare(QUEUE_NAME, true, false, false, args);
            channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "alta.clinica");

            channel.exchangeDeclare("clinica_dlx", BuiltinExchangeType.DIRECT, true);
            channel.queueDeclare("nodo_dlq", true, false, false, null);
            channel.queueBind("nodo_dlq", "clinica_dlx", "failed.alta");

            channel.basicQos(1);

            logger.info(" [*] Bridge AMQP->JMS: esperando mensajes de RabbitMQ. Para salir, presione CTRL+C");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                logger.info(" [x] Recibido (AMQP): '" + message + "'");

                try (jakarta.jms.JMSContext ctx = jmsConnectionFactory.createContext()) {
                    ctx.createProducer().send(jmsQueue, message);
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    logger.info("Forwarded message to JMS queue java:/jms/queue/nodoConfigQueue");
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to forward message to JMS queue", e);
                    try {
                        channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
                    } catch (Exception nackEx) {
                        logger.log(Level.SEVERE, "Error enviando NACK", nackEx);
                    }
                }
            };

            channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {});

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Fallo al iniciar el consumidor de RabbitMQ (bridge).", e);
        }
    }
    
    // The bridge only forwards AMQP messages into the container-managed JMS queue.
    // Message processing (HTTP integration and repository updates) is performed by a MDB
    // which consumes from java:/jms/queue/nodoConfigQueue. Keeping this class focused
    // avoids invoking EJB repositories from non-managed RabbitMQ client threads.
}
