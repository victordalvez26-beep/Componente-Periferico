package uy.edu.tse.hcen.messaging;

import jakarta.ejb.Local;

@Local
public interface RabbitSenderLocal {
    void send(String queueName, String text);
    /**
     * Send a message to a RabbitMQ exchange with a routing key. Implementations
     * should ensure the exchange exists (durable) and publish the payload bytes.
     */
    void sendToExchange(String exchange, String routingKey, String text);
}
