package org.shark.melian.repository;

import org.shark.melian.document.MovieDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB Repository for MovieDocument following best practices.
 */
@Repository
public interface MovieDocumentRepository extends MongoRepository<MovieDocument, String> {

    /**
     * Find movie by title and source (for unique constraint handling)
     */
    Optional<MovieDocument> findByTitleAndSource(String title, String source);

    /**
     * Find movies by source with pagination
     */
    Page<MovieDocument> findBySource(String source, Pageable pageable);

    /**
     * Search movies by title containing search term (case insensitive)
     */
    @Query("{ 'title': { $regex: ?0, $options: 'i' } }")
    List<MovieDocument> searchByTitle(String query, Pageable pageable);

    /**
     * Find movies by source and ID greater than afterId for pagination
     */
    @Query("{ 'source': ?0, '_id': { $gt: ?1 } }")
    List<MovieDocument> findBySourceAndIdGreaterThan(String source, String afterId, Pageable pageable);

    /**
     * Complex query with multiple criteria
     */
    @Query("{ $and: [ " +
           "  { $or: [ { 'source': { $exists: false } }, { 'source': ?0 } ] }, " +
           "  { $or: [ { '_id': { $exists: false } }, { '_id': { $gt: ?1 } } ] } " +
           "] }")
    List<MovieDocument> findMoviesWithCriteria(String source, String afterId, Pageable pageable);
}