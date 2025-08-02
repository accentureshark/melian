package org.shark.melian;

import org.shark.melian.client.TMDBApiClientPure;
import org.shark.melian.config.DatabaseConfig;
import org.shark.melian.config.MelianConfig;
import org.shark.melian.config.MongoConfig;
import org.shark.melian.mcp.PureMcpServer;
import org.shark.melian.service.MongoMovieChunkServicePure;
import org.shark.melian.service.SqlMovieChunkServicePure;
import org.shark.melian.service.TMDBServicePure;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot application that exposes MCP endpoints with Swagger documentation.
 */
@SpringBootApplication
public class MelianApplication {

    public static void main(String[] args) {
        SpringApplication.run(MelianApplication.class, args);
    }

    @Bean
    public MelianConfig melianConfig() {
        return new MelianConfig();
    }

    @Bean
    public DatabaseConfig databaseConfig(MelianConfig config) {
        return new DatabaseConfig(config);
    }

    @Bean
    public MongoConfig mongoConfig(MelianConfig config) {
        return new MongoConfig(config);
    }

    @Bean
    public TMDBApiClientPure tmdbApiClientPure(MelianConfig config) {
        return new TMDBApiClientPure(config);
    }

    @Bean
    public TMDBServicePure tmdbServicePure(TMDBApiClientPure client) {
        return new TMDBServicePure(client);
    }

    @Bean
    public SqlMovieChunkServicePure sqlMovieChunkServicePure(DatabaseConfig databaseConfig,
                                                             TMDBServicePure tmdbService) {
        return new SqlMovieChunkServicePure(databaseConfig, tmdbService);
    }

    @Bean
    public MongoMovieChunkServicePure mongoMovieChunkServicePure(MongoConfig mongoConfig,
                                                                 TMDBServicePure tmdbService) {
        return new MongoMovieChunkServicePure(mongoConfig, tmdbService);
    }

    @Bean
    public PureMcpServer pureMcpServer(TMDBServicePure tmdbService,
                                       SqlMovieChunkServicePure sqlService,
                                       MongoMovieChunkServicePure mongoService) {
        return new PureMcpServer(tmdbService, sqlService, mongoService);
    }
}

