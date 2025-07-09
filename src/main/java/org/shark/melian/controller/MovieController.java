package org.shark.melian.controller;

import org.shark.melian.model.ChunkDto;
import org.shark.melian.model.MovieResult;
import org.shark.melian.service.MovieChunkService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MCP-compliant REST controller for movie operations.
 * Provides endpoints for movie search, storage, and chunk retrieval.
 */
@RestController
@RequestMapping("/mcp/movies")
public class MovieController {
    
    private final MovieChunkService sqlMovieChunkService;
    private final MovieChunkService mongoMovieChunkService;
    
    public MovieController(
            @Qualifier("sqlMovieChunkService") MovieChunkService sqlMovieChunkService,
            @Qualifier("mongoMovieChunkService") MovieChunkService mongoMovieChunkService
    ) {
        this.sqlMovieChunkService = sqlMovieChunkService;
        this.mongoMovieChunkService = mongoMovieChunkService;
    }
    
    @GetMapping("/search")
    public List<MovieResult> searchMovies(
            @RequestParam String title,
            @RequestParam(defaultValue = "3") int limit,
            @RequestParam(defaultValue = "false") boolean store,
            @RequestParam(defaultValue = "sql") String storage
    ) {
        MovieChunkService service = getMovieService(storage);
        return service.searchAndStore(title, limit, store);
    }
    
    @GetMapping("/chunks")
    public List<ChunkDto> getMovieChunks(
            @RequestParam(defaultValue = "sql") String storage,
            @RequestParam(defaultValue = "tmdb") String source,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String afterId,
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String sort
    ) {
        MovieChunkService service = getMovieService(storage);
        return service.getMovieChunks(source, limit, afterId, filter, tags, sort);
    }
    
    @PostMapping("/store")
    public void storeMovies(
            @RequestBody List<MovieResult> movies,
            @RequestParam(defaultValue = "manual") String source,
            @RequestParam(defaultValue = "sql") String storage
    ) {
        MovieChunkService service = getMovieService(storage);
        service.storeMovies(movies, source);
    }
    
    private MovieChunkService getMovieService(String storage) {
        return switch (storage.toLowerCase()) {
            case "mongo", "mongodb" -> mongoMovieChunkService;
            case "sql", "db" -> sqlMovieChunkService;
            default -> throw new IllegalArgumentException("Invalid storage: " + storage);
        };
    }
}