package org.shark.melian;

import org.shark.melian.service.MovieToolService;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class MelianApplication {
    public static void main(String[] args) {
        SpringApplication.run(MelianApplication.class, args);
    }

    @Bean
    public List<ToolCallback> melianTools(MovieToolService movieToolService) {
        return List.of(ToolCallbacks.from(movieToolService));
    }
}
