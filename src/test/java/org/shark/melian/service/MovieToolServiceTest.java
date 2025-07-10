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
        String title = "Matrix";
        int limit = 5;
        when(tmdbService.search(title, limit)).thenReturn(mockMovieResults);

        List<MovieResult> result = movieToolService.searchMovies(title, limit);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(tmdbService).search(title, limit);
    }

    @Test
    void testSearchMovies_NullLimit_UsesDefault() {
        String title = "Matrix";
        when(tmdbService.search(title, 3)).thenReturn(mockMovieResults);

        List<MovieResult> result = movieToolService.searchMovies(title, null);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(tmdbService).search(title, 3);
    }

    @Test
    void testSearchAndStoreMoviesSQL_CallsCorrectService() {
        when(sqlMovieChunkService.searchAndStore(anyString(), anyInt(), anyBoolean())).thenReturn(mockMovieResults);
        List<MovieResult> result = movieToolService.searchAndStoreMoviesSQL("Matrix", 4);
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(sqlMovieChunkService, atLeastOnce()).searchAndStore(anyString(), anyInt(), anyBoolean());
    }

    @Test
    void testSearchAndStoreMoviesMongo_CallsCorrectService() {
        when(mongoMovieChunkService.searchAndStore(anyString(), anyInt(), anyBoolean())).thenReturn(mockMovieResults);
        List<MovieResult> result = movieToolService.searchAndStoreMoviesMongo("Inception", 2);
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(mongoMovieChunkService, atLeastOnce()).searchAndStore(anyString(), anyInt(), anyBoolean());
    }

    @Test
    void testGetStoredMoviesSQL_CallsCorrectService() {
        String filter = "title='Matrix'";
        Integer limit = 10;
        String afterId = "5";
        when(sqlMovieChunkService.getMovieChunks(anyString(), anyInt(), any(), any(), any(), any())).thenReturn(mockChunks);

        List<ChunkDto> result = movieToolService.getStoredMoviesSQL(filter, limit, afterId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("The Matrix content", result.get(0).getText());
        verify(sqlMovieChunkService, atLeastOnce()).getMovieChunks(anyString(), anyInt(), any(), any(), any(), any());
    }

    @Test
    void testGetStoredMoviesMongo_CallsCorrectService() {
        when(mongoMovieChunkService.getMovieChunks(anyString(), anyInt(), any(), any(), any(), any())).thenReturn(mockChunks);
        List<ChunkDto> result = movieToolService.getStoredMoviesMongo("title='Inception'", 15, "10");
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Inception content", result.get(1).getText());
        verify(mongoMovieChunkService, atLeastOnce()).getMovieChunks(anyString(), anyInt(), any(), any(), any(), any());
    }

    @Test
    void testGetStoredMoviesSQL_NullLimit_UsesDefault() {
        when(sqlMovieChunkService.getMovieChunks(anyString(), eq(3), any(), any(), any(), any()))
                .thenReturn(mockChunks);

        List<ChunkDto> result = movieToolService.getStoredMoviesSQL("title='Matrix'", null, "5");

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(sqlMovieChunkService).getMovieChunks(anyString(), eq(3), any(), any(), any(), any());
    }
}