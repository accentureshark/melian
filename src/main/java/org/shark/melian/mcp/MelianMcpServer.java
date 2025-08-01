package org.shark.melian.mcp;

import org.shark.melian.client.TMDBApiClientPure;
import org.shark.melian.config.DatabaseConfig;
import org.shark.melian.config.MelianConfig;
import org.shark.melian.config.MongoConfig;
import org.shark.melian.mcp.transport.McpHttpTransport;
import org.shark.melian.mcp.transport.McpStdioTransport;
import org.shark.melian.service.MongoMovieChunkServicePure;
import org.shark.melian.service.SqlMovieChunkServicePure;
import org.shark.melian.service.TMDBServicePure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pure MCP Server implementation without OpenAI dependencies.
 * Follows the Model Context Protocol specification for movie data access.
 */
public class MelianMcpServer {

    private static final Logger log = LoggerFactory.getLogger(MelianMcpServer.class);

    private final MelianConfig config;
    private final DatabaseConfig databaseConfig;
    private final MongoConfig mongoConfig;
    private final TMDBApiClientPure tmdbClient;
    private final TMDBServicePure tmdbService;
    private final SqlMovieChunkServicePure sqlService;
    private final MongoMovieChunkServicePure mongoService;
    private final PureMcpServer mcpServer;
    
    private McpHttpTransport httpTransport;
    private McpStdioTransport stdioTransport;

    public MelianMcpServer() {
        log.info("Initializing MELIAN Pure MCP Server...");

        this.config = new MelianConfig();
        this.databaseConfig = new DatabaseConfig(config);
        this.mongoConfig = new MongoConfig(config);
        this.tmdbClient = new TMDBApiClientPure(config);
        this.tmdbService = new TMDBServicePure(tmdbClient);
        this.sqlService = new SqlMovieChunkServicePure(databaseConfig, tmdbService);
        this.mongoService = new MongoMovieChunkServicePure(mongoConfig, tmdbService);
        this.mcpServer = new PureMcpServer(tmdbService, sqlService, mongoService);

        log.info("MELIAN Pure MCP Server initialized successfully");
    }

    public void start() {
        log.info("Starting MELIAN MCP Server...");

        try {
            // Determine transport mode from environment or arguments
            boolean httpEnabled = isHttpTransportEnabled();
            boolean stdioEnabled = isStdioTransportEnabled();

            if (httpEnabled) {
                startHttpTransport();
            }

            if (stdioEnabled) {
                startStdioTransport();
            }

            if (!httpEnabled && !stdioEnabled) {
                // Default to stdio if nothing specified
                log.info("No transport specified, defaulting to stdio");
                startStdioTransport();
            }

            log.info("MELIAN MCP Server started successfully");

            // Keep the server running
            if (stdioEnabled) {
                // If stdio is enabled, the main thread will handle stdio communication
                waitForStdioCompletion();
            } else {
                // If only HTTP is enabled, keep the main thread alive
                waitForShutdown();
            }

        } catch (Exception e) {
            log.error("Failed to start MCP server", e);
            System.exit(1);
        }
    }

    private boolean isHttpTransportEnabled() {
        String httpEnabled = System.getenv("MCP_SERVER_HTTP_ENABLED");
        return "true".equalsIgnoreCase(httpEnabled) || 
               System.getProperty("mcp.http.enabled", "false").equals("true");
    }

    private boolean isStdioTransportEnabled() {
        String stdioEnabled = System.getenv("MCP_SERVER_STDIO_ENABLED");
        return "true".equalsIgnoreCase(stdioEnabled) || 
               System.getProperty("mcp.stdio.enabled", "true").equals("true");
    }

    private void startHttpTransport() throws Exception {
        String host = System.getenv("MCP_SERVER_HOST");
        if (host == null) {
            host = System.getProperty("mcp.http.host", "0.0.0.0");
        }

        String portStr = System.getenv("MCP_SERVER_PORT");
        if (portStr == null) {
            portStr = System.getProperty("mcp.http.port", "3000");
        }
        int port = Integer.parseInt(portStr);

        httpTransport = new McpHttpTransport(mcpServer, host, port);
        httpTransport.start();
    }

    private void startStdioTransport() {
        stdioTransport = new McpStdioTransport(mcpServer);
        stdioTransport.start();
    }

    private void waitForStdioCompletion() {
        // Wait for stdio transport to complete
        while (stdioTransport != null && stdioTransport.isRunning()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.info("Interrupted, shutting down...");
                break;
            }
        }
    }

    private void waitForShutdown() {
        // Add shutdown hook and wait
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        
        try {
            // Keep the main thread alive
            Object lock = new Object();
            synchronized (lock) {
                lock.wait();
            }
        } catch (InterruptedException e) {
            log.info("Shutting down...");
        }
    }

    public void shutdown() {
        log.info("Shutting down MELIAN MCP Server...");

        try {
            if (stdioTransport != null) {
                stdioTransport.stop();
            }
            if (httpTransport != null) {
                httpTransport.stop();
            }
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
        // Check for help flag
        for (String arg : args) {
            if ("--help".equals(arg) || "-h".equals(arg)) {
                printUsage();
                return;
            }
        }

        // Set pure mode to disable OpenAI features
        System.setProperty("mcp.pure.mode", "true");
        System.setProperty("disable.openai", "true");

        MelianMcpServer server = new MelianMcpServer();
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));

        try {
            server.start();
        } catch (Exception e) {
            log.error("Error in main execution", e);
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("MELIAN Pure MCP Server");
        System.out.println("======================");
        System.out.println();
        System.out.println("A Model Context Protocol server for movie data access.");
        System.out.println();
        System.out.println("Environment Variables:");
        System.out.println("  MCP_SERVER_HTTP_ENABLED    - Enable HTTP transport (true/false)");
        System.out.println("  MCP_SERVER_STDIO_ENABLED   - Enable Stdio transport (true/false)");
        System.out.println("  MCP_SERVER_HOST            - HTTP server host (default: 0.0.0.0)");
        System.out.println("  MCP_SERVER_PORT            - HTTP server port (default: 3000)");
        System.out.println("  TMDB_ACCESS_TOKEN          - TMDB API access token");
        System.out.println("  DB_URL                     - Database URL (optional)");
        System.out.println("  DB_USERNAME                - Database username (optional)");
        System.out.println("  DB_PASSWORD                - Database password (optional)");
        System.out.println("  MONGODB_URI                - MongoDB connection URI (optional)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # Start with stdio transport (default)");
        System.out.println("  java -jar melian-mcp-server.jar");
        System.out.println();
        System.out.println("  # Start with HTTP transport");
        System.out.println("  MCP_SERVER_HTTP_ENABLED=true java -jar melian-mcp-server.jar");
        System.out.println();
        System.out.println("  # Start with both transports");
        System.out.println("  MCP_SERVER_HTTP_ENABLED=true MCP_SERVER_STDIO_ENABLED=true java -jar melian-mcp-server.jar");
    }
}