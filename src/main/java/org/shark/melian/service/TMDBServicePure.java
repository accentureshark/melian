package org.shark.melian.service;

import org.shark.melian.client.TMDBApiClientPure;
import org.shark.melian.client.TMDBApiClientPure.TMDBMovie;
import org.shark.melian.client.TMDBApiClientPure.TMDBResponse;
import org.shark.melian.model.MovieResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pure Java TMDB service without Spring dependencies.
 * Provides movie search functionality using TMDB API.
 */
public class TMDBServicePure {

    private static final Logger log = LoggerFactory.getLogger(TMDBServicePure.class);
    private final TMDBApiClientPure tmdbApiClient;

    public TMDBServicePure(TMDBApiClientPure tmdbApiClient) {
        this.tmdbApiClient = tmdbApiClient;
        log.info("[TMDBServicePure] Initialized");
    }

    public List<MovieResult> search(String title, int limit) {
        return searchByParams(Map.of("query", title), limit);
    }

    public List<MovieResult> searchByParams(Map<String, String> params, int limit) {
        log.info("[TMDBServicePure] Searching with params: {}", params);

        TMDBResponse response = tmdbApiClient.searchMovies(params);
        if (response == null || response.results == null || response.results.isEmpty()) {
            log.info("[TMDBServicePure] No movie results found");
            return List.of();
        }

        log.info("[TMDBServicePure] Found {} movie(s)", response.results.size());
        List<MovieResult> results = new ArrayList<>();
        for (TMDBMovie movie : response.results) {
            log.info("[TMDBServicePure] Movie: {} ({}) | Rating: {:.2f}",
                    movie.title, movie.release_date, movie.vote_average);
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