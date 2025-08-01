package org.shark.melian;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;

import org.shark.melian.client.TMDBApiClientPure;
import org.shark.melian.config.DatabaseConfig;
import org.shark.melian.config.MelianConfig;
import org.shark.melian.config.MongoConfig;
import org.shark.melian.service.*;
import org.shark.melian.tools.MovieTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * MELIAN AI Assistant using LangChain4j.
 * Provides movie search and data access capabilities with optional external MCP server integration.
 */
public class MelianAiAssistant {

    private static final Logger log = LoggerFactory.getLogger(MelianAiAssistant.class);

    private final MelianConfig config;
    private final DatabaseConfig databaseConfig;
    private final MongoConfig mongoConfig;
    private final TMDBApiClientPure tmdbClient;
    private final TMDBServicePure tmdbService;
    private final MovieChunkService sqlMovieService;
    private final MovieChunkService mongoMovieService;
    private final MovieTools movieTools;
    private final List<McpClient> mcpClients = new ArrayList<>();
    private MovieBot movieBot;

    public interface MovieBot {
        String chat(String prompt);
    }

    public MelianAiAssistant() {
        log.info("Initializing MELIAN AI Assistant...");

        this.config = new MelianConfig();
        this.databaseConfig = new DatabaseConfig(config);
        this.mongoConfig = new MongoConfig(config);
        this.tmdbClient = new TMDBApiClientPure(config);
        this.tmdbService = new TMDBServicePure(tmdbClient);
        this.sqlMovieService = new SqlMovieChunkServicePure(databaseConfig, tmdbService);
        this.mongoMovieService = new MongoMovieChunkServicePure(mongoConfig, tmdbService);
        this.movieTools = new MovieTools(tmdbService, sqlMovieService, mongoMovieService);

        initializeAiServices();
        log.info("MELIAN AI Assistant initialized successfully");
    }

    private void initializeAiServices() {
        log.info("Initializing AI services...");

        // Initialize ChatModel - check for OpenAI API key
        ChatModel chatModel = createChatModel();
        if (chatModel == null) {
            log.warn("No ChatModel available - will run in tool-only mode");
            return;
        }

        // Initialize external MCP clients if configured
        List<McpClient> externalClients = initializeExternalMcpClients();

        // Create ToolProvider combining movie tools and external MCP tools
        ToolProvider toolProvider;
        if (!externalClients.isEmpty()) {
            // Combine movie tools with external MCP tools
            ToolProvider mcpToolProvider = McpToolProvider.builder()
                    .mcpClients(externalClients)
                    .build();
            toolProvider = mcpToolProvider; // For now, use MCP tools only
            log.info("Using external MCP tools: {} clients", externalClients.size());
        } else {
            log.info("No external MCP clients configured - using movie tools only");
            toolProvider = null; // Will use @Tool annotations directly
        }

        // Build AI service
        var builder = AiServices.builder(MovieBot.class)
                .chatModel(chatModel)
                .tools(movieTools);

        if (toolProvider != null) {
            builder.toolProvider(toolProvider);
        }

        this.movieBot = builder.build();
        log.info("AI services initialized successfully");
    }

