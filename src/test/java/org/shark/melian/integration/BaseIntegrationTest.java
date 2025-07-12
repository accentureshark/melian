package org.shark.melian.integration;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests with TestContainers.
 * Provides MySQL and MongoDB containers for testing.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("integration")
public abstract class BaseIntegrationTest {
    
    @Container
    protected static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("melian_test")
            .withUsername("test")
            .withPassword("test");
    
    @Container
    protected static final MongoDBContainer mongodb = new MongoDBContainer("mongo:7.0");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Configure MySQL
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        
        // Configure MongoDB
        registry.add("spring.data.mongodb.uri", mongodb::getConnectionString);
        
        // Configure JPA for MySQL
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.jpa.show-sql", () -> "false");
    }
    
    @BeforeAll
    static void beforeAll() {
        // Ensure containers are running
        mysql.start();
        mongodb.start();
    }
}