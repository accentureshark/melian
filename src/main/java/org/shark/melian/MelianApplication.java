package org.shark.melian;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application that exposes MCP endpoints with Swagger documentation.
 * Refactored to use Spring Boot best practices for datasources and REST APIs.
 */
@SpringBootApplication
public class MelianApplication {

    public static void main(String[] args) {
        SpringApplication.run(MelianApplication.class, args);
    }
}

