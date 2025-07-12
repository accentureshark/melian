package org.shark.melian.integration;

import org.junit.jupiter.api.Test;
import org.shark.melian.integration.config.IntegrationTestConfig;
import org.shark.melian.model.ChunkDto;
import org.shark.melian.model.MovieResult;
import org.shark.melian.service.MovieToolService;
import org.shark.melian.service.MongoMovieChunkService;
import org.shark.melian.service.SqlMovieChunkService;
import org.shark.melian.service.TMDBService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration test for all services together.
 * Tests MCP server, MySQL, MongoDB, and REST API integration.
 */
@SpringBootTest(
    properties = {"spring.ai.mcp.server.enabled=true"},
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@Import(IntegrationTestConfig.class)
@ActiveProfiles("integration")
@IntegrationTest
class FullStackIntegrationTest extends BaseIntegrationTest {
    
    @Autowired
    private MovieToolService movieToolService;
    
    @Autowired
    private TMDBService tmdbService;
    
    @Autowired
    private SqlMovieChunkService sqlMovieChunkService;
    
    @Autowired
    private MongoMovieChunkService mongoMovieChunkService;
    
    @Test
    void testAllServicesAreAvailable() {
        // Verify all services are properly autowired
        assertNotNull(movieToolService);
        assertNotNull(tmdbService);
        assertNotNull(sqlMovieChunkService);
        assertNotNull(mongoMovieChunkService);
    }
    
    @Test
    void testFullWorkflowWithMcpServer() {
        // Test complete workflow: REST API -> MCP Tools -> Database Storage -> Retrieval
        
        // Step 1: Search movies via REST API
        List<MovieResult> apiResults = tmdbService.search("Matrix", 2);
        assertNotNull(apiResults);
        assertFalse(apiResults.isEmpty());
        
        // Step 2: Search and store via MCP SQL tool
        List<MovieResult> sqlResults = movieToolService.searchAndStoreMoviesSQL("Matrix", 2);
        assertNotNull(sqlResults);
        assertEquals(apiResults.size(), sqlResults.size());
        
        // Step 3: Search and store via MCP MongoDB tool
        List<MovieResult> mongoResults = movieToolService.searchAndStoreMoviesMongo("Matrix", 2);
        assertNotNull(mongoResults);
        assertEquals(apiResults.size(), mongoResults.size());
        
        // Step 4: Retrieve stored data via MCP tools
        List<ChunkDto> sqlChunks = movieToolService.getStoredMoviesSQL(null, 10, null);
        List<ChunkDto> mongoChunks = movieToolService.getStoredMoviesMongo(null, 10, null);
        
        assertNotNull(sqlChunks);
        assertNotNull(mongoChunks);
        assertTrue(sqlChunks.size() >= 2);
        assertTrue(mongoChunks.size() >= 2);
        
        // Step 5: Verify data consistency across storage systems
        verifyDataConsistency(sqlChunks, mongoChunks);
    }
    
    @Test
    void testConcurrentOperations() throws ExecutionException, InterruptedException {
        // Test concurrent operations across different services
        
        CompletableFuture<List<MovieResult>> sqlFuture = CompletableFuture.supplyAsync(() ->
            movieToolService.searchAndStoreMoviesSQL("Matrix", 2)
        );
        
        CompletableFuture<List<MovieResult>> mongoFuture = CompletableFuture.supplyAsync(() ->
            movieToolService.searchAndStoreMoviesMongo("Matrix", 2)
        );
        
        CompletableFuture<List<MovieResult>> apiFuture = CompletableFuture.supplyAsync(() ->
            tmdbService.search("Matrix", 2)
        );
        
        // Wait for all operations to complete
        List<MovieResult> sqlResults = sqlFuture.get();
        List<MovieResult> mongoResults = mongoFuture.get();
        List<MovieResult> apiResults = apiFuture.get();
        
        // Verify all operations completed successfully
        assertNotNull(sqlResults);
        assertNotNull(mongoResults);
        assertNotNull(apiResults);
        
        assertEquals(apiResults.size(), sqlResults.size());
        assertEquals(apiResults.size(), mongoResults.size());
    }
    
    @Test
    void testDataPersistenceAndRetrieval() {
        // Test that data persists correctly across multiple operations
        
        // Store initial data
        movieToolService.searchAndStoreMoviesSQL("Matrix", 2);
        movieToolService.searchAndStoreMoviesMongo("Matrix", 2);
        
        // Retrieve and verify
        List<ChunkDto> sqlChunks1 = movieToolService.getStoredMoviesSQL(null, 10, null);
        List<ChunkDto> mongoChunks1 = movieToolService.getStoredMoviesMongo(null, 10, null);
        
        // Store more data
        movieToolService.searchAndStoreMoviesSQL("Matrix", 2);
        movieToolService.searchAndStoreMoviesMongo("Matrix", 2);
        
        // Retrieve again
        List<ChunkDto> sqlChunks2 = movieToolService.getStoredMoviesSQL(null, 10, null);
        List<ChunkDto> mongoChunks2 = movieToolService.getStoredMoviesMongo(null, 10, null);
        
        // Verify data consistency (should be same due to upsert behavior)
        assertEquals(sqlChunks1.size(), sqlChunks2.size());
        assertEquals(mongoChunks1.size(), mongoChunks2.size());
    }
    
    @Test
    void testErrorHandlingAndRecovery() {
        // Test system behavior with various error conditions
        
        // Test empty search results
        List<MovieResult> emptyResults = tmdbService.search("NonExistentMovie12345", 5);
        assertNotNull(emptyResults);
        // Mock always returns results, so we expect data
        
        // Test zero limit
        List<MovieResult> zeroLimitResults = tmdbService.search("Matrix", 0);
        assertNotNull(zeroLimitResults);
        assertTrue(zeroLimitResults.isEmpty());
        
        // Test very large limit
        List<MovieResult> largeLimitResults = tmdbService.search("Matrix", 1000);
        assertNotNull(largeLimitResults);
        assertTrue(largeLimitResults.size() <= 2); // Mock returns max 2 results
    }
    
    @Test
    void testMcpServerToolsWithRealDatabases() {
        // Test all MCP tools work with real database connections
        
        // Test search_movies_by_tmdb_api
        List<MovieResult> searchResults = movieToolService.searchMovies("Matrix", 2);
        assertNotNull(searchResults);
        assertFalse(searchResults.isEmpty());
        
        // Test search_and_store_movies_sql
        List<MovieResult> sqlStoreResults = movieToolService.searchAndStoreMoviesSQL("Matrix", 2);
        assertNotNull(sqlStoreResults);
        assertEquals(searchResults.size(), sqlStoreResults.size());
        
        // Test search_and_store_movies_mongo
        List<MovieResult> mongoStoreResults = movieToolService.searchAndStoreMoviesMongo("Matrix", 2);
        assertNotNull(mongoStoreResults);
        assertEquals(searchResults.size(), mongoStoreResults.size());
        
        // Test get_stored_movies_sql
        List<ChunkDto> sqlChunks = movieToolService.getStoredMoviesSQL(null, 10, null);
        assertNotNull(sqlChunks);
        assertFalse(sqlChunks.isEmpty());
        
        // Test get_stored_movies_mongo
        List<ChunkDto> mongoChunks = movieToolService.getStoredMoviesMongo(null, 10, null);
        assertNotNull(mongoChunks);
        assertFalse(mongoChunks.isEmpty());
        
        // Test filtered retrieval
        List<ChunkDto> filteredSqlChunks = movieToolService.getStoredMoviesSQL("title like 'Matrix'", 5, null);
        List<ChunkDto> filteredMongoChunks = movieToolService.getStoredMoviesMongo("title like Matrix", 5, null);
        
        assertNotNull(filteredSqlChunks);
        assertNotNull(filteredMongoChunks);
        assertFalse(filteredSqlChunks.isEmpty());
        assertFalse(filteredMongoChunks.isEmpty());
    }
    
    @Test
    void testSystemPerformanceWithRealDatabases() {
        // Test system performance with real database operations
        long startTime = System.currentTimeMillis();
        
        // Perform multiple operations
        for (int i = 0; i < 3; i++) {
            movieToolService.searchAndStoreMoviesSQL("Matrix", 2);
            movieToolService.searchAndStoreMoviesMongo("Matrix", 2);
            movieToolService.getStoredMoviesSQL(null, 10, null);
            movieToolService.getStoredMoviesMongo(null, 10, null);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Should complete within reasonable time
        assertTrue(duration < 30000); // Less than 30 seconds
    }
    
    private void verifyDataConsistency(List<ChunkDto> sqlChunks, List<ChunkDto> mongoChunks) {
        // Verify that data stored in SQL and MongoDB is consistent
        assertNotNull(sqlChunks);
        assertNotNull(mongoChunks);
        
        // Both should have same number of records
        assertEquals(sqlChunks.size(), mongoChunks.size());
        
        // Verify content consistency
        for (int i = 0; i < sqlChunks.size(); i++) {
            ChunkDto sqlChunk = sqlChunks.get(i);
            ChunkDto mongoChunk = mongoChunks.get(i);
            
            // Text content should be similar (contains same movie information)
            assertTrue(sqlChunk.getText().contains("Matrix"));
            assertTrue(mongoChunk.getText().contains("Matrix"));
            
            // Metadata should contain similar information
            assertNotNull(sqlChunk.getMetadata().get("title"));
            assertNotNull(mongoChunk.getMetadata().get("title"));
            
            // Both should have same source
            assertEquals("tmdb", sqlChunk.getMetadata().get("source"));
            assertEquals("tmdb", mongoChunk.getMetadata().get("source"));
        }
    }
}