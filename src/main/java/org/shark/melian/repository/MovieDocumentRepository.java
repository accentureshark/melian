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
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Set;
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

    private static final Set<String> STOP_WORDS = Set.of("the", "a", "an", "of", "and");

    private String[] prepareWords(String query) {
        String normalized = Normalizer.normalize(query, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim()
                .replaceAll("\\s+", " ");
        return Arrays.stream(normalized.split(" "))
                .filter(word -> !STOP_WORDS.contains(word))
                .toArray(String[]::new);
    }

    @Override
    public List<MovieDocument> searchByTitle(String query, Pageable pageable, String locale) {
        String[] words = prepareWords(query);
        if (words.length == 0) {
            return List.of();
        }
        log.info("Buscando películas con título que contenga: '{}'", String.join(" ", words));

        Query mongoQuery = new Query();

        if (words.length > 1) {
            Criteria[] criterias = new Criteria[words.length];
            for (int i = 0; i < words.length; i++) {
                String quotedWord = Pattern.quote(words[i]);
                criterias[i] = Criteria.where("title")
                        .regex(Pattern.compile(".*" + quotedWord + ".*", Pattern.CASE_INSENSITIVE));
            }
            mongoQuery.addCriteria(new Criteria().andOperator(criterias));
        } else {
            String quotedQuery = Pattern.quote(words[0]);
            mongoQuery.addCriteria(Criteria.where("title")
                    .regex(Pattern.compile(".*" + quotedQuery + ".*", Pattern.CASE_INSENSITIVE)));
        }

        mongoQuery.limit(pageable.getPageSize()).skip(pageable.getOffset());
        mongoQuery.with(pageable.getSort());
        mongoQuery.collation(Collation.of("en").strength(1));
        log.info("Consulta generada: {}", mongoQuery);

        List<MovieDocument> results = mongoTemplate.find(mongoQuery, MovieDocument.class);
        log.info("Encontradas {} películas para consulta: '{}'", results.size(), String.join(" ", words));

        return results;
    }

    @Override
    public List<MovieDocument> searchByTitleExact(String exactTitle, String locale) {
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
        String[] words = prepareWords(query);
        if (words.length == 0) {
            return List.of();
        }
        String normalizedQuery = String.join(" ", words);
        log.info("Realizando búsqueda fuzzy para: '{}'", normalizedQuery);

        String quotedQuery = Pattern.quote(normalizedQuery);
        Pattern pattern = Pattern.compile(".*" + quotedQuery + ".*", Pattern.CASE_INSENSITIVE);
        Criteria criteria = new Criteria().orOperator(
                Criteria.where("title").regex(pattern),
                Criteria.where("orig_title").regex(pattern),
                Criteria.where("overview").regex(pattern)
        );

        Query mongoQuery = new Query(criteria);

        mongoQuery.limit(pageable.getPageSize()).skip(pageable.getOffset());
        mongoQuery.with(pageable.getSort());
        mongoQuery.collation(Collation.of("en").strength(1));
        log.info("Consulta generada: {}", mongoQuery);

        List<MovieDocument> results = mongoTemplate.find(mongoQuery, MovieDocument.class);
        log.info("Encontradas {} películas en búsqueda fuzzy para: '{}'", results.size(), normalizedQuery);

        return results;
    }
}
