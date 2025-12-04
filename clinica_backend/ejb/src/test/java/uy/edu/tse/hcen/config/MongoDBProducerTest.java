package uy.edu.tse.hcen.config;

import com.mongodb.client.MongoClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MongoDBProducer Tests")
class MongoDBProducerTest {

    @Test
    @DisplayName("Producer debe tener constructor público")
    void producer_shouldHavePublicConstructor() {
        MongoDBProducer producer = new MongoDBProducer();
        assertNotNull(producer);
    }
}
