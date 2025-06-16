package org.shark.melian.mcp.mapper;

import org.shark.melian.mcp.model.DatabaseMetadataDto;
import org.shark.melian.mcp.model.TableMetadataDto;
import org.shark.melian.mcp.model.ColumnMetadataDto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SchemaMapper {

    public static Map<String, Object> fromDto(DatabaseMetadataDto dto) {
        List<Map<String, Object>> tables = dto.getTables().stream().map(SchemaMapper::tableToMap).collect(Collectors.toList());
        return Map.of("tables", tables);
    }

    private static Map<String, Object> tableToMap(TableMetadataDto table) {
        List<Map<String, Object>> columns = table.getColumns().stream().map(SchemaMapper::columnToMap).collect(Collectors.toList());
        return Map.of(
                "name", table.getName(),
                "columns", columns
        );
    }

    private static Map<String, Object> columnToMap(ColumnMetadataDto column) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", column.getName());
        map.put("type", column.getType());
        map.put("primaryKey", column.isPrimaryKey());
        map.put("foreignKey", column.isForeignKey());
        if (column.getForeignTable() != null) map.put("foreignTable", column.getForeignTable());
        if (column.getForeignColumn() != null) map.put("foreignColumn", column.getForeignColumn());
        if (column.getDescription() != null) map.put("description", column.getDescription());
        return map;
    }
}
