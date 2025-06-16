package org.shark.melian.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForeignKeyShortDto {
    private String column;
    private String referencedTable;
    private String referencedColumn;
}
