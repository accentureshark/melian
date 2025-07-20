package org.shark.alma.mcp.service;

import org.shark.alma.mcp.model.McpServer;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing MCP server registrations
 */
@Service
@Slf4j
public class McpServerService {
    
    private final Map<String, McpServer> servers = new ConcurrentHashMap<>();
    
    public McpServerService() {
        // Initialize with some sample MCP servers
        initializeSampleServers();
    }
    
    private void initializeSampleServers() {
        addServer(McpServer.builder()
            .id("local-file-server")
            .name("Local File Server")
            .description("Provides access to local file system")
            .url("stdio:///usr/local/bin/mcp-file-server")
            .status("CONNECTED")
            .lastConnected(System.currentTimeMillis())
            .build());
            
        addServer(McpServer.builder()
            .id("web-search-server")
            .name("Web Search Server")
            .description("Provides web search capabilities")
            .url("stdio:///usr/local/bin/mcp-web-search")
            .status("DISCONNECTED")
            .lastConnected(System.currentTimeMillis() - 300000)
            .build());
            
        addServer(McpServer.builder()
            .id("database-server")
            .name("Database Server")
            .description("Provides database query capabilities")
            .url("stdio:///usr/local/bin/mcp-database")
            .status("CONNECTED")
            .lastConnected(System.currentTimeMillis())
            .build());
    }
    
    public List<McpServer> getAllServers() {
        return new ArrayList<>(servers.values());
    }
    
    public Optional<McpServer> getServer(String id) {
        return Optional.ofNullable(servers.get(id));
    }
    
    public McpServer addServer(McpServer server) {
        if (server.getId() == null) {
            server.setId(UUID.randomUUID().toString());
        }
        servers.put(server.getId(), server);
        log.info("Added MCP server: {}", server.getName());
        return server;
    }
    
    public boolean removeServer(String id) {
        McpServer removed = servers.remove(id);
        if (removed != null) {
            log.info("Removed MCP server: {}", removed.getName());
            return true;
        }
        return false;
    }
    
    public McpServer updateServerStatus(String id, String status) {
        McpServer server = servers.get(id);
        if (server != null) {
            server.setStatus(status);
            server.setLastConnected(System.currentTimeMillis());
            log.info("Updated server {} status to {}", server.getName(), status);
        }
        return server;
    }
}