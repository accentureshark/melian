package org.shark.melian.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import org.shark.melian.mcp.McpDto;
import org.shark.melian.mcp.PureMcpServer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring MVC controller exposing MCP endpoints.
 */
@RestController
@RequestMapping("/mcp")
public class McpController {

    private final PureMcpServer mcpServer;
    private final ObjectMapper objectMapper;

    public McpController(PureMcpServer mcpServer, ObjectMapper objectMapper) {
        this.mcpServer = mcpServer;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "Handle MCP JSON-RPC requests")
    @PostMapping
    public McpDto.JsonRpcResponse handle(@RequestBody McpDto.JsonRpcRequest request) throws Exception {
        Object result = handleMcpRequest(request);
        return McpDto.JsonRpcResponse.builder()
                .jsonrpc("2.0")
                .result(result)
                .id(request.getId())
                .build();
    }

    @PostMapping("/handle")
    @Operation(summary = "Maneja una solicitud MCP")
    public Object handleMcpRequest(@RequestBody McpDto.JsonRpcRequest request) {
        if (request == null || request.getMethod() == null) {
            throw new IllegalArgumentException("El método no puede ser null");
        }
        String method = request.getMethod();
        Object params = request.getParams();

        // Validar que los parámetros requeridos no sean null
        if ("tools/call".equals(method)) {
            Map<String, Object> args = (Map<String, Object>) params;
            if (args == null || args.get("name") == null) {
                throw new IllegalArgumentException("El nombre de la herramienta no puede ser null");
            }
        }

        switch (method) {
            case "initialize": {
                McpDto.InitializeRequest initReq = objectMapper.convertValue(params, McpDto.InitializeRequest.class);
                McpDto.InitializeResult result = mcpServer.initialize(initReq);
                Map<String, Object> response = new HashMap<>();
                response.put("serverInfo", result.getServerInfo());
                response.put("capabilities", result.getCapabilities());
                response.put("protocolVersion", result.getProtocolVersion());
                return response;
            }
            case "tools/list":
                return mcpServer.listTools();
            case "tools/call": {
                McpDto.CallToolRequest callReq = objectMapper.convertValue(params, McpDto.CallToolRequest.class);
                return mcpServer.callTool(callReq);
            }
            case "resources/list":
                return mcpServer.listResources();
            case "resources/read": {
                McpDto.ReadResourceRequest readReq = objectMapper.convertValue(params, McpDto.ReadResourceRequest.class);
                return mcpServer.readResource(readReq);
            }
            case "ping": {
                McpDto.PingRequest pingReq = objectMapper.convertValue(params, McpDto.PingRequest.class);
                return mcpServer.ping(pingReq);
            }
            case "prompts/list": {
                McpDto.PromptsListRequest promptsReq = objectMapper.convertValue(params, McpDto.PromptsListRequest.class);
                return mcpServer.listPrompts(promptsReq);
            }
            case "prompts/get": {
                McpDto.PromptsGetRequest promptReq = objectMapper.convertValue(params, McpDto.PromptsGetRequest.class);
                return mcpServer.getPrompt(promptReq);
            }
            case "resources/templates/list": {
                McpDto.ResourceTemplatesListRequest templatesReq = objectMapper.convertValue(params, McpDto.ResourceTemplatesListRequest.class);
                return mcpServer.listResourceTemplates(templatesReq);
            }
            case "resources/subscribe": {
                McpDto.ResourcesSubscribeRequest subscribeReq = objectMapper.convertValue(params, McpDto.ResourcesSubscribeRequest.class);
                return mcpServer.subscribeToResource(subscribeReq);
            }
            case "logging/setLevel": {
                McpDto.SetLoggingLevelRequest loggingReq = objectMapper.convertValue(params, McpDto.SetLoggingLevelRequest.class);
                return mcpServer.setLoggingLevel(loggingReq);
            }
            case "completion/complete": {
                McpDto.CompletionRequest completionReq = objectMapper.convertValue(params, McpDto.CompletionRequest.class);
                return mcpServer.complete(completionReq);
            }
            default:
                throw new IllegalArgumentException("Unknown method: " + method);
        }
    }

    @Operation(summary = "Health check")
    @GetMapping("/health")
    public McpDto.HealthStatus health() {
        return mcpServer.getHealth();
    }

    @Operation(summary = "List available tools")
    @GetMapping("/tools")
    public McpDto.ToolsListResult tools() {
        return mcpServer.listTools();
    }

    @Operation(summary = "List or read resources")
    @GetMapping("/resources")
    public Object resources(@RequestParam(value = "uri", required = false) String uri) {
        if (uri != null) {
            return mcpServer.readResource(McpDto.ReadResourceRequest.builder().uri(uri).build());
        }
        return mcpServer.listResources();
    }
}