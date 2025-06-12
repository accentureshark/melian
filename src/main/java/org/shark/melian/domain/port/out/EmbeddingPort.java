
package org.shark.melian.domain.port.out;

import dev.langchain4j.data.embedding.Embedding;
import java.util.List;

public interface EmbeddingPort {
    List<Embedding> embedAll(List<String> texts);
}
