package org.shark.melian;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Spring Boot application that exposes MCP endpoints with Swagger documentation.
 * Refactored to use Spring Boot best practices for datasources and REST APIs.
 */
@SpringBootApplication
@EntityScan(basePackages = "org.shark.melian.entity")
@EnableJpaRepositories(basePackages = "org.shark.melian.repository.jpa")
public class MelianApplication {

    public static void main(String[] args) {
        SpringApplication.run(MelianApplication.class, args);
    }
}

