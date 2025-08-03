package org.shark.melian.repository;

import lombok.extern.slf4j.Slf4j;
import org.shark.melian.document.MovieDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Spring Data MongoDB Repository para MovieDocument con métodos mejorados de búsqueda.
 */
@Repository
public interface MovieDocumentRepository extends MongoRepository<MovieDocument, String>, CustomMovieDocumentRepository {

    /**
     * Encuentra película por título y fuente (para manejar restricciones únicas)
     */
    Optional<MovieDocument> findByTitleAndSource(String title, String source);

    /**
     * Encuentra películas por fuente con paginación
     */
    Page<MovieDocument> findBySource(String source, Pageable pageable);

    /**
     * Encuentra películas por fuente e ID mayor que afterId para paginación
     */
    @org.springframework.data.mongodb.repository.Query("{ 'source': ?0, '_id': { $gt: ?1 } }")
    List<MovieDocument> findBySourceAndIdGreaterThan(String source, String afterId, Pageable pageable);

    /**
     * Consulta compleja con múltiples criterios
     */
    @org.springframework.data.mongodb.repository.Query("{ $and: [ " +
            "  { $or: [ { 'source': { $exists: false } }, { 'source': ?0 } ] }, " +
            "  { $or: [ { '_id': { $exists: false } }, { '_id': { $gt: ?1 } } ] } " +
            "] }")
    List<MovieDocument> findMoviesWithCriteria(String source, String afterId, Pageable pageable);
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

    /**
     * Busca películas por título usando expresión regular con caracteres especiales escapados
     */
    @Override
    public List<MovieDocument> searchByTitle(String query, Pageable pageable) {
        // Escapar caracteres especiales de regex
        String escapedQuery = Pattern.quote(query);

        log.info("Buscando películas con título que contenga: '{}' (escapado: '{}')", query, escapedQuery);

        Query mongoQuery = new Query();
        // Usa criterio con regex escapado correctamente
        mongoQuery.addCriteria(Criteria.where("title").regex(escapedQuery, "i"));
        mongoQuery.with(pageable);

        List<MovieDocument> results = mongoTemplate.find(mongoQuery, MovieDocument.class);
        log.info("Encontradas {} películas para consulta: '{}'", results.size(), query);

        // Log detallado si no hay resultados
        if (results.isEmpty()) {
            log.warn("No se encontraron películas para '{}'. Verificando si existen documentos similares...", query);
            // Busca títulos que podrían coincidir parcialmente para diagnóstico
            Query diagnosticQuery = new Query();
            diagnosticQuery.addCriteria(new Criteria().orOperator(
                    Criteria.where("title").regex(".*" + query.substring(0, Math.min(3, query.length())) + ".*", "i")
            ));
            diagnosticQuery.limit(5);
            List<MovieDocument> similarDocs = mongoTemplate.find(diagnosticQuery, MovieDocument.class);

            if (!similarDocs.isEmpty()) {
                log.info("Películas similares encontradas que podrían coincidir:");
                similarDocs.forEach(doc -> log.info(" - '{}' (id: {})", doc.getTitle(), doc.getId()));
            } else {
                log.warn("No se encontraron películas similares en la colección");
            }
        }

        return results;
    }

    /**
     * Busca películas por título exacto (útil para depuración)
     */
    @Override
    public List<MovieDocument> searchByTitleExact(String exactTitle) {
        log.info("Buscando películas con título exacto: '{}'", exactTitle);

        Query mongoQuery = new Query();
        mongoQuery.addCriteria(Criteria.where("title").is(exactTitle));

        List<MovieDocument> results = mongoTemplate.find(mongoQuery, MovieDocument.class);
        log.info("Encontradas {} películas con título exacto: '{}'", results.size(), exactTitle);

        return results;
    }

    /**
     * Busca películas usando la funcionalidad de búsqueda de texto de MongoDB
     * Requiere un índice de texto en el campo 'title'
     */
    @Override
    public List<MovieDocument> searchByTitleFuzzy(String query, Pageable pageable) {
        log.info("Realizando búsqueda fuzzy para: '{}'", query);

        Query mongoQuery = new Query();
        mongoQuery.addCriteria(Criteria.where("$text").exists(true).andOperator(
                Criteria.where("$text").is(query)
        ));
        mongoQuery.with(pageable);

        List<MovieDocument> results = mongoTemplate.find(mongoQuery, MovieDocument.class);
        log.info("Encontradas {} películas en búsqueda fuzzy para: '{}'", results.size(), query);

        return results;
    }
}