package org.shark.melian.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class McpMetadataDto {
    private String name;
    private String description;
    private String version;
    private String buildDate;
    private List<String> capabilities;
    private List<String> types;
    private DatabaseMetadataDto database; // tu estructura actual
}