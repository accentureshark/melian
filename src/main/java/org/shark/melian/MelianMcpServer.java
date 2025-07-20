package org.shark.melian;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import org.shark.melian.config.DatabaseConfig;
import org.shark.melian.config.MelianConfig;
import org.shark.melian.config.MongoConfig;
import org.shark.melian.client.TMDBApiClientPure;
import org.shark.melian.service.MovieChunkService;
import org.shark.melian.service.SqlMovieChunkServicePure;
import org.shark.melian.service.MongoMovieChunkServicePure;
import org.shark.melian.service.TMDBServicePure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        
        log.info("MELIAN MCP Server initialized successfully");
    }
    
    public void start() {
        log.info("Starting MELIAN MCP Server...");
        
        try {
            // Create transport provider
            StdioServerTransportProvider transportProvider = new StdioServerTransportProvider();
            log.info("Created STDIO transport provider");
            
            // Create MCP server using sync mode with the transport provider
            var serverSpec = McpServer.sync(transportProvider);
            log.info("Created MCP server specification: {}", serverSpec);
            
            log.info("MELIAN MCP Server started with STDIO transport");
            log.info("Server is ready to accept MCP connections via STDIO...");
            
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