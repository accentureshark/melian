package org.shark.melian.integration;

import org.junit.jupiter.api.Test;
import org.shark.melian.integration.config.IntegrationTestConfig;
import org.shark.melian.model.MovieResult;
import org.shark.melian.service.TMDBService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for REST API operations.
 * Tests TMDBService with mocked external API calls.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(IntegrationTestConfig.class)
@ActiveProfiles("integration")
@IntegrationTest
class RestApiIntegrationTest extends BaseIntegrationTest {
    
    @Autowired
    private TMDBService tmdbService;
    
    @Test
    void testRestApiConnection() {
        // Verify REST API service is available
        assertNotNull(tmdbService);
    }
    
    @Test
    void testSearchMoviesViaRestApi() {
        // Test basic search functionality
        List<MovieResult> results = tmdbService.search("Matrix", 2);
        
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(2, results.size());
        
        // Verify movie data structure
        MovieResult movie = results.get(0);
        assertNotNull(movie.title());
        assertNotNull(movie.overview());
        assertNotNull(movie.releaseDate());
        assertNotNull(movie.rating());
        
        assertEquals("The Matrix", movie.title());
        assertEquals("1999-03-30", movie.releaseDate());
        assertEquals(8.7, movie.rating());
    }
    
    @Test
    void testSearchMoviesWithParameters() {
        // Test search with custom parameters
        Map<String, String> params = Map.of(
            "query", "Matrix",
            "year", "1999"
        );
        
        List<MovieResult> results = tmdbService.searchByParams(params, 5);
        
        assertNotNull(results);
        assertFalse(results.isEmpty());
        
        // Verify that the mocked API returns expected data
        MovieResult movie = results.get(0);
        assertEquals("The Matrix", movie.title());
        assertEquals("1999-03-30", movie.releaseDate());
    }
    
    @Test
    void testSearchMoviesWithLimit() {
        // Test that limit parameter is respected
        List<MovieResult> singleResult = tmdbService.search("Matrix", 1);
        List<MovieResult> multipleResults = tmdbService.search("Matrix", 5);
        
        assertNotNull(singleResult);
        assertNotNull(multipleResults);
        
        assertEquals(1, singleResult.size());
        assertEquals(2, multipleResults.size()); // Max 2 from mock data
        
        // Verify same movie is returned in both cases
        assertEquals(singleResult.get(0).title(), multipleResults.get(0).title());
    }
    
    @Test
    void testSearchMoviesDataIntegrity() {
        // Test that returned data has proper structure and values
        List<MovieResult> results = tmdbService.search("Matrix", 2);
        
        assertNotNull(results);
        assertEquals(2, results.size());
        
        // Verify first movie
        MovieResult movie1 = results.get(0);
        assertEquals("The Matrix", movie1.title());
        assertTrue(movie1.overview().contains("computer hacker"));
        assertEquals("1999-03-30", movie1.releaseDate());
        assertEquals(8.7, movie1.rating());
        
        // Verify second movie
        MovieResult movie2 = results.get(1);
        assertEquals("The Matrix Reloaded", movie2.title());
        assertTrue(movie2.overview().contains("Neo"));
        assertEquals("2003-05-15", movie2.releaseDate());
        assertEquals(7.2, movie2.rating());
    }
    
    @Test
    void testSearchMoviesWithDifferentQueries() {
        // Test that different queries return the same mock data
        // (since we're using a mock that returns the same data for any query)
        List<MovieResult> matrixResults = tmdbService.search("Matrix", 2);
        List<MovieResult> otherResults = tmdbService.search("Inception", 2);
        
        assertNotNull(matrixResults);
        assertNotNull(otherResults);
        
        // Both should return same data from mock
        assertEquals(matrixResults.size(), otherResults.size());
        assertEquals(matrixResults.get(0).title(), otherResults.get(0).title());
    }
    
    @Test
    void testSearchMoviesApiResponseHandling() {
        // Test that service handles API response correctly
        List<MovieResult> results = tmdbService.search("Test", 10);
        
        assertNotNull(results);
        assertTrue(results.size() <= 10); // Should respect limit
        
        // Verify each result has required fields
        for (MovieResult movie : results) {
            assertNotNull(movie.title());
            assertNotNull(movie.overview());
            assertNotNull(movie.releaseDate());
            assertNotNull(movie.rating());
            
            // Verify rating is within expected range
            assertTrue(movie.rating() >= 0.0);
            assertTrue(movie.rating() <= 10.0);
        }
    }
    
    @Test
    void testSearchMoviesWithEdgeCases() {
        // Test edge cases
        List<MovieResult> emptyQuery = tmdbService.search("", 5);
        List<MovieResult> zeroLimit = tmdbService.search("Matrix", 0);
        
        assertNotNull(emptyQuery);
        assertNotNull(zeroLimit);
        
        // With mock data, should still return results for empty query
        assertFalse(emptyQuery.isEmpty());
        assertTrue(zeroLimit.isEmpty()); // Zero limit should return empty
    }
    
    @Test
    void testSearchMoviesPerformance() {
        // Test that multiple API calls work efficiently
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 5; i++) {
            List<MovieResult> results = tmdbService.search("Matrix", 2);
            assertNotNull(results);
            assertFalse(results.isEmpty());
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Should complete quickly with mock API
        assertTrue(duration < 5000); // Less than 5 seconds
    }
}