package org.shark.melian.mcp.service;


import org.shark.melian.mcp.model.ChunkDto;

import java.util.List;

public interface ChunkService {
    List<ChunkDto> getChunks(
            String table,
            String source,
            int limit,
            String afterId,
            String filter,
            List<String> tags,
            String sort
    );
}
