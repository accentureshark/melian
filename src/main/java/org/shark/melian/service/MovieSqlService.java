package org.shark.melian.service;

import org.shark.melian.model.MovieResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MovieSqlService {
    private final JdbcTemplate jdbcTemplate;

    public MovieSqlService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MovieResult> search(String title, int limit) {
        String sql = """
                SELECT title, description, release_year, imdb_rating
                FROM film
                WHERE LOWER(title) LIKE LOWER(?)
                LIMIT ?
                """;
        return jdbcTemplate.query(
                sql,
                (rs, i) -> new MovieResult(
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("release_year"),
                        rs.getDouble("imdb_rating")
                ),
                "%" + title + "%", limit
        );
    }
}
