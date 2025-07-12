package org.shark.melian.integration;

import org.junit.jupiter.api.Test;
import org.shark.melian.integration.config.IntegrationTestConfig;
import org.shark.melian.model.ChunkDto;
import org.shark.melian.model.MovieResult;
import org.shark.melian.service.MovieToolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for MCP server functionality.
 * Tests the MovieToolService with real MCP server running and database connectivity.
 */
@SpringBootTest(
    properties = {"spring.ai.mcp.server.enabled=true"},
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@Import(IntegrationTestConfig.class)
@ActiveProfiles("integration")
@IntegrationTest
class McpServerIntegrationTest extends BaseIntegrationTest {
    
    @Autowired
    private MovieToolService movieToolService;
    
    @Test
    void testMcpServerContextLoads() {
        // Verify MCP server context loads successfully
        assertNotNull(movieToolService);
    }
    
    @Test
    void testSearchMoviesViaMcp() {
        // Test MCP tool: search_movies_by_tmdb_api
        List<MovieResult> results = movieToolService.searchMovies("Matrix", 2);
        
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(2, results.size());
        
        MovieResult firstMovie = results.get(0);
        assertEquals("The Matrix", firstMovie.title());
        assertEquals("1999-03-30", firstMovie.releaseDate());
        assertEquals(8.7, firstMovie.rating());
        assertNotNull(firstMovie.overview());
    }
    
    @Test
    void testSearchAndStoreSqlViaMcp() {
        // Test MCP tool: search_and_store_movies_sql
        List<MovieResult> results = movieToolService.searchAndStoreMoviesSQL("Matrix", 2);
        
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(2, results.size());
        
        // Verify data was stored in SQL database
        List<ChunkDto> storedChunks = movieToolService.getStoredMoviesSQL(null, 10, null);
        assertNotNull(storedChunks);
        assertFalse(storedChunks.isEmpty());
        assertTrue(storedChunks.size() >= 2);
        
        // Verify chunk structure
        ChunkDto chunk = storedChunks.get(0);
        assertNotNull(chunk.getId());
        assertNotNull(chunk.getText());
        assertNotNull(chunk.getMetadata());
        assertTrue(chunk.getText().contains("Movie:"));
        assertTrue(chunk.getText().contains("Matrix"));
    }
    
    @Test
    void testSearchAndStoreMongoViaMcp() {
        // Test MCP tool: search_and_store_movies_mongo
        List<MovieResult> results = movieToolService.searchAndStoreMoviesMongo("Matrix", 2);
        
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(2, results.size());
        
        // Verify data was stored in MongoDB
        List<ChunkDto> storedChunks = movieToolService.getStoredMoviesMongo(null, 10, null);
        assertNotNull(storedChunks);
        assertFalse(storedChunks.isEmpty());
        assertTrue(storedChunks.size() >= 2);
        
        // Verify chunk structure
        ChunkDto chunk = storedChunks.get(0);
        assertNotNull(chunk.getId());
        assertNotNull(chunk.getText());
        assertNotNull(chunk.getMetadata());
        assertTrue(chunk.getText().contains("Movie:"));
        assertTrue(chunk.getText().contains("Matrix"));
    }
    
    @Test
    void testGetStoredMoviesWithFilterViaMcp() {
        // First store some movies
        movieToolService.searchAndStoreMoviesSQL("Matrix", 2);
        
        // Test MCP tool: get_stored_movies_sql with filter
        List<ChunkDto> filteredChunks = movieToolService.getStoredMoviesSQL("title like 'Matrix'", 5, null);
        
        assertNotNull(filteredChunks);
        assertFalse(filteredChunks.isEmpty());
        
        // Verify all results contain Matrix in title
        for (ChunkDto chunk : filteredChunks) {
            assertTrue(chunk.getText().contains("Matrix"));
        }
    }
    
    @Test
    void testCrossServiceIntegration() {
        // Test integration between SQL and MongoDB services
        
        // Store movies in SQL
        List<MovieResult> sqlResults = movieToolService.searchAndStoreMoviesSQL("Matrix", 1);
        
        // Store movies in MongoDB
        List<MovieResult> mongoResults = movieToolService.searchAndStoreMoviesMongo("Matrix", 1);
        
        // Verify both results are similar (same source data)
        assertEquals(sqlResults.size(), mongoResults.size());
        assertEquals(sqlResults.get(0).title(), mongoResults.get(0).title());
        
        // Verify both storages work independently
        List<ChunkDto> sqlChunks = movieToolService.getStoredMoviesSQL(null, 10, null);
        List<ChunkDto> mongoChunks = movieToolService.getStoredMoviesMongo(null, 10, null);
        
        assertTrue(sqlChunks.size() >= 1);
        assertTrue(mongoChunks.size() >= 1);
    }
}