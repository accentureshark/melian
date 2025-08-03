package org.shark.melian.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shark.melian.entity.Movie;
import org.shark.melian.model.ChunkDto;
import org.shark.melian.model.MovieResult;
import org.shark.melian.repository.MovieRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring Service for SQL Movie operations using Spring Data JPA and best practices.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SqlMovieChunkService implements MovieChunkService {

    private final MovieRepository movieRepository;
    private final TMDBService tmdbService;

    @Override
    @Transactional
    public void storeMovies(List<MovieResult> movies, String source) {
        log.info("[SqlMovieChunkService] Storing {} movies from source: {}", movies.size(), source);

        for (MovieResult movieResult : movies) {
            movieRepository.findByTitleAndSource(movieResult.title(), source)
                    .ifPresentOrElse(
                            existingMovie -> {
                                // Update existing movie
                                existingMovie.setOverview(movieResult.overview());
                                existingMovie.setRating(movieResult.rating());
                                movieRepository.save(existingMovie);
                            },
                            () -> {
                                // Create new movie
                                Movie newMovie = new Movie(
                                        movieResult.title(),
                                        movieResult.overview(),
                                        movieResult.releaseDate(),
                                        movieResult.rating(),
                                        source
                                );
                                movieRepository.save(newMovie);
                            }
                    );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChunkDto> getMovieChunks(String source, int limit, String afterId, String filter, List<String> tags, String sort) {
        log.info("[SqlMovieChunkService] Getting movie chunks for source: {}", source);

        Pageable pageable = PageRequest.of(0, limit);
        List<Movie> movies;

        if (afterId != null && !afterId.isBlank()) {
            Long afterIdLong = Long.parseLong(afterId);
            movies = movieRepository.findBySourceAndIdGreaterThan(source, afterIdLong, pageable);
        } else {
            movies = movieRepository.findMoviesWithCriteria(source, null, pageable);
        }

        return movies.stream()
                .map(this::mapMovieToChunk)
                .toList();
    }

    @Override
    @Transactional
    public List<MovieResult> searchAndStore(String title, int limit, boolean store) {
        log.info("[SqlMovieChunkService] Searching for movies with title: {}, store: {}", title, store);

        List<MovieResult> movies = tmdbService.search(title, limit);

        if (store && !movies.isEmpty()) {
            storeMovies(movies, "tmdb");
        }

        return movies;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieResult> search(String query, int limit) {
        log.info("[SqlMovieChunkService] Searching local DB for movies with title LIKE: {} (limit: {})", query, limit);
        
        Pageable pageable = PageRequest.of(0, limit);
        List<Movie> movies = movieRepository.searchByTitle(query, pageable);

        return movies.stream()
                .map(movie -> new MovieResult(
                        movie.getTitle(),
                        movie.getOverview(),
                        movie.getReleaseDate(),
                        movie.getRating()
                ))
                .toList();
    }

    private ChunkDto mapMovieToChunk(Movie movie) {
        ChunkDto chunk = new ChunkDto();
        chunk.setId(String.valueOf(movie.getId()));

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