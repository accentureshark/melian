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

@Component
@RequiredArgsConstructor
@Slf4j
public class PureMcpServer {

    private final TMDBService tmdbService;
    private final AggregatedMovieService aggregatedMovieService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> initialize(Map<String, Object> request) {
        log.info("[PureMcpServer] Inicializando MCP con request: {}", request);

        Map<String, Object> response = new HashMap<>();
        response.put("name", "Melian Movie Search");
        response.put("version", "1.0");
        response.put("display_name", "Melian - Buscador de películas inteligente");
        response.put("description", "Busca películas en diferentes fuentes y devuelve información detallada");
        response.put("user_context_strategy", "ignore");
        response.put("schema_format", "jsonschema");

        log.info("[PureMcpServer] Inicialización completada con éxito");
        return response;
    }

    // Método compatible con la nueva API
    public McpDto.InitializeResult initialize(McpDto.InitializeRequest request) {
        log.info("[PureMcpServer] Inicializando MCP con nuevo formato: {}", request);

        return McpDto.InitializeResult.builder()
                .protocolVersion("2024-11-05")
                .serverInfo(McpDto.ServerInfo.builder()
                        .name("Melian Movie Search")
                        .version("1.0")
                        .build())
                .capabilities(McpDto.ServerCapabilities.builder()
                        .logging(McpDto.LoggingCapability.builder().build())
                        .tools(McpDto.ToolsCapability.builder().listChanged(true).build())
                        .resources(McpDto.ResourcesCapability.builder()
                                .subscribe(true)
                                .listChanged(true)
                                .build())
                        .prompts(McpDto.PromptsCapability.builder().build())
                        .build())
                .build();
    }

    public McpDto.ToolsListResult listTools() {
        log.info("[PureMcpServer] Listando herramientas disponibles");

        List<McpDto.Tool> tools = Arrays.asList(
                McpDto.Tool.builder()
                        .name("search_movies")
                        .description("Search for movies using TMDB API and store in all available databases")
                        .inputSchema(createSearchSchema())
                        .build(),
                McpDto.Tool.builder()
                        .name("get_movie_chunks")
                        .description("Get movie data chunks from ALL sources (SQL, MongoDB, TMDB) in parallel for RAG applications")
                        .inputSchema(createChunksSchema())
                        .build(),
                McpDto.Tool.builder()
                        .name("get_server_status")
                        .description("Get server status and configuration")
                        .inputSchema(createStatusSchema())
                        .build()
        );

        return McpDto.ToolsListResult.builder().tools(tools).build();
    }

    public McpDto.CallToolResult callTool(McpDto.CallToolRequest request) {
        log.info("[PureMcpServer] Llamando a herramienta: {} con argumentos: {}",
                request.getName(), request.getArguments());

        try {
            Map<String, Object> result = null;

            if ("search_movies".equals(request.getName())) {
                String query = (String) request.getArguments().get("query");
                if (query == null || query.trim().isEmpty()) {
                    return McpDto.CallToolResult.builder()
                            .isError(true)
                            .content(List.of(McpDto.ToolContent.builder()
                                    .type("text")
                                    .text("Query parameter is required")
                                    .build()))
                            .build();
                }
                
                Integer limit = request.getArguments().get("limit") != null ? 
                    Integer.parseInt(request.getArguments().get("limit").toString()) : 10;
                
                List<MovieResult> movies = tmdbService.search(query, limit);
                
                return McpDto.CallToolResult.builder()
                        .isError(false)
                        .content(List.of(McpDto.ToolContent.builder()
                                .type("text")
                                .text("Found " + movies.size() + " movies for query: " + query)
                                .data(movies)
                                .build()))
                        .build();

            } else if ("get_movie_chunks".equals(request.getName())) {
                Integer limit = request.getArguments().get("limit") != null ? 
                    Integer.parseInt(request.getArguments().get("limit").toString()) : 10;
                String filter = (String) request.getArguments().get("filter");
                
                List<ChunkDto> chunks = aggregatedMovieService.getMovieChunks(limit, null, filter, null, null);
                
                return McpDto.CallToolResult.builder()
                        .isError(false)
                        .content(List.of(McpDto.ToolContent.builder()
                                .type("text")
                                .text("Retrieved " + chunks.size() + " chunks from sql and other sources")
                                .data(chunks)
                                .build()))
                        .build();

            } else if ("get_server_status".equals(request.getName())) {
                Map<String, Object> status = new HashMap<>();
                status.put("status", "OK");
                status.put("timestamp", Instant.now().toString());
                status.put("version", "1.0");
                status.put("services", Map.of(
                    "tmdb", "AVAILABLE",
                    "aggregated", "AVAILABLE"
                ));
                
                return McpDto.CallToolResult.builder()
                        .isError(false)
                        .content(List.of(McpDto.ToolContent.builder()
                                .type("text")
                                .text("Server status retrieved successfully")
                                .data(status)
                                .build()))
                        .build();
            }

            return McpDto.CallToolResult.builder()
                    .isError(true)
                    .content(List.of(McpDto.ToolContent.builder()
                            .type("text")
                            .text("Unknown tool: " + request.getName())
                            .build()))
                    .build();

        } catch (Exception e) {
            log.error("[PureMcpServer] Error al ejecutar herramienta: {}", e.getMessage(), e);

            return McpDto.CallToolResult.builder()
                    .isError(true)
                    .content(List.of(McpDto.ToolContent.builder()
                            .type("text")
                            .text("Error: " + e.getMessage())
                            .build()))
                    .build();
        }
    }

