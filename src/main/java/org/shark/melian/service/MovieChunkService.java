package org.shark.melian.service;

import org.shark.melian.model.ChunkDto;
import org.shark.melian.model.MovieResult;

import java.util.List;

/**
 * MCP-compliant interface for movie-related chunk operations.
 * Allows storing and retrieving movie data as chunks in different storage backends.
 */
public interface MovieChunkService {
    
    /**
     * Store movie data as chunks for later retrieval
     */
    void storeMovies(List<MovieResult> movies, String source);
    
    /**
     * Retrieve movie chunks from storage
     */
    List<ChunkDto> getMovieChunks(
            String source, 
            int limit, 
            String afterId, 
            String filter, 
            List<String> tags, 
            String sort
    );
    
    /**
     * Search movies by title and optionally store results
     */
    List<MovieResult> searchAndStore(String title, int limit, boolean store);
}