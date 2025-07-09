package org.shark.melian.service;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.shark.melian.model.ChunkDto;
import org.shark.melian.model.MovieResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * MCP-compliant MongoDB implementation for movie storage and retrieval.
 * Stores movies in a MongoDB collection and provides chunk-based access.
 */
@Service("mongoMovieChunkService")
public class MongoMovieChunkService implements MovieChunkService {
    
    private static final Logger log = Logger.getLogger(MongoMovieChunkService.class.getName());
    private static final String MOVIES_COLLECTION = "movies";
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @Autowired
    private TMDBService tmdbService;
    
    @Override
    public void storeMovies(List<MovieResult> movies, String source) {
        log.info("[MongoMovieChunkService] Storing " + movies.size() + " movies from source: " + source);
        
        for (MovieResult movie : movies) {
            Document doc = new Document();
            doc.put("title", movie.title());
            doc.put("overview", movie.overview());
            doc.put("release_date", movie.releaseDate());
            doc.put("rating", movie.rating());
            doc.put("source", source);
            doc.put("created_at", System.currentTimeMillis());
            
            // Upsert based on title and source
            Query query = new Query(Criteria.where("title").is(movie.title()).and("source").is(source));
            Update update = new Update()
                    .set("overview", movie.overview())
                    .set("release_date", movie.releaseDate())
                    .set("rating", movie.rating())
                    .setOnInsert("created_at", System.currentTimeMillis());
            
            mongoTemplate.upsert(query, update, MOVIES_COLLECTION);
        }
    }
    
    @Override
    public List<ChunkDto> getMovieChunks(String source, int limit, String afterId, String filter, List<String> tags, String sort) {
        log.info("[MongoMovieChunkService] Getting movie chunks for source: " + source);
        
        Query query = new Query();
        
        // Add source filter
        if (source != null && !source.isBlank()) {
            query.addCriteria(Criteria.where("source").is(source));
        }
        
        // Add afterId pagination
        if (afterId != null && !afterId.isBlank()) {
            query.addCriteria(Criteria.where("_id").gt(new ObjectId(afterId)));
        }
        
        // Add filter
        if (filter != null && !filter.isBlank()) {
            if (filter.toLowerCase().contains(" like ")) {
                String[] parts = filter.split("(?i)like", 2);
                String field = parts[0].trim();
                String val = cleanQuotes(parts[1].trim());
                query.addCriteria(Criteria.where(field).regex(val, "i"));
            } else if (filter.contains("=")) {
                String[] parts = filter.split("=", 2);
                String field = parts[0].trim();
                String val = cleanQuotes(parts[1].trim());
                query.addCriteria(Criteria.where(field).is(val));
            }
        }
        
        // Add sorting
        if (sort != null && !sort.isBlank()) {
            query.with(Sort.by(Sort.Order.asc(sort)));
        } else {
            query.with(Sort.by(Sort.Order.asc("_id")));
        }
        
        query.limit(limit);
        
        List<Document> documents = mongoTemplate.find(query, Document.class, MOVIES_COLLECTION);
        
        List<ChunkDto> chunks = new ArrayList<>();
        for (Document doc : documents) {
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
            Map<String, Object> metadata = new HashMap<>(doc);
            chunk.setMetadata(metadata);
            
            chunks.add(chunk);
        }
        
        return chunks;
    }
    
    @Override
    public List<MovieResult> searchAndStore(String title, int limit, boolean store) {
        log.info("[MongoMovieChunkService] Searching for movies with title: " + title + ", store: " + store);
        
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