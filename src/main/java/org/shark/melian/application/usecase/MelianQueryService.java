
package org.shark.melian.application.usecase;

import org.shark.melian.domain.port.out.DataSourcePort;
import org.shark.melian.domain.port.out.EmbeddingPort;
import org.shark.melian.domain.service.ChunkingService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.embedding.Embedding;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MelianQueryService {

    private final DataSourcePort dataSourcePort;
    private final ChunkingService chunkingService;
    private final EmbeddingPort embeddingPort;

    public MelianQueryService(
            DataSourcePort dataSourcePort,
            ChunkingService chunkingService,
            EmbeddingPort embeddingPort
    ) {
        this.dataSourcePort = dataSourcePort;
        this.chunkingService = chunkingService;
        this.embeddingPort = embeddingPort;
    }

    public List<Embedding> processQuery(String queryOrConfig) {
        List<String> chunks = dataSourcePort.fetchRawChunks(queryOrConfig);
        // Si cada chunk es texto, los paso por chunking
        // (o salto este paso si el adapter ya entrega chunks)
        List<TextSegment> segments = chunks.stream()
                .flatMap(text -> chunkingService.chunk(text).stream())
                .toList();
        List<String> toEmbed = segments.stream()
                .map(TextSegment::text)
                .toList();
        return embeddingPort.embedAll(toEmbed);
    }
}