    private ChatModel createChatModel() {
        String openAiKey = System.getenv("OPENAI_API_KEY");
        if (openAiKey == null || openAiKey.trim().isEmpty()) {
            log.warn("OPENAI_API_KEY not set - ChatModel will not be available");
            return null;
        }

        return OpenAiChatModel.builder()
                .apiKey(openAiKey)
                .modelName("gpt-4o-mini")
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    private List<McpClient> initializeExternalMcpClients() {
        List<McpClient> clients = new ArrayList<>();

        // Example: file system MCP server (if npm available)
        String enableFilesystem = System.getenv("ENABLE_FILESYSTEM_MCP");
        if ("true".equalsIgnoreCase(enableFilesystem)) {
            try {
                McpTransport fsTransport = new StdioMcpTransport.Builder()
                        .command(List.of("/usr/bin/npm", "exec", 
                                "@modelcontextprotocol/server-filesystem@0.6.2",
                                "/tmp"))
                        .logEvents(true)
                        .build();

                McpClient fsClient = new DefaultMcpClient.Builder()
                        .transport(fsTransport)
                        .build();

                clients.add(fsClient);
                mcpClients.add(fsClient);
                log.info("Initialized filesystem MCP client");
            } catch (Exception e) {
                log.warn("Failed to initialize filesystem MCP client: {}", e.getMessage());
            }
        }

        // Add more external MCP clients here as needed

        return clients;
    }

    public void startInteractiveMode() {
        if (movieBot == null) {
            startToolOnlyMode();
            return;
        }

        log.info("Starting MELIAN AI Assistant in interactive mode...");
        log.info("Available commands:");
        log.info("  - Chat with the AI about movies");
        log.info("  - Type 'quit' or 'exit' to stop");
        log.info("  - Type 'help' for assistance");
        System.out.println("\n🎬 MELIAN AI Assistant ready! Ask me about movies or type 'help' for commands.");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("\n> ");
                String input = scanner.nextLine().trim();

                if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
                    break;
                }

                if (input.equalsIgnoreCase("help")) {
                    showHelp();
                    continue;
                }

                if (input.isEmpty()) {
                    continue;
                }

                try {
                    String response = movieBot.chat(input);
                    System.out.println("\n🤖 " + response);
                } catch (Exception e) {
                    log.error("Error processing chat request", e);
                    System.out.println("\n❌ Error: " + e.getMessage());
                }
            }
        }

        log.info("Interactive mode ended");
    }

    private void startToolOnlyMode() {
        log.info("Starting MELIAN AI Assistant in tool-only mode...");
        System.out.println("\n🔧 MELIAN Tool Mode - No ChatModel available");
        System.out.println("Available commands:");
        System.out.println("  search <query> [limit] - Search movies");
        System.out.println("  chunks <source> [limit] [filter] - Get movie chunks");
        System.out.println("  status - Get server status");
        System.out.println("  quit/exit - Stop");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("\n> ");
                String input = scanner.nextLine().trim();

                if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
                    break;
                }

                try {
                    processToolCommand(input);
                } catch (Exception e) {
                    log.error("Error processing tool command", e);
                    System.out.println("❌ Error: " + e.getMessage());
                }
            }
        }
    }

    private void processToolCommand(String input) {
        String[] parts = input.split("\\s+");
        if (parts.length == 0) return;

        String command = parts[0].toLowerCase();
        switch (command) {
            case "search":
                if (parts.length < 2) {
                    System.out.println("Usage: search <query> [limit]");
                    return;
                }
                String query = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
                int limit = 10;
                if (parts.length > 2) {
                    try {
                        limit = Integer.parseInt(parts[parts.length - 1]);
                        query = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length - 1));
                    } catch (NumberFormatException e) {
                        // Keep query as is, use default limit
                    }
                }
                var results = movieTools.searchMovies(query, limit);
                System.out.println("Found " + results.size() + " movies:");
                results.forEach(movie -> System.out.println("  " + movie.title() + " (" + movie.releaseDate() + ") - " + movie.rating() + "/10"));
                break;

            case "chunks":
                String source = parts.length > 1 ? parts[1] : "sql";
                int chunkLimit = parts.length > 2 ? Integer.parseInt(parts[2]) : 10;
                String filter = parts.length > 3 ? parts[3] : null;
                var chunks = movieTools.getMovieChunks(source, chunkLimit, filter);
                System.out.println("Retrieved " + chunks.size() + " chunks from " + source);
                break;

            case "status":
                var status = movieTools.getServerStatus();
                System.out.println("Server status: " + status);
                break;

            default:
                System.out.println("Unknown command: " + command);
        }
    }

    private void showHelp() {
        System.out.println("\n🎬 MELIAN AI Assistant Help");
        System.out.println("==========================");
        System.out.println("I can help you with movie-related queries!");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  'Search for Matrix movies'");
        System.out.println("  'Find comedies from 2020'");
        System.out.println("  'Get movie chunks about action films'");
        System.out.println("  'What's the status of the server?'");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  help - Show this help");
        System.out.println("  quit/exit - Stop the assistant");
    }

    public void shutdown() {
        log.info("Shutting down MELIAN AI Assistant...");

        try {
            // Close MCP clients
            for (McpClient client : mcpClients) {
                try {
                    client.close();
                } catch (Exception e) {
                    log.warn("Error closing MCP client", e);
                }
            }

            // Close other resources
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

        log.info("MELIAN AI Assistant shutdown complete");
    }

    public static void main(String[] args) {
        MelianAiAssistant assistant = new MelianAiAssistant();
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(assistant::shutdown));

        try {
            assistant.startInteractiveMode();
        } catch (Exception e) {
            log.error("Error in main execution", e);
        } finally {
            assistant.shutdown();
        }
    }
}