package org.shark.melian.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * MCP (Model Context Protocol) standard DTOs for pure compliance.
 * These DTOs follow the official MCP specification.
 */
public class McpDto {

    @Data
    @Builder
    public static class JsonRpcRequest {
        private String jsonrpc = "2.0";
        private String method;
        private Object params;
        private Object id;
    }

    @Data
    @Builder
    public static class JsonRpcResponse {
        private String jsonrpc = "2.0";
        private Object result;
        private JsonRpcError error;
        private Object id;
    }

    @Data
    @Builder
    public static class JsonRpcError {
        private int code;
        private String message;
        private Object data;
    }

    @Data
    @Builder
    public static class InitializeRequest {
        private String protocolVersion;
        private ClientCapabilities capabilities;
        private ClientInfo clientInfo;
    }

    @Data
    @Builder
    public static class InitializeResult {
        private String protocolVersion = "2024-11-05";
        private ServerCapabilities capabilities;
        private ServerInfo serverInfo;
    }

    @Data
    @Builder
    public static class ClientInfo {
        private String name;
        private String version;
    }

    @Data
    @Builder
    public static class ServerInfo {
        private String name;
        private String version;
    }

    @Data
    @Builder
    public static class ClientCapabilities {
        private RootsCapability roots;
        private SamplingCapability sampling;
    }

    @Data
    @Builder
    public static class ServerCapabilities {
        private LoggingCapability logging;
        private PromptsCapability prompts;
        private ResourcesCapability resources;
        private ToolsCapability tools;
    }

    @Data
    @Builder
    public static class RootsCapability {
        @JsonProperty("listChanged")
        private boolean listChanged;
    }

    @Data
    @Builder
    public static class SamplingCapability {
        // Empty for now
    }

    @Data
    @Builder
    public static class LoggingCapability {
        // Empty for now
    }

    @Data
    @Builder
    public static class PromptsCapability {
        @JsonProperty("listChanged")
        private boolean listChanged;
    }

    @Data
    @Builder
    public static class ResourcesCapability {
        @JsonProperty("subscribe")
        private boolean subscribe;
        @JsonProperty("listChanged")
        private boolean listChanged;
    }

    @Data
    @Builder
    public static class ToolsCapability {
        @JsonProperty("listChanged")
        private boolean listChanged;
    }

    @Data
    @Builder
    public static class Tool {
        private String name;
        private String description;
        private Object inputSchema;
    }

    @Data
    @Builder
    public static class ToolsListResult {
        private List<Tool> tools;
    }

    @Data
    @Builder
    public static class CallToolRequest {
        private String name;
        private Map<String, Object> arguments;
    }

    @Data
    @Builder
    public static class CallToolResult {
        private List<ToolContent> content;
        @JsonProperty("isError")
        private boolean isError;
    }

    @Data
    @Builder
    public static class ToolContent {
        private String type;
        private String text;
        private Object data;
    }

    @Data
    @Builder
    public static class Resource {
        private String uri;
        private String name;
        private String description;
        private String mimeType;
    }

    @Data
    @Builder
    public static class ResourcesListResult {
        private List<Resource> resources;
    }

    @Data
    @Builder
    public static class ReadResourceRequest {
        private String uri;
    }

    @Data
    @Builder
    public static class ReadResourceResult {
        private List<ResourceContent> contents;
    }

    @Data
    @Builder
    public static class ResourceContent {
        private String uri;
        private String mimeType;
        private String text;
        private String blob;
    }

    @Data
    @Builder
    public static class HealthStatus {
        private String status;
        private Map<String, Object> details;
        private String timestamp;
    }
}