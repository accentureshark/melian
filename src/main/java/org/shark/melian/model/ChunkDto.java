package org.shark.melian.model;

import java.util.List;
import java.util.Map;

/**
 * MCP-compliant chunk DTO for movie data.
 * Contains text content and metadata as required by MCP protocol.
 */
public class ChunkDto {
    private String id;
    private String text; // <-- esto es clave, MCP espera "text", NO "content"
    private Map<String, Object> metadata; // MCP recomienda "object" para máxima compatibilidad

    // Opcionales para compatibilidad future-proof:
    private List<Float> embedding; // opcional
    private String source;         // opcional
    private List<String> tags;     // opcional

    public ChunkDto() {
    }

    public ChunkDto(String id, String text, Map<String, Object> metadata) {
        this.id = id;
        this.text = text;
        this.metadata = metadata;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public List<Float> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(List<Float> embedding) {
        this.embedding = embedding;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}

