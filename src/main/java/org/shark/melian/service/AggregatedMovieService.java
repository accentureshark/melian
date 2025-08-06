package org.shark.melian.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shark.melian.model.ChunkDto;
import org.shark.melian.model.MovieResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Aggregated movie service using Spring best practices that fetches data from all available sources in parallel:
 * - MySQL database (SQL) via Spring Data JPA
 * - MongoDB collection via Spring Data MongoDB
 * - TMDB API
 * - IMDB API
 * 
 * Provides clean results by filtering out unavailable sources and empty responses.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AggregatedMovieService {
    
    private final TMDBService tmdbService;
    private final IMDBService imdbService;
    private final SqlMovieChunkService sqlService;
    private final Optional<MongoMovieChunkService> mongoService; // Optional since MongoDB might not be configured
    private final ExecutorService executorService = Executors.newFixedThreadPool(4); // Increased to 4 for IMDB


    @PostConstruct
    public void init() {
        log.info("Estado de servicios disponibles:");
        log.info("TMDB Service: {}", tmdbService != null ? "DISPONIBLE" : "NO DISPONIBLE");
        log.info("IMDB Service: {}", imdbService != null ? "DISPONIBLE" : "NO DISPONIBLE");
        log.info("SQL Service: {}", sqlService != null ? "DISPONIBLE" : "NO DISPONIBLE");
        log.info("MongoDB Service: {}", mongoService.isPresent() ? "DISPONIBLE" : "NO DISPONIBLE");

        if (!mongoService.isPresent()) {
            log.warn("El servicio de MongoDB no está disponible. Verifica la configuración spring.data.mongodb.uri");
        }
    }

    /**
     * Search movies from TMDB API and store in all available databases
     */
    public List<MovieResult> searchMovies(String query, int limit) {
        String cleanedQuery = query != null ? query.trim().replaceAll("\\s+", " ") : "";
        log.info("Searching movies with query: '{}', limit: {}", cleanedQuery, limit);

        List<CompletableFuture<List<MovieResult>>> futures = new ArrayList<>();

        // Search in TMDB
        if (tmdbService != null) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    List<MovieResult> movies = tmdbService.search(cleanedQuery, limit);
                    log.info("Found {} movies from TMDB", movies.size());
                    return movies;
                } catch (Exception e) {
                    log.error("Error searching movies from TMDB", e);
                    return Collections.emptyList();
                }
            }, executorService));
        }

        // Search in IMDB
        if (imdbService != null) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    List<MovieResult> movies = imdbService.search(cleanedQuery, limit);
                    log.info("Found {} movies from IMDB", movies.size());
                    return movies;
                } catch (Exception e) {
                    log.error("Error searching movies from IMDB", e);
                    return Collections.emptyList();
                }
            }, executorService));
        }

        // Search in SQL database
        if (sqlService != null) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    List<MovieResult> movies = sqlService.search(cleanedQuery, limit);
                    log.info("Found {} movies from SQL", movies.size());
                    return movies;
                } catch (Exception e) {
                    log.error("Error searching movies from SQL", e);
                    return Collections.emptyList();
                }
            }, executorService));
        }

        // Search in MongoDB
        mongoService.ifPresent(service -> {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    List<MovieResult> movies = service.search(cleanedQuery, limit);
                    log.info("Found {} movies from MongoDB", movies.size());
                    return movies;
                } catch (Exception e) {
                    log.error("Error searching movies from MongoDB", e);
                    return Collections.emptyList();
                }
            }, executorService));
        });

        List<MovieResult> aggregatedMovies = new ArrayList<>();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .toList())
            .thenAccept(results -> {
                for (List<MovieResult> movies : results) {
                    aggregatedMovies.addAll(movies);
                }
            })
            .join();

        log.info("Aggregated {} total movies from all sources", aggregatedMovies.size());
        return aggregatedMovies.stream()
            .limit(limit)
            .toList();
    }

    /**
     * Get movie chunks from all available sources in parallel
     */
    public List<ChunkDto> getMovieChunks(int limit, String afterId, String filter, List<String> tags, String sort) {
        log.info("Getting movie chunks from all sources - limit: {}, filter: '{}'", limit, filter);

        List<CompletableFuture<List<ChunkDto>>> futures = new ArrayList<>();

        // Fetch from SQL database
        if (sqlService != null) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    List<ChunkDto> chunks = sqlService.getMovieChunks( limit, afterId, filter, tags, sort);
                    log.debug("Retrieved {} chunks from SQL source", chunks.size());
                    // Add source identifier to metadata
                    chunks.forEach(chunk -> addSourceToMetadata(chunk, "sql"));
                    return chunks;
                } catch (Exception e) {
                    log.warn("Error fetching chunks from SQL source: {}", e.getMessage());
                    return Collections.<ChunkDto>emptyList();
                }
            }, executorService));
        }

        // Fetch from MongoDB
        mongoService.ifPresent(service -> {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    List<ChunkDto> chunks = service.getMovieChunks( limit, afterId, filter, tags, sort);
                    log.debug("Retrieved {} chunks from MongoDB source", chunks.size());
                    // Add source identifier to metadata
                    chunks.forEach(chunk -> addSourceToMetadata(chunk, "mongo"));
                    return chunks;
                } catch (Exception e) {
                    log.warn("Error fetching chunks from MongoDB source: {}", e.getMessage());
                    return Collections.<ChunkDto>emptyList();
                }
            }, executorService));
        });

        // Fetch from TMDB (search recent popular movies and convert to chunks)
        if (tmdbService != null) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    // Use a generic search to get recent popular movies if no specific filter
                    String searchQuery = extractSearchQueryFromFilter(filter);
                    if (searchQuery == null || searchQuery.isEmpty()) {
                        searchQuery = "2024"; // Default to recent movies
                    }
                    
                    List<MovieResult> movies = tmdbService.search(searchQuery, limit);
                    List<ChunkDto> chunks = convertMoviesToChunks(movies, "tmdb");
                    log.debug("Retrieved {} chunks from TMDB source", chunks.size());
                    // Add source identifier to metadata
                    chunks.forEach(chunk -> addSourceToMetadata(chunk, "tmdb"));
                    return chunks;
                } catch (Exception e) {
                    log.warn("Error fetching chunks from TMDB source: {}", e.getMessage());
                    return Collections.<ChunkDto>emptyList();
                }
            }, executorService));
        }

        // Fetch from IMDB (search recent popular movies and convert to chunks)
        if (imdbService != null) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    // Use a generic search to get recent popular movies if no specific filter
                    String searchQuery = extractSearchQueryFromFilter(filter);
                    if (searchQuery == null || searchQuery.isEmpty()) {
                        searchQuery = "2024"; // Default to recent movies
                    }
                    
                    List<MovieResult> movies = imdbService.search(searchQuery, limit);
                    List<ChunkDto> chunks = convertMoviesToChunks(movies, "imdb");
                    log.debug("Retrieved {} chunks from IMDB source", chunks.size());
                    // Add source identifier to metadata
                    chunks.forEach(chunk -> addSourceToMetadata(chunk, "imdb"));
                    return chunks;
                } catch (Exception e) {
                    log.warn("Error fetching chunks from IMDB source: {}", e.getMessage());
                    return Collections.<ChunkDto>emptyList();
                }
            }, executorService));
        }

        // Collect results from all sources
        List<ChunkDto> aggregatedChunks = new ArrayList<>();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .toList())
            .thenAccept(results -> {
                for (List<ChunkDto> chunks : results) {
                    aggregatedChunks.addAll(chunks);
                }
            })
            .join();

        log.info("Aggregated {} total chunks from all sources", aggregatedChunks.size());
        
        // Sort and limit final results
        return aggregatedChunks.stream()
            .sorted((a, b) -> a.getId().compareTo(b.getId())) // Simple ID-based sorting
            .limit(limit)
            .toList();
    }

    /**
     * Store movies in all available databases asynchronously
     */
    private void storeMoviesInAllSources(List<MovieResult> movies) {
        log.debug("Storing {} movies in all available sources", movies.size());

        List<CompletableFuture<Void>> storeFutures = new ArrayList<>();

        // Store in SQL
        if (sqlService != null) {
            storeFutures.add(CompletableFuture.runAsync(() -> {
                try {
                    sqlService.storeMovies(movies);
                    log.debug("Successfully stored movies in SQL database");
                } catch (Exception e) {
                    log.warn("Failed to store movies in SQL database: {}", e.getMessage());
                }
            }, executorService));
        }

        // Store in MongoDB
        mongoService.ifPresent(service -> {
            storeFutures.add(CompletableFuture.runAsync(() -> {
                try {
                    service.storeMovies(movies);
                    log.debug("Successfully stored movies in MongoDB");
                } catch (Exception e) {
                    log.warn("Failed to store movies in MongoDB: {}", e.getMessage());
                }
            }, executorService));
        });

        // Wait for all storage operations to complete
        CompletableFuture.allOf(storeFutures.toArray(new CompletableFuture[0])).join();
    }

    /**
     * Convert MovieResult objects to ChunkDto objects for unified response format
     */
    private List<ChunkDto> convertMoviesToChunks(List<MovieResult> movies) {
        return movies.stream()
            .map(movie -> convertMovieToChunk(movie, "tmdb"))
            .toList();
    }

    private List<ChunkDto> convertMoviesToChunks(List<MovieResult> movies, String source) {
        return movies.stream()
            .map(movie -> convertMovieToChunk(movie, source))
            .toList();
    }

    private ChunkDto convertMovieToChunk(MovieResult movie) {
        return convertMovieToChunk(movie, "tmdb");
    }

    private ChunkDto convertMovieToChunk(MovieResult movie, String source) {
        ChunkDto chunk = new ChunkDto();
        
        // Generate a unique ID for chunks
        chunk.setId(source + "_" + movie.title().hashCode());
        
        // Build text content for MCP compliance
        String text = String.format("Movie: %s (%s)\nOverview: %s\nRating: %.1f",
                movie.title(),
                movie.releaseDate(),
                movie.overview(),
                movie.rating());
        chunk.setText(text);

        // Build metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("title", movie.title());
        metadata.put("overview", movie.overview());
        metadata.put("release_date", movie.releaseDate());
        metadata.put("rating", movie.rating());
        metadata.put("source", source);
        chunk.setMetadata(metadata);

        return chunk;
    }

    /**
     * Add source information to chunk metadata
     */
    private void addSourceToMetadata(ChunkDto chunk, String source) {
        if (chunk.getMetadata() == null) {
            chunk.setMetadata(new HashMap<>());
        }
        chunk.getMetadata().put("data_source", source);
    }

    /**
     * Extract search query from filter string for TMDB searches
     */
    private String extractSearchQueryFromFilter(String filter) {
        if (filter == null || filter.trim().isEmpty()) {
            return null;
        }

        // Simple extraction: look for title LIKE 'value' patterns
        if (filter.toLowerCase().contains("title") && filter.toLowerCase().contains("like")) {
            String[] parts = filter.split("(?i)like", 2);
            if (parts.length > 1) {
                String value = parts[1].trim();
                // Remove quotes
                if ((value.startsWith("'") && value.endsWith("'")) || 
                    (value.startsWith("\"") && value.endsWith("\""))) {
                    value = value.substring(1, value.length() - 1);
                }
                // Remove SQL wildcards
                return value.replace("%", "").trim();
            }
        }

        return null;
    }

    /**
     * Get health status of all services
     */
    public Map<String, String> getServicesStatus() {
        Map<String, String> status = new HashMap<>();
        
        status.put("tmdb_service", tmdbService != null ? "AVAILABLE" : "NOT_AVAILABLE");
        status.put("imdb_service", imdbService != null ? "AVAILABLE" : "NOT_AVAILABLE");
        status.put("sql_service", sqlService != null ? "AVAILABLE" : "NOT_AVAILABLE");
        status.put("mongo_service", mongoService != null ? "AVAILABLE" : "NOT_AVAILABLE");
        
        return status;
    }

    /**
     * Shutdown the executor service when the application stops
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            log.info("AggregatedMovieService executor shutdown");
        }
    }
}