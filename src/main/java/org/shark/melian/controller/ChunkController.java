package org.shark.melian.controller;

import org.shark.melian.model.ChunkDto;
import org.shark.melian.service.ChunkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/mcp/chunks")
public class ChunkController {

    @Autowired
    @Qualifier("sqlChunkService")
    private ChunkService sqlChunkService;

    @Autowired
    @Qualifier("restApiChunkService")
    private ChunkService restApiChunkService;

    @GetMapping
    public List<ChunkDto> getChunks(
            @RequestParam(name = "table") String table,
            @RequestParam(name = "source", required = false, defaultValue = "sql") String source,
            @RequestParam(name = "limit", required = false, defaultValue = "100") int limit,
            @RequestParam(name = "afterId", required = false) String afterId,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "tags", required = false) List<String> tags,
            @RequestParam(name = "sort", required = false) String sort
    ) {
        ChunkService service = switch (source.toLowerCase()) {
            case "rest", "api", "tmdb" -> restApiChunkService;
            case "sql", "db", "" -> sqlChunkService;
            default -> throw new IllegalArgumentException("Invalid source: " + source);
        };
        return service.getChunks(table, source, limit, afterId, filter, tags, sort);
    }
}

