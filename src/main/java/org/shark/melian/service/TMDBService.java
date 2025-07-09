package org.shark.melian.service;

import org.shark.melian.client.TMDBApiClient;
import org.shark.melian.client.TMDBApiClient.TMDBMovie;
import org.shark.melian.client.TMDBApiClient.TMDBResponse;
import org.shark.melian.model.MovieResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Simplified MCP-compliant TMDB service for external API calls only.
 * Storage functionality moved to dedicated MovieChunkService implementations.
 */
@Service
public class TMDBService {
    
    private static final Logger log = Logger.getLogger(TMDBService.class.getName());
    private final TMDBApiClient tmdbApiClient;

    public TMDBService(TMDBApiClient tmdbApiClient) {
        this.tmdbApiClient = tmdbApiClient;
        log.info("[TMDBService] Initialized");
    }

    public List<MovieResult> search(String title, int limit) {
        return searchByParams(Map.of("query", title), limit);
    }

    public List<MovieResult> searchByParams(Map<String, String> params, int limit) {
        log.info("[TMDBService] Searching with params: " + params);
        
        TMDBResponse response = tmdbApiClient.searchMovies(params);
        if (response == null || response.results == null || response.results.isEmpty()) {
            log.info("[TMDBService] No movie results found");
            return List.of();
        }

        log.info("[TMDBService] Found " + response.results.size() + " movie(s)");
        List<MovieResult> results = new ArrayList<>();
        for (TMDBMovie movie : response.results) {
            log.info(String.format("[TMDBService] Movie: %s (%s) | Rating: %.2f", 
                    movie.title, movie.release_date, movie.vote_average));
            results.add(new MovieResult(
                    movie.title,
                    movie.overview,
                    movie.release_date,
                    movie.vote_average
            ));
            if (results.size() >= limit) break;
        }
        return results;
    }
}
