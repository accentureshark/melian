package org.shark.melian.integration;

import org.junit.jupiter.api.Test;
import org.shark.melian.integration.config.IntegrationTestConfig;
import org.shark.melian.model.ChunkDto;
import org.shark.melian.model.MovieResult;
import org.shark.melian.service.MongoMovieChunkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for MongoDB database operations.
 * Tests MongoMovieChunkService with real MongoDB database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(IntegrationTestConfig.class)
@ActiveProfiles("integration")
@IntegrationTest
class MongoDbIntegrationTest extends BaseIntegrationTest {
    
    @Autowired
    private MongoMovieChunkService mongoMovieChunkService;
    
    @Test
    void testMongoDbConnection() {
        // Verify MongoDB connection works
        assertNotNull(mongoMovieChunkService);
    }
    
    @Test
    void testSearchAndStoreMoviesInMongoDB() {
        // Test search and store functionality with MongoDB
        List<MovieResult> results = mongoMovieChunkService.searchAndStore("Matrix", 2, true);
        
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(2, results.size());
        
        // Verify movies were stored in MongoDB
        List<ChunkDto> storedChunks = mongoMovieChunkService.getMovieChunks("tmdb", 10, null, null, null, null);
        assertNotNull(storedChunks);
        assertFalse(storedChunks.isEmpty());
        assertTrue(storedChunks.size() >= 2);
    }
    
    @Test
    void testMongoDbChunkRetrieval() {
        // Store some test data
        mongoMovieChunkService.searchAndStore("Matrix", 2, true);
        
        // Test chunk retrieval
        List<ChunkDto> chunks = mongoMovieChunkService.getMovieChunks("tmdb", 5, null, null, null, null);
        
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
    void testMongoDbFilteredQuery() {
        // Store test data
        mongoMovieChunkService.searchAndStore("Matrix", 2, true);
        
        // Test filtered query with MongoDB regex
        List<ChunkDto> filteredChunks = mongoMovieChunkService.getMovieChunks("tmdb", 10, null, "title like Matrix", null, null);
        
        assertNotNull(filteredChunks);
        assertFalse(filteredChunks.isEmpty());
        
        // Verify all results contain Matrix
        for (ChunkDto chunk : filteredChunks) {
            assertTrue(chunk.getText().contains("Matrix"));
        }
    }
    
    @Test
    void testMongoDbPagination() {
        // Store test data
        mongoMovieChunkService.searchAndStore("Matrix", 2, true);
        
        // Test pagination with MongoDB ObjectId
        List<ChunkDto> firstPage = mongoMovieChunkService.getMovieChunks("tmdb", 1, null, null, null, "_id");
        assertNotNull(firstPage);
        assertEquals(1, firstPage.size());
        
        String firstId = firstPage.get(0).getId();
        
        // Get next page using ObjectId
        List<ChunkDto> secondPage = mongoMovieChunkService.getMovieChunks("tmdb", 1, firstId, null, null, "_id");
        assertNotNull(secondPage);
        assertTrue(secondPage.size() <= 1);
        
        // If there's a second page, verify it's different from first
        if (!secondPage.isEmpty()) {
            assertNotEquals(firstId, secondPage.get(0).getId());
        }
    }
    
    @Test
    void testMongoDbUpsertFunctionality() {
        // Test that storing the same movie twice doesn't create duplicates
        mongoMovieChunkService.searchAndStore("Matrix", 1, true);
        List<ChunkDto> firstStore = mongoMovieChunkService.getMovieChunks("tmdb", 10, null, null, null, null);
        
        mongoMovieChunkService.searchAndStore("Matrix", 1, true);
        List<ChunkDto> secondStore = mongoMovieChunkService.getMovieChunks("tmdb", 10, null, null, null, null);
        
        // Should have same number of records (upsert behavior)
        assertEquals(firstStore.size(), secondStore.size());
    }
    
    @Test
    void testMongoDbDataIntegrity() {
        // Test data integrity and proper field mapping
        mongoMovieChunkService.searchAndStore("Matrix", 1, true);
        List<ChunkDto> chunks = mongoMovieChunkService.getMovieChunks("tmdb", 1, null, null, null, null);
        
        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        
        ChunkDto chunk = chunks.get(0);
        
        // Verify metadata contains expected fields
        assertNotNull(chunk.getMetadata().get("_id"));
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
    
    @Test
    void testMongoDbSortingAndFiltering() {
        // Store test data
        mongoMovieChunkService.searchAndStore("Matrix", 2, true);
        
        // Test sorting by rating
        List<ChunkDto> sortedChunks = mongoMovieChunkService.getMovieChunks("tmdb", 10, null, null, null, "rating");
        
        assertNotNull(sortedChunks);
        assertFalse(sortedChunks.isEmpty());
        
        // Verify sorting works (should be sorted by rating)
        if (sortedChunks.size() > 1) {
            Double firstRating = (Double) sortedChunks.get(0).getMetadata().get("rating");
            Double secondRating = (Double) sortedChunks.get(1).getMetadata().get("rating");
            assertTrue(firstRating <= secondRating);
        }
    }
    
    @Test
    void testMongoDbComplexQuery() {
        // Store test data
        mongoMovieChunkService.searchAndStore("Matrix", 2, true);
        
        // Test complex query with exact match
        List<ChunkDto> exactMatch = mongoMovieChunkService.getMovieChunks("tmdb", 10, null, "title=The Matrix", null, null);
        
        assertNotNull(exactMatch);
        assertFalse(exactMatch.isEmpty());
        
        // Verify exact match
        for (ChunkDto chunk : exactMatch) {
            assertTrue(chunk.getMetadata().get("title").equals("The Matrix"));
        }
    }
}