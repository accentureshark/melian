package org.shark.melian;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Spring Boot application that exposes MCP endpoints with Swagger documentation.
 * Refactored to use Spring Boot best practices for datasources and REST APIs.
 */
@SpringBootApplication
@EnableConfigurationProperties
@EnableJpaRepositories(basePackages = "org.shark.melian.repository.jpa")
@EnableMongoRepositories(basePackages = "org.shark.melian.repository.mongo")
@EnableMongoAuditing
@EnableTransactionManagement
public class MelianApplication {

    public static void main(String[] args) {
        SpringApplication.run(MelianApplication.class, args);
    }
}

