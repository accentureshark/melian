package org.shark.melian.integration.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.shark.melian.client.TMDBApiClient;
import org.shark.melian.client.TMDBApiClient.TMDBMovie;
import org.shark.melian.client.TMDBApiClient.TMDBResponse;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test configuration for integration tests.
 * Provides mocked TMDB API client for testing.
 */
@TestConfiguration
@Profile("integration")
public class IntegrationTestConfig {
    
    @Bean
    @Primary
    public TMDBApiClient mockTMDBApiClient() {
        TMDBApiClient mockClient = mock(TMDBApiClient.class);
        
        // Create sample movie data
        TMDBMovie movie1 = new TMDBMovie();
        movie1.title = "The Matrix";
        movie1.overview = "A computer hacker learns from mysterious rebels about the true nature of his reality and his role in the war against its controllers.";
        movie1.release_date = "1999-03-30";
        movie1.vote_average = 8.7;
        
        TMDBMovie movie2 = new TMDBMovie();
        movie2.title = "The Matrix Reloaded";
        movie2.overview = "Neo and his allies race against time before the machines discover the city of Zion and destroy it.";
        movie2.release_date = "2003-05-15";
        movie2.vote_average = 7.2;
        
        TMDBResponse response = new TMDBResponse();
        response.results = List.of(movie1, movie2);
        
        when(mockClient.searchMovies(any(Map.class))).thenReturn(response);
        
        return mockClient;
    }
}