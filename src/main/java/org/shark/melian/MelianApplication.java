package org.shark.melian;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

/**
 * Spring Boot application that exposes MCP endpoints with Swagger documentation.
 * Refactored to use Spring Boot best practices for datasources and REST APIs.
 */
@SpringBootApplication(exclude = {JpaRepositoriesAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class MelianApplication {

    public static void main(String[] args) {
        SpringApplication.run(MelianApplication.class, args);
    }
}

