package org.shark.melian.mcp.provider;

import org.shark.melian.mcp.model.ChunkDto;
import org.shark.melian.mcp.service.ChunkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class RestApiChunkProvider {

    @Autowired
    @Qualifier("restApiChunkService")
    private ChunkService restChunkService;

    public Stream<Map<String, Object>> getChunks(String table, int limit, String afterId, String filter, List<String> tags, String sort) {
        List<ChunkDto> dtos = restChunkService.getChunks(table, "tmdb", limit, afterId, filter, tags, sort);
        return dtos.stream().map(dto -> Map.of(
                "id", dto.getId(),
                "text", dto.getText(),
                "metadata", dto.getMetadata(),
                "embedding", dto.getEmbedding(),
                "source", dto.getSource(),
                "tags", dto.getTags()
        ));
    }
}