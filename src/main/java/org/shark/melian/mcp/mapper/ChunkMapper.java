package org.shark.melian.mcp.mapper;


import org.shark.melian.mcp.model.ChunkDto;

import java.util.HashMap;
import java.util.Map;

public class ChunkMapper {
    public static Map<String, Object> fromDto(ChunkDto dto) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", dto.getId());
        map.put("text", dto.getText());
        map.put("metadata", dto.getMetadata());
        map.put("embedding", dto.getEmbedding());
        map.put("source", dto.getSource());
        map.put("tags", dto.getTags());
        return map;
    }
}