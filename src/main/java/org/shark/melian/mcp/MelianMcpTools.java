package org.shark.melian.mcp;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.shark.melian.service.ToolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Implementación abstracta y extensible de ToolService para MCP.
 * Permite registrar herramientas y handlers dinámicamente.
 */
public class MelianMcpTools implements ToolService {

    private static final Logger log = LoggerFactory.getLogger(MelianMcpTools.class);

    // Registro de definiciones y handlers de herramientas
    private final Map<String, McpSchema.Tool> toolDefs = new LinkedHashMap<>();
    private final Map<String, ToolHandler> toolHandlers = new HashMap<>();

    /**
     * Constructor vacío para permitir registro dinámico.
     */
    public MelianMcpTools() {
        log.info("MelianMcpTools initialized (abstract)");
    }

    /**
     * Registra una herramienta MCP y su handler.
     */
    public void registerTool(McpSchema.Tool def, ToolHandler handler) {
        Objects.requireNonNull(def, "Tool definition required");
        Objects.requireNonNull(handler, "Tool handler required");
        toolDefs.put(def.name(), def);
        toolHandlers.put(def.name(), handler);
        log.info("Registered tool: {}", def.name());
    }

    /**
     * Devuelve la lista de nombres de herramientas registradas.
     */
    @Override
    public List<String> listTools() {
        return new ArrayList<>(toolDefs.keySet());
    }

    /**
     * Devuelve las capacidades (descripciones) de las herramientas.
     */
    @Override
    public Map<String, Object> capabilities() {
        Map<String, Object> caps = new LinkedHashMap<>();
        for (var entry : toolDefs.entrySet()) {
            caps.put(entry.getKey(), entry.getValue().description());
        }
        return caps;
    }

    /**
     * Llama a la herramienta registrada por nombre.
     */
    @Override
    public Object callTool(String name, Map<String, Object> arguments) {
        ToolHandler handler = toolHandlers.get(name);
        if (handler == null) {
            throw new IllegalArgumentException("Tool not found: " + name);
        }
        return handler.handle(null, arguments != null ? arguments : Map.of());
    }

    /**
     * Devuelve la definición de una herramienta por nombre.
     */
    public Optional<McpSchema.Tool> getToolDefinition(String name) {
        return Optional.ofNullable(toolDefs.get(name));
    }

    /**
     * Devuelve todas las definiciones de herramientas.
     */
    public Collection<McpSchema.Tool> getAllToolDefinitions() {
        return toolDefs.values();
    }

    /**
     * Interfaz funcional para handlers de herramientas.
     */
    @FunctionalInterface
    public interface ToolHandler {
        Object handle(McpSyncServerExchange exchange, Map<String, Object> args);
    }
}