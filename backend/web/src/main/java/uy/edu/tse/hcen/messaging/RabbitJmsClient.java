package uy.edu.tse.hcen.messaging;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.Queue;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class RabbitJmsClient {

    private static final Logger logger = Logger.getLogger(RabbitJmsClient.class.getName());

    private ConnectionFactory connectionFactory;

    @PostConstruct
    public void init() {
        try {
            Class<?> icClass = Class.forName("jakarta.naming.InitialContext");
            Object ic = icClass.getDeclaredConstructor().newInstance();
            java.lang.reflect.Method lookupMethod = icClass.getMethod("lookup", String.class);
            Object looked = lookupMethod.invoke(ic, "java:/ConnectionFactory");
            if (looked instanceof ConnectionFactory) {
                connectionFactory = (ConnectionFactory) looked;
                logger.info("Using ConnectionFactory from JNDI java:/ConnectionFactory");
                return;
            }
        } catch (Exception e) {
            logger.log(Level.FINE, "No JNDI ConnectionFactory java:/ConnectionFactory found, will try RabbitMQ client: " + e.getMessage());
        }

        try {
            String uri = System.getenv().getOrDefault("RABBITMQ_URI", "amqp://localhost:5672");
            String user = System.getenv("RABBITMQ_USER");
            String pass = System.getenv("RABBITMQ_PASSWORD");

            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class<?> rmqClass = Class.forName("com.rabbitmq.jms.client.RMQConnectionFactory", true, cl);
            Object rmq = rmqClass.getDeclaredConstructor().newInstance();

            try { Method m = rmqClass.getMethod("setUri", String.class); m.invoke(rmq, uri); } catch (NoSuchMethodException ignore) {}
            try { Method m = rmqClass.getMethod("setUsername", String.class); if (user != null) m.invoke(rmq, user); } catch (NoSuchMethodException ignore) {}
            try { Method m = rmqClass.getMethod("setPassword", String.class); if (pass != null) m.invoke(rmq, pass); } catch (NoSuchMethodException ignore) {}

            if (rmq instanceof ConnectionFactory) {
                connectionFactory = (ConnectionFactory) rmq;
                logger.info("Created RMQConnectionFactory via reflection (RabbitMQ JMS client)");
                return;
            } else {
                logger.warning("RMQConnectionFactory instance is not a JMS ConnectionFactory");
            }
        } catch (ClassNotFoundException cnfe) {
            logger.log(Level.WARNING, "RabbitMQ JMS client library not found on classpath: {0}", cnfe.getMessage());
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error creating RMQConnectionFactory", ex);
        }

        logger.warning("No ConnectionFactory available; RabbitJmsClient will be inactive until configured.");
    }

    public void sendTextToQueue(String queueName, String text) {
        if (connectionFactory == null) {
            throw new IllegalStateException("No ConnectionFactory configured for RabbitJmsClient");
        }
        try (JMSContext ctx = connectionFactory.createContext()) {
            Queue q = ctx.createQueue(queueName);
            JMSProducer p = ctx.createProducer();
            p.send(q, text);
            logger.info("Sent message to queue " + queueName + ": " + text);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error sending JMS message", e);
            throw new RuntimeException(e);
        }
    }
}
