package org.shark.melian.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shark.melian.client.TMDBApiClientPure;
import org.shark.melian.model.MovieResult;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio Spring para operaciones TMDB con logs detallados.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TMDBService {

    private final TMDBApiClientPure tmdbApiClient;

    public List<MovieResult> search(String title, int limit) {
        String cleanedTitle = title != null ? title.trim().replaceAll("\\s+", " ") : "";
        log.info("[TMDBService] Buscando películas con título: '{}' (límite: {})", cleanedTitle, limit);
        List<MovieResult> results = tmdbApiClient.searchMovies(cleanedTitle, limit);
        if (results == null) {
            log.warn("[TMDBService] Resultado nulo recibido del cliente TMDB");
        } else {
            log.info("[TMDBService] Resultados obtenidos: {}", results.size());
        }
        return results;
    }
}