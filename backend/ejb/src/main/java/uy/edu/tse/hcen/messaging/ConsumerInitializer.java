package uy.edu.tse.hcen.messaging;

import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

@Singleton
@Startup
public class ConsumerInitializer {
    
    @Inject
    private NodoRegistrationConsumer consumer;

    @jakarta.annotation.PostConstruct
    public void init() {
        // Ejecutar el consumidor en un nuevo hilo para no bloquear el inicio del servidor
        new Thread(() -> {
            consumer.startConsuming();
        }).start();
    }
}
