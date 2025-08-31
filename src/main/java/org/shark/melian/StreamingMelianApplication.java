package org.shark.melian;

import org.shark.melian.assistant.MelianAiAssistant;
import org.shark.melian.client.TMDBApiClientPure;
import org.shark.melian.config.MelianProperties;
import org.shark.melian.rest.StreamingChatController;
import org.shark.melian.tools.SimpleMovieTools;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * Minimal Spring Boot application focused on streaming functionality
 */
@SpringBootApplication
@ComponentScan(
    basePackageClasses = {
        MelianAiAssistant.class, 
        StreamingChatController.class, 
        SimpleMovieTools.class,
        MelianProperties.class,
        TMDBApiClientPure.class
    },
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "org\\.shark\\.melian\\.rest\\.McpController"
        ),
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "org\\.shark\\.melian\\.config\\.DatabaseConfig"
        ),
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "org\\.shark\\.melian\\.health\\..*"
        )
    }
)
public class StreamingMelianApplication {

    public static void main(String[] args) {
        SpringApplication.run(StreamingMelianApplication.class, args);
    }
}