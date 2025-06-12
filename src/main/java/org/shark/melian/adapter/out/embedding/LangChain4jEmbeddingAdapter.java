// adapter/out/embedding/LangChain4jEmbeddingAdapter.java
package org.shark.melian.adapter.out.embedding;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import org.shark.melian.domain.port.out.EmbeddingPort;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class LangChain4jEmbeddingAdapter implements EmbeddingPort {

    private final EmbeddingModel embeddingModel;

    public LangChain4jEmbeddingAdapter(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public List<Embedding> embedAll(List<String> texts) {
        return embeddingModel.embedAll(texts).content();
    }
}
{
}
