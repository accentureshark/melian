package org.shark.melian.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shark.melian.mcp.model.SourceType;
import org.shark.melian.mcp.service.ChunkService;
import org.shark.melian.mcp.service.MetadataService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class McpRestController {

    private final List<ChunkService> chunkServices;
    private final List<MetadataService> metadataServices;

    @Value("${melian.default-source:sql}")
    private String defaultSource;

    @GetMapping("/schema")
    public List<Map<String, Object>> getSchema(@RequestParam(required = false) String source) {
        SourceType sourceType = SourceType.from(source != null ? source : defaultSource);
        log.info("Serving /schema for source {}", sourceType);

        return metadataServices.stream()
                .filter(s -> s.supports(sourceType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No schema provider for source: " + sourceType))
                .getSchema();
    }

    @GetMapping("/chunks")
    public List<Map<String, Object>> getChunks(
            @RequestParam String table,
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String afterId,
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = "false", defaultValue = "id") String sort
    ) {
        SourceType sourceType = SourceType.from(source != null ? source : defaultSource);
        log.info("Serving /chunks for source {}, table={}, filter={}", sourceType, table, filter);

        return chunkServices.stream()
                .filter(s -> s.supports(sourceType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No chunk provider for source: " + sourceType))
                .getChunks(table, sourceType.name().toLowerCase(), limit, afterId, filter, tags, sort);
    }
}
