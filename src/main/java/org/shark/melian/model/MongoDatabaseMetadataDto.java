package org.shark.melian.model;

import java.util.List;

public class MongoDatabaseMetadataDto {
	
	private List<TableMetadataDto> tables;

    public MongoDatabaseMetadataDto(List<TableMetadataDto> tables) {
        this.tables = tables;
    }

    public List<TableMetadataDto> getTables() {
        return tables;
    }

    public void setTables(List<TableMetadataDto> tables) {
        this.tables = tables;
    }

}
