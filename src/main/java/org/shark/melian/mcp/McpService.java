package org.shark.melian.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Minimal in-memory MCP service implementing core JSON-RPC methods.
 */
@Service
public class McpService {

    private boolean initialized = false;

    /**
     * Dispatch a JSON-RPC method call.
     *
     * @param method method name
     * @param params parameters as JsonNode (may be null)
     * @return result object
     * @throws NoSuchMethodException when the method is unknown
     */
    public Object dispatch(String method, JsonNode params) throws Exception {
        if (method == null) {
            throw new NoSuchMethodException("Method must be provided");
        }
        switch (method) {
            case "initialize":
                return initialize();
            case "ping":
                return ping();
            case "tools/list":
                return listTools();
            case "tools/call":
                return callTool(params);
            default:
                throw new NoSuchMethodException("Unknown method: " + method);
        }
    }

    private Map<String, Object> initialize() {
        initialized = true;
        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("tools", Map.of("listChanged", true));
        capabilities.put("resources", Map.of("listChanged", true));
        capabilities.put("prompts", Map.of());
        capabilities.put("logging", Map.of());
        capabilities.put("progress", true);

        Map<String, Object> serverInfo = Map.of(
                "name", "melian",
                "version", "0.1.0"
        );

        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", "2024-11-05");
        result.put("serverInfo", serverInfo);
        result.put("capabilities", capabilities);
        return result;
    }

    private Map<String, Object> ping() {
        if (!initialized) {
            throw new IllegalStateException("Server not initialized");
        }
        return Map.of("ok", true);
    }

    private Map<String, Object> listTools() {
        Map<String, Object> inputSchema = new HashMap<>();
        inputSchema.put("type", "object");
        inputSchema.put("properties", Map.of("question", Map.of("type", "string")));
        inputSchema.put("required", List.of("question"));

        Map<String, Object> tool = new HashMap<>();
        tool.put("name", "ask_data");
        tool.put("description", "Dummy tool that echoes a question");
        tool.put("inputSchema", inputSchema);

        return Map.of("tools", List.of(tool));
    }

    private Map<String, Object> callTool(JsonNode params) {
        if (params == null) {
            throw new IllegalArgumentException("params required");
        }
        String name = optionalText(params.get("name"));
        if (name == null) {
            throw new IllegalArgumentException("name is required");
        }
        if ("raise_error".equals(name)) {
            throw new RuntimeException("Simulated internal error");
        }
        JsonNode arguments = params.get("arguments");
        if (arguments == null) {
            throw new IllegalArgumentException("arguments are required");
        }
        if ("ask_data".equals(name)) {
            String question = optionalText(arguments.get("question"));
            if (question == null) {
                throw new IllegalArgumentException("question is required");
            }
            if (arguments.has("causeError") && arguments.get("causeError").asBoolean()) {
                throw new RuntimeException("Simulated internal error");
            }
            Map<String, Object> content = new HashMap<>();
            content.put("type", "text");
            content.put("text", "answer to " + question);
            return Map.of("content", List.of(content));
        }
        throw new IllegalArgumentException("Unknown tool: " + name);
    }

    private String optionalText(JsonNode node) {
        return node != null && !node.isNull() ? node.asText() : null;
    }

    public Map<String, Object> health() {
        return Map.of("status", "ok");
    }

    public Map<String, Object> listResources() {
        return Map.of("resources", Collections.emptyList());
    }
}

