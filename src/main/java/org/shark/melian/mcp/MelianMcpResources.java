package org.shark.melian.mcp;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.shark.melian.service.MovieChunkService;
import org.shark.melian.service.TMDBServicePure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * MCP Resources implementation for MELIAN metadata and chunks.
 * Provides resources for movie metadata, database schema, and chunk data.
 */
public class MelianMcpResources {
    
    private static final Logger log = LoggerFactory.getLogger(MelianMcpResources.class);
    
    private final TMDBServicePure tmdbService;
    private final MovieChunkService sqlMovieService;
    private final MovieChunkService mongoMovieService;
    
    public MelianMcpResources(TMDBServicePure tmdbService, 
                             MovieChunkService sqlMovieService, 
                             MovieChunkService mongoMovieService) {
        this.tmdbService = tmdbService;
        this.sqlMovieService = sqlMovieService;
        this.mongoMovieService = mongoMovieService;
        log.info("MelianMcpResources initialized");
    }
    
    /**
     * Resource definition for movie metadata
     */
    public static McpSchema.Resource movieMetadataResourceDef() {
        return new McpSchema.Resource(
            "movies/metadata",
            "Movie database metadata and schema information",
            "Movie database metadata and schema information",
            "application/json",
            null
        );
    }
    
    /**
     * Handler for movie metadata resource
     */
    public McpSchema.ReadResourceResult readMovieMetadata(McpSyncServerExchange exchange, McpSchema.ReadResourceRequest request) {
        try {
            log.info("Reading movie metadata resource");
            
            StringBuilder metadata = new StringBuilder();
            metadata.append("{\n");
            metadata.append("  \"database\": {\n");
            metadata.append("    \"name\": \"melian_movies\",\n");
            metadata.append("    \"description\": \"MELIAN movie database with TMDB integration\",\n");
            metadata.append("    \"version\": \"1.0.0\",\n");
            metadata.append("    \"sources\": [\"TMDB API\", \"SQL Database\", \"MongoDB\"]\n");
            metadata.append("  },\n");
            metadata.append("  \"tables\": {\n");
            metadata.append("    \"movies\": {\n");
            metadata.append("      \"description\": \"Main movie information table\",\n");
            metadata.append("      \"columns\": {\n");
            metadata.append("        \"id\": {\"type\": \"string\", \"description\": \"Unique movie identifier\"},\n");
            metadata.append("        \"title\": {\"type\": \"string\", \"description\": \"Movie title\"},\n");
            metadata.append("        \"overview\": {\"type\": \"text\", \"description\": \"Movie plot summary\"},\n");
            metadata.append("        \"release_date\": {\"type\": \"date\", \"description\": \"Release date\"},\n");
            metadata.append("        \"vote_average\": {\"type\": \"float\", \"description\": \"Average user rating (0-10)\"},\n");
            metadata.append("        \"genre\": {\"type\": \"string\", \"description\": \"Primary movie genre\"},\n");
            metadata.append("        \"runtime\": {\"type\": \"integer\", \"description\": \"Runtime in minutes\"}\n");
            metadata.append("      }\n");
            metadata.append("    }\n");
            metadata.append("  },\n");
            metadata.append("  \"capabilities\": {\n");
            metadata.append("    \"search\": true,\n");
            metadata.append("    \"chunks\": true,\n");
            metadata.append("    \"embeddings\": false,\n");
            metadata.append("    \"real_time\": true\n");
            metadata.append("  },\n");
            metadata.append("  \"api_endpoints\": {\n");
            metadata.append("    \"search\": \"/mcp/tools/search_movies\",\n");
            metadata.append("    \"chunks\": \"/mcp/tools/get_movie_chunks\",\n");
            metadata.append("    \"status\": \"/mcp/tools/get_server_status\"\n");
            metadata.append("  }\n");
            metadata.append("}\n");
            
            log.info("Movie metadata resource read successfully");
            
            return new McpSchema.ReadResourceResult(
                List.of(new McpSchema.TextResourceContents(
                    "movies/metadata",
                    "application/json",
                    metadata.toString()
                ))
            );
            
        } catch (Exception e) {
            log.error("Error reading movie metadata resource", e);
            return new McpSchema.ReadResourceResult(
                List.of(new McpSchema.TextResourceContents(
                    "movies/metadata",
                    "text/plain",
                    "Error reading metadata: " + e.getMessage()
                ))
            );
        }
    }
    
    /**
     * Resource definition for movie chunks
     */
    public static McpSchema.Resource movieChunksResourceDef() {
        return new McpSchema.Resource(
            "movies/chunks",
            "Movie data chunks for RAG applications",
            "Movie data chunks for RAG applications",
            "text/plain",
            null
        );
    }
    
