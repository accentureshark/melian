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
     * Buscar película por título (único por título)
     */
    Optional<Movie> findByTitle(String title);

    /**
     * Paginación simple de películas
     */
    Page<Movie> findAll(Pageable pageable);

    /**
     * Buscar películas con id mayor a afterId (para paginación incremental)
     */
    @Query("SELECT m FROM Movie m WHERE m.id > :afterId ORDER BY m.id")
    List<Movie> findByIdGreaterThan(@Param("afterId") Long afterId, Pageable pageable);

    /**
     * Buscar películas por título (búsqueda parcial, insensible a mayúsculas)
     */
    @Query("SELECT m FROM Movie m WHERE LOWER(m.title) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY m.id")
    List<Movie> searchByTitle(@Param("query") String query, Pageable pageable);
}