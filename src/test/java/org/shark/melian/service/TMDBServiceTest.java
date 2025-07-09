package org.shark.melian.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.shark.melian.client.TMDBApiClient;
import org.shark.melian.client.TMDBApiClient.TMDBMovie;
import org.shark.melian.client.TMDBApiClient.TMDBResponse;
import org.shark.melian.model.MovieResult;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TMDBServiceTest {

    @Mock
    private TMDBApiClient tmdbApiClient;

    @InjectMocks
    private TMDBService tmdbService;

    private TMDBResponse mockResponse;
    private TMDBMovie mockMovie;

    @BeforeEach
    void setUp() {
        mockMovie = new TMDBMovie();
        mockMovie.title = "The Matrix";
        mockMovie.overview = "A computer hacker learns about the true nature of reality.";
        mockMovie.release_date = "1999-03-30";
        mockMovie.vote_average = 8.7;

        mockResponse = new TMDBResponse();
        mockResponse.results = List.of(mockMovie);
    }

    @Test
    void testSearch_ValidTitle_ReturnsMovies() {
        // Given
        String title = "Matrix";
        int limit = 5;
        when(tmdbApiClient.searchMovies(any(Map.class))).thenReturn(mockResponse);

        // When
        List<MovieResult> result = tmdbService.search(title, limit);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        
        MovieResult movie = result.get(0);
        assertEquals("The Matrix", movie.title());
        assertEquals("A computer hacker learns about the true nature of reality.", movie.overview());
        assertEquals("1999-03-30", movie.releaseDate());
        assertEquals(8.7, movie.rating());
        
        verify(tmdbApiClient).searchMovies(argThat(params -> 
            params.containsKey("query") && "Matrix".equals(params.get("query"))));
    }

    @Test
    void testSearch_NoResults_ReturnsEmptyList() {
        // Given
        String title = "NonExistentMovie";
        int limit = 5;
        TMDBResponse emptyResponse = new TMDBResponse();
        emptyResponse.results = List.of();
        when(tmdbApiClient.searchMovies(any(Map.class))).thenReturn(emptyResponse);

        // When
        List<MovieResult> result = tmdbService.search(title, limit);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testSearch_NullResponse_ReturnsEmptyList() {
        // Given
        String title = "Matrix";
        int limit = 5;
        when(tmdbApiClient.searchMovies(any(Map.class))).thenReturn(null);

        // When
        List<MovieResult> result = tmdbService.search(title, limit);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testSearchByParams_WithCustomParams_CallsApiWithParams() {
        // Given
        Map<String, String> params = Map.of("query", "Thor", "year", "2011");
        int limit = 3;
        when(tmdbApiClient.searchMovies(params)).thenReturn(mockResponse);

        // When
        List<MovieResult> result = tmdbService.searchByParams(params, limit);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(tmdbApiClient).searchMovies(params);
    }

    @Test
    void testSearch_WithLimit_RespectsLimit() {
        // Given
        String title = "Matrix";
        int limit = 1;
        
        // Create response with multiple movies
        TMDBMovie movie2 = new TMDBMovie();
        movie2.title = "The Matrix Reloaded";
        movie2.overview = "The second Matrix movie";
        movie2.release_date = "2003-05-15";
        movie2.vote_average = 7.2;
        
        TMDBResponse multiResponse = new TMDBResponse();
        multiResponse.results = List.of(mockMovie, movie2);
        
        when(tmdbApiClient.searchMovies(any(Map.class))).thenReturn(multiResponse);

        // When
        List<MovieResult> result = tmdbService.search(title, limit);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size()); // Should respect limit
        assertEquals("The Matrix", result.get(0).title());
    }
}