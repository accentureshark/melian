package org.shark.melian.health;


import com.mongodb.client.MongoClient;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseHealthChecker implements ApplicationRunner {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private MongoClient mongoClient;

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        // Chequeo MySQL/H2 - SQL Database
        if (jdbcTemplate != null) {
            try {
                jdbcTemplate.execute("SELECT 1");
                System.out.println("✅ SQL Database connection: OK");
            } catch (Exception e) {
                System.err.println("❌ SQL Database connection failed: " + e.getMessage());
                // Don't fail startup for SQL issues when using H2
            }
        } else {
            System.out.println("ℹ️  SQL Database not configured");
        }

        // Chequeo MongoDB - opcional
        if (mongoClient != null) {
            try {
                mongoClient.getDatabase("melian_movies")
                        .runCommand(new Document("ping", 1));
                System.out.println("✅ MongoDB connection: OK");
            } catch (Exception e) {
                System.err.println("⚠️  MongoDB connection failed (optional): " + e.getMessage());
                // Don't fail startup for MongoDB issues - it's optional
            }
        } else {
            System.out.println("ℹ️  MongoDB client not available - running without MongoDB");
        }
    }
}