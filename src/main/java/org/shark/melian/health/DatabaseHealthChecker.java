package org.shark.melian.health;


import com.mongodb.client.MongoClient;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseHealthChecker implements ApplicationRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private MongoClient mongoClient;

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        // Chequeo MySQL
        try {
            jdbcTemplate.execute("SELECT 1");
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo conectar a MySQL: " + e.getMessage(), e);
        }

        // Chequeo MongoDB
        if (mongoClient != null) {
            try {
                mongoClient.getDatabase("melian_movies")
                        .runCommand(new Document("ping", 1));
            } catch (Exception e) {
                throw new IllegalStateException("No se pudo conectar a MongoDB: " + e.getMessage(), e);
            }
        }
    }
}