    /**
     * Handler for movie chunks resource
     */
    public McpSchema.ReadResourceResult readMovieChunks(McpSyncServerExchange exchange, McpSchema.ReadResourceRequest request) {
        try {
            log.info("Reading movie chunks resource");
            
            // Extract parameters from URI if provided
            String uri = request.uri();
            String source = "sql";  // default
            int limit = 5;          // default
            
            // Simple parameter parsing from URI query string
            if (uri != null && uri.contains("?")) {
                String query = uri.substring(uri.indexOf("?") + 1);
                String[] params = query.split("&");
                for (String param : params) {
                    String[] keyValue = param.split("=");
                    if (keyValue.length == 2) {
                        switch (keyValue[0]) {
                            case "source":
                                source = keyValue[1];
                                break;
                            case "limit":
                                try {
                                    limit = Integer.parseInt(keyValue[1]);
                                } catch (NumberFormatException e) {
                                    log.warn("Invalid limit parameter: {}", keyValue[1]);
                                }
                                break;
                        }
                    }
                }
            }
            
            // Get movie chunks using TMDB service as a proxy
            var movies = tmdbService.search("popular", limit);
            
            StringBuilder chunks = new StringBuilder();
            chunks.append("MOVIE DATA CHUNKS FOR RAG APPLICATIONS\n");
            chunks.append("=====================================\n");
            chunks.append("Source: ").append(source.toUpperCase()).append(" | Limit: ").append(limit).append("\n\n");
            
            for (int i = 0; i < movies.size(); i++) {
                var movie = movies.get(i);
                chunks.append("CHUNK ").append(i + 1).append(":\n");
                chunks.append("Title: ").append(movie.title()).append("\n");
                chunks.append("Release: ").append(movie.releaseDate() != null ? movie.releaseDate() : "Unknown").append("\n");
                chunks.append("Rating: ").append(movie.rating()).append("/10\n");
                chunks.append("Overview: ").append(movie.overview() != null ? movie.overview() : "No overview available").append("\n");
                chunks.append("---\n\n");
            }
            
            chunks.append("Total chunks: ").append(movies.size()).append("\n");
            chunks.append("Generated for RAG context and semantic search.\n");
            
            log.info("Movie chunks resource read successfully - {} chunks from {}", movies.size(), source);
            
            return new McpSchema.ReadResourceResult(
                List.of(new McpSchema.TextResourceContents(
                    "movies/chunks",
                    "text/plain",
                    chunks.toString()
                ))
            );
            
        } catch (Exception e) {
            log.error("Error reading movie chunks resource", e);
            return new McpSchema.ReadResourceResult(
                List.of(new McpSchema.TextResourceContents(
                    "movies/chunks",
                    "text/plain",
                    "Error reading chunks: " + e.getMessage()
                ))
            );
        }
    }
    
    /**
     * Resource definition for server documentation
     */
    public static McpSchema.Resource serverDocsResourceDef() {
        return new McpSchema.Resource(
            "server/docs",
            "MELIAN MCP Server documentation and usage guide",
            "MELIAN MCP Server documentation and usage guide",
            "text/markdown",
            null
        );
    }
    
    /**
     * Handler for server documentation resource
     */
    public McpSchema.ReadResourceResult readServerDocs(McpSyncServerExchange exchange, McpSchema.ReadResourceRequest request) {
        try {
            log.info("Reading server documentation resource");
            
            StringBuilder docs = new StringBuilder();
            docs.append("# MELIAN MCP Server Documentation\n\n");
            docs.append("## Overview\n");
            docs.append("MELIAN is a Model Context Protocol (MCP) compliant server that provides movie data access and search capabilities.\n\n");
            
            docs.append("## Available Tools\n\n");
            docs.append("### 1. search_movies\n");
            docs.append("Search for movies using TMDB API.\n");
            docs.append("**Parameters:**\n");
            docs.append("- `query` (required): Search term for movies\n");
            docs.append("- `limit` (optional): Maximum results (default: 10, max: 50)\n\n");
            
            docs.append("### 2. get_movie_chunks\n");
            docs.append("Retrieve movie data chunks for RAG applications.\n");
            docs.append("**Parameters:**\n");
            docs.append("- `source` (optional): 'sql' or 'mongo' (default: 'sql')\n");
            docs.append("- `limit` (optional): Maximum chunks (default: 10, max: 100)\n");
            docs.append("- `filter` (optional): Filter criteria\n\n");
            
            docs.append("### 3. get_server_status\n");
            docs.append("Get current server status and configuration.\n");
            docs.append("**Parameters:** None\n\n");
            
            docs.append("## Available Resources\n\n");
            docs.append("### 1. movies/metadata\n");
            docs.append("Database schema and metadata information.\n");
            docs.append("**Format:** JSON\n\n");
            
            docs.append("### 2. movies/chunks\n");
            docs.append("Movie data chunks for RAG context.\n");
            docs.append("**Format:** Plain text\n");
            docs.append("**Parameters:** ?source=sql|mongo&limit=N\n\n");
            
            docs.append("### 3. server/docs\n");
            docs.append("This documentation.\n");
            docs.append("**Format:** Markdown\n\n");
            
            docs.append("## Configuration\n");
            docs.append("The server can be configured using environment variables:\n");
            docs.append("- `TMDB_ACCESS_TOKEN`: TMDB API access token\n");
            docs.append("- `DB_URL`: Database connection URL\n");
            docs.append("- `MONGODB_URI`: MongoDB connection URI\n\n");
            
            docs.append("## Usage Examples\n");
            docs.append("```\n");
            docs.append("# Search for movies\n");
            docs.append("search_movies({\"query\": \"Matrix\", \"limit\": 5})\n\n");
            docs.append("# Get movie chunks\n");
            docs.append("get_movie_chunks({\"source\": \"sql\", \"limit\": 10})\n\n");
            docs.append("# Check server status\n");
            docs.append("get_server_status({})\n");
            docs.append("```\n");
            
            log.info("Server documentation resource read successfully");
            
            return new McpSchema.ReadResourceResult(
                List.of(new McpSchema.TextResourceContents(
                    "server/docs",
                    "text/markdown",
                    docs.toString()
                ))
            );
            
        } catch (Exception e) {
            log.error("Error reading server documentation resource", e);
            return new McpSchema.ReadResourceResult(
                List.of(new McpSchema.TextResourceContents(
                    "server/docs",
                    "text/plain",
                    "Error reading documentation: " + e.getMessage()
                ))
            );
        }
    }
}