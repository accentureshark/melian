package org.shark.melian.controller;

import org.shark.melian.model.ChunkDto;
import org.shark.melian.service.ChunkService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/mcp")
public class ChunkController {

    private final ChunkService chunkService;

    public ChunkController(ChunkService chunkService) {
        this.chunkService = chunkService;
    }

    @GetMapping("/chunks")
    public ChunkPageDto getChunks(
            @RequestParam(value = "table", required = false) String table,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "limit", defaultValue = "100") int limit,
            @RequestParam(value = "afterId", required = false) String afterId,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "tags", required = false) List<String> tags,
            @RequestParam(value = "sort", required = false) String sort
    ) {
        List<ChunkDto> chunkList = chunkService.getChunks(table, source, limit, afterId, filter, tags, sort);
        boolean hasMore = chunkList.size() == limit;
        String nextAfterId = hasMore && !chunkList.isEmpty() ? chunkList.get(chunkList.size() - 1).getId() : null;
        return new ChunkPageDto(chunkList, hasMore, nextAfterId);
    }
}

