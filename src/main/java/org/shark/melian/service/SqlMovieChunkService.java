package org.shark.melian.service;

import org.shark.melian.model.ChunkDto;
import org.shark.melian.model.MovieResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * MCP-compliant SQL implementation for movie storage and retrieval.
 * Stores movies in a SQL table and provides chunk-based access.
 */
@Service("sqlMovieChunkService")
public class SqlMovieChunkService implements MovieChunkService {
    
    private static final Logger log = Logger.getLogger(SqlMovieChunkService.class.getName());
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private TMDBService tmdbService;
    
    @Override
    public void storeMovies(List<MovieResult> movies, String source) {
        log.info("[SqlMovieChunkService] Storing " + movies.size() + " movies from source: " + source);
        
        // Create movies table if it doesn't exist
        createMoviesTableIfNeeded();
        
        for (MovieResult movie : movies) {
            // First try to update existing record
            int updated = jdbcTemplate.update(
                "UPDATE movies SET overview = ?, rating = ? WHERE title = ? AND source = ?",
                movie.overview(), movie.rating(), movie.title(), source
            );
            
            // If no record was updated, insert new one
            if (updated == 0) {
                jdbcTemplate.update(
                    "INSERT INTO movies (title, overview, release_date, rating, source) VALUES (?, ?, ?, ?, ?)",
                    movie.title(), movie.overview(), movie.releaseDate(), movie.rating(), source
                );
            }
        }
    }
    
    @Override
    public List<ChunkDto> getMovieChunks(String source, int limit, String afterId, String filter, List<String> tags, String sort) {
        log.info("[SqlMovieChunkService] Getting movie chunks for source: " + source);
        
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
        
        return jdbcTemplate.query(sql.toString(), params.toArray(), this::mapRowToChunk);
    }
    
    @Override
    public List<MovieResult> searchAndStore(String title, int limit, boolean store) {
        log.info("[SqlMovieChunkService] Searching for movies with title: " + title + ", store: " + store);
        
        List<MovieResult> movies = tmdbService.search(title, limit);
        
        if (store && !movies.isEmpty()) {
            storeMovies(movies, "tmdb");
        }
        
        return movies;
    }
    
    private void createMoviesTableIfNeeded() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS movies (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "title VARCHAR(255) NOT NULL, " +
                "overview TEXT, " +
                "release_date VARCHAR(50), " +
                "rating DECIMAL(3,1), " +
                "source VARCHAR(50), " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE KEY unique_title_source (title, source)" +
                ")"
            );
        } catch (Exception e) {
            log.warning("[SqlMovieChunkService] Error creating movies table: " + e.getMessage());
        }
    }
    
    private ChunkDto mapRowToChunk(ResultSet rs, int rowNum) throws SQLException {
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