package org.shark.melian;

import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerBuilder;
import io.modelcontextprotocol.server.transport.McpSyncServerTransport;
import io.modelcontextprotocol.server.transport.http.servlet.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.server.transport.stdio.StdioTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.shark.melian.client.TMDBApiClientPure;
import org.shark.melian.config.DatabaseConfig;
import org.shark.melian.config.MelianConfig;
import org.shark.melian.config.MongoConfig;
import org.shark.melian.mcp.MelianMcpResources;
import org.shark.melian.mcp.MelianMcpTools;
import org.shark.melian.service.MongoMovieChunkServicePure;
import org.shark.melian.service.MovieChunkService;
import org.shark.melian.service.SqlMovieChunkServicePure;
import org.shark.melian.service.TMDBServicePure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MelianMcpServer {

    private static final Logger log = LoggerFactory.getLogger(MelianMcpServer.class);

    private final MelianConfig config;
    private final DatabaseConfig databaseConfig;
    private final MongoConfig mongoConfig;
    private Server httpServer;
    private final TMDBApiClientPure tmdbClient;
    private final TMDBServicePure tmdbService;
    private final MovieChunkService sqlMovieService;
    private final MovieChunkService mongoMovieService;
    private final MelianMcpTools mcpTools;
    private final MelianMcpResources mcpResources;
    private McpSyncServer mcpServer;

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
                """
                {
                  "type": "object",
                  "properties": {
                    "query": { "type": "string", "description": "Título a buscar", "minLength": 1 },
                    "limit": { "type": "integer", "description": "Cantidad máxima de resultados", "minimum": 1, "maximum": 50, "default": 10 }
                  },
                  "required": ["query"]
                }
                """
        );
        mcpTools.registerTool(searchMoviesTool, (exchange, args) -> {
            String query = (String) args.get("query");
            int limit = args.get("limit") != null ? ((Number) args.get("limit")).intValue() : 10;
            var results = tmdbService.search(query, limit);
            return java.util.Map.of("results", results);
        });

        // --- Definición y registro de get_movie_chunks ---
        McpSchema.Tool getMovieChunksTool = new McpSchema.Tool(
                "get_movie_chunks",
                "Obtener chunks de películas para RAG",
                """
                {
                  "type": "object",
                  "properties": {
                    "source": {
                      "type": "string",
                      "description": "Fuente de datos: sql o mongo",
                      "enum": ["sql", "mongo"],
                      "default": "sql"
                    },
                    "limit": {
                      "type": "integer",
                      "description": "Cantidad máxima de chunks",
                      "minimum": 1,
                      "maximum": 100,
                      "default": 10
                    },
                    "filter": {
                      "type": "string",
                      "description": "Filtro opcional para los chunks"
                    }
                  },
                  "required": ["source", "limit"]
                }
                """
        );
        mcpTools.registerTool(getMovieChunksTool, (exchange, args) -> {
            String source = (String) args.getOrDefault("source", "sql");
            int limit = args.get("limit") != null ? ((Number) args.get("limit")).intValue() : 10;
            String filter = (String) args.getOrDefault("filter", null);
            var service = "mongo".equalsIgnoreCase(source) ? mongoMovieService : sqlMovieService;
            var chunks = service.getMovieChunks(source, limit, null, filter, null, null);
            return java.util.Map.of("chunks", chunks);
        });

        // --- Definición y registro de get_server_status ---
   McpSchema.Tool getServerStatusTool = new McpSchema.Tool(
       "get_server_status",
       "Obtener estado del servidor",
       """
       {
         "type": "object",
         "properties": {}
       }
       """
   );
        mcpTools.registerTool(getServerStatusTool, (exchange, args) -> {
            return java.util.Map.of(
                    "status", "OK",
                    "timestamp", System.currentTimeMillis()
            );
        });

        log.info("MELIAN MCP Server initialized successfully");
        this.mcpServer = buildMcpServer();
    }

    private McpSyncServer buildMcpServer() {
        McpSyncServerBuilder builder = McpSyncServer.builder();
        for (var tool : mcpTools.getAllToolDefinitions()) {
            builder.addTool(tool, (ex, args) -> mcpTools.callTool(tool.name(), args));
        }
        builder.addResource(MelianMcpResources.movieMetadataResourceDef(), mcpResources::readMovieMetadata);
        builder.addResource(MelianMcpResources.movieChunksResourceDef(), mcpResources::readMovieChunks);
        builder.addResource(MelianMcpResources.serverDocsResourceDef(), mcpResources::readServerDocs);
        return builder.build();
    }

    public static void main(String[] args) {
        MelianMcpServer server = new MelianMcpServer();
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        try {
            if (server.getConfig().getBooleanProperty("mcp.server.http.enabled", false)) {
                server.startHttpServer();
            } else {
                server.startStdioServer();
            }
        } catch (Exception e) {
            log.error("Error starting MCP server", e);
        }
    }

    private void startStdioServer() throws IOException {
        McpSyncServerTransport transport = new StdioTransport();
        mcpServer.start(transport);
        log.info("MCP server started on STDIO transport");
    }

    private void startHttpServer() throws Exception {
        int port = config.getIntProperty("mcp.server.port", 3000);
        String host = config.getProperty("mcp.server.host", "0.0.0.0");

        ObjectMapper mapper = new ObjectMapper();
        HttpServletSseServerTransportProvider transport =
                new HttpServletSseServerTransportProvider(mapper, "/mcp/message");

        httpServer = new Server(new InetSocketAddress(host, port));
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.addServlet(new ServletHolder(transport), "/mcp/message");
        httpServer.setHandler(context);

        mcpServer.start(transport);
        httpServer.start();
        log.info("MCP server started on HTTP SSE transport at {}:{}", host, port);
        httpServer.join();
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
            if (mcpServer != null) {
                mcpServer.stop();
            }
            if (httpServer != null) {
                httpServer.stop();
            }
        } catch (Exception e) {
            log.warn("Error during shutdown", e);
        }

        log.info("MELIAN MCP Server shutdown complete");
    }

    public McpSyncServer getMcpServer() {
        return mcpServer;
    }

    public MelianConfig getConfig() {
        return config;
    }
}