package uy.edu.tse.hcen.messaging;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Queue;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
public class RabbitClientConsumer implements MessageListener {

    private static final Logger logger = Logger.getLogger(RabbitClientConsumer.class.getName());

    private ConnectionFactory connectionFactory;
    private Connection connection;
    private jakarta.jms.MessageConsumer consumer;

    @PostConstruct
    public void init() {
        try {

            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class<?> rmqClass = Class.forName("com.rabbitmq.jms.client.RMQConnectionFactory", true, cl);
            Object rmq = rmqClass.getDeclaredConstructor().newInstance();
            try { Method m = rmqClass.getMethod("setUri", String.class); m.invoke(rmq, System.getenv().getOrDefault("RABBITMQ_URI", "amqp://localhost:5672")); } catch (NoSuchMethodException ignore) {}
            try { Method m = rmqClass.getMethod("setUsername", String.class); String u = System.getenv("RABBITMQ_USER"); if (u!=null) m.invoke(rmq, u); } catch (NoSuchMethodException ignore) {}
            try { Method m = rmqClass.getMethod("setPassword", String.class); String p = System.getenv("RABBITMQ_PASSWORD"); if (p!=null) m.invoke(rmq, p); } catch (NoSuchMethodException ignore) {}

            if (rmq instanceof ConnectionFactory) {
                connectionFactory = (ConnectionFactory) rmq;
                logger.info("RabbitClientConsumer: using RMQConnectionFactory (client)");
            } else {
                logger.warning("RabbitClientConsumer: RMQConnectionFactory is not a ConnectionFactory");
                return;
            }

            // create a JMS connection and consumer
            connection = connectionFactory.createConnection();
            Session sess = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue q = sess.createQueue("RabbitQueue");
            consumer = sess.createConsumer(q);
            consumer.setMessageListener(this);
            connection.start();
            logger.info("RabbitClientConsumer started and listening on jms.queue.RabbitQueue");
        } catch (ClassNotFoundException cnf) {
            logger.log(Level.WARNING, "Rabbit JMS client not available in classpath: {0}", cnf.getMessage());
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error initializing RabbitClientConsumer", ex);
        }
    }

    @PreDestroy
    public void stop() {
        try {
            if (connection != null) connection.close();
        } catch (JMSException e) {
            logger.log(Level.WARNING, "Error closing JMS connection", e);
        }
    }

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                logger.info("RabbitClientConsumer received: " + ((TextMessage) message).getText());
            } else {
                logger.info("RabbitClientConsumer received non-text message: " + message);
            }
        } catch (JMSException e) {
            logger.log(Level.SEVERE, "Error processing message", e);
        }
    }
}
