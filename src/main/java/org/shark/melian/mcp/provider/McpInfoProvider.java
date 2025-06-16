package org.shark.melian.mcp.provider;

import org.shark.melian.mcp.model.McpMetadataDto;
import org.springframework.stereotype.Service;

@Service
public class McpInfoProvider {

    public McpMetadataDto getInfo() {
        return new McpMetadataDto(
                "MELIAN",
                "MCP server para fuentes SQL y APIs TMDB",
                "1.0.0",
                java.time.LocalDate.now().toString(),
                java.util.List.of("schema", "chunks", "info"),
                java.util.List.of("sql", "tmdb"),
                null // opcional: agregar DatabaseMetadataDto si querés
        );
    }
}