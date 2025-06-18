package org.shark.melian.service;

import org.shark.melian.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service("sqlMetadataService")
public class SqlMetadataService implements MetadataService<DatabaseMetadataDto> {

    private final DataSource dataSource;

    @Autowired
    public SqlMetadataService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<TableShortDto> extractShortSummary() {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            List<TableShortDto> tables = new ArrayList<>();

            ResultSet rsTables = meta.getTables(null, null, "%", new String[]{"TABLE"});
            while (rsTables.next()) {
                String tableName = rsTables.getString("TABLE_NAME");
                List<ColumnShortDto> columns = new ArrayList<>();
                List<ForeignKeyShortDto> foreignKeys = new ArrayList<>();

                // Columnas
                ResultSet rsColumns = meta.getColumns(null, null, tableName, "%");
                while (rsColumns.next()) {
                    String columnName = rsColumns.getString("COLUMN_NAME");
                    String typeName = rsColumns.getString("TYPE_NAME");
                    columns.add(new ColumnShortDto(columnName, typeName));
                }
                rsColumns.close();

                // FKs
                ResultSet rsFks = meta.getImportedKeys(null, null, tableName);
                while (rsFks.next()) {
                    String fkColumn = rsFks.getString("FKCOLUMN_NAME");
                    String pkTable = rsFks.getString("PKTABLE_NAME");
                    String pkColumn = rsFks.getString("PKCOLUMN_NAME");
                    foreignKeys.add(new ForeignKeyShortDto(
                            fkColumn,
                            pkTable,
                            pkColumn
                    ));
                }
                rsFks.close();

                tables.add(new TableShortDto(tableName, columns, foreignKeys));
            }
            rsTables.close();
            return tables;
        } catch (SQLException e) {
            throw new RuntimeException("Error extracting metadata", e);
        }
    }

    @Override
    public DatabaseMetadataDto extractMetadata() {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            List<TableMetadataDto> tables = new ArrayList<>();

            ResultSet rsTables = meta.getTables(null, null, "%", new String[]{"TABLE"});
            while (rsTables.next()) {
                String tableName = rsTables.getString("TABLE_NAME");
                List<ColumnMetadataDto> columns = new ArrayList<>();
                List<ForeignKeyShortDto> foreignKeys = new ArrayList<>();

                // Columns with type info
                ResultSet rsColumns = meta.getColumns(null, null, tableName, "%");
                Map<String, ColumnMetadataDto> columnMap = new LinkedHashMap<>();
                while (rsColumns.next()) {
                    String columnName = rsColumns.getString("COLUMN_NAME");
                    String typeName = rsColumns.getString("TYPE_NAME");
                    columnMap.put(columnName, ColumnMetadataDto.builder()
                            .name(columnName)
                            .type(typeName)
                            .primaryKey(false)
                            .foreignKey(false)
                            .description(null)
                            .build());
                }
                rsColumns.close();

                // PKs
                ResultSet rsPks = meta.getPrimaryKeys(null, null, tableName);
                while (rsPks.next()) {
                    String pkColumn = rsPks.getString("COLUMN_NAME");
                    ColumnMetadataDto col = columnMap.get(pkColumn);
                    if (col != null) col.setPrimaryKey(true);
                }
                rsPks.close();

                // FKs
                ResultSet rsFks = meta.getImportedKeys(null, null, tableName);
                while (rsFks.next()) {
                    String fkColumn = rsFks.getString("FKCOLUMN_NAME");
                    String pkTable = rsFks.getString("PKTABLE_NAME");
                    String pkColumn = rsFks.getString("PKCOLUMN_NAME");
                    ColumnMetadataDto col = columnMap.get(fkColumn);
                    if (col != null) {
                        col.setForeignKey(true);
                        col.setForeignTable(pkTable);
                        col.setForeignColumn(pkColumn);
                    }
                    foreignKeys.add(new ForeignKeyShortDto(fkColumn, pkTable, pkColumn));
                }
                rsFks.close();

                columns.addAll(columnMap.values());
                tables.add(new TableMetadataDto(tableName, columns, foreignKeys));
            }
            rsTables.close();

            return new DatabaseMetadataDto(tables);
        } catch (SQLException e) {
            throw new RuntimeException("Error extracting full metadata", e);
        }
    }
}
