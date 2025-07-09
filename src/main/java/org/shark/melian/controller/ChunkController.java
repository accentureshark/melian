package org.shark.melian.controller;

import org.shark.melian.model.ChunkDto;
import org.shark.melian.service.ChunkService;
import org.shark.melian.service.MovieChunkService;
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
    
    @Autowired
    @Qualifier("mongoChunkService")
    private ChunkService mongoChunkService;
    
    @Autowired
    @Qualifier("sqlMovieChunkService")
    private MovieChunkService sqlMovieChunkService;
    
    @Autowired
    @Qualifier("mongoMovieChunkService")
    private MovieChunkService mongoMovieChunkService;

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
        // Handle movie-specific chunks
        if ("movies".equalsIgnoreCase(table)) {
            MovieChunkService movieService = switch (source.toLowerCase()) {
                case "mongo", "mongodb" -> mongoMovieChunkService;
                case "sql", "db" -> sqlMovieChunkService;
                default -> sqlMovieChunkService; // default to SQL
            };
            return movieService.getMovieChunks(source, limit, afterId, filter, tags, sort);
        }
        
        // Handle regular chunks
        ChunkService service = switch (source.toLowerCase()) {
            case "rest", "api", "tmdb" -> restApiChunkService;
            case "sql", "db" -> sqlChunkService;
            case "mongodb", "mongo" -> mongoChunkService;
            default -> throw new IllegalArgumentException("Invalid source: " + source);
        };
        return service.getChunks(table, source, limit, afterId, filter, tags, sort);
    }
}

