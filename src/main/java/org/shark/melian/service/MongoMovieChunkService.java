package org.shark.melian.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shark.melian.document.MovieDocument;
import org.shark.melian.model.ChunkDto;
import org.shark.melian.model.MovieResult;
import org.shark.melian.repository.MovieDocumentRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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

    @Override
    public void storeMovies(List<MovieResult> movies, String source) {
        log.info("[MongoMovieChunkService] Storing {} movies from source: {}", movies.size(), source);

        for (MovieResult movieResult : movies) {
            movieDocumentRepository.findByTitleAndSource(movieResult.title(), source)
                    .ifPresentOrElse(
                            existingMovie -> {
                                // Update existing movie
                                existingMovie.setOverview(movieResult.overview());
                                existingMovie.setRating(movieResult.rating());
                                movieDocumentRepository.save(existingMovie);
                            },
                            () -> {
                                // Create new movie
                                MovieDocument newMovie = new MovieDocument(
                                        movieResult.title(),
                                        movieResult.overview(),
                                        movieResult.releaseDate(),
                                        movieResult.rating(),
                                        source
                                );
                                movieDocumentRepository.save(newMovie);
                            }
                    );
        }
    }

    @Override
    public List<ChunkDto> getMovieChunks(String source, int limit, String afterId, String filter, List<String> tags, String sort) {
        log.info("[MongoMovieChunkService] Getting movie chunks for source: {}", source);

        Pageable pageable = PageRequest.of(0, limit);
        List<MovieDocument> movies;

        if (afterId != null && !afterId.isBlank()) {
            movies = movieDocumentRepository.findBySourceAndIdGreaterThan(source, afterId, pageable);
        } else {
            movies = movieDocumentRepository.findMoviesWithCriteria(source, null, pageable);
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
            storeMovies(movies, "tmdb");
        }

        return movies;
    }

    @Override
    public List<MovieResult> search(String query, int limit) {
        log.info("[MongoMovieChunkService] Searching local MongoDB for movies with title LIKE: {} (limit: {})", query, limit);

        Pageable pageable = PageRequest.of(0, limit);
        List<MovieDocument> movies = movieDocumentRepository.searchByTitle(query, pageable);

        return movies.stream()
                .map(movie -> new MovieResult(
                        movie.getTitle(),
                        movie.getOverview(),
                        movie.getReleaseDate(),
                        movie.getRating()
                ))
                .toList();
    }

    private ChunkDto mapMovieToChunk(MovieDocument movie) {
        ChunkDto chunk = new ChunkDto();
        chunk.setId(movie.getId());

        // Build text content for MCP compliance
        String text = String.format("Movie: %s (%s)\nOverview: %s\nRating: %.1f",
                movie.getTitle(),
                movie.getReleaseDate(),
                movie.getOverview(),
                movie.getRating());
        chunk.setText(text);

        // Build metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", movie.getId());
        metadata.put("title", movie.getTitle());
        metadata.put("overview", movie.getOverview());
        metadata.put("release_date", movie.getReleaseDate());
        metadata.put("rating", movie.getRating());
        metadata.put("source", movie.getSource());
        metadata.put("created_at", movie.getCreatedAt());
        chunk.setMetadata(metadata);

        return chunk;
    }
}