package org.shark.melian.service;


import org.shark.melian.model.ColumnMetadataDto;
import org.shark.melian.model.DatabaseMetadataDto;
import org.shark.melian.model.TableMetadataDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MetadataService {

    private final DataSource dataSource;

    @Autowired
    public MetadataService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DatabaseMetadataDto extractMetadata() {
        List<TableMetadataDto> tables = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            ResultSet rsTables = metaData.getTables(null, null, "%", new String[]{"TABLE"});
            while (rsTables.next()) {
                String tableName = rsTables.getString("TABLE_NAME");
                List<ColumnMetadataDto> columns = new ArrayList<>();

                ResultSet rsColumns = metaData.getColumns(null, null, tableName, "%");
                Map<String, Boolean> pkCols = getPrimaryKeys(metaData, tableName);
                Map<String, ForeignKey> fkCols = getForeignKeys(metaData, tableName);

                while (rsColumns.next()) {
                    String colName = rsColumns.getString("COLUMN_NAME");
                    String colType = rsColumns.getString("TYPE_NAME");
                    boolean isPk = pkCols.getOrDefault(colName, false);
                    ForeignKey fk = fkCols.get(colName);

                    ColumnMetadataDto col = new ColumnMetadataDto();
                    col.setName(colName);
                    col.setType(colType);
                    col.setPrimaryKey(isPk);
                    if (fk != null) {
                        col.setForeignKey(true);
                        col.setForeignTable(fk.pkTable);
                        col.setForeignColumn(fk.pkColumn);
                    } else {
                        col.setForeignKey(false);
                    }
                    columns.add(col);
                }
                rsColumns.close();

                TableMetadataDto table = new TableMetadataDto();
                table.setName(tableName);
                table.setColumns(columns);
                tables.add(table);
            }
            rsTables.close();
        } catch (SQLException e) {
            throw new RuntimeException("Error extracting DB metadata", e);
        }
        DatabaseMetadataDto result = new DatabaseMetadataDto();
        result.setTables(tables);
        return result;
    }

    private Map<String, Boolean> getPrimaryKeys(DatabaseMetaData metaData, String tableName) throws SQLException {
        Map<String, Boolean> pkCols = new HashMap<>();
        ResultSet rsPk = metaData.getPrimaryKeys(null, null, tableName);
        while (rsPk.next()) {
            pkCols.put(rsPk.getString("COLUMN_NAME"), true);
        }
        rsPk.close();
        return pkCols;
    }

    private Map<String, ForeignKey> getForeignKeys(DatabaseMetaData metaData, String tableName) throws SQLException {
        Map<String, ForeignKey> fkCols = new HashMap<>();
        ResultSet rsFk = metaData.getImportedKeys(null, null, tableName);
        while (rsFk.next()) {
            String fkCol = rsFk.getString("FKCOLUMN_NAME");
            String pkTable = rsFk.getString("PKTABLE_NAME");
            String pkCol = rsFk.getString("PKCOLUMN_NAME");
            fkCols.put(fkCol, new ForeignKey(pkTable, pkCol));
        }
        rsFk.close();
        return fkCols;
    }

    private static class ForeignKey {
        String pkTable;
        String pkColumn;

        ForeignKey(String pkTable, String pkColumn) {
            this.pkTable = pkTable;
            this.pkColumn = pkColumn;
        }
    }
}
