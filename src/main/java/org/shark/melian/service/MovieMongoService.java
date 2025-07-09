package org.shark.melian.service;

import org.bson.Document;
import org.shark.melian.model.MovieResult;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MovieMongoService {
    private final MongoTemplate mongoTemplate;

    public MovieMongoService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<MovieResult> search(String title, int limit) {
        Query query = Query.query(Criteria.where("title").regex(title, "i")).limit(limit);
        return mongoTemplate.find(query, Document.class, "film")
                .stream()
                .map(doc -> new MovieResult(
                        doc.getString("title"),
                        doc.getString("description"),
                        String.valueOf(doc.get("release_year")),
                        doc.get("imdb_rating") instanceof Number
                                ? ((Number) doc.get("imdb_rating")).doubleValue()
                                : 0.0
                ))
                .toList();
    }
}
