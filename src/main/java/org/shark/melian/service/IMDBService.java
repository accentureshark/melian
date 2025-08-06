package org.shark.melian.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shark.melian.client.IMDBApiClientPure;
import org.shark.melian.config.MelianProperties;
import org.shark.melian.model.MovieResult;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring Service for IMDB operations following Spring best practices.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IMDBService {

    private final IMDBApiClientPure imdbApiClient;

    public List<MovieResult> search(String title, int limit) {
        String cleanedTitle = title != null ? title.trim().replaceAll("\\s+", " ") : "";
        log.info("[IMDBService] Searching for movies with title: {} (limit: {})", cleanedTitle, limit);
        return imdbApiClient.searchMovies(cleanedTitle, limit);
    }
}