    public McpDto.ResourcesListResult listResources() {
        log.info("[PureMcpServer] Listando recursos disponibles");

        List<McpDto.Resource> resources = Arrays.asList(
                McpDto.Resource.builder()
                        .uri("melian://movies/sql")
                        .name("SQL Movies")
                        .description("Movies from SQL database")
                        .mimeType("application/json")
                        .build(),
                McpDto.Resource.builder()
                        .uri("melian://movies/mongo")
                        .name("MongoDB Movies")
                        .description("Movies from MongoDB")
                        .mimeType("application/json")
                        .build(),
                McpDto.Resource.builder()
                        .uri("melian://movies/tmdb")
                        .name("TMDB Movies")
                        .description("Movies from TMDB API")
                        .mimeType("application/json")
                        .build()
        );

        return McpDto.ResourcesListResult.builder().resources(resources).build();
    }

    public McpDto.ReadResourceResult readResource(McpDto.ReadResourceRequest request) {
        log.info("[PureMcpServer] Leyendo recurso: {}", request.getUri());

        try {
            String uri = request.getUri();
            List<ChunkDto> chunks = new ArrayList<>();

            if (uri.startsWith("movies/")) {
                String source = uri.substring("movies/".length());
                if ("all".equals(source)) {
                    chunks = aggregatedMovieService.getMovieChunks(10, null, null, null, null);
                } else if ("sql".equals(source)) {
                    // Obtener solo de SQL
                } else if ("mongo".equals(source)) {
                    // Obtener solo de MongoDB
                }
            }

            String content = objectMapper.writeValueAsString(chunks);

            return McpDto.ReadResourceResult.builder()
                    .contents(List.of(McpDto.ResourceContent.builder()
                            .uri(request.getUri())
                            .mimeType("application/json")
                            .text(content)
                            .build()))
                    .build();

        } catch (Exception e) {
            log.error("[PureMcpServer] Error al leer recurso: {}", e.getMessage(), e);
            throw new RuntimeException("Error al leer recurso: " + e.getMessage());
        }
    }

    public Map<String, Object> executeSearch(Map<String, Object> request) {
        log.info("[PureMcpServer] Ejecutando búsqueda con request: {}", request);

        try {
            Map<String, Object> params = (Map<String, Object>) request.get("parameters");
            String rawQuery = params.containsKey("query") ? params.get("query").toString() : "";
            String query = rawQuery.trim().replaceAll("\\s+", " ");
            int limit = params.containsKey("limit") ? Integer.parseInt(params.get("limit").toString()) : 10;

            log.info("[PureMcpServer] Buscando películas con query='{}', limit={}", query, limit);

            List<MovieResult> movies = aggregatedMovieService.searchMovies(query, limit);
            log.info("[PureMcpServer] Encontradas {} películas para la consulta '{}'", movies.size(), query);

            if (movies.isEmpty()) {
                log.warn("[PureMcpServer] No se encontraron películas para la consulta: '{}'", query);
            } else {
                log.info("[PureMcpServer] Primera película encontrada: '{}'", movies.get(0).title());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("movies", movies);
            return response;
        } catch (Exception e) {
            log.error("[PureMcpServer] Error al ejecutar búsqueda: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            return response;
        }
    }

    // Resto de métodos sin cambios

    // Métodos auxiliares para crear esquemas
    private Object createSearchSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> queryProp = new HashMap<>();
        queryProp.put("type", "string");
        queryProp.put("description", "Consulta de búsqueda");
        properties.put("query", queryProp);

        Map<String, Object> limitProp = new HashMap<>();
        limitProp.put("type", "integer");
        limitProp.put("description", "Límite de resultados");
        properties.put("limit", limitProp);

        schema.put("properties", properties);
        schema.put("required", List.of("query"));

        return schema;
    }

    private Object createChunksSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> limitProp = new HashMap<>();
        limitProp.put("type", "integer");
        limitProp.put("description", "Límite de chunks");
        properties.put("limit", limitProp);

        Map<String, Object> filterProp = new HashMap<>();
        filterProp.put("type", "string");
        filterProp.put("description", "Filtro opcional");
        properties.put("filter", filterProp);

        schema.put("properties", properties);

        return schema;
    }

