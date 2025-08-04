package org.shark.melian.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shark.melian.service.TMDBService;
import org.shark.melian.service.AggregatedMovieService;
import org.shark.melian.model.MovieResult;
import org.shark.melian.model.ChunkDto;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * Spring MCP Server implementation following the Model Context Protocol specification.
 * Provides movie search and data access capabilities using Spring best practices.
 * Uses AggregatedMovieService for parallel data fetching from all sources.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PureMcpServer {
    
    private final TMDBService tmdbService;
    private final AggregatedMovieService aggregatedMovieService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Handle MCP initialize request
     */
    public McpDto.InitializeResult initialize(McpDto.InitializeRequest request) {
        if (request.getClientInfo() == null) {

            throw new IllegalArgumentException("ClientInfo es obligatorio");
        }
        String clientName = request.getClientInfo().getName();
        log.info("MCP Initialize request from client: {}", request.getClientInfo().getName());

        return McpDto.InitializeResult.builder()
                .protocolVersion("2024-11-05")
                .serverInfo(McpDto.ServerInfo.builder()
                        .name("melian-movie-server")
                        .version("1.0.0")
                        .build())
                .capabilities(McpDto.ServerCapabilities.builder()
                        .logging(McpDto.LoggingCapability.builder().build())
                        .tools(McpDto.ToolsCapability.builder()
                                .listChanged(true)
                                .build())
                        .resources(McpDto.ResourcesCapability.builder()
                                .subscribe(true)
                                .listChanged(true)
                                .build())
                        .prompts(McpDto.PromptsCapability.builder()
                                .listChanged(false)
                                .build())
                        .build())
                .build();
    }

    /**
     * List available tools
     */
    public McpDto.ToolsListResult listTools() {
        log.debug("Listing available MCP tools");

        List<McpDto.Tool> tools = Arrays.asList(
                McpDto.Tool.builder()
                        .name("search_movies")
                        .description("Search for movies using TMDB API and store in all available databases")
                        .inputSchema(createSearchMoviesSchema())
                        .build(),
                McpDto.Tool.builder()
                        .name("get_movie_chunks")
                        .description("Get movie data chunks from ALL sources (SQL, MongoDB, TMDB) in parallel for RAG applications")
                        .inputSchema(createGetChunksSchema())
                        .build(),
                McpDto.Tool.builder()
                        .name("get_server_status")
                        .description("Get server status and configuration")
                        .inputSchema(createStatusSchema())
                        .build()
        );

        return McpDto.ToolsListResult.builder()
                .tools(tools)
                .build();
    }

    /**
     * Call a tool with given arguments
     */
    public McpDto.CallToolResult callTool(McpDto.CallToolRequest request) {
        log.info("Calling tool: {} with args: {}", request.getName(), request.getArguments());

        try {
            switch (request.getName()) {
                case "search_movies":
                    return handleSearchMovies(request.getArguments());
                case "get_movie_chunks":
                    return handleGetMovieChunks(request.getArguments());
                case "get_server_status":
                    return handleGetServerStatus(request.getArguments());
                default:
                    return McpDto.CallToolResult.builder()
                            .isError(true)
                            .content(List.of(McpDto.ToolContent.builder()
                                    .type("text")
                                    .text("Unknown tool: " + request.getName())
                                    .build()))
                            .build();
            }
        } catch (Exception e) {
            log.error("Error calling tool: " + request.getName(), e);
            return McpDto.CallToolResult.builder()
                    .isError(true)
                    .content(List.of(McpDto.ToolContent.builder()
                            .type("text")
                            .text("Error: " + e.getMessage())
                            .build()))
                    .build();
        }
    }

    /**
     * List available resources
     */
    public McpDto.ResourcesListResult listResources() {
        log.info("Listing available MCP resources");

        List<McpDto.Resource> resources = Arrays.asList(
                McpDto.Resource.builder()
                        .uri("melian://movies/aggregated")
                        .name("Aggregated Movie Data")
                        .description("Movie data from ALL sources (SQL, MongoDB, TMDB) combined")
                        .mimeType("application/json")
                        .build(),
                McpDto.Resource.builder()
                        .uri("melian://movies/sql")
                        .name("SQL Movie Database")
                        .description("Movie data from MySQL Sakila database")
                        .mimeType("application/json")
                        .build(),
                McpDto.Resource.builder()
                        .uri("melian://movies/mongo")
                        .name("MongoDB Movie Collection")
                        .description("Movie data from MongoDB collection")
                        .mimeType("application/json")
                        .build(),
                McpDto.Resource.builder()
                        .uri("melian://movies/tmdb")
                        .name("TMDB API Movies")
                        .description("Live movie data from TMDB API")
                        .mimeType("application/json")
                        .build()
        );

        return McpDto.ResourcesListResult.builder()
                .resources(resources)
                .build();
    }

    /**
     * Read a resource by URI
     */
    public McpDto.ReadResourceResult readResource(McpDto.ReadResourceRequest request) {
        log.info("Reading resource: {}", request.getUri());

        try {
            String uri = request.getUri();
            String content;

            if (uri.startsWith("melian://movies/")) {
                String source = uri.substring("melian://movies/".length());
                List<ChunkDto> chunks;
                
                if ("aggregated".equals(source)) {
                    // Get chunks from all sources aggregated
                    chunks = aggregatedMovieService.getMovieChunks(20, null, null, null, null);
                } else if ("tmdb".equals(source)) {
                    // Get chunks from TMDB by searching for popular recent movies
                    List<MovieResult> movies = tmdbService.search("2024", 20);
                    chunks = movies.stream()
                            .map(movie -> {
                                ChunkDto chunk = new ChunkDto();
                                chunk.setId("tmdb_" + movie.title().hashCode());
                                chunk.setText(String.format("Movie: %s (%s)\nOverview: %s\nRating: %.1f",
                                        movie.title(), movie.releaseDate(), movie.overview(), movie.rating()));
                                Map<String, Object> metadata = new HashMap<>();
                                metadata.put("title", movie.title());
                                metadata.put("overview", movie.overview());
                                metadata.put("release_date", movie.releaseDate());
                                metadata.put("rating", movie.rating());
                                metadata.put("source", "tmdb");
                                chunk.setMetadata(metadata);
                                return chunk;
                            })
                            .collect(java.util.stream.Collectors.toList());
                } else {
                    // Get chunks from aggregated sources
                    chunks = aggregatedMovieService.getMovieChunks(20, null, null, null, null);
                }
                
                content = objectMapper.writeValueAsString(chunks);
            } else {
                throw new IllegalArgumentException("Unknown resource URI: " + uri);
            }

            return McpDto.ReadResourceResult.builder()
                    .contents(List.of(McpDto.ResourceContent.builder()
                            .uri(uri)
                            .mimeType("application/json")
                            .text(content)
                            .build()))
                    .build();
        } catch (Exception e) {
            log.error("Error reading resource: " + request.getUri(), e);
            throw new RuntimeException("Failed to read resource: " + e.getMessage());
        }
    }

    /**
     * Get health status
     */
    public McpDto.HealthStatus getHealth() {
        Map<String, Object> details = new HashMap<>();
        
        // Get status from aggregated service
        Map<String, String> servicesStatus = aggregatedMovieService.getServicesStatus();
        details.putAll(servicesStatus);
        
        details.put("tools", Arrays.asList("search_movies", "get_movie_chunks", "get_server_status"));
        details.put("resources", Arrays.asList("melian://movies/aggregated", "melian://movies/sql", "melian://movies/mongo", "melian://movies/tmdb"));
        details.put("aggregated_service", "ENABLED");

        return McpDto.HealthStatus.builder()
                .status("OK")
                .details(details)
                .timestamp(Instant.now().toString())
                .build();
    }

    // Private helper methods

    private McpDto.CallToolResult handleSearchMovies(Map<String, Object> args) {
        String query = (String) args.get("query");
        Integer limit = args.containsKey("limit") ? (Integer) args.get("limit") : 10;
        log.info("handleSearchMovies: {}", query);

        if (query == null || query.trim().isEmpty()) {
            return McpDto.CallToolResult.builder()
                    .isError(true)
                    .content(List.of(McpDto.ToolContent.builder()
                            .type("text")
                            .text("Query parameter is required")
                            .build()))
                    .build();
        }

        List<MovieResult> results = aggregatedMovieService.searchMovies(query, limit);
        try {
            String jsonResults = objectMapper.writeValueAsString(results);
            return McpDto.CallToolResult.builder()
                    .isError(false)
                    .content(List.of(McpDto.ToolContent.builder()
                            .type("text")
                            .text("Found " + results.size() + " movies for query: " + query + " (automatically stored in all available databases)")
                            .data(results)
                            .build()))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize search results", e);
        }
    }

    private McpDto.CallToolResult handleGetMovieChunks(Map<String, Object> args) {
        Integer limit = args.containsKey("limit") ? (Integer) args.get("limit") : 10;
        String filter = (String) args.get("filter");
        String afterId = (String) args.get("afterId");
        String sort = (String) args.get("sort");

        List<ChunkDto> chunks = aggregatedMovieService.getMovieChunks(limit, afterId, filter, null, sort);
        
        try {
            return McpDto.CallToolResult.builder()
                    .isError(false)
                    .content(List.of(McpDto.ToolContent.builder()
                            .type("text")
                            .text("Retrieved " + chunks.size() + " chunks from ALL sources (SQL, MongoDB, TMDB) in parallel")
                            .data(chunks)
                            .build()))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get movie chunks", e);
        }
    }

    private McpDto.CallToolResult handleGetServerStatus(Map<String, Object> args) {
        McpDto.HealthStatus health = getHealth();
        
        try {
            return McpDto.CallToolResult.builder()
                    .isError(false)
                    .content(List.of(McpDto.ToolContent.builder()
                            .type("text")
                            .text("Server status: " + health.getStatus())
                            .data(health)
                            .build()))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get server status", e);
        }
    }

    // Schema creation methods

    private Object createSearchMoviesSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> queryProp = new HashMap<>();
        queryProp.put("type", "string");
        queryProp.put("description", "Movie search query");
        properties.put("query", queryProp);
        
        Map<String, Object> limitProp = new HashMap<>();
        limitProp.put("type", "integer");
        limitProp.put("description", "Maximum number of results");
        limitProp.put("default", 10);
        properties.put("limit", limitProp);
        
        schema.put("properties", properties);
        schema.put("required", Arrays.asList("query"));
        
        return schema;
    }

    private Object createGetChunksSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> limitProp = new HashMap<>();
        limitProp.put("type", "integer");
        limitProp.put("description", "Maximum number of chunks to retrieve from ALL sources");
        limitProp.put("default", 10);
        properties.put("limit", limitProp);
        
        Map<String, Object> filterProp = new HashMap<>();
        filterProp.put("type", "string");
        filterProp.put("description", "Optional filter for chunks (applied to all sources)");
        properties.put("filter", filterProp);
        
        Map<String, Object> afterIdProp = new HashMap<>();
        afterIdProp.put("type", "string");
        afterIdProp.put("description", "Pagination: get chunks after this ID");
        properties.put("afterId", afterIdProp);
        
        Map<String, Object> sortProp = new HashMap<>();
        sortProp.put("type", "string");
        sortProp.put("description", "Sort field for results");
        properties.put("sort", sortProp);
        
        schema.put("properties", properties);
        
        return schema;
    }

    private Object createStatusSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", new HashMap<>());
        return schema;
    }
}