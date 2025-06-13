package org.shark.melian.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColumnMetadataDto {
    private String name;
    private String type;
    private boolean primaryKey;
    private boolean foreignKey;
    private String foreignTable;   // opcional
    private String foreignColumn;  // opcional
}
