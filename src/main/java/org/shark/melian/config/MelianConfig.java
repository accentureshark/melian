
package org.shark.melian.config;

import dev.langchain4j.model.embedding.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MelianConfig {

    @Bean
    public EmbeddingModel embeddingModel() {
        // Puedes parametrizar por properties/modelo
        return OllamaEmbeddingModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("nomic-embed-text")
                .build();
    }
}
