package org.shark.melian.service;

import org.shark.melian.model.ChunkDto;
import org.shark.melian.model.MovieResult;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.logging.Logger;

/**
 * MCP-compliant Movie Tool Service providing AI tools for movie search and storage.
 * Integrates with both SQL and MongoDB storage backends.
 */
@Service
public class MovieToolService {
    
    private static final Logger log = Logger.getLogger(MovieToolService.class.getName());
    
    private final TMDBService tmdbService;
    private final MovieChunkService sqlMovieChunkService;
    private final MovieChunkService mongoMovieChunkService;

    public MovieToolService(
            TMDBService tmdbService,
            @Qualifier("sqlMovieChunkService") MovieChunkService sqlMovieChunkService,
            @Qualifier("mongoMovieChunkService") MovieChunkService mongoMovieChunkService
    ) {
        this.tmdbService = tmdbService;
        this.sqlMovieChunkService = sqlMovieChunkService;
        this.mongoMovieChunkService = mongoMovieChunkService;
        log.info("[MovieToolService] Initialized with MCP-compliant services");
    }

    @Tool(name = "search_movies_by_tmdb_api", description = "Busca peliculas por titulo usando la API de TMDB")
    public List<MovieResult> searchMovies(String title, Integer limit) {
        return tmdbService.search(title, limit != null ? limit : 3);
    }
    
    @Tool(name = "search_and_store_movies_sql", description = "Busca peliculas y las almacena en base de datos SQL")
    public List<MovieResult> searchAndStoreMoviesSQL(String title, Integer limit) {
        return sqlMovieChunkService.searchAndStore(title, limit != null ? limit : 3, true);
    }
    
    @Tool(name = "search_and_store_movies_mongo", description = "Busca peliculas y las almacena en MongoDB")
    public List<MovieResult> searchAndStoreMoviesMongo(String title, Integer limit) {
        return mongoMovieChunkService.searchAndStore(title, limit != null ? limit : 3, true);
    }
    
    @Tool(name = "get_stored_movies_sql", description = "Obtiene peliculas almacenadas desde SQL como chunks MCP")
    public List<ChunkDto> getStoredMoviesSQL(String filter, Integer limit, String afterId) {
        return sqlMovieChunkService.getMovieChunks("tmdb", limit != null ? limit : 10, afterId, filter, null, null);
    }
    
    @Tool(name = "get_stored_movies_mongo", description = "Obtiene peliculas almacenadas desde MongoDB como chunks MCP") 
    public List<ChunkDto> getStoredMoviesMongo(String filter, Integer limit, String afterId) {
        return mongoMovieChunkService.getMovieChunks("tmdb", limit != null ? limit : 10, afterId, filter, null, null);
    }
}
