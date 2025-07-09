package org.shark.melian.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.shark.melian.model.ChunkDto;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestApiChunkServiceTest {

    @InjectMocks
    private RestApiChunkService restApiChunkService;

    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        // Use reflection to set the RestTemplate since it's created internally
    }

    @Test
    void testGetChunks_ValidFilmTable_ReturnsChunks() {
        // Given
        String table = "film";
        String source = "rest";
        int limit = 10;
        String afterId = null;
        String filter = "title='Matrix'";
        List<String> tags = null;
        String sort = null;

        // Mock TMDB API response
        String mockResponse = """
                {
                    "results": [
                        {
                            "id": 603,
                            "title": "The Matrix",
                            "overview": "Set in the 22nd century, The Matrix tells the story of a computer hacker...",
                            "release_date": "1999-03-30",
                            "vote_average": 8.2
                        }
                    ],
                    "total_results": 1
                }
                """;

        try (MockedStatic<RestTemplate> mockedRestTemplate = mockStatic(RestTemplate.class)) {
            RestTemplate mockRestTemplate = mock(RestTemplate.class);
            mockedRestTemplate.when(RestTemplate::new).thenReturn(mockRestTemplate);
            
            when(mockRestTemplate.exchange(
                    anyString(), 
                    eq(HttpMethod.GET), 
                    any(HttpEntity.class), 
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

            // When
            List<ChunkDto> result = restApiChunkService.getChunks(table, source, limit, afterId, filter, tags, sort);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            
            ChunkDto chunk = result.get(0);
            assertEquals("603", chunk.getId());
            assertNotNull(chunk.getText());
            assertTrue(chunk.getText().contains("The Matrix"));
            assertNotNull(chunk.getMetadata());
            assertEquals("The Matrix", chunk.getMetadata().get("title"));
        }
    }

    @Test
    void testGetChunks_InvalidTable_ThrowsException() {
        // Given
        String invalidTable = "books";

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            restApiChunkService.getChunks(invalidTable, "rest", 10, null, null, null, null));
    }

    @Test
    void testGetChunks_NoResults_ReturnsEmptyList() {
        // Given
        String table = "film";
        String filter = "title='NonexistentMovie'";

        // Mock empty TMDB API response
        String mockResponse = """
                {
                    "results": [],
                    "total_results": 0
                }
                """;

        try (MockedStatic<RestTemplate> mockedRestTemplate = mockStatic(RestTemplate.class)) {
            RestTemplate mockRestTemplate = mock(RestTemplate.class);
            mockedRestTemplate.when(RestTemplate::new).thenReturn(mockRestTemplate);
            
            when(mockRestTemplate.exchange(
                    anyString(), 
                    eq(HttpMethod.GET), 
                    any(HttpEntity.class), 
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

            // When
            List<ChunkDto> result = restApiChunkService.getChunks(table, "rest", 10, null, filter, null, null);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    void testGetChunks_WithComplexFilter_ExtractsTitleCorrectly() {
        // Given
        String table = "film";
        String filter = "title LIKE '%Matrix%'";

        // Mock TMDB API response
        String mockResponse = """
                {
                    "results": [
                        {
                            "id": 603,
                            "title": "The Matrix",
                            "overview": "A computer hacker learns about the true nature of reality.",
                            "release_date": "1999-03-30",
                            "vote_average": 8.2
                        }
                    ],
                    "total_results": 1
                }
                """;

        try (MockedStatic<RestTemplate> mockedRestTemplate = mockStatic(RestTemplate.class)) {
            RestTemplate mockRestTemplate = mock(RestTemplate.class);
            mockedRestTemplate.when(RestTemplate::new).thenReturn(mockRestTemplate);
            
            when(mockRestTemplate.exchange(
                    contains("Matrix"), 
                    eq(HttpMethod.GET), 
                    any(HttpEntity.class), 
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

            // When
            List<ChunkDto> result = restApiChunkService.getChunks(table, "rest", 10, null, filter, null, null);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            verify(mockRestTemplate).exchange(
                    contains("Matrix"), 
                    eq(HttpMethod.GET), 
                    any(HttpEntity.class), 
                    eq(String.class)
            );
        }
    }

    @Test
    void testGetChunks_WithUrlEncodedFilter_DecodesCorrectly() {
        // Given
        String table = "film";
        String encodedFilter = "title%3D%27Matrix%27"; // title='Matrix' URL encoded

        // Mock TMDB API response
        String mockResponse = """
                {
                    "results": [
                        {
                            "id": 603,
                            "title": "The Matrix",
                            "overview": "A computer hacker learns about the true nature of reality.",
                            "release_date": "1999-03-30",
                            "vote_average": 8.2
                        }
                    ],
                    "total_results": 1
                }
                """;

        try (MockedStatic<RestTemplate> mockedRestTemplate = mockStatic(RestTemplate.class)) {
            RestTemplate mockRestTemplate = mock(RestTemplate.class);
            mockedRestTemplate.when(RestTemplate::new).thenReturn(mockRestTemplate);
            
            when(mockRestTemplate.exchange(
                    anyString(), 
                    eq(HttpMethod.GET), 
                    any(HttpEntity.class), 
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

            // When
            List<ChunkDto> result = restApiChunkService.getChunks(table, "rest", 10, null, encodedFilter, null, null);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    @Test
    void testExtractTitleFromFilter_SimpleEquality() {
        // This tests the private method indirectly through the behavior
        // We can verify the URL contains the expected title
        String table = "film";
        String filter = "title='Thor'";

        try (MockedStatic<RestTemplate> mockedRestTemplate = mockStatic(RestTemplate.class)) {
            RestTemplate mockRestTemplate = mock(RestTemplate.class);
            mockedRestTemplate.when(RestTemplate::new).thenReturn(mockRestTemplate);
            
            when(mockRestTemplate.exchange(
                    contains("Thor"), 
                    eq(HttpMethod.GET), 
                    any(HttpEntity.class), 
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>("{\"results\":[]}", HttpStatus.OK));

            // When
            restApiChunkService.getChunks(table, "rest", 10, null, filter, null, null);

            // Then
            verify(mockRestTemplate).exchange(
                    contains("Thor"), 
                    eq(HttpMethod.GET), 
                    any(HttpEntity.class), 
                    eq(String.class)
            );
        }
    }

    @Test
    void testGetChunks_ApiError_HandlesGracefully() {
        // Given
        String table = "film";
        String filter = "title='Matrix'";

        try (MockedStatic<RestTemplate> mockedRestTemplate = mockStatic(RestTemplate.class)) {
            RestTemplate mockRestTemplate = mock(RestTemplate.class);
            mockedRestTemplate.when(RestTemplate::new).thenReturn(mockRestTemplate);
            
            when(mockRestTemplate.exchange(
                    anyString(), 
                    eq(HttpMethod.GET), 
                    any(HttpEntity.class), 
                    eq(String.class)
            )).thenThrow(new RuntimeException("API Error"));

            // When & Then
            assertThrows(RuntimeException.class, () -> 
                restApiChunkService.getChunks(table, "rest", 10, null, filter, null, null));
        }
    }
}