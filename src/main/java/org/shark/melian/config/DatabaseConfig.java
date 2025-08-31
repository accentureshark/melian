package org.shark.melian.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Database configuration that supports environment variable overrides
 */
@Configuration
public class DatabaseConfig {

    @Value("${DB_URL:#{null}}")
    private String envDbUrl;

    @Value("${DB_USERNAME:#{null}}")
    private String envDbUsername;

    @Value("${DB_PASSWORD:#{null}}")
    private String envDbPassword;

    @Value("${DB_DRIVER:#{null}}")
    private String envDbDriver;

    @Value("${spring.datasource.url}")
    private String defaultDbUrl;

    @Value("${spring.datasource.username}")
    private String defaultDbUsername;

    @Value("${spring.datasource.password}")
    private String defaultDbPassword;

    @Value("${spring.datasource.driver-class-name}")
    private String defaultDbDriver;

    @Bean
    @Primary
    public DataSource dataSource() {
        String dbUrl = envDbUrl != null ? envDbUrl : defaultDbUrl;
        String dbUsername = envDbUsername != null ? envDbUsername : defaultDbUsername;
        String dbPassword = envDbPassword != null ? envDbPassword : defaultDbPassword;
        String dbDriver = envDbDriver != null ? envDbDriver : defaultDbDriver;

        return DataSourceBuilder.create()
                .url(dbUrl)
                .username(dbUsername)
                .password(dbPassword)
                .driverClassName(dbDriver)
                .build();
    }
}