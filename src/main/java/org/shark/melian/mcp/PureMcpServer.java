package org.shark.melian.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.shark.melian.service.TMDBServicePure;
import org.shark.melian.service.MovieChunkService;
import org.shark.melian.model.MovieResult;
import org.shark.melian.model.ChunkDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

/**
 * Pure MCP Server implementation following the Model Context Protocol specification.
 * Provides movie search and data access capabilities without OpenAI dependencies.
 */
public class PureMcpServer {

    private static final Logger log = LoggerFactory.getLogger(PureMcpServer.class);
    
    private final TMDBServicePure tmdbService;
    private final MovieChunkService sqlService;
    private final MovieChunkService mongoService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public PureMcpServer(TMDBServicePure tmdbService, MovieChunkService sqlService, MovieChunkService mongoService) {
        this.tmdbService = tmdbService;
        this.sqlService = sqlService;
        this.mongoService = mongoService;
        log.info("Pure MCP Server initialized");
    }

    /**
     * Handle MCP initialize request
     */
    public McpDto.InitializeResult initialize(McpDto.InitializeRequest request) {
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
                        .description("Search for movies using TMDB API")
                        .inputSchema(createSearchMoviesSchema())
                        .build(),
                McpDto.Tool.builder()
                        .name("get_movie_chunks")
                        .description("Get movie data chunks for RAG applications")
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
        log.debug("Calling tool: {} with args: {}", request.getName(), request.getArguments());

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
        log.debug("Listing available MCP resources");

        List<McpDto.Resource> resources = Arrays.asList(
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
        log.debug("Reading resource: {}", request.getUri());

        try {
            String uri = request.getUri();
            String content;

            if (uri.startsWith("melian://movies/")) {
                String source = uri.substring("melian://movies/".length());
                List<ChunkDto> chunks = getMovieChunks(source, 20, null);
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
        details.put("tmdbService", tmdbService != null ? "OK" : "NOT_AVAILABLE");
        details.put("sqlService", sqlService != null ? "OK" : "NOT_AVAILABLE");
        details.put("mongoService", mongoService != null ? "OK" : "NOT_AVAILABLE");
        details.put("tools", Arrays.asList("search_movies", "get_movie_chunks", "get_server_status"));
        details.put("resources", Arrays.asList("melian://movies/sql", "melian://movies/mongo", "melian://movies/tmdb"));

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

        if (query == null || query.trim().isEmpty()) {
            return McpDto.CallToolResult.builder()
                    .isError(true)
                    .content(List.of(McpDto.ToolContent.builder()
                            .type("text")
                            .text("Query parameter is required")
                            .build()))
                    .build();
        }

        List<MovieResult> results = tmdbService.search(query, limit);
        try {
            String jsonResults = objectMapper.writeValueAsString(results);
            return McpDto.CallToolResult.builder()
                    .isError(false)
                    .content(List.of(McpDto.ToolContent.builder()
                            .type("text")
                            .text("Found " + results.size() + " movies for query: " + query)
                            .data(results)
                            .build()))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize search results", e);
        }
    }

    private McpDto.CallToolResult handleGetMovieChunks(Map<String, Object> args) {
        String source = args.containsKey("source") ? (String) args.get("source") : "sql";
        Integer limit = args.containsKey("limit") ? (Integer) args.get("limit") : 10;
        String filter = (String) args.get("filter");

        List<ChunkDto> chunks = getMovieChunks(source, limit, filter);
        
        try {
            return McpDto.CallToolResult.builder()
                    .isError(false)
                    .content(List.of(McpDto.ToolContent.builder()
                            .type("text")
                            .text("Retrieved " + chunks.size() + " chunks from " + source + " source")
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

    private List<ChunkDto> getMovieChunks(String source, int limit, String filter) {
        MovieChunkService service = "mongo".equalsIgnoreCase(source) ? mongoService : sqlService;
        return service.getMovieChunks(source, limit, null, filter, null, null);
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
        
        Map<String, Object> sourceProp = new HashMap<>();
        sourceProp.put("type", "string");
        sourceProp.put("description", "Data source (sql, mongo)");
        sourceProp.put("enum", Arrays.asList("sql", "mongo"));
        sourceProp.put("default", "sql");
        properties.put("source", sourceProp);
        
        Map<String, Object> limitProp = new HashMap<>();
        limitProp.put("type", "integer");
        limitProp.put("description", "Maximum number of chunks");
        limitProp.put("default", 10);
        properties.put("limit", limitProp);
        
        Map<String, Object> filterProp = new HashMap<>();
        filterProp.put("type", "string");
        filterProp.put("description", "Optional filter for chunks");
        properties.put("filter", filterProp);
        
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