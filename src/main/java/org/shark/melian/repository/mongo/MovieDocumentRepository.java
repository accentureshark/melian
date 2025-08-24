package org.shark.melian.repository.mongo;

import lombok.extern.slf4j.Slf4j;
import org.shark.melian.document.MovieDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Spring Data MongoDB Repository para MovieDocument con métodos mejorados de búsqueda.
 */
@Repository

public interface MovieDocumentRepository extends MongoRepository<MovieDocument, String>, CustomMovieDocumentRepository {

    Optional<MovieDocument> findByTitle(String title);

    @org.springframework.data.mongodb.repository.Query("{ '_id': { $gt: ?1 } }")
    List<MovieDocument> findByIdGreaterThan(String afterId, Pageable pageable);

    @org.springframework.data.mongodb.repository.Query("{ $and: [ { $or: [ { '_id': { $exists: false } }, { '_id': { $gt: ?1 } } ] } ] }")
    List<MovieDocument> findMoviesWithCriteria(String afterId, Pageable pageable);
}

/**
 * Interfaz para métodos personalizados
 */
interface CustomMovieDocumentRepository {
    List<MovieDocument> searchByTitle(String query, Pageable pageable, String locale);
    List<MovieDocument> searchByTitleExact(String exactTitle, String locale);
    List<MovieDocument> searchByTitleFuzzy(String query, Pageable pageable, String locale);
}

/**
 * Implementación personalizada para búsquedas avanzadas con logs
 */
@Component
@Slf4j
class CustomMovieDocumentRepositoryImpl implements CustomMovieDocumentRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    private String normalizeQuery(String query) {
        return Normalizer.normalize(query, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .replaceAll("\\s+", " ");
    }

    @Override
    public List<MovieDocument> searchByTitle(String query, Pageable pageable, String locale) {
        if (mongoTemplate == null) {
            log.warn("MongoTemplate not available, returning empty results");
            return List.of();
        }
        
        String normalizedQuery = normalizeQuery(query);
        if (normalizedQuery.isEmpty()) {
            return List.of();
        }
        log.info("Buscando películas con título que contenga: '{}'", normalizedQuery);

        Pattern pattern = Pattern.compile(".*" + Pattern.quote(normalizedQuery) + ".*", Pattern.CASE_INSENSITIVE);
        Criteria criteria = new Criteria().orOperator(
                Criteria.where("title").regex(pattern),
                Criteria.where("orig_title").regex(pattern)
        );

        Query mongoQuery = new Query(criteria);

        mongoQuery.limit(pageable.getPageSize()).skip(pageable.getOffset());
        mongoQuery.with(pageable.getSort());
        mongoQuery.collation(Collation.of(locale).strength(1));
        log.info("Consulta generada: {}", mongoQuery);

        List<MovieDocument> results = mongoTemplate.find(mongoQuery, MovieDocument.class);
        log.info("Encontradas {} películas para consulta: '{}'", results.size(), normalizedQuery);

        return results;
    }

    @Override
    public List<MovieDocument> searchByTitleExact(String exactTitle, String locale) {
        if (mongoTemplate == null) {
            log.warn("MongoTemplate not available, returning empty results");
            return List.of();
        }
        
        exactTitle = exactTitle.trim();
        log.info("Buscando películas con título exacto: '{}'", exactTitle);

        Query mongoQuery = new Query();
        mongoQuery.addCriteria(new Criteria().orOperator(
                Criteria.where("title").is(exactTitle),
                Criteria.where("orig_title").is(exactTitle)
        ));
        mongoQuery.collation(Collation.of(locale).strength(1));

        List<MovieDocument> results = mongoTemplate.find(mongoQuery, MovieDocument.class);
        log.info("Encontradas {} películas con título exacto: '{}'", results.size(), exactTitle);

        return results;
    }

    @Override
    public List<MovieDocument> searchByTitleFuzzy(String query, Pageable pageable, String locale) {
        if (mongoTemplate == null) {
            log.warn("MongoTemplate not available, returning empty results");
            return List.of();
        }
        
        String normalizedQuery = normalizeQuery(query);
        if (normalizedQuery.isEmpty()) {
            return List.of();
        }
        log.info("Realizando búsqueda fuzzy para: '{}'", normalizedQuery);

        Pattern pattern = Pattern.compile(".*" + Pattern.quote(normalizedQuery) + ".*", Pattern.CASE_INSENSITIVE);
        Criteria criteria = new Criteria().orOperator(
                Criteria.where("title").regex(pattern),
                Criteria.where("orig_title").regex(pattern),
                Criteria.where("overview").regex(pattern)
        );

        Query mongoQuery = new Query(criteria);

        mongoQuery.limit(pageable.getPageSize()).skip(pageable.getOffset());
        mongoQuery.with(pageable.getSort());
        mongoQuery.collation(Collation.of(locale).strength(1));
        log.info("Consulta generada: {}", mongoQuery);

        List<MovieDocument> results = mongoTemplate.find(mongoQuery, MovieDocument.class);
        log.info("Encontradas {} películas en búsqueda fuzzy para: '{}'", results.size(), normalizedQuery);

        return results;
    }
}
