package org.shark.melian.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration management for MELIAN MCP Server.
 * Loads configuration from environment variables and properties files.
 */
public class MelianConfig {
    
    private final Properties properties;
    
    public MelianConfig() {
        this.properties = new Properties();
        loadProperties();
    }
    
    private void loadProperties() {
        // Load from application.properties if exists
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                properties.load(is);
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load application.properties: " + e.getMessage());
        }
        
        // Override with environment variables
        loadFromEnvironment();
    }
    
    private void loadFromEnvironment() {
        // Database configuration
        setPropertyFromEnv("db.url", "DB_URL", "jdbc:h2:mem:melian;DB_CLOSE_DELAY=-1");
        setPropertyFromEnv("db.username", "DB_USERNAME", "sa");
        setPropertyFromEnv("db.password", "DB_PASSWORD", "");
        setPropertyFromEnv("db.driver", "DB_DRIVER", "org.h2.Driver");
        
        // MongoDB configuration
        setPropertyFromEnv("mongodb.uri", "MONGODB_URI", "mongodb://localhost:27017");
        setPropertyFromEnv("mongodb.database", "MONGODB_DATABASE", "melian");
        
        // TMDB API configuration
        setPropertyFromEnv("tmdb.api-url", "TMDB_API_URL", "https://api.themoviedb.org/3");
        setPropertyFromEnv("tmdb.access-token", "TMDB_ACCESS_TOKEN", "");
        
        // MCP Server configuration
        setPropertyFromEnv("mcp.server.port", "MCP_SERVER_PORT", "3000");
        setPropertyFromEnv("mcp.server.host", "MCP_SERVER_HOST", "localhost");
    }
    
    private void setPropertyFromEnv(String propKey, String envKey, String defaultValue) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.trim().isEmpty()) {
            properties.setProperty(propKey, envValue);
        } else if (!properties.containsKey(propKey)) {
            properties.setProperty(propKey, defaultValue);
        }
    }
    
    public String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    public int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) return defaultValue;
        return Boolean.parseBoolean(value);
    }
}