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
        Optional<MongoMovieChunkService> optionalMongoService = Optional.of(mongoService);
        aggregatedMovieService = new AggregatedMovieService(tmdbService, sqlService, optionalMongoService);
    }

    @Test
    void searchMovies_shouldAggregateResultsFromAllSources() {
        // Arrange
        String query = "Matrix";
        int limit = 10;
        List<MovieResult> tmdbMovies = List.of(
                new MovieResult("The Matrix", "Sci-fi movie", "1999", 8.7),
                new MovieResult("Matrix Reloaded", "Sequel", "2003", 7.2)
        );
        List<MovieResult> sqlMovies = List.of(
                new MovieResult("Matrix Revolutions", "Third part", "2003", 6.8)
        );
        List<MovieResult> mongoMovies = List.of(
                new MovieResult("The Matrix Resurrections", "Fourth part", "2021", 6.5)
        );

        when(tmdbService.search(query, limit)).thenReturn(tmdbMovies);
        when(sqlService.search(query, limit)).thenReturn(sqlMovies);
        when(mongoService.search(query, limit)).thenReturn(mongoMovies);

        // Act
        List<MovieResult> result = aggregatedMovieService.searchMovies(query, limit);

        // Assert
        assertEquals(4, result.size());
        verify(tmdbService).search(query, limit);
        verify(sqlService).search(query, limit);
        verify(mongoService).search(query, limit);

        // No se debería llamar a storeMovies ya que searchMovies no lo hace
        verify(sqlService, never()).storeMovies(tmdbMovies);
        verify(mongoService, never()).storeMovies(tmdbMovies);
    }

    @Test
    void getMovieChunks_shouldAggregateChunksFromAllSources() {
        // Arrange
        int limit = 5;
        String afterId = null;
        String filter = null;
        List<String> tags = List.of();
        String sort = null;

        ChunkDto sqlChunk = new ChunkDto();
        sqlChunk.setId("sql_1");
        sqlChunk.setText("SQL Movie");

        ChunkDto mongoChunk = new ChunkDto();
        mongoChunk.setId("mongo_1");
        mongoChunk.setText("MongoDB Movie");

        when(sqlService.getMovieChunks(limit, afterId, filter, tags, sort)).thenReturn(List.of(sqlChunk));
        when(mongoService.getMovieChunks(limit, afterId, filter, tags, sort)).thenReturn(List.of(mongoChunk));

        List<MovieResult> tmdbMovies = List.of(
                new MovieResult("TMDB Movie", "Description", "2024", 7.5)
        );
        when(tmdbService.search(anyString(), anyInt())).thenReturn(tmdbMovies);

        // Act
        List<ChunkDto> result = aggregatedMovieService.getMovieChunks(limit, afterId, filter, tags, sort);

        // Assert
        assertEquals(3, result.size()); // Incluye 1 de SQL, 1 de MongoDB y 1 de TMDB (convertido)

        verify(sqlService).getMovieChunks(limit, afterId, filter, tags, sort);
        verify(mongoService).getMovieChunks(limit, afterId, filter, tags, sort);
        verify(tmdbService).search(anyString(), eq(limit));
    }

    @Test
    void getMovieChunks_shouldHandleEmptyResults() {
        // Arrange
        int limit = 5;
        String afterId = null;
        String filter = null;
        List<String> tags = Collections.emptyList();
        String sort = null;

        when(sqlService.getMovieChunks(limit, afterId, filter, tags, sort)).thenReturn(Collections.emptyList());
        when(mongoService.getMovieChunks(limit, afterId, filter, tags, sort)).thenReturn(Collections.emptyList());
        when(tmdbService.search(anyString(), anyInt())).thenReturn(Collections.emptyList());

        // Act
        List<ChunkDto> result = aggregatedMovieService.getMovieChunks(limit, afterId, filter, tags, sort);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void getMovieChunks_shouldHandleFilterWithSearch() {
        // Arrange
        int limit = 5;
        String afterId = null;
        String filter = "title LIKE '%Matrix%'";
        List<String> tags = Collections.emptyList();
        String sort = null;

        when(sqlService.getMovieChunks(limit, afterId, filter, tags, sort)).thenReturn(Collections.emptyList());
        when(mongoService.getMovieChunks(limit, afterId, filter, tags, sort)).thenReturn(Collections.emptyList());

        List<MovieResult> tmdbMovies = List.of(
                new MovieResult("The Matrix", "Sci-fi movie", "1999", 8.7)
        );
        when(tmdbService.search(eq("Matrix"), anyInt())).thenReturn(tmdbMovies);

        // Act
        List<ChunkDto> result = aggregatedMovieService.getMovieChunks(limit, afterId, filter, tags, sort);

        // Assert
        assertEquals(1, result.size());
        verify(tmdbService).search(eq("Matrix"), anyInt());
    }

    @Test
    void searchMovies_shouldFindMoviesIgnoringAccents() {
        // Arrange
        String query = "Amelie";
        int limit = 5;

        List<MovieResult> mongoMovies = List.of(
                new MovieResult("Amélie", "French film", "2001", 8.3)
        );

        when(tmdbService.search(query, limit)).thenReturn(Collections.emptyList());
        when(sqlService.search(query, limit)).thenReturn(Collections.emptyList());
        when(mongoService.search(query, limit)).thenReturn(mongoMovies);

        // Act
        List<MovieResult> result = aggregatedMovieService.searchMovies(query, limit);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Amélie", result.get(0).title());
        verify(mongoService).search(query, limit);
    }
}