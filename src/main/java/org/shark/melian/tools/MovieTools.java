package org.shark.melian.tools;

import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shark.melian.model.ChunkDto;
import org.shark.melian.model.MovieResult;
import org.shark.melian.service.AggregatedMovieService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LangChain4j tools for movie operations using Spring best practices.
 * These tools are automatically exposed to AI services.
 */
@Component("movieTools")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean(AggregatedMovieService.class)
public class MovieTools {

    private final AggregatedMovieService aggregatedMovieService;

    @Tool("Search for movies using multiple sources")
    public List<MovieResult> searchMovies(String query, int limit) {
        log.info("Searching movies: query='{}', limit={}", query, limit);
        if (limit <= 0) limit = 10;
        if (limit > 50) limit = 50;
        
        List<MovieResult> results = aggregatedMovieService.searchMovies(query, limit);
        log.info("Found {} movies for query '{}'", results.size(), query);
        return results;
    }

    @Tool("Get movie data chunks for RAG applications")
    public List<ChunkDto> getMovieChunks(int limit, String filter) {
        log.info("Getting movie chunks: limit={}, filter='{}'", limit, filter);
        if (limit <= 0) limit = 10;
        if (limit > 100) limit = 100;
        
        List<ChunkDto> chunks = aggregatedMovieService.getMovieChunks(limit, null, filter, null, null);
        log.info("Retrieved {} chunks from all sources", chunks.size());
        return chunks;
    }
}