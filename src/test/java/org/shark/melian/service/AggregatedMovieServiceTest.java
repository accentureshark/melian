package org.shark.melian.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.shark.melian.model.ChunkDto;
import org.shark.melian.model.MovieResult;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AggregatedMovieServiceTest {

    @Mock
    private TMDBService tmdbService;

    @Mock
    private SqlMovieChunkService sqlService;

    @Mock
    private MongoMovieChunkService mongoService;

    private AggregatedMovieService aggregatedMovieService;

    @BeforeEach
    void setUp() {
        aggregatedMovieService = new AggregatedMovieService(tmdbService, sqlService, Optional.of(mongoService));
    }

    @Test
    void searchMovies_shouldSearchFromTMDBAndStoreInAllSources() {
        // Arrange
        String query = "Matrix";
        int limit = 5;
        List<MovieResult> expectedMovies = Arrays.asList(
                new MovieResult("The Matrix", "A computer programmer discovers reality is a simulation", "1999-03-31", 8.7),
                new MovieResult("The Matrix Reloaded", "Neo fights against the machines", "2003-05-15", 7.2)
        );

        when(tmdbService.search(query, limit)).thenReturn(expectedMovies);

        // Act
        List<MovieResult> results = aggregatedMovieService.searchMovies(query, limit);

        // Assert
        assertEquals(expectedMovies, results);
        verify(tmdbService).search(query, limit);
        
        // Give some time for async storage operations to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        verify(sqlService).storeMovies(expectedMovies, "tmdb");
        verify(mongoService).storeMovies(expectedMovies, "tmdb");
    }

    @Test
    void searchMovies_shouldReturnEmptyWhenTMDBServiceIsNull() {
        // Arrange
        AggregatedMovieService serviceWithoutTMDB = new AggregatedMovieService(null, sqlService, mongoService);

        // Act
        List<MovieResult> results = serviceWithoutTMDB.searchMovies("test", 5);

        // Assert
        assertTrue(results.isEmpty());
    }

    @Test
    void getMovieChunks_shouldAggregateFromAllSources() {
        // Arrange
        int limit = 10;
        String filter = "title LIKE 'Matrix%'";

        // Mock SQL chunks
        ChunkDto sqlChunk = new ChunkDto();
        sqlChunk.setId("sql_1");
        sqlChunk.setText("Movie: The Matrix (1999)\nOverview: Neo discovers reality\nRating: 8.7");
        Map<String, Object> sqlMetadata = new HashMap<>();
        sqlMetadata.put("title", "The Matrix");
        sqlMetadata.put("source", "sql");
        sqlChunk.setMetadata(sqlMetadata);

        // Mock MongoDB chunks
        ChunkDto mongoChunk = new ChunkDto();
        mongoChunk.setId("mongo_1");
        mongoChunk.setText("Movie: The Matrix Reloaded (2003)\nOverview: Neo fights machines\nRating: 7.2");
        Map<String, Object> mongoMetadata = new HashMap<>();
        mongoMetadata.put("title", "The Matrix Reloaded");
        mongoMetadata.put("source", "mongo");
        mongoChunk.setMetadata(mongoMetadata);

        // Mock TMDB search results
        List<MovieResult> tmdbMovies = Arrays.asList(
                new MovieResult("The Matrix Revolutions", "The final battle", "2003-11-05", 6.8)
        );

        when(sqlService.getMovieChunks(eq("sql"), eq(limit), isNull(), eq(filter), isNull(), isNull()))
                .thenReturn(Arrays.asList(sqlChunk));
        when(mongoService.getMovieChunks(eq("mongo"), eq(limit), isNull(), eq(filter), isNull(), isNull()))
                .thenReturn(Arrays.asList(mongoChunk));
        when(tmdbService.search(eq("Matrix"), eq(limit)))
                .thenReturn(tmdbMovies);

        // Act
        List<ChunkDto> results = aggregatedMovieService.getMovieChunks(limit, null, filter, null, null);

        // Assert
        assertNotNull(results);
        assertEquals(3, results.size()); // SQL + MongoDB + TMDB chunks

        // Verify all sources were called
        verify(sqlService).getMovieChunks(eq("sql"), eq(limit), isNull(), eq(filter), isNull(), isNull());
        verify(mongoService).getMovieChunks(eq("mongo"), eq(limit), isNull(), eq(filter), isNull(), isNull());
        verify(tmdbService).search(eq("Matrix"), eq(limit));

        // Verify metadata contains data_source
        boolean hasSqlSource = results.stream().anyMatch(chunk -> 
                chunk.getMetadata() != null && "sql".equals(chunk.getMetadata().get("data_source")));
        boolean hasMongoSource = results.stream().anyMatch(chunk -> 
                chunk.getMetadata() != null && "mongo".equals(chunk.getMetadata().get("data_source")));
        boolean hasTmdbSource = results.stream().anyMatch(chunk -> 
                chunk.getMetadata() != null && "tmdb".equals(chunk.getMetadata().get("data_source")));

        assertTrue(hasSqlSource, "Should have chunks from SQL source");
        assertTrue(hasMongoSource, "Should have chunks from MongoDB source");
        assertTrue(hasTmdbSource, "Should have chunks from TMDB source");
    }

    @Test
    void getMovieChunks_shouldHandleServiceErrors() {
        // Arrange
        int limit = 5;

        when(sqlService.getMovieChunks(anyString(), anyInt(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("SQL connection error"));
        when(mongoService.getMovieChunks(anyString(), anyInt(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(tmdbService.search(anyString(), anyInt()))
                .thenReturn(Arrays.asList(new MovieResult("Test Movie", "Description", "2024", 7.5)));

        // Act
        List<ChunkDto> results = aggregatedMovieService.getMovieChunks(limit, null, null, null, null);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size()); // Only TMDB chunk should be returned
        
        // Verify TMDB chunk
        ChunkDto tmdbChunk = results.get(0);
        assertEquals("tmdb", tmdbChunk.getMetadata().get("data_source"));
    }

    @Test
    void getServicesStatus_shouldReturnCorrectStatus() {
        // Act
        Map<String, String> status = aggregatedMovieService.getServicesStatus();

        // Assert
        assertNotNull(status);
        assertEquals("AVAILABLE", status.get("tmdb_service"));
        assertEquals("AVAILABLE", status.get("sql_service"));
        assertEquals("AVAILABLE", status.get("mongo_service"));
    }

    @Test
    void getServicesStatus_shouldReturnNotAvailableForNullServices() {
        // Arrange
        AggregatedMovieService serviceWithNulls = new AggregatedMovieService(null, null, null);

        // Act
        Map<String, String> status = serviceWithNulls.getServicesStatus();

        // Assert
        assertNotNull(status);
        assertEquals("NOT_AVAILABLE", status.get("tmdb_service"));
        assertEquals("NOT_AVAILABLE", status.get("sql_service"));
        assertEquals("NOT_AVAILABLE", status.get("mongo_service"));
    }

    @Test
    void searchMovies_shouldHandleTMDBErrors() {
        // Arrange
        when(tmdbService.search(anyString(), anyInt()))
                .thenThrow(new RuntimeException("TMDB API error"));

        // Act
        List<MovieResult> results = aggregatedMovieService.searchMovies("test", 5);

        // Assert
        assertTrue(results.isEmpty());
        verify(tmdbService).search("test", 5);
        verifyNoInteractions(sqlService);
        verifyNoInteractions(mongoService);
    }
}