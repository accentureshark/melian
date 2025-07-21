package org.shark.melian;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.modelcontextprotocol.spec.McpSchema;
import org.shark.melian.client.TMDBApiClientPure;
import org.shark.melian.config.DatabaseConfig;
import org.shark.melian.config.MelianConfig;
import org.shark.melian.config.MongoConfig;
import org.shark.melian.controller.McpHttpController;
import org.shark.melian.mcp.MelianMcpResources;
import org.shark.melian.mcp.MelianMcpTools;
import org.shark.melian.service.MongoMovieChunkServicePure;
import org.shark.melian.service.MovieChunkService;
import org.shark.melian.service.SqlMovieChunkServicePure;
import org.shark.melian.service.TMDBServicePure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;

public class MelianMcpServer {

    private static final Logger log = LoggerFactory.getLogger(MelianMcpServer.class);

    private final MelianConfig config;
    private final DatabaseConfig databaseConfig;
    private final MongoConfig mongoConfig;
    private final TMDBApiClientPure tmdbClient;
    private final TMDBServicePure tmdbService;
    private final MovieChunkService sqlMovieService;
    private final MovieChunkService mongoMovieService;
    private final MelianMcpTools mcpTools;
    private final MelianMcpResources mcpResources;

    public MelianMcpServer() {
        log.info("Initializing MELIAN MCP Server...");

        this.config = new MelianConfig();
        this.databaseConfig = new DatabaseConfig(config);
        this.mongoConfig = new MongoConfig(config);
        this.tmdbClient = new TMDBApiClientPure(config);
        this.tmdbService = new TMDBServicePure(tmdbClient);
        this.sqlMovieService = new SqlMovieChunkServicePure(databaseConfig, tmdbService);
        this.mongoMovieService = new MongoMovieChunkServicePure(mongoConfig, tmdbService);
        this.mcpTools = new MelianMcpTools();
        this.mcpResources = new MelianMcpResources(tmdbService, sqlMovieService, mongoMovieService);

        // --- Definición y registro de search_movies ---
        McpSchema.Tool searchMoviesTool = new McpSchema.Tool(
                "search_movies",
                "Buscar películas usando TMDB API",
                "Busca películas por título usando TMDB. Parámetros: query (string, requerido), limit (int, opcional, default 10, max 50)",
                Map.of(
                        "query", Map.of("type", "string", "description", "Término de búsqueda"),
                        "limit", Map.of("type", "integer", "description", "Máximo de resultados")
                ),
                Map.of(
                        "results", Map.of("type", "array", "description", "Lista de películas")
                )
        );
        mcpTools.registerTool(searchMoviesTool, (exchange, args) -> {
            String query = (String) args.get("query");
            int limit = args.get("limit") != null ? ((Number) args.get("limit")).intValue() : 10;
            var results = tmdbService.search(query, limit);
            return Map.of("results", results);
        });

        // --- Definición y registro de get_movie_chunks ---
        McpSchema.Tool getMovieChunksTool = new McpSchema.Tool(
                "get_movie_chunks",
                "Obtener chunks de películas para RAG",
                "Devuelve chunks de datos de películas. Parámetros: source (sql|mongo), limit (int), filter (string, opcional)",
                Map.of(
                        "source", Map.of("type", "string", "description", "Fuente: sql o mongo"),
                        "limit", Map.of("type", "integer", "description", "Máximo de chunks"),
                        "filter", Map.of("type", "string", "description", "Filtro opcional")
                ),
                Map.of(
                        "chunks", Map.of("type", "array", "description", "Chunks de películas")
                )
        );
        mcpTools.registerTool(getMovieChunksTool, (exchange, args) -> {
            String source = (String) args.getOrDefault("source", "sql");
            int limit = args.get("limit") != null ? ((Number) args.get("limit")).intValue() : 10;
            String filter = (String) args.getOrDefault("filter", null);
            var service = "mongo".equalsIgnoreCase(source) ? mongoMovieService : sqlMovieService;
            var chunks = service.getMovieChunks(source, limit, null, filter, null, null);
            return Map.of("chunks", chunks);
        });

        // --- Definición y registro de get_server_status ---
        McpSchema.Tool getServerStatusTool = new McpSchema.Tool(
                "get_server_status",
                "Obtener estado del servidor",
                "Devuelve informaci��n de estado y configuración del servidor.",
                Map.of(),
                Map.of(
                        "status", Map.of("type", "string", "description", "Estado"),
                        "timestamp", Map.of("type", "integer", "description", "Marca de tiempo")
                )
        );
        mcpTools.registerTool(getServerStatusTool, (exchange, args) -> {
            return Map.of(
                    "status", "OK",
                    "timestamp", System.currentTimeMillis()
            );
        });

        log.info("MELIAN MCP Server initialized successfully");
    }

    public static void main(String[] args) {
        MelianMcpServer server = new MelianMcpServer();
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        server.start();
    }

    private void startHttpServer() throws IOException {
        int port = config.getIntProperty("mcp.server.port", 3000);
        String host = config.getProperty("mcp.server.host", "0.0.0.0");

        McpHttpController httpController = new McpHttpController(mcpTools, mcpResources);

        HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);

        server.createContext("/mcp", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    InputStream is = exchange.getRequestBody();
                    String request = new String(is.readAllBytes());
                    String response = httpController.handleMcpRequest(request);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            }
        });

        server.createContext("/mcp/health", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    String response = httpController.health();
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            }
        });

        server.setExecutor(null);
        server.start();
        log.info("HTTP MCP server listening on {}:{}", host, port);
    }

    public void shutdown() {
        log.info("Shutting down MELIAN MCP Server...");

        try {
            if (tmdbClient != null) {
                tmdbClient.close();
            }
            if (databaseConfig != null) {
                databaseConfig.close();
            }
            if (mongoConfig != null) {
                mongoConfig.close();
            }
        } catch (Exception e) {
            log.warn("Error during shutdown", e);
        }

        log.info("MELIAN MCP Server shutdown complete");
    }
}