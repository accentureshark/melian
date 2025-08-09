package org.shark.melian.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import lombok.Value;
import org.shark.melian.mcp.McpDto;
import org.shark.melian.mcp.McpService;
import org.shark.melian.mcp.PureMcpServer;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring MVC controller exposing MCP endpoints.
 */
@RestController
@RequestMapping("/mcp")

public class McpController {

    private final McpService mcpService;

    private boolean helpersEnabled;

    public McpController(McpService mcpService) {
        this.mcpService = mcpService;
    }

    @Operation(summary = "Handle MCP JSON-RPC requests")
    @PostMapping
    public McpDto.JsonRpcResponse handle(@RequestBody JsonNode request) {
        Object id = extractId(request.get("id"));
        McpDto.JsonRpcResponse.JsonRpcResponseBuilder builder = McpDto.JsonRpcResponse.builder()
                .jsonrpc("2.0")
                .id(id);

        String jsonrpc = request.path("jsonrpc").asText(null);
        if (!"2.0".equals(jsonrpc)) {
            builder.error(McpDto.JsonRpcError.builder().code(-32600).message("Invalid JSON-RPC version").build());
            return builder.build();
        }

        String method = request.path("method").asText(null);
        JsonNode params = request.get("params");

        try {
            Object result = mcpService.dispatch(method, params);
            builder.result(result);
        } catch (NoSuchMethodException e) {
            builder.error(McpDto.JsonRpcError.builder().code(-32601).message(e.getMessage()).build());
        } catch (IllegalArgumentException e) {
            builder.error(McpDto.JsonRpcError.builder().code(-32602).message(e.getMessage()).build());
        } catch (Exception e) {
            builder.error(McpDto.JsonRpcError.builder().code(-32603).message(e.getMessage()).build());
        }

        return builder.build();
    }

    private Object extractId(JsonNode idNode) {
        if (idNode == null || idNode.isNull()) {
            return null;
        }
        if (idNode.isIntegralNumber()) {
            return idNode.longValue();
        }
        if (idNode.isNumber()) {
            return idNode.numberValue();
        }
        return idNode.asText();
    }

    @Operation(summary = "Health check")
    @GetMapping("/health")
    public Map<String, Object> health() {
        return mcpService.health();
    }

    @Operation(summary = "List available tools (non-standard helper)")
    @GetMapping("/tools")
    public Object tools() throws Exception {
        if (!helpersEnabled) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return mcpService.dispatch("tools/list", null);
    }

    @Operation(summary = "List or read resources (non-standard helper)")
    @GetMapping("/resources")
    public Object resources(@RequestParam(value = "uri", required = false) String uri) {
        if (!helpersEnabled) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (uri != null) {
            // No resource storage yet
            return mcpService.listResources();
        }
        return mcpService.listResources();
    }
}