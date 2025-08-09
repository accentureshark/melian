package org.shark.melian.mcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.shark.melian.service.TMDBService;
import org.shark.melian.service.AggregatedMovieService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for MCP protocol compliance - verifying new endpoints work correctly
 */
@ExtendWith(MockitoExtension.class)
class McpComplianceTest {

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
    void testPingEndpoint() {
        McpDto.PingRequest request = McpDto.PingRequest.builder().build();
        McpDto.PingResult result = mcpServer.ping(request);

        assertNotNull(result);
        assertEquals("OK", result.getStatus());
        assertNotNull(result.getTimestamp());
    }

    @Test
    void testListPrompts() {
        McpDto.PromptsListRequest request = McpDto.PromptsListRequest.builder().build();
        McpDto.PromptsListResult result = mcpServer.listPrompts(request);

        assertNotNull(result);
        assertNotNull(result.getPrompts());
        assertEquals(2, result.getPrompts().size());
        
        assertTrue(result.getPrompts().stream()
                .anyMatch(p -> "movie_search_prompt".equals(p.getName())));
        assertTrue(result.getPrompts().stream()
                .anyMatch(p -> "movie_analysis_prompt".equals(p.getName())));
    }

    @Test
    void testGetPrompt() {
        McpDto.PromptsGetRequest request = McpDto.PromptsGetRequest.builder()
                .name("movie_search_prompt")
                .arguments(java.util.Map.of("topic", "action"))
                .build();

        McpDto.PromptsGetResult result = mcpServer.getPrompt(request);

        assertNotNull(result);
        assertNotNull(result.getDescription());
        assertNotNull(result.getMessages());
        assertFalse(result.getMessages().isEmpty());
        assertTrue(result.getMessages().get(0).getContent().getText().contains("action"));
    }

    @Test
    void testGetPromptUnknown() {
        McpDto.PromptsGetRequest request = McpDto.PromptsGetRequest.builder()
                .name("unknown_prompt")
                .arguments(java.util.Map.of())
                .build();

        assertThrows(IllegalArgumentException.class, () -> mcpServer.getPrompt(request));
    }

    @Test
    void testListResourceTemplates() {
        McpDto.ResourceTemplatesListRequest request = McpDto.ResourceTemplatesListRequest.builder().build();
        McpDto.ResourceTemplatesListResult result = mcpServer.listResourceTemplates(request);

        assertNotNull(result);
        assertNotNull(result.getResourceTemplates());
        assertEquals(2, result.getResourceTemplates().size());

        assertTrue(result.getResourceTemplates().stream()
                .anyMatch(t -> t.getUriTemplate().contains("movies/{source}")));
        assertTrue(result.getResourceTemplates().stream()
                .anyMatch(t -> t.getUriTemplate().contains("movies/search/{query}")));
    }

    @Test
    void testSubscribeToResource() {
        McpDto.ResourcesSubscribeRequest request = McpDto.ResourcesSubscribeRequest.builder()
                .uri("melian://movies/sql")
                .build();

        McpDto.ResourcesSubscribeResult result = mcpServer.subscribeToResource(request);

        assertNotNull(result);
        // Just verify it doesn't throw an exception - actual subscription logic would be tested elsewhere
    }

    @Test
    void testSetLoggingLevel() {
        McpDto.SetLoggingLevelRequest request = McpDto.SetLoggingLevelRequest.builder()
                .level("DEBUG")
                .build();

        McpDto.SetLoggingLevelResult result = mcpServer.setLoggingLevel(request);

        assertNotNull(result);
        // Just verify it doesn't throw an exception - actual logging configuration would be tested elsewhere
    }

    @Test
    void testCompleteResource() {
        McpDto.CompletionRequest request = McpDto.CompletionRequest.builder()
                .ref(McpDto.CompletionRef.builder()
                        .type("resource")
                        .name("movies")
                        .build())
                .argument("melian://")
                .build();

        McpDto.CompletionResult result = mcpServer.complete(request);

        assertNotNull(result);
        assertNotNull(result.getCompletion());
        assertEquals(3, result.getCompletion().size());
        
        assertTrue(result.getCompletion().stream()
                .anyMatch(c -> c.getValue().contains("sql")));
        assertTrue(result.getCompletion().stream()
                .anyMatch(c -> c.getValue().contains("mongo")));
        assertTrue(result.getCompletion().stream()
                .anyMatch(c -> c.getValue().contains("tmdb")));
    }

    @Test
    void testCompleteArgument() {
        McpDto.CompletionRequest request = McpDto.CompletionRequest.builder()
                .ref(McpDto.CompletionRef.builder()
                        .type("argument")
                        .name("search_movies")
                        .build())
                .argument("query")
                .build();

        McpDto.CompletionResult result = mcpServer.complete(request);

        assertNotNull(result);
        assertNotNull(result.getCompletion());
        assertEquals(3, result.getCompletion().size());
        
        assertTrue(result.getCompletion().stream()
                .anyMatch(c -> "action".equals(c.getValue())));
        assertTrue(result.getCompletion().stream()
                .anyMatch(c -> "comedy".equals(c.getValue())));
        assertTrue(result.getCompletion().stream()
                .anyMatch(c -> "drama".equals(c.getValue())));
    }
}