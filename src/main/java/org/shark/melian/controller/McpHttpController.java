package org.shark.melian.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.shark.melian.service.ResourceService;
import org.shark.melian.service.ToolService;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class McpHttpController {

    private final ToolService toolService;
    private final ResourceService resourceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public McpHttpController(ToolService toolService, ResourceService resourceService) {
        this.toolService = toolService;
        this.resourceService = resourceService;
    }

    public String handleMcpRequest(String jsonRequest) {
        try {
            Map<String, Object> request = objectMapper.readValue(jsonRequest, Map.class);
            CompletableFuture<Object> future = processMcpRequest(request);
            Object response = future.get();
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            // Manejo de error JSON-RPC
            return errorResponse(-32603, "Internal error: " + e.getMessage(), null);
        }
    }

    public String health() {
        try {
            Map<String, Object> health = Map.of(
                    "status", "OK",
                    "server", "MELIAN MCP Server",
                    "transport", "HTTP/STDIO Bridge",
                    "timestamp", System.currentTimeMillis(),
                    "tools", toolService.listTools(),
                    "resources", resourceService.listResources()
            );
            return objectMapper.writeValueAsString(health);
        } catch (Exception e) {
            return errorResponse(-32603, "Health error: " + e.getMessage(), null);
        }
    }

    private CompletableFuture<Object> processMcpRequest(Map<String, Object> request) {
        String method = (String) request.get("method");
        switch (method) {
            case "initialize":
                return handleInitialize(request);
            case "tools/list":
                return handleToolsList(request);
            case "tools/call":
                return handleToolsCall(request);
            case "resources/list":
                return handleResourcesList(request);
            case "resources/read":
                return handleResourcesRead(request);
            default:
                return CompletableFuture.completedFuture(Map.of(
                        "jsonrpc", "2.0",
                        "error", Map.of(
                                "code", -32601,
                                "message", "Method not found: " + method
                        ),
                        "id", request.get("id")
                ));
        }
    }

    private CompletableFuture<Object> handleInitialize(Map<String, Object> request) {
        return CompletableFuture.completedFuture(Map.of(
                "jsonrpc", "2.0",
                "result", Map.of(
                        "protocolVersion", "2024-11-05",
                        "serverInfo", Map.of(
                                "name", "melian-movie-server",
                                "version", "1.0.0"
                        ),
                        "capabilities", Map.of(
                                "tools", toolService.capabilities(),
                                "resources", resourceService.capabilities(),
                                "notifications", Map.of()
                        )
                ),
                "id", request.get("id")
        ));
    }

    private CompletableFuture<Object> handleToolsList(Map<String, Object> request) {
        return CompletableFuture.completedFuture(Map.of(
                "jsonrpc", "2.0",
                "result", Map.of(
                        "tools", toolService.listTools()
                ),
                "id", request.get("id")
        ));
    }

    private CompletableFuture<Object> handleToolsCall(Map<String, Object> request) {
        Map<String, Object> params = (Map<String, Object>) request.get("params");
        String toolName = params != null ? (String) params.get("name") : null;
        Map<String, Object> arguments = params != null ? (Map<String, Object>) params.get("arguments") : null;

        try {
            Object result = toolService.callTool(toolName, arguments);
            return CompletableFuture.completedFuture(Map.of(
                    "jsonrpc", "2.0",
                    "result", result,
                    "id", request.get("id")
            ));
        } catch (Exception e) {
            return CompletableFuture.completedFuture(Map.of(
                    "jsonrpc", "2.0",
                    "error", Map.of(
                            "code", -32603,
                            "message", "Tool call failed: " + e.getMessage()
                    ),
                    "id", request.get("id")
            ));
        }
    }

    private CompletableFuture<Object> handleResourcesList(Map<String, Object> request) {
        return CompletableFuture.completedFuture(Map.of(
                "jsonrpc", "2.0",
                "result", Map.of(
                        "resources", resourceService.listResources()
                ),
                "id", request.get("id")
        ));
    }

    private CompletableFuture<Object> handleResourcesRead(Map<String, Object> request) {
        Map<String, Object> params = (Map<String, Object>) request.get("params");
        String uri = params != null ? (String) params.get("uri") : null;

        try {
            Object result = resourceService.readResource(uri);
            return CompletableFuture.completedFuture(Map.of(
                    "jsonrpc", "2.0",
                    "result", result,
                    "id", request.get("id")
            ));
        } catch (Exception e) {
            return CompletableFuture.completedFuture(Map.of(
                    "jsonrpc", "2.0",
                    "error", Map.of(
                            "code", -32603,
                            "message", "Resource read failed: " + e.getMessage()
                    ),
                    "id", request.get("id")
            ));
        }
    }

    private String errorResponse(int code, String message, Object id) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "jsonrpc", "2.0",
                    "error", Map.of(
                            "code", code,
                            "message", message
                    ),
                    "id", id
            ));
        } catch (Exception e) {
            return "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32603,\"message\":\"Critical error\"},\"id\":null}";
        }
    }
}