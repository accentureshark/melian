package org.shark.melian.service;

import io.modelcontextprotocol.spec.McpSchema;
import org.shark.melian.model.MovieResult;
import org.springframework.ai.tool.annotation.Tool;

import java.util.List;
import java.util.Map;

public class MovieToolService {
	private final TMDBService tmdb;

	public MovieToolService(TMDBService tmdb) {
		this.tmdb = tmdb;
	}

	@Tool(name = "search_movies_", description = "Busca películas por título usando la API de TMDB")
	public List<MovieResult> searchMovies(String title, Integer limit) {
		return tmdb.search(title, limit != null ? limit : 3);
	}
}