    private Object createStatusSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", new HashMap<>());
        schema.put("description", "Get server status - no parameters required");
        return schema;
    }

    // Los demás métodos existentes permanecen iguales
    public Map<String, Object> executeChunks(Map<String, Object> request) {
        // Código original sin cambios
        log.info("[PureMcpServer] Ejecutando obtención de chunks con request: {}", request);

        try {
            Map<String, Object> params = (Map<String, Object>) request.get("parameters");
            int limit = params.containsKey("limit") ? Integer.parseInt(params.get("limit").toString()) : 10;
            String afterId = params.containsKey("after_id") ? params.get("after_id").toString() : null;
            String filter = params.containsKey("filter") ? params.get("filter").toString() : null;
            List<String> tags = params.containsKey("tags") ? (List<String>) params.get("tags") : List.of();
            String sort = params.containsKey("sort") ? params.get("sort").toString() : null;

            log.info("[PureMcpServer] Obteniendo chunks con limit={}, afterId={}, filter={}", limit, afterId, filter);

            List<ChunkDto> chunks = aggregatedMovieService.getMovieChunks(limit, afterId, filter, tags, sort);
            log.info("[PureMcpServer] Obtenidos {} chunks", chunks.size());

            if (chunks.isEmpty()) {
                log.warn("[PureMcpServer] No se encontraron chunks para los parámetros dados");
            } else {
                log.info("[PureMcpServer] Primer chunk ID: {}", chunks.get(0).getId());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("chunks", chunks);
            return response;
        } catch (Exception e) {
            log.error("[PureMcpServer] Error al obtener chunks: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            return response;
        }
    }

    public Map<String, Object> execute(Map<String, Object> request) {
        // Código original sin cambios
        String function = request.containsKey("function") ? request.get("function").toString() : "";
        log.info("[PureMcpServer] Recibida solicitud de ejecución para función: '{}'", function);

        switch (function) {
            case "search":
                return executeSearch(request);
            case "chunks":
                return executeChunks(request);
            default:
                log.warn("[PureMcpServer] Función no reconocida: '{}'", function);
                Map<String, Object> response = new HashMap<>();
                response.put("error", "Función no soportada: " + function);
                return response;
        }
    }

    public McpDto.HealthStatus getHealth() {
        log.info("[PureMcpServer] Verificando estado de salud del servidor");
        
        Map<String, Object> details = new HashMap<>();
        details.put("tmdbService", "AVAILABLE");
        details.put("sqlService", "AVAILABLE");
        details.put("mongoService", "AVAILABLE");
        details.put("aggregatedService", "AVAILABLE");
        
        return McpDto.HealthStatus.builder()
                .status("OK")
                .details(details)
                .timestamp(Instant.now().toString())
                .build();
    }

    // New MCP Protocol Methods

    public McpDto.PingResult ping(McpDto.PingRequest request) {
        log.info("[PureMcpServer] Handling ping request");
        return McpDto.PingResult.builder()
                .status("OK")
                .timestamp(Instant.now().toString())
                .build();
    }

    public McpDto.PromptsListResult listPrompts(McpDto.PromptsListRequest request) {
        log.info("[PureMcpServer] Listing available prompts");
        
        List<McpDto.Prompt> prompts = Arrays.asList(
                McpDto.Prompt.builder()
                        .name("movie_search_prompt")
                        .description("Generate a search query for movies")
                        .arguments(createMovieSearchPromptSchema())
                        .build(),
                McpDto.Prompt.builder()
                        .name("movie_analysis_prompt")
                        .description("Analyze movie data and provide insights")
                        .arguments(createMovieAnalysisPromptSchema())
                        .build()
        );
        
        return McpDto.PromptsListResult.builder()
                .prompts(prompts)
                .nextCursor(null) // No pagination for now
                .build();
    }

    public McpDto.PromptsGetResult getPrompt(McpDto.PromptsGetRequest request) {
        log.info("[PureMcpServer] Getting prompt: {}", request.getName());
        
        switch (request.getName()) {
            case "movie_search_prompt":
                return McpDto.PromptsGetResult.builder()
                        .description("Generate a search query for movies")
                        .messages(List.of(
                                McpDto.PromptMessage.builder()
                                        .role("user")
                                        .content(McpDto.PromptContent.builder()
                                                .type("text")
                                                .text("Search for movies about: " + request.getArguments().get("topic"))
                                                .build())
                                        .build()
                        ))
                        .build();
                        
            case "movie_analysis_prompt":
                return McpDto.PromptsGetResult.builder()
                        .description("Analyze movie data and provide insights")
                        .messages(List.of(
                                McpDto.PromptMessage.builder()
                                        .role("system")
                                        .content(McpDto.PromptContent.builder()
                                                .type("text")
                                                .text("Analyze the following movie data and provide insights about trends, ratings, and patterns:")
                                                .build())
                                        .build()
                        ))
                        .build();
                        
            default:
                throw new IllegalArgumentException("Unknown prompt: " + request.getName());
        }
    }

    public McpDto.ResourceTemplatesListResult listResourceTemplates(McpDto.ResourceTemplatesListRequest request) {
        log.info("[PureMcpServer] Listing resource templates");
        
        List<McpDto.ResourceTemplate> templates = Arrays.asList(
                McpDto.ResourceTemplate.builder()
                        .uriTemplate("melian://movies/{source}")
                        .name("Movies by Source")
                        .description("Get movies from specific source (sql, mongo, tmdb)")
                        .mimeType("application/json")
                        .build(),
                McpDto.ResourceTemplate.builder()
                        .uriTemplate("melian://movies/search/{query}")
                        .name("Movie Search")
                        .description("Search movies by query")
                        .mimeType("application/json")
                        .build()
        );
        
        return McpDto.ResourceTemplatesListResult.builder()
                .resourceTemplates(templates)
                .nextCursor(null)
                .build();
    }

    public McpDto.ResourcesSubscribeResult subscribeToResource(McpDto.ResourcesSubscribeRequest request) {
        log.info("[PureMcpServer] Subscribing to resource: {}", request.getUri());
        // For now, just log the subscription - in a real implementation you'd store the subscription
        return McpDto.ResourcesSubscribeResult.builder().build();
    }

    public McpDto.SetLoggingLevelResult setLoggingLevel(McpDto.SetLoggingLevelRequest request) {
        log.info("[PureMcpServer] Setting logging level to: {}", request.getLevel());
        // In a real implementation, you'd update the logging configuration
        return McpDto.SetLoggingLevelResult.builder().build();
    }

    public McpDto.CompletionResult complete(McpDto.CompletionRequest request) {
        log.info("[PureMcpServer] Handling completion request for: {} {}", 
                request.getRef().getType(), request.getRef().getName());
        
        List<McpDto.CompletionOption> options = new ArrayList<>();
        
        if ("resource".equals(request.getRef().getType())) {
            options.addAll(Arrays.asList(
                    McpDto.CompletionOption.builder()
                            .value("melian://movies/sql")
                            .label("SQL Movies")
                            .description("Movies from SQL database")
                            .build(),
                    McpDto.CompletionOption.builder()
                            .value("melian://movies/mongo") 
                            .label("MongoDB Movies")
                            .description("Movies from MongoDB")
                            .build(),
                    McpDto.CompletionOption.builder()
                            .value("melian://movies/tmdb")
                            .label("TMDB Movies")
                            .description("Movies from TMDB API")
                            .build()
            ));
        } else if ("argument".equals(request.getRef().getType())) {
            // Provide argument completions based on the tool
            if ("search_movies".equals(request.getRef().getName())) {
                options.addAll(Arrays.asList(
                        McpDto.CompletionOption.builder()
                                .value("action")
                                .label("Action Movies")
                                .description("Search for action movies")
                                .build(),
                        McpDto.CompletionOption.builder()
                                .value("comedy")
                                .label("Comedy Movies")
                                .description("Search for comedy movies")
                                .build(),
                        McpDto.CompletionOption.builder()
                                .value("drama")
                                .label("Drama Movies")
                                .description("Search for drama movies")
                                .build()
                ));
            }
        }
        
        return McpDto.CompletionResult.builder()
                .completion(options)
                .build();
    }

    // Helper methods for prompt schemas
    private Object createMovieSearchPromptSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> topicProp = new HashMap<>();
        topicProp.put("type", "string");
        topicProp.put("description", "Topic to search movies about");
        properties.put("topic", topicProp);
        
        schema.put("properties", properties);
        schema.put("required", List.of("topic"));
        return schema;
    }

    private Object createMovieAnalysisPromptSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", new HashMap<>());
        schema.put("description", "No arguments required for movie analysis");
        return schema;
    }
}