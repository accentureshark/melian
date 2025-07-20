package org.shark.melian.config;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

/**
 * MongoDB configuration for MELIAN MCP Server.
 * Provides MongoDB client and database access.
 */
public class MongoConfig {
    
    private final MongoClient mongoClient;
    private final MongoDatabase database;
    
    public MongoConfig(MelianConfig config) {
        String uri = config.getProperty("mongodb.uri");
        String databaseName = config.getProperty("mongodb.database");
        
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new com.mongodb.ConnectionString(uri))
                .build();
        
        this.mongoClient = MongoClients.create(settings);
        this.database = mongoClient.getDatabase(databaseName);
    }
    
    public MongoClient getMongoClient() {
        return mongoClient;
    }
    
    public MongoDatabase getDatabase() {
        return database;
    }
    
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}