package org.shark.melian.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shark.melian.document.MovieDocument;
import org.shark.melian.model.ChunkDto;
import org.shark.melian.model.MovieResult;
import org.shark.melian.repository.MovieDocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring Service for MongoDB Movie operations using Spring Data MongoDB and best practices.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "spring.data.mongodb.uri")
public class MongoMovieChunkService implements MovieChunkService {

    private final MovieDocumentRepository movieDocumentRepository;
    private final TMDBService tmdbService;

    @Value("${melian.search.locale:en}")
    private String searchLocale;

    @Override
    public void storeMovies(List<MovieResult> movies) {
        log.info("[MongoMovieChunkService] Storing {} movies from source", movies.size());

        for (MovieResult movieResult : movies) {
            movieDocumentRepository.findByTitle(movieResult.title())
                    .ifPresentOrElse(
                            existingMovie -> {
                                existingMovie.setOverview(movieResult.overview());
                                existingMovie.setRating(movieResult.rating());
                                existingMovie.setReleaseDate(movieResult.releaseDate());
                                movieDocumentRepository.save(existingMovie);
                                log.debug("Updated existing movie: {}", existingMovie.getTitle());
                            },
                            () -> {
                                MovieDocument newMovie = new MovieDocument(
                                        movieResult.title(),
                                        movieResult.overview(),
                                        movieResult.releaseDate(),
                                        movieResult.rating()
                                );
                                movieDocumentRepository.save(newMovie);
                                log.debug("Created new movie: {}", newMovie.getTitle());
                            }
                    );
        }
    }

    @Override
    public List<ChunkDto> getMovieChunks(int limit, String afterId, String filter, List<String> tags, String sort) {
        log.info("[MongoMovieChunkService] Getting movie chunks");

        Pageable pageable = PageRequest.of(0, limit);
        List<MovieDocument> movies;

        if (afterId != null && !afterId.isBlank()) {
            movies = movieDocumentRepository.findByIdGreaterThan(afterId, pageable);
        } else {
            movies = movieDocumentRepository.findMoviesWithCriteria(null, pageable);
        }

        return movies.stream()
                .map(this::mapMovieToChunk)
                .toList();
    }

    @Override
    public List<MovieResult> searchAndStore(String title, int limit, boolean store) {
        log.info("[MongoMovieChunkService] Searching for movies with title: {}, store: {}", title, store);

        List<MovieResult> movies = tmdbService.search(title, limit);

        if (store && !movies.isEmpty()) {
            storeMovies(movies);
        }

        return movies;
    }

    @Override
    public List<MovieResult> search(String query, int limit) {
        log.info("[MongoMovieChunkService] Searching local MongoDB for movies with title LIKE: {} (limit: {})", query, limit);

        Pageable pageable = PageRequest.of(0, limit);
        String cleanedQuery = query.trim().replaceAll("\\s+", " ");

        List<MovieDocument> movies = movieDocumentRepository.searchByTitle(cleanedQuery, pageable, searchLocale);

        if (movies.isEmpty()) {
            movies = movieDocumentRepository.searchByTitleFuzzy(cleanedQuery, pageable, searchLocale);
        }

        if (movies.isEmpty()) {
            movies = movieDocumentRepository.searchByTitleExact(cleanedQuery, searchLocale);
        }

        return movies.stream()
                .map(movie -> new MovieResult(
                        movie.getTitle(),
                        movie.getOverview() != null ? movie.getOverview() : "",
                        movie.getReleaseDate() != null ? movie.getReleaseDate() : "",
                        convertRating(movie.getRating())
                ))
                .toList();
    }

    private double convertRating(Object rating) {
        if (rating == null) return 0.0;
        if (rating instanceof Number) return ((Number) rating).doubleValue();
        try {
            return Double.parseDouble(rating.toString());
        } catch (Exception e) {
            log.warn("No se pudo convertir el rating '{}' a double: {}", rating, e.getMessage());
            return 0.0;
        }
    }

    private ChunkDto mapMovieToChunk(MovieDocument movie) {
        ChunkDto chunk = new ChunkDto();
        chunk.setId(movie.getId());

        String text = String.format("Movie: %s (%s)\nOverview: %s\nRating: %.1f",
                movie.getTitle(),
                movie.getReleaseDate() != null ? movie.getReleaseDate() : "Unknown",
                movie.getOverview() != null ? movie.getOverview() : "No overview available",
                convertRating(movie.getRating()));
        chunk.setText(text);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", movie.getId());
        metadata.put("title", movie.getTitle());
        metadata.put("overview", movie.getOverview());
        metadata.put("release_date", movie.getReleaseDate());
        metadata.put("rating", convertRating(movie.getRating()));

        if (movie.getGenre() != null) metadata.put("genre", movie.getGenre());
        if (movie.getOrig_title() != null) metadata.put("orig_title", movie.getOrig_title());
        if (movie.getCrew() != null) metadata.put("crew", movie.getCrew());
        if (movie.getStatus() != null) metadata.put("status", movie.getStatus());
        if (movie.getOrig_lang() != null) metadata.put("orig_lang", movie.getOrig_lang());
        if (movie.getBudget_x() != null) metadata.put("budget", movie.getBudget_x());
        if (movie.getRevenue() != null) metadata.put("revenue", movie.getRevenue());
        if (movie.getCountry() != null) metadata.put("country", movie.getCountry());

        chunk.setMetadata(metadata);
        return chunk;
    }
}
