package org.shark.melian;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.shark.melian.config.DatabaseConfig;
import org.shark.melian.config.MelianConfig;
import org.shark.melian.config.MongoConfig;
import org.shark.melian.client.TMDBApiClientPure;
import org.shark.melian.mcp.MelianMcpTools;
import org.shark.melian.mcp.MelianMcpResources;
import org.shark.melian.service.MovieChunkService;
import org.shark.melian.service.SqlMovieChunkServicePure;
import org.shark.melian.service.MongoMovieChunkServicePure;
import org.shark.melian.service.TMDBServicePure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * MELIAN MCP Server - Pure Java implementation using official MCP SDK.
 * Provides movie data access through MCP-compliant protocol.
 */
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
        
        // Initialize configuration
        this.config = new MelianConfig();
        
        // Initialize database connections
        this.databaseConfig = new DatabaseConfig(config);
        this.mongoConfig = new MongoConfig(config);
        
        // Initialize TMDB client and service
        this.tmdbClient = new TMDBApiClientPure(config);
        this.tmdbService = new TMDBServicePure(tmdbClient);
        
        // Initialize movie services
        this.sqlMovieService = new SqlMovieChunkServicePure(databaseConfig, tmdbService);
        this.mongoMovieService = new MongoMovieChunkServicePure(mongoConfig, tmdbService);
        
        // Initialize MCP tools and resources
        this.mcpTools = new MelianMcpTools(tmdbService, sqlMovieService, mongoMovieService);
        this.mcpResources = new MelianMcpResources(tmdbService, sqlMovieService, mongoMovieService);
        
        log.info("MELIAN MCP Server initialized successfully");
    }
    
    public void start() {
        log.info("Starting MELIAN MCP Server...");
        
        try {
            // Create transport provider
            StdioServerTransportProvider transportProvider = new StdioServerTransportProvider();
            log.info("Created STDIO transport provider");
            
            // Create server info
            McpSchema.Implementation serverInfo = new McpSchema.Implementation(
                "melian-movie-server",
                "0.1.0-SNAPSHOT"
            );
            
            // Create MCP server with tools and resources
            var mcpServer = McpServer.sync(transportProvider)
                .serverInfo(serverInfo)
                .instructions("MELIAN MCP Server provides movie search and data access capabilities using TMDB API, SQL, and MongoDB backends.")
                
                // Register tools
                .tool(MelianMcpTools.searchMoviesToolDef(), mcpTools::searchMovies)
                .tool(MelianMcpTools.getMovieChunksToolDef(), mcpTools::getMovieChunks)
                .tool(MelianMcpTools.getServerStatusToolDef(), mcpTools::getServerStatus)
                
                // Register resources  
                .resources(
                    new McpServerFeatures.SyncResourceSpecification(
                        MelianMcpResources.movieMetadataResourceDef(),
                        mcpResources::readMovieMetadata
                    ),
                    new McpServerFeatures.SyncResourceSpecification(
                        MelianMcpResources.movieChunksResourceDef(),
                        mcpResources::readMovieChunks
                    ),
                    new McpServerFeatures.SyncResourceSpecification(
                        MelianMcpResources.serverDocsResourceDef(),
                        mcpResources::readServerDocs
                    )
                )
                
                .build();
            
            log.info("Created MCP server with {} tools and {} resources", 3, 3);
            log.info("MELIAN MCP Server started with STDIO transport");
            log.info("Server is ready to accept MCP connections via STDIO...");
            log.info("Available tools: search_movies, get_movie_chunks, get_server_status");
            log.info("Available resources: movies/metadata, movies/chunks, server/docs");
            
            // Keep the server running
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.info("Server interrupted, shutting down...");
                    break;
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to start MELIAN MCP Server", e);
            throw new RuntimeException("Server startup failed", e);
        }
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
    
    public static void main(String[] args) {
        MelianMcpServer server = new MelianMcpServer();
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        
        // Start the server
        server.start();
    }
}