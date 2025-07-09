package org.shark.melian.service;

import org.shark.melian.client.TMDBApiClient;
import org.shark.melian.client.TMDBApiClient.TMDBMovie;
import org.shark.melian.client.TMDBApiClient.TMDBResponse;
import org.shark.melian.model.MovieResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TMDBService {
    private final TMDBApiClient tmdbApiClient;

    public TMDBService(TMDBApiClient tmdbApiClient) {
        this.tmdbApiClient = tmdbApiClient;
        System.err.println("[DEBUG] TMDBService initialized");
    }

    public List<MovieResult> search(String title, int limit) {
        return searchByParams(Map.of("query", title), limit);
    }

    public List<MovieResult> searchByParams(Map<String, String> params, int limit) {
        System.err.println("[DEBUG] Searching with params: " + params);
        TMDBResponse response = tmdbApiClient.searchMovies(params);
        if (response == null || response.results == null || response.results.isEmpty()) {
            System.err.println("[DEBUG] No movie results found.");
            return List.of();
        }

        System.err.println("[DEBUG] Found " + response.results.size() + " movie(s).");
        List<MovieResult> results = new ArrayList<>();
        for (TMDBMovie movie : response.results) {
            System.err.printf("[DEBUG] Movie: %s (%s) | Rating: %.2f%n", movie.title, movie.release_date, movie.vote_average);
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
