package org.shark.melian.service;

import org.shark.melian.model.MovieResult;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MovieToolService {
    private final TMDBService tmdb;
    private final MovieSqlService movieSqlService;
    private final MovieMongoService movieMongoService;

    public MovieToolService(TMDBService tmdbService,
                            MovieSqlService movieSqlService,
                            MovieMongoService movieMongoService) {
        this.tmdb = tmdbService;
        this.movieSqlService = movieSqlService;
        this.movieMongoService = movieMongoService;
    }

    @Tool(name = "search_movies_tmdb", description = "Busca peliculas por titulo usando la API de TMDB")
    public List<MovieResult> searchMoviesApi(String title, Integer limit) {
        return tmdb.search(title, limit != null ? limit : 3);
    }

    @Tool(name = "search_movies_sql", description = "Busca peliculas por titulo usando la base SQL")
    public List<MovieResult> searchMoviesSql(String title, Integer limit) {
        return movieSqlService.search(title, limit != null ? limit : 3);
    }

    @Tool(name = "search_movies_mongo", description = "Busca peliculas por titulo usando MongoDB")
    public List<MovieResult> searchMoviesMongo(String title, Integer limit) {
        return movieMongoService.search(title, limit != null ? limit : 3);
    }
}
