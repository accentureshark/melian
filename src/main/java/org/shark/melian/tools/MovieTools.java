package org.shark.melian.tools;

import dev.langchain4j.agent.tool.Tool;
import org.shark.melian.model.ChunkDto;
import org.shark.melian.model.MovieResult;
import org.shark.melian.service.MovieChunkService;
import org.shark.melian.service.TMDBServicePure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * LangChain4j tools for movie operations.
 * These tools are automatically exposed to AI services.
 */
public class MovieTools {

    private static final Logger log = LoggerFactory.getLogger(MovieTools.class);

    private final TMDBServicePure tmdbService;
    private final MovieChunkService sqlMovieService;
    private final MovieChunkService mongoMovieService;

    public MovieTools(TMDBServicePure tmdbService,
                      MovieChunkService sqlMovieService,
                      MovieChunkService mongoMovieService) {
        this.tmdbService = tmdbService;
        this.sqlMovieService = sqlMovieService;
        this.mongoMovieService = mongoMovieService;
        log.info("MovieTools initialized");
    }

    @Tool("Search for movies using TMDB API")
    public List<MovieResult> searchMovies(String query, int limit) {
        log.info("Searching movies: query='{}', limit={}", query, limit);
        if (limit <= 0) limit = 10;
        if (limit > 50) limit = 50;
        
        List<MovieResult> results = tmdbService.search(query, limit);
        log.info("Found {} movies for query '{}'", results.size(), query);
        return results;
    }

    @Tool("Get movie data chunks for RAG applications")
    public List<ChunkDto> getMovieChunks(String source, int limit, String filter) {
        log.info("Getting movie chunks: source='{}', limit={}, filter='{}'", source, limit, filter);
        if (limit <= 0) limit = 10;
        if (limit > 100) limit = 100;
        
        MovieChunkService service = "mongo".equalsIgnoreCase(source) ? mongoMovieService : sqlMovieService;
        List<ChunkDto> chunks = service.getMovieChunks(source, limit, null, filter, null, null);
        log.info("Retrieved {} chunks from {}", chunks.size(), source);
        return chunks;
    }

    @Tool("Get server status and configuration")
    public Map<String, Object> getServerStatus() {
        log.info("Getting server status");
        return Map.of(
                "status", "OK",
                "timestamp", System.currentTimeMillis(),
                "services", Map.of(
                        "tmdb", tmdbService != null ? "available" : "unavailable",
                        "sql", sqlMovieService != null ? "available" : "unavailable",
                        "mongo", mongoMovieService != null ? "available" : "unavailable"
                )
        );
    }
}