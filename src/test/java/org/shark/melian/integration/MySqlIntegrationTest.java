package org.shark.melian.integration;

import org.junit.jupiter.api.Test;
import org.shark.melian.integration.config.IntegrationTestConfig;
import org.shark.melian.model.ChunkDto;
import org.shark.melian.model.MovieResult;
import org.shark.melian.service.SqlMovieChunkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for MySQL database operations.
 * Tests SqlMovieChunkService with real MySQL database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(IntegrationTestConfig.class)
@ActiveProfiles("integration")
@IntegrationTest
class MySqlIntegrationTest extends BaseIntegrationTest {
    
    @Autowired
    private SqlMovieChunkService sqlMovieChunkService;
    
    @Test
    void testMySqlConnection() {
        // Verify MySQL connection works
        assertNotNull(sqlMovieChunkService);
    }
    
    @Test
    void testSearchAndStoreMoviesInMySql() {
        // Test search and store functionality with MySQL
        List<MovieResult> results = sqlMovieChunkService.searchAndStore("Matrix", 2, true);
        
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(2, results.size());
        
        // Verify movies were stored in MySQL
        List<ChunkDto> storedChunks = sqlMovieChunkService.getMovieChunks("tmdb", 10, null, null, null, null);
        assertNotNull(storedChunks);
        assertFalse(storedChunks.isEmpty());
        assertTrue(storedChunks.size() >= 2);
    }
    
    @Test
    void testMySqlChunkRetrieval() {
        // Store some test data
        sqlMovieChunkService.searchAndStore("Matrix", 2, true);
        
        // Test chunk retrieval with various parameters
        List<ChunkDto> chunks = sqlMovieChunkService.getMovieChunks("tmdb", 5, null, null, null, null);
        
        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        
        ChunkDto chunk = chunks.get(0);
        assertNotNull(chunk.getId());
        assertNotNull(chunk.getText());
        assertNotNull(chunk.getMetadata());
        assertTrue(chunk.getText().contains("Movie:"));
        assertTrue(chunk.getText().contains("Matrix"));
    }
    
    @Test
    void testMySqlFilteredQuery() {
        // Store test data
        sqlMovieChunkService.searchAndStore("Matrix", 2, true);
        
        // Test filtered query
        List<ChunkDto> filteredChunks = sqlMovieChunkService.getMovieChunks("tmdb", 10, null, "title like 'Matrix'", null, null);
        
        assertNotNull(filteredChunks);
        assertFalse(filteredChunks.isEmpty());
        
        // Verify all results contain Matrix
        for (ChunkDto chunk : filteredChunks) {
            assertTrue(chunk.getText().contains("Matrix"));
        }
    }
    
    @Test
    void testMySqlPagination() {
        // Store test data
        sqlMovieChunkService.searchAndStore("Matrix", 2, true);
        
        // Test pagination
        List<ChunkDto> firstPage = sqlMovieChunkService.getMovieChunks("tmdb", 1, null, null, null, "id");
        assertNotNull(firstPage);
        assertEquals(1, firstPage.size());
        
        String firstId = firstPage.get(0).getId();
        
        // Get next page
        List<ChunkDto> secondPage = sqlMovieChunkService.getMovieChunks("tmdb", 1, firstId, null, null, "id");
        assertNotNull(secondPage);
        assertTrue(secondPage.size() <= 1);
        
        // If there's a second page, verify it's different from first
        if (!secondPage.isEmpty()) {
            assertNotEquals(firstId, secondPage.get(0).getId());
        }
    }
    
    @Test
    void testMySqlUpsertFunctionality() {
        // Test that storing the same movie twice doesn't create duplicates
        sqlMovieChunkService.searchAndStore("Matrix", 1, true);
        List<ChunkDto> firstStore = sqlMovieChunkService.getMovieChunks("tmdb", 10, null, null, null, null);
        
        sqlMovieChunkService.searchAndStore("Matrix", 1, true);
        List<ChunkDto> secondStore = sqlMovieChunkService.getMovieChunks("tmdb", 10, null, null, null, null);
        
        // Should have same number of records (upsert behavior)
        assertEquals(firstStore.size(), secondStore.size());
    }
    
    @Test
    void testMySqlDataIntegrity() {
        // Test data integrity and proper field mapping
        sqlMovieChunkService.searchAndStore("Matrix", 1, true);
        List<ChunkDto> chunks = sqlMovieChunkService.getMovieChunks("tmdb", 1, null, null, null, null);
        
        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        
        ChunkDto chunk = chunks.get(0);
        
        // Verify metadata contains expected fields
        assertNotNull(chunk.getMetadata().get("id"));
        assertNotNull(chunk.getMetadata().get("title"));
        assertNotNull(chunk.getMetadata().get("overview"));
        assertNotNull(chunk.getMetadata().get("release_date"));
        assertNotNull(chunk.getMetadata().get("rating"));
        assertNotNull(chunk.getMetadata().get("source"));
        
        // Verify text format
        String text = chunk.getText();
        assertTrue(text.contains("Movie:"));
        assertTrue(text.contains("Overview:"));
        assertTrue(text.contains("Rating:"));
        assertTrue(text.contains("Matrix"));
    }
}