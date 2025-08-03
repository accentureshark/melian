package org.shark.melian.repository;

import org.shark.melian.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Movie entity following best practices.
 */
@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    /**
     * Find movie by title and source (for unique constraint handling)
     */
    Optional<Movie> findByTitleAndSource(String title, String source);

    /**
     * Find movies by source with pagination
     */
    Page<Movie> findBySource(String source, Pageable pageable);

    /**
     * Find movies by source, ordered by ID, with ID greater than afterId
     */
    @Query("SELECT m FROM Movie m WHERE m.source = :source AND m.id > :afterId ORDER BY m.id")
    List<Movie> findBySourceAndIdGreaterThan(@Param("source") String source, 
                                           @Param("afterId") Long afterId, 
                                           Pageable pageable);

    /**
     * Search movies by title containing search term (case insensitive)
     */
    @Query("SELECT m FROM Movie m WHERE LOWER(m.title) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY m.id")
    List<Movie> searchByTitle(@Param("query") String query, Pageable pageable);

    /**
     * Custom query to filter movies with dynamic criteria
     */
    @Query("SELECT m FROM Movie m WHERE (:source IS NULL OR m.source = :source) " +
           "AND (:afterId IS NULL OR m.id > :afterId) " +
           "ORDER BY m.id")
    List<Movie> findMoviesWithCriteria(@Param("source") String source,
                                      @Param("afterId") Long afterId,
                                      Pageable pageable);
}