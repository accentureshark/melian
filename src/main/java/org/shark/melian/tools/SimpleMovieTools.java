package org.shark.melian.tools;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.shark.melian.client.TMDBApiClientPure;
import org.shark.melian.config.MelianProperties;
import org.shark.melian.model.MovieResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Simplified LangChain4j tools for movie operations that work independently
 */
@Component
@Slf4j
public class SimpleMovieTools {

    private final TMDBApiClientPure tmdbClient;
    private final MelianProperties melianProperties;

    public SimpleMovieTools(TMDBApiClientPure tmdbClient, MelianProperties melianProperties) {
        this.tmdbClient = tmdbClient;
        this.melianProperties = melianProperties;
    }

    @Tool("Search for movies using TMDB API")
    public List<MovieResult> searchMovies(String query, int limit) {
        log.info("Searching movies: query='{}', limit={}", query, limit);
        if (limit <= 0) limit = 10;
        if (limit > 50) limit = 50;
        
        try {
            List<MovieResult> results = tmdbClient.searchMovies(query, limit);
            log.info("Found {} movies for query '{}'", results.size(), query);
            return results;
        } catch (Exception e) {
            log.error("Error searching movies", e);
            return List.of();
        }
    }

    @Tool("Get server status and configuration")
    public String getServerStatus() {
        log.info("Getting server status");
        
        String tmdbStatus = (melianProperties.getTmdb().getAccessToken() != null) ? "available" : "not configured";
        
        return String.format("Server status: {status=OK, timestamp=%d, services={tmdb=%s}}", 
                           System.currentTimeMillis(), tmdbStatus);
    }

    @Tool("Get sample movie data chunks for demonstration")
    public String getMovieChunks(int limit, String filter) {
        log.info("Getting sample movie chunks: limit={}, filter='{}'", limit, filter);
        if (limit <= 0) limit = 10;
        if (limit > 100) limit = 100;
        
        // Return sample data for demonstration
        return String.format("Sample movie data chunks (limit=%d, filter='%s'): " +
                           "This would contain movie metadata, descriptions, and analysis data for RAG applications. " +
                           "In a full implementation, this would query the database.", limit, filter != null ? filter : "none");
    }
}