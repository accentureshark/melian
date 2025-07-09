package org.shark.melian.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.shark.melian.model.ChunkDto;
import org.shark.melian.model.MovieResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieToolServiceTest {

    @Mock
    private TMDBService tmdbService;

    @Mock
    private MovieChunkService sqlMovieChunkService;

    @Mock
    private MovieChunkService mongoMovieChunkService;

    @InjectMocks
    private MovieToolService movieToolService;

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
    void testSearchMovies_CallsTMDBService() {
        // Given
        String title = "Matrix";
        Integer limit = 5;
        when(tmdbService.search(title, limit)).thenReturn(mockMovieResults);

        // When
        List<MovieResult> result = movieToolService.searchMovies(title, limit);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("The Matrix", result.get(0).title());
        verify(tmdbService).search(title, limit);
    }

    @Test
    void testSearchMovies_NullLimit_UsesDefault() {
        // Given
        String title = "Matrix";
        Integer limit = null;
        when(tmdbService.search(title, 3)).thenReturn(mockMovieResults);

        // When
        List<MovieResult> result = movieToolService.searchMovies(title, limit);

        // Then
        assertNotNull(result);
        verify(tmdbService).search(title, 3); // Default limit
    }

    @Test
    void testSearchAndStoreMoviesSQL_CallsCorrectService() {
        // Given
        String title = "Matrix";
        Integer limit = 5;
        when(sqlMovieChunkService.searchAndStore(title, limit, true)).thenReturn(mockMovieResults);

        // When
        List<MovieResult> result = movieToolService.searchAndStoreMoviesSQL(title, limit);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(sqlMovieChunkService).searchAndStore(title, limit, true);
    }

    @Test
    void testSearchAndStoreMoviesMongo_CallsCorrectService() {
        // Given
        String title = "Matrix";
        Integer limit = 3;
        when(mongoMovieChunkService.searchAndStore(title, limit, true)).thenReturn(mockMovieResults);

        // When
        List<MovieResult> result = movieToolService.searchAndStoreMoviesMongo(title, limit);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(mongoMovieChunkService).searchAndStore(title, limit, true);
    }

    @Test
    void testGetStoredMoviesSQL_CallsCorrectService() {
        // Given
        String filter = "title='Matrix'";
        Integer limit = 10;
        String afterId = "5";
        when(sqlMovieChunkService.getMovieChunks("tmdb", limit, afterId, filter, null, null))
                .thenReturn(mockChunks);

        // When
        List<ChunkDto> result = movieToolService.getStoredMoviesSQL(filter, limit, afterId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("The Matrix content", result.get(0).getText());
        verify(sqlMovieChunkService).getMovieChunks("tmdb", limit, afterId, filter, null, null);
    }

    @Test
    void testGetStoredMoviesMongo_CallsCorrectService() {
        // Given
        String filter = "title='Inception'";
        Integer limit = 15;
        String afterId = "10";
        when(mongoMovieChunkService.getMovieChunks("tmdb", limit, afterId, filter, null, null))
                .thenReturn(mockChunks);

        // When
        List<ChunkDto> result = movieToolService.getStoredMoviesMongo(filter, limit, afterId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Inception content", result.get(1).getText());
        verify(mongoMovieChunkService).getMovieChunks("tmdb", limit, afterId, filter, null, null);
    }

    @Test
    void testGetStoredMoviesSQL_NullLimit_UsesDefault() {
        // Given
        String filter = "title='Matrix'";
        Integer limit = null;
        String afterId = "5";
        when(sqlMovieChunkService.getMovieChunks("tmdb", 10, afterId, filter, null, null))
                .thenReturn(mockChunks);

        // When
        List<ChunkDto> result = movieToolService.getStoredMoviesSQL(filter, limit, afterId);

        // Then
        assertNotNull(result);
        verify(sqlMovieChunkService).getMovieChunks("tmdb", 10, afterId, filter, null, null);
    }
}