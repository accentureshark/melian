package org.shark.melian.mcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.shark.melian.service.TMDBService;
import org.shark.melian.service.AggregatedMovieService;
import org.shark.melian.model.MovieResult;
import org.shark.melian.model.ChunkDto;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PureMcpServer
 */
@ExtendWith(MockitoExtension.class)
class PureMcpServerTest {

    @Mock
    private TMDBService tmdbService;

    @Mock
    private AggregatedMovieService aggregatedMovieService;

    private PureMcpServer mcpServer;

    @BeforeEach
    void setUp() {
        mcpServer = new PureMcpServer(tmdbService, aggregatedMovieService);
    }

    @Test
    void testInitialize() {
        // Given
        McpDto.InitializeRequest request = McpDto.InitializeRequest.builder()
                .protocolVersion("2024-11-05")
                .clientInfo(McpDto.ClientInfo.builder()
                        .name("test-client")
                        .version("1.0.0")
                        .build())
                .build();

        // When
        McpDto.InitializeResult result = mcpServer.initialize(request);

        // Then
        assertNotNull(result);
        assertEquals("2024-11-05", result.getProtocolVersion());
        assertEquals("melian-movie-server", result.getServerInfo().getName());
        assertEquals("1.0.0", result.getServerInfo().getVersion());
        assertNotNull(result.getCapabilities());
        assertTrue(result.getCapabilities().getTools().isListChanged());
        assertTrue(result.getCapabilities().getResources().isSubscribe());
    }

    @Test
    void testListTools() {
        // When
        McpDto.ToolsListResult result = mcpServer.listTools();

        // Then
        assertNotNull(result);
        assertNotNull(result.getTools());
        assertEquals(3, result.getTools().size());
        
        List<String> toolNames = result.getTools().stream()
                .map(McpDto.Tool::getName)
                .toList();
        
        assertTrue(toolNames.contains("search_movies"));
        assertTrue(toolNames.contains("get_movie_chunks"));
        assertTrue(toolNames.contains("get_server_status"));
    }

    @Test
    void testCallSearchMoviesTool() {
        // Given
        List<MovieResult> mockResults = Arrays.asList(
                new MovieResult("The Matrix", "Description", "1999", 8.7),
                new MovieResult("Matrix Reloaded", "Description", "2003", 7.2)
        );
        when(tmdbService.search("matrix", 10)).thenReturn(mockResults);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("query", "matrix");
        arguments.put("limit", 10);

        McpDto.CallToolRequest request = McpDto.CallToolRequest.builder()
                .name("search_movies")
                .arguments(arguments)
                .build();

        // When
        McpDto.CallToolResult result = mcpServer.callTool(request);

        // Then
        assertNotNull(result);
        assertFalse(result.isError());
        assertEquals(1, result.getContent().size());
        assertTrue(result.getContent().get(0).getText().contains("Found"));
        assertNotNull(result.getContent().get(0).getData());
        verify(tmdbService).search("matrix", 10);
    }

    @Test
    void testCallGetMovieChunksTool() {
        // Given
        List<ChunkDto> mockChunks = Arrays.asList(
                new ChunkDto("1", "Movie chunk 1", Map.of("source", "sql")),
                new ChunkDto("2", "Movie chunk 2", Map.of("source", "sql"))
        );
        when(aggregatedMovieService.getMovieChunks(eq(10), any(), any(), any(), any()))
                .thenReturn(mockChunks);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("source", "sql");
        arguments.put("limit", 10);

        McpDto.CallToolRequest request = McpDto.CallToolRequest.builder()
                .name("get_movie_chunks")
                .arguments(arguments)
                .build();

        // When
        McpDto.CallToolResult result = mcpServer.callTool(request);

        // Then
        assertNotNull(result);
        assertFalse(result.isError());
        assertEquals(1, result.getContent().size());
        assertTrue(result.getContent().get(0).getText().contains("Retrieved"));
        assertTrue(result.getContent().get(0).getText().contains("sql"));
        assertNotNull(result.getContent().get(0).getData());
        verify(aggregatedMovieService).getMovieChunks(eq(10), any(), any(), any(), any());
    }

    @Test
    void testCallGetServerStatusTool() {
        // Given
        Map<String, Object> arguments = new HashMap<>();

        McpDto.CallToolRequest request = McpDto.CallToolRequest.builder()
                .name("get_server_status")
                .arguments(arguments)
                .build();

        // When
        McpDto.CallToolResult result = mcpServer.callTool(request);

        // Then
        assertNotNull(result);
        assertFalse(result.isError());
        assertEquals(1, result.getContent().size());
        assertTrue(result.getContent().get(0).getText().contains("Server status"));
        assertNotNull(result.getContent().get(0).getData());
    }

    @Test
    void testCallUnknownTool() {
        // Given
        Map<String, Object> arguments = new HashMap<>();

        McpDto.CallToolRequest request = McpDto.CallToolRequest.builder()
                .name("unknown_tool")
                .arguments(arguments)
                .build();

        // When
        McpDto.CallToolResult result = mcpServer.callTool(request);

        // Then
        assertNotNull(result);
        assertTrue(result.isError());
        assertEquals(1, result.getContent().size());
        assertTrue(result.getContent().get(0).getText().contains("Unknown tool"));
    }

    @Test
    void testListResources() {
        // When
        McpDto.ResourcesListResult result = mcpServer.listResources();

        // Then
        assertNotNull(result);
        assertNotNull(result.getResources());
        assertEquals(3, result.getResources().size());

        List<String> resourceUris = result.getResources().stream()
                .map(McpDto.Resource::getUri)
                .toList();

        assertTrue(resourceUris.contains("melian://movies/sql"));
        assertTrue(resourceUris.contains("melian://movies/mongo"));
        assertTrue(resourceUris.contains("melian://movies/tmdb"));
    }

    @Test
    void testGetHealth() {
        // When
        McpDto.HealthStatus health = mcpServer.getHealth();

        // Then
        assertNotNull(health);
        assertEquals("OK", health.getStatus());
        assertNotNull(health.getDetails());
        assertNotNull(health.getTimestamp());
        assertTrue(health.getDetails().containsKey("tmdbService"));
        assertTrue(health.getDetails().containsKey("sqlService"));
        assertTrue(health.getDetails().containsKey("mongoService"));
    }

    @Test
    void testSearchMoviesWithMissingQuery() {
        // Given
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("limit", 10);

        McpDto.CallToolRequest request = McpDto.CallToolRequest.builder()
                .name("search_movies")
                .arguments(arguments)
                .build();

        // When
        McpDto.CallToolResult result = mcpServer.callTool(request);

        // Then
        assertNotNull(result);
        assertTrue(result.isError());
        assertEquals(1, result.getContent().size());
        assertTrue(result.getContent().get(0).getText().contains("Query parameter is required"));
    }
}