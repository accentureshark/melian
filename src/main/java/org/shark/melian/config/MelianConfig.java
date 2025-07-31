package org.shark.melian.config;

import java.util.Properties;

/**
 * Configuration management for MELIAN MCP Server.
 * Loads configuration from environment variables.
 * Spring Boot maneja application.yml automáticamente.
 */
public class MelianConfig {

    private final Properties properties;

    public MelianConfig() {
        this.properties = new Properties();
        loadProperties();
    }

    private void loadProperties() {
        // Spring Boot maneja application.yml automáticamente
        // Esta clase solo maneja variables de entorno adicionales
        loadFromEnvironment();
    }

    private void loadFromEnvironment() {
        // Database configuration - MySQL (eliminar H2)
        setPropertyFromEnv("db.url", "DB_URL", "jdbc:mysql://mysql-sakila:3306/sakila");
        setPropertyFromEnv("db.username", "DB_USERNAME", "sakila");
        setPropertyFromEnv("db.password", "DB_PASSWORD", "sakila");
        setPropertyFromEnv("db.driver", "DB_DRIVER", "com.mysql.cj.jdbc.Driver");

        // MongoDB configuration - usar servicio Docker
        setPropertyFromEnv("mongodb.uri", "MONGODB_URI", "mongodb://root:example@mongo:27017/melian_movies?authSource=admin");
        setPropertyFromEnv("mongodb.database", "MONGODB_DATABASE", "melian_movies");

        // TMDB API configuration
        setPropertyFromEnv("tmdb.api-url", "TMDB_API_URL", "https://api.themoviedb.org/3");
        // Token sin valor por defecto; debe proveerse via variable de entorno
        setPropertyFromEnv("tmdb.access-token", "TMDB_ACCESS_TOKEN", null);

        // MCP Server configuration - usar 0.0.0.0 para Docker
        setPropertyFromEnv("mcp.server.port", "MCP_SERVER_PORT", "3000");
        setPropertyFromEnv("mcp.server.host", "MCP_SERVER_HOST", "0.0.0.0");
        setPropertyFromEnv("mcp.server.http.enabled", "MCP_SERVER_HTTP_ENABLED", "false");
    }

    private void setPropertyFromEnv(String propKey, String envKey, String defaultValue) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.trim().isEmpty()) {
            properties.setProperty(propKey, envValue);
        } else if (defaultValue != null && !properties.containsKey(propKey)) {
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