package org.shark.melian.repository;

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
    List<MovieDocument> searchByTitle(String query, Pageable pageable);
    List<MovieDocument> searchByTitleExact(String exactTitle);
    List<MovieDocument> searchByTitleFuzzy(String query, Pageable pageable);
}

/**
 * Implementación personalizada para búsquedas avanzadas con logs
 */
@Component
@Slf4j
class CustomMovieDocumentRepositoryImpl implements CustomMovieDocumentRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<MovieDocument> searchByTitle(String query, Pageable pageable) {
        query = query.trim().replaceAll("\\s+", " ");
        log.info("Buscando películas con título que contenga: '{}'", query);

        Query mongoQuery = new Query();

        String[] words = query.split(" ");
        if (words.length > 1) {
            Criteria[] criterias = new Criteria[words.length];
            for (int i = 0; i < words.length; i++) {
                criterias[i] = Criteria.where("title").regex(".*" + words[i] + ".*", "i");
            }
            mongoQuery.addCriteria(new Criteria().andOperator(criterias));
        } else {
            mongoQuery.addCriteria(Criteria.where("title").regex(".*" + query + ".*", "i"));
        }

        mongoQuery.with(pageable).collation(Collation.of("en").strength(1));
        List<MovieDocument> results = mongoTemplate.find(mongoQuery, MovieDocument.class);
        log.info("Encontradas {} películas para consulta: '{}'", results.size(), query);

        return results;
    }

    @Override
    public List<MovieDocument> searchByTitleExact(String exactTitle) {
        exactTitle = exactTitle.trim();
        log.info("Buscando películas con título exacto: '{}'", exactTitle);

        Query mongoQuery = new Query();
        mongoQuery.addCriteria(new Criteria().orOperator(
                Criteria.where("title").is(exactTitle),
                Criteria.where("orig_title").is(exactTitle)
        ));
        mongoQuery.collation(Collation.of("en").strength(1));

        List<MovieDocument> results = mongoTemplate.find(mongoQuery, MovieDocument.class);
        log.info("Encontradas {} películas con título exacto: '{}'", results.size(), exactTitle);

        return results;
    }

    @Override
    public List<MovieDocument> searchByTitleFuzzy(String query, Pageable pageable) {
        query = query.trim().replaceAll("\\s+", " ");
        log.info("Realizando búsqueda fuzzy para: '{}'", query);

        Criteria criteria = new Criteria().orOperator(
                Criteria.where("title").regex(".*" + query + ".*", "i"),
                Criteria.where("orig_title").regex(".*" + query + ".*", "i"),
                Criteria.where("overview").regex(".*" + query + ".*", "i")
        );

        Query mongoQuery = new Query(criteria);
        mongoQuery.with(pageable).collation(Collation.of("en").strength(1));

        List<MovieDocument> results = mongoTemplate.find(mongoQuery, MovieDocument.class);
        log.info("Encontradas {} películas en búsqueda fuzzy para: '{}'", results.size(), query);

        return results;
    }
}
