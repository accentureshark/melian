package org.shark.melian.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChunkDto {
    private String id;
    private String text; // <-- esto es clave, MCP espera "text", NO "content"
    private Map<String, Object> metadata; // MCP recomienda "object" para máxima compatibilidad

    // Opcionales para compatibilidad future-proof:
    private List<Float> embedding; // opcional
    private String source;         // opcional
    private List<String> tags;     // opcional
}

