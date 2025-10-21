package uy.edu.tse.hcen.messaging;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Stateless;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.Queue;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.MessageProperties;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class RabbitSender implements RabbitSenderLocal {

    private static final Logger logger = Logger.getLogger(RabbitSender.class.getName());

    private jakarta.jms.ConnectionFactory connectionFactory;

    @PostConstruct
    public void init() {
        // try to obtain a server-provided ConnectionFactory via JNDI reflectively
        try {
            Class<?> icClass = Class.forName("jakarta.naming.InitialContext");
            Object ic = icClass.getDeclaredConstructor().newInstance();
            Method lookupMethod = icClass.getMethod("lookup", String.class);
            Object looked = lookupMethod.invoke(ic, "java:/ConnectionFactory");
            if (looked instanceof jakarta.jms.ConnectionFactory) {
                connectionFactory = (jakarta.jms.ConnectionFactory) looked;
                logger.info("Using ConnectionFactory from JNDI java:/ConnectionFactory (EJB)");
                return;
            }
        } catch (Exception e) {
            logger.log(Level.FINE, "No JNDI ConnectionFactory found in EJB context: " + e.getMessage());
        }

        // Fallback: try to create a RabbitMQ JMS RMQConnectionFactory reflectively
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

            if (rmq instanceof jakarta.jms.ConnectionFactory) {
                connectionFactory = (jakarta.jms.ConnectionFactory) rmq;
                logger.info("Created RMQConnectionFactory via reflection (EJB)");
                return;
            }
        } catch (ClassNotFoundException cnfe) {
            logger.log(Level.WARNING, "RabbitMQ JMS client library not found on EJB classpath: {0}", cnfe.getMessage());
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error creating RMQConnectionFactory in EJB", ex);
        }

        logger.warning("No ConnectionFactory available in RabbitSender; send() will fail until configured.");
    }

    /**
     * Ensure there is a ConnectionFactory available. This method is safe to call multiple times
     * and will attempt JNDI first, then several reflective strategies to create a
     * com.rabbitmq.jms.client.RMQConnectionFactory (constructor(String uri), setters).
     */
    private synchronized void ensureConnectionFactory() {
        if (connectionFactory != null) return;

        // try JNDI again (some containers initialize JNDI later)
        try {
            Class<?> icClass = Class.forName("jakarta.naming.InitialContext");
            Object ic = icClass.getDeclaredConstructor().newInstance();
            Method lookupMethod = icClass.getMethod("lookup", String.class);
            Object looked = lookupMethod.invoke(ic, "java:/ConnectionFactory");
            if (looked instanceof jakarta.jms.ConnectionFactory) {
                connectionFactory = (jakarta.jms.ConnectionFactory) looked;
                logger.info("Using ConnectionFactory from JNDI java:/ConnectionFactory (EJB) [late]");
                return;
            }
        } catch (Exception e) {
            logger.log(Level.FINE, "Late JNDI lookup for java:/ConnectionFactory failed: " + e.getMessage());
        }

        try {
            String uri = System.getenv().getOrDefault("RABBITMQ_URI", "amqp://localhost:5672");
            String user = System.getenv("RABBITMQ_USER");
            String pass = System.getenv("RABBITMQ_PASSWORD");

            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            logger.info("Attempting late RMQ load with context classloader: " + cl);
            if (cl != null) {
                java.net.URL r = cl.getResource("com/rabbitmq/jms/client/RMQConnectionFactory.class");
                logger.info("Resource lookup for RMQConnectionFactory.class via context CL -> " + r);
            }

            Class<?> rmqClass = Class.forName("com.rabbitmq.jms.client.RMQConnectionFactory", true, cl);

            Object rmq = null;
            try {
                rmq = rmqClass.getConstructor(String.class).newInstance(uri);
            } catch (NoSuchMethodException ns) {
            }

            if (rmq == null) {
                rmq = rmqClass.getDeclaredConstructor().newInstance();
                try { Method m = rmqClass.getMethod("setUri", String.class); m.invoke(rmq, uri); } catch (NoSuchMethodException ignore) {}
            }

            try { Method m = rmqClass.getMethod("setUsername", String.class); if (user != null) m.invoke(rmq, user); } catch (NoSuchMethodException ignore) {}
            try { Method m = rmqClass.getMethod("setPassword", String.class); if (pass != null) m.invoke(rmq, pass); } catch (NoSuchMethodException ignore) {}

            if (rmq instanceof jakarta.jms.ConnectionFactory) {
                connectionFactory = (jakarta.jms.ConnectionFactory) rmq;
                logger.info("Created RMQConnectionFactory via reflection (EJB) [late]");
                return;
            } else {
                logger.warning("RMQConnectionFactory instance is not a JMS ConnectionFactory [late]");
            }
        } catch (ClassNotFoundException cnfe) {
            logger.log(Level.WARNING, "RabbitMQ JMS client library not found on EJB classpath (late): {0}", cnfe.getMessage());
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error creating RMQConnectionFactory in EJB [late]", ex);
        }
    }

    @Override
    public void send(String queueName, String text) {
        // ensure a connection factory exists (try late-binding/fallback strategies)
        ensureConnectionFactory();
        if (connectionFactory == null) {
            logger.warning("No JMS ConnectionFactory configured for RabbitSender, attempting AMQP fallback.");
            try {
                sendViaAmqp(queueName, text);
                return;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "AMQP fallback failed in RabbitSender", e);
                throw new IllegalStateException("No ConnectionFactory configured for RabbitSender", e);
            }
        }
        try (JMSContext ctx = connectionFactory.createContext()) {
            Queue q = ctx.createQueue(queueName);
            JMSProducer p = ctx.createProducer();
            p.send(q, text);
            logger.info("EJB sent message to " + queueName + ": " + text);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error sending JMS message from EJB", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendToExchange(String exchange, String routingKey, String text) {
        // Try JMS publish if ConnectionFactory available
        ensureConnectionFactory();
        if (connectionFactory != null) {
            try (JMSContext ctx = connectionFactory.createContext()) {
               
                String destName = exchange + ":" + routingKey;
                Queue q = ctx.createQueue(destName);
                JMSProducer p = ctx.createProducer();
                p.send(q, text);
                logger.info("EJB sent message to exchange " + exchange + " routingKey " + routingKey);
                return;
            } catch (Exception e) {
                logger.log(Level.WARNING, "JMS exchange publish failed, falling back to AMQP: " + e.getMessage());
            }
        }

        // AMQP fallback: publish to given exchange with routing key
        try {
            com.rabbitmq.client.ConnectionFactory factory = new com.rabbitmq.client.ConnectionFactory();
            factory.setHost(RabbitConfig.host());
            factory.setPort(RabbitConfig.port());
            // enable automatic recovery and heartbeat
            factory.setAutomaticRecoveryEnabled(true);
            factory.setNetworkRecoveryInterval(5000);
            factory.setRequestedHeartbeat(30);
            String user = RabbitConfig.user();
            String pass = RabbitConfig.pass();
            if (user != null) factory.setUsername(user);
            if (pass != null) factory.setPassword(pass);

            try (Connection conn = factory.newConnection(); Channel ch = conn.createChannel()) {
                ch.exchangeDeclare(exchange, "topic", true);
                // publisher confirms
                ch.confirmSelect();
                ch.basicPublish(exchange, routingKey, MessageProperties.PERSISTENT_TEXT_PLAIN, text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                ch.waitForConfirmsOrDie(5000);
                logger.info("AMQP published message to exchange " + exchange + " rk=" + routingKey + ": " + text);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "AMQP publish to exchange failed", e);
            throw new RuntimeException(e);
        }
    }

    //  publish directly using RabbitMQ Java client (AMQP) when JMS provider not available
    private void sendViaAmqp(String queueName, String text) throws Exception {
        String host = RabbitConfig.host();
        int port = RabbitConfig.port();
        String user = RabbitConfig.user();
        String pass = RabbitConfig.pass();

        com.rabbitmq.client.ConnectionFactory factory = new com.rabbitmq.client.ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        if (user != null) factory.setUsername(user);
        if (pass != null) factory.setPassword(pass);
    // enable automatic recovery and heartbeat
    factory.setAutomaticRecoveryEnabled(true);
    factory.setNetworkRecoveryInterval(5000);
    factory.setRequestedHeartbeat(30);

        try (Connection conn = factory.newConnection(); Channel ch = conn.createChannel()) {
            // Ensure queue exists (durable)
            ch.queueDeclare(queueName, true, false, false, null);
            byte[] body = text.getBytes(StandardCharsets.UTF_8);
            // publisher confirms
            ch.confirmSelect();
            // Publish to default exchange with routing key = queueName as persistent
            ch.basicPublish("", queueName, MessageProperties.PERSISTENT_TEXT_PLAIN, body);
            ch.waitForConfirmsOrDie(5000);
            logger.info("AMQP published message to queue " + queueName + ": " + text);
        }
    }
}
