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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SqlMovieChunkService implements MovieChunkService {

    private final MovieRepository movieRepository;
    private final TMDBService tmdbService;

    @Override
    @Transactional
    public void storeMovies(List<MovieResult> movies) {
        log.info("[SqlMovieChunkService] Storing {} movies from source: {}", movies.size());

        for (MovieResult movieResult : movies) {

            movieRepository.findByTitle(movieResult.title())
                    .ifPresentOrElse(
                            existingMovie -> {
                                existingMovie.setOverview(movieResult.overview());
                                existingMovie.setRating(BigDecimal.valueOf(movieResult.rating()));
                                existingMovie.setReleaseDate(movieResult.releaseDate());
                                movieRepository.save(existingMovie);
                            },
                            () -> {
                                Movie newMovie = new Movie();
                                newMovie.setTitle(movieResult.title());
                                newMovie.setOverview(movieResult.overview());
                                newMovie.setReleaseDate(movieResult.releaseDate());
                                newMovie.setRating(BigDecimal.valueOf(movieResult.rating()));
                                movieRepository.save(newMovie);
                            }
                    );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChunkDto> getMovieChunks( int limit, String afterId, String filter, List<String> tags, String sort) {
        log.info("[SqlMovieChunkService] Getting movie chunks ");

        Pageable pageable = PageRequest.of(0, limit);
        List<Movie> movies;

        if (afterId != null && !afterId.isBlank()) {
            Long afterIdLong = Long.parseLong(afterId);
            movies = movieRepository.findByIdGreaterThan(afterIdLong, pageable);
        } else {
            movies = movieRepository.findAll(pageable).getContent();
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
            storeMovies(movies );
        }

        return movies;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieResult> search(String query, int limit) {
        String cleanedQuery = query != null ? query.trim().replaceAll("\\s+", " ") : "";
        log.info("[SqlMovieChunkService] Searching local DB for movies with title LIKE: {} (limit: {})", cleanedQuery, limit);

        Pageable pageable = PageRequest.of(0, limit);
        List<Movie> movies = movieRepository.searchByTitle(cleanedQuery, pageable);

        return movies.stream()
                .map(movie -> new MovieResult(
                        movie.getTitle(),
                        movie.getOverview(),
                        movie.getReleaseDate(),
                        movie.getRating() != null ? movie.getRating().doubleValue() : 0.0
                ))
                .toList();
    }

    private ChunkDto mapMovieToChunk(Movie movie) {
        ChunkDto chunk = new ChunkDto();
        chunk.setId(String.valueOf(movie.getId()));

        String text = String.format("Movie: %s (%s)\nOverview: %s\nRating: %.1f",
                movie.getTitle(),
                movie.getReleaseDate(),
                movie.getOverview(),
                movie.getRating() != null ? movie.getRating().doubleValue() : 0.0
        );
        chunk.setText(text);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", movie.getId());
        metadata.put("title", movie.getTitle());
        metadata.put("overview", movie.getOverview());
        metadata.put("release_date", movie.getReleaseDate());
        metadata.put("rating", movie.getRating() != null ? movie.getRating().doubleValue() : 0.0);
        chunk.setMetadata(metadata);

        return chunk;
    }
}