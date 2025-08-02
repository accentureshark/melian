package org.shark.melian.service;

import org.shark.melian.config.DatabaseConfig;
import org.shark.melian.model.ChunkDto;
import org.shark.melian.model.MovieResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure JDBC implementation for movie storage and retrieval without Spring dependencies.
 * Stores movies in a SQL database and provides chunk-based access.
 */
public class SqlMovieChunkServicePure implements MovieChunkService {

    private static final Logger log = LoggerFactory.getLogger(SqlMovieChunkServicePure.class);

    private final DatabaseConfig databaseConfig;
    private final TMDBServicePure tmdbService;

    public SqlMovieChunkServicePure(DatabaseConfig databaseConfig, TMDBServicePure tmdbService) {
        this.databaseConfig = databaseConfig;
        this.tmdbService = tmdbService;
        createMoviesTableIfNeeded();
    }

    @Override
    public void storeMovies(List<MovieResult> movies, String source) {
        log.info("[SqlMovieChunkServicePure] Storing {} movies from source: {}", movies.size(), source);

        String updateSql = "UPDATE movies SET overview = ?, rating = ? WHERE title = ? AND source = ?";
        String insertSql = "INSERT INTO movies (title, overview, release_date, rating, source) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = databaseConfig.getConnection()) {
            for (MovieResult movie : movies) {
                // First try to update existing record
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, movie.overview());
                    updateStmt.setDouble(2, movie.rating());
                    updateStmt.setString(3, movie.title());
                    updateStmt.setString(4, source);

                    int updated = updateStmt.executeUpdate();

                    // If no record was updated, insert new one
                    if (updated == 0) {
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setString(1, movie.title());
                            insertStmt.setString(2, movie.overview());
                            insertStmt.setString(3, movie.releaseDate());
                            insertStmt.setDouble(4, movie.rating());
                            insertStmt.setString(5, source);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            log.error("[SqlMovieChunkServicePure] Error storing movies", e);
        }
    }

    @Override
    public List<ChunkDto> getMovieChunks(String source, int limit, String afterId, String filter, List<String> tags, String sort) {
        log.info("[SqlMovieChunkServicePure] Getting movie chunks for source: {}", source);

        StringBuilder sql = new StringBuilder("SELECT id, title, overview, release_date, rating, source FROM movies WHERE 1=1");
        List<Object> params = new ArrayList<>();

        // Add source filter
        if (source != null && !source.isBlank()) {
            sql.append(" AND source = ?");
            params.add(source);
        }

        // Add afterId pagination
        if (afterId != null && !afterId.isBlank()) {
            sql.append(" AND id > ?");
            params.add(Long.parseLong(afterId));
        }

        // Add filter
        if (filter != null && !filter.isBlank()) {
            if (filter.toLowerCase().contains(" like ")) {
                String[] parts = filter.split("(?i)like", 2);
                String field = parts[0].trim();
                String val = cleanQuotes(parts[1].trim());
                sql.append(" AND ").append(field).append(" LIKE ?");
                params.add("%" + val + "%");
            } else if (filter.contains("=")) {
                String[] parts = filter.split("=", 2);
                String field = parts[0].trim();
                String val = cleanQuotes(parts[1].trim());
                sql.append(" AND ").append(field).append(" = ?");
                params.add(val);
            }
        }

        // Add sorting
        if (sort != null && !sort.isBlank()) {
            sql.append(" ORDER BY ").append(sort);
        } else {
            sql.append(" ORDER BY id");
        }

        sql.append(" LIMIT ?");
        params.add(limit);

        List<ChunkDto> chunks = new ArrayList<>();
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            // Set parameters
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    chunks.add(mapRowToChunk(rs));
                }
            }
        } catch (SQLException e) {
            log.error("[SqlMovieChunkServicePure] Error retrieving chunks", e);
        }

        return chunks;
    }

    @Override
    public List<MovieResult> searchAndStore(String title, int limit, boolean store) {
        log.info("[SqlMovieChunkServicePure] Searching for movies with title: {}, store: {}", title, store);

        List<MovieResult> movies = tmdbService.search(title, limit);

        if (store && !movies.isEmpty()) {
            storeMovies(movies, "tmdb");
        }

        return movies;
    }

    @Override
    public List<MovieResult> search(String query, int limit) {
        log.info("[SqlMovieChunkServicePure] Searching local DB for movies with title LIKE: {} (limit: {})", query, limit);
        List<MovieResult> results = new ArrayList<>();
        String sql = "SELECT title, overview, release_date, rating FROM movies WHERE LOWER(title) LIKE ? ORDER BY id LIMIT ?";
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + query.toLowerCase() + "%");
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new MovieResult(
                        rs.getString("title"),
                        rs.getString("overview"),
                        rs.getString("release_date"),
                        rs.getDouble("rating")
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("[SqlMovieChunkServicePure] Error searching local DB", e);
        }
        return results;
    }

    private void createMoviesTableIfNeeded() {
        String createTableSql = """
                CREATE TABLE IF NOT EXISTS movies (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    title VARCHAR(255) NOT NULL,
                    overview TEXT,
                    release_date VARCHAR(50),
                    rating DECIMAL(3,1),
                    source VARCHAR(50),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT unique_title_source UNIQUE (title, source)
                )
                """;

        try (Connection conn = databaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSql);
        } catch (SQLException e) {
            log.warn("[SqlMovieChunkServicePure] Error creating movies table: {}", e.getMessage());
        }
    }

    private ChunkDto mapRowToChunk(ResultSet rs) throws SQLException {
        ChunkDto chunk = new ChunkDto();
        chunk.setId(String.valueOf(rs.getLong("id")));

        // Build text content for MCP compliance
        String text = String.format("Movie: %s (%s)\nOverview: %s\nRating: %.1f",
                rs.getString("title"),
                rs.getString("release_date"),
                rs.getString("overview"),
                rs.getDouble("rating"));
        chunk.setText(text);

        // Build metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", rs.getLong("id"));
        metadata.put("title", rs.getString("title"));
        metadata.put("overview", rs.getString("overview"));
        metadata.put("release_date", rs.getString("release_date"));
        metadata.put("rating", rs.getDouble("rating"));
        metadata.put("source", rs.getString("source"));
        chunk.setMetadata(metadata);

        return chunk;
    }

    private String cleanQuotes(String val) {
        val = val.trim();
        if ((val.startsWith("'") && val.endsWith("'")) || (val.startsWith("\"") && val.endsWith("\""))) {
            return val.substring(1, val.length() - 1);
        }
        return val;
    }
}