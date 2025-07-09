package org.shark.melian.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.shark.melian.model.ChunkDto;
import org.shark.melian.model.MovieResult;
import org.shark.melian.service.MovieChunkService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieControllerTest {

    @Mock
    private MovieChunkService sqlMovieChunkService;

    @Mock
    private MovieChunkService mongoMovieChunkService;

    @InjectMocks
    private MovieController movieController;

    private List<MovieResult> mockMovieResults;
    private List<ChunkDto> mockChunks;

    @BeforeEach
    void setUp() {
        mockMovieResults = List.of(
            new MovieResult("The Matrix", "A computer hacker learns about reality", "1999", 8.7),
            new MovieResult("Inception", "A thief enters dreams", "2010", 8.8)
        );

        mockChunks = List.of(
            new ChunkDto("1", "The Matrix content", null, null, null, null),
            new ChunkDto("2", "Inception content", null, null, null, null)
        );
    }

    @Test
    void testSearchMovies_SqlStorage_CallsSqlService() {
        // Given
        String title = "Matrix";
        int limit = 5;
        boolean store = true;
        String storage = "sql";
        
        when(sqlMovieChunkService.searchAndStore(title, limit, store)).thenReturn(mockMovieResults);

        // When
        List<MovieResult> result = movieController.searchMovies(title, limit, store, storage);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("The Matrix", result.get(0).title());
        verify(sqlMovieChunkService).searchAndStore(title, limit, store);
        verifyNoInteractions(mongoMovieChunkService);
    }

    @Test
    void testSearchMovies_MongoStorage_CallsMongoService() {
        // Given
        String title = "Inception";
        int limit = 3;
        boolean store = false;
        String storage = "mongodb";
        
        when(mongoMovieChunkService.searchAndStore(title, limit, store)).thenReturn(mockMovieResults);

        // When
        List<MovieResult> result = movieController.searchMovies(title, limit, store, storage);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(mongoMovieChunkService).searchAndStore(title, limit, store);
        verifyNoInteractions(sqlMovieChunkService);
    }

    @Test
    void testSearchMovies_DefaultParameters() {
        // Given
        String title = "Thor";
        // Using defaults: limit=3, store=false, storage=sql
        
        when(sqlMovieChunkService.searchAndStore(title, 3, false)).thenReturn(mockMovieResults);

        // When
        List<MovieResult> result = movieController.searchMovies(title, 3, false, "sql");

        // Then
        assertNotNull(result);
        verify(sqlMovieChunkService).searchAndStore(title, 3, false);
    }

    @Test
    void testGetMovieChunks_SqlStorage_CallsSqlService() {
        // Given
        String storage = "sql";
        String source = "tmdb";
        int limit = 10;
        String afterId = "5";
        String filter = "title='Matrix'";
        List<String> tags = List.of("action");
        String sort = "title";
        
        when(sqlMovieChunkService.getMovieChunks(source, limit, afterId, filter, tags, sort))
                .thenReturn(mockChunks);

        // When
        List<ChunkDto> result = movieController.getMovieChunks(storage, source, limit, afterId, filter, tags, sort);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("The Matrix content", result.get(0).getText());
        verify(sqlMovieChunkService).getMovieChunks(source, limit, afterId, filter, tags, sort);
        verifyNoInteractions(mongoMovieChunkService);
    }

    @Test
    void testGetMovieChunks_MongoStorage_CallsMongoService() {
        // Given
        String storage = "mongo";
        String source = "tmdb";
        int limit = 15;
        
        when(mongoMovieChunkService.getMovieChunks(source, limit, null, null, null, null))
                .thenReturn(mockChunks);

        // When
        List<ChunkDto> result = movieController.getMovieChunks(storage, source, limit, null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(mongoMovieChunkService).getMovieChunks(source, limit, null, null, null, null);
        verifyNoInteractions(sqlMovieChunkService);
    }

    @Test
    void testGetMovieChunks_DefaultParameters() {
        // Given - using defaults: storage=sql, source=tmdb, limit=10
        
        when(sqlMovieChunkService.getMovieChunks("tmdb", 10, null, null, null, null))
                .thenReturn(mockChunks);

        // When
        List<ChunkDto> result = movieController.getMovieChunks("sql", "tmdb", 10, null, null, null, null);

        // Then
        assertNotNull(result);
        verify(sqlMovieChunkService).getMovieChunks("tmdb", 10, null, null, null, null);
    }

    @Test
    void testStoreMovies_SqlStorage_CallsSqlService() {
        // Given
        List<MovieResult> movies = mockMovieResults;
        String source = "manual";
        String storage = "sql";

        // When
        movieController.storeMovies(movies, source, storage);

        // Then
        verify(sqlMovieChunkService).storeMovies(movies, source);
        verifyNoInteractions(mongoMovieChunkService);
    }

    @Test
    void testStoreMovies_MongoStorage_CallsMongoService() {
        // Given
        List<MovieResult> movies = mockMovieResults;
        String source = "tmdb";
        String storage = "mongodb";

        // When
        movieController.storeMovies(movies, source, storage);

        // Then
        verify(mongoMovieChunkService).storeMovies(movies, source);
        verifyNoInteractions(sqlMovieChunkService);
    }

    @Test
    void testStoreMovies_DefaultParameters() {
        // Given
        List<MovieResult> movies = mockMovieResults;
        // Using defaults: source=manual, storage=sql

        // When
        movieController.storeMovies(movies, "manual", "sql");

        // Then
        verify(sqlMovieChunkService).storeMovies(movies, "manual");
    }

    @Test
    void testGetMovieService_InvalidStorage_ThrowsException() {
        // Given
        String invalidStorage = "invalid";

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            movieController.searchMovies("title", 3, false, invalidStorage));
    }

    @Test
    void testGetMovieService_DbStorage_ReturnsSqlService() {
        // Given
        String storage = "db"; // Should map to SQL service
        
        when(sqlMovieChunkService.searchAndStore("test", 3, false)).thenReturn(mockMovieResults);

        // When
        List<MovieResult> result = movieController.searchMovies("test", 3, false, storage);

        // Then
        assertNotNull(result);
        verify(sqlMovieChunkService).searchAndStore("test", 3, false);
    }
}