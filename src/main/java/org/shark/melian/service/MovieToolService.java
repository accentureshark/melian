package org.shark.melian.service;

import org.shark.melian.model.MovieResult;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MovieToolService {
	private final TMDBService tmdb;

    public MovieToolService(TMDBService tmdbService) {
        this.tmdb = tmdbService;
    }

    @Tool(name = "search_movies_by_tmdb_api", description = "Busca peliculas por titulo usando la API de TMDB")
	public List<MovieResult> searchMovies(String title, Integer limit) {
		return tmdb.search(title, limit != null ? limit : 3);
	}
}
