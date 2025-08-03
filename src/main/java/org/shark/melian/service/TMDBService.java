package org.shark.melian.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shark.melian.client.TMDBApiClientPure;
import org.shark.melian.config.MelianProperties;
import org.shark.melian.model.MovieResult;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring Service for TMDB operations following Spring best practices.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TMDBService {

    private final TMDBApiClientPure tmdbApiClient;

    public List<MovieResult> search(String title, int limit) {
        log.info("[TMDBService] Searching for movies with title: {} (limit: {})", title, limit);
        return tmdbApiClient.searchMovies(title, limit);
    }
}