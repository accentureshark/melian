package org.shark.melian.service;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.shark.melian.config.MongoConfig;
import org.shark.melian.model.ChunkDto;
import org.shark.melian.model.MovieResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure MongoDB implementation for movie storage and retrieval without Spring dependencies.
 * Stores movies in a MongoDB collection and provides chunk-based access.
 */
public class MongoMovieChunkServicePure implements MovieChunkService {

    private static final Logger log = LoggerFactory.getLogger(MongoMovieChunkServicePure.class);
    private static final String MOVIES_COLLECTION = "movies";

    private final MongoCollection<Document> moviesCollection;
    private final TMDBServicePure tmdbService;

    public MongoMovieChunkServicePure(MongoConfig mongoConfig, TMDBServicePure tmdbService) {
        this.tmdbService = tmdbService;
        if (mongoConfig == null || mongoConfig.getDatabase() == null) {
            log.warn("MongoDB not configured - MongoMovieChunkService will be disabled");
            this.moviesCollection = null;
        } else {
            this.moviesCollection = mongoConfig.getDatabase().getCollection(MOVIES_COLLECTION);
            log.info("MongoMovieChunkServicePure initialized with MongoDB");
        }
    }

    @Override
    public void storeMovies(List<MovieResult> movies, String source) {
        if (moviesCollection == null) {
            log.warn("MongoDB not available - cannot store movies");
            return;
        }
        
        log.info("[MongoMovieChunkServicePure] Storing {} movies from source: {}", movies.size(), source);

        for (MovieResult movie : movies) {
            Document filter = new Document("title", movie.title()).append("source", source);
            Document update = new Document("$set", new Document()
                    .append("overview", movie.overview())
                    .append("release_date", movie.releaseDate())
                    .append("rating", movie.rating()))
                    .append("$setOnInsert", new Document("created_at", System.currentTimeMillis()));

            moviesCollection.updateOne(filter, update, new UpdateOptions().upsert(true));
        }
    }

    @Override
    public List<ChunkDto> getMovieChunks(String source, int limit, String afterId, String filter, List<String> tags, String sort) {
        if (moviesCollection == null) {
            log.warn("MongoDB not available - returning empty chunks list");
            return List.of();
        }
        
        log.info("[MongoMovieChunkServicePure] Getting movie chunks for source: {}", source);

        Document query = new Document();

        // Add source filter
        if (source != null && !source.isBlank()) {
            query.append("source", source);
        }

        // Add afterId pagination
        if (afterId != null && !afterId.isBlank()) {
            query.append("_id", new Document("$gt", new ObjectId(afterId)));
        }

        // Add filter
        if (filter != null && !filter.isBlank()) {
            if (filter.toLowerCase().contains(" like ")) {
                String[] parts = filter.split("(?i)like", 2);
                String field = parts[0].trim();
                String val = cleanQuotes(parts[1].trim());
                query.append(field, new Document("$regex", val).append("$options", "i"));
            } else if (filter.contains("=")) {
                String[] parts = filter.split("=", 2);
                String field = parts[0].trim();
                String val = cleanQuotes(parts[1].trim());
                query.append(field, val);
            }
        }

        // Build the query
        var findQuery = moviesCollection.find(query);

        // Add sorting
        if (sort != null && !sort.isBlank()) {
            findQuery = findQuery.sort(Sorts.ascending(sort));
        } else {
            findQuery = findQuery.sort(Sorts.ascending("_id"));
        }

        findQuery = findQuery.limit(limit);

        List<ChunkDto> chunks = new ArrayList<>();
        try (MongoCursor<Document> cursor = findQuery.iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                ChunkDto chunk = new ChunkDto();
                chunk.setId(doc.getObjectId("_id").toHexString());

                // Build text content for MCP compliance
                String text = String.format("Movie: %s (%s)\nOverview: %s\nRating: %.1f",
                        doc.getString("title"),
                        doc.getString("release_date"),
                        doc.getString("overview"),
                        doc.getDouble("rating"));
                chunk.setText(text);

                // Use document as metadata
                Map<String, Object> metadata = new HashMap<>();
                for (Map.Entry<String, Object> entry : doc.entrySet()) {
                    metadata.put(entry.getKey(), entry.getValue());
                }
                chunk.setMetadata(metadata);

                chunks.add(chunk);
            }
        }

        return chunks;
    }

    @Override
    public List<MovieResult> searchAndStore(String title, int limit, boolean store) {
        log.info("[MongoMovieChunkServicePure] Searching for movies with title: {}, store: {}", title, store);

        List<MovieResult> movies = tmdbService.search(title, limit);

        if (store && !movies.isEmpty()) {
            storeMovies(movies, "tmdb");
        }

        return movies;
    }

    private String cleanQuotes(String val) {
        val = val.trim();
        if ((val.startsWith("'") && val.endsWith("'")) || (val.startsWith("\"") && val.endsWith("\""))) {
            return val.substring(1, val.length() - 1);
        }
        return val;
    }
}