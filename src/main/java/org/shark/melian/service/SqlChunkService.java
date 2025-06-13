package org.shark.melian.service;

import org.shark.melian.model.ChunkDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Service
public class SqlChunkService implements ChunkService {

    private final DataSource dataSource;
    private final int defaultLimit;

    public SqlChunkService(DataSource dataSource,
                           @Value("${melian.adapters.sql.chunking.max-chunks:50}") int defaultLimit) {
        this.dataSource = dataSource;
        this.defaultLimit = defaultLimit;
    }

    @Override
    public List<ChunkDto> getChunks(
            String table,
            String source,
            int limit,
            String afterId,
            String filter,
            List<String> tags,
            String sort
    ) {
        // Prioridad: table > source (para futura evolución)
        String tableName = table != null ? table : source;

        int fetchLimit = (limit > 0) ? limit : defaultLimit;
        List<ChunkDto> chunks = new ArrayList<>();
        if (tableName == null || tableName.isEmpty()) {
            // No hay tabla ni fuente, no devuelvas nada
            return chunks;
        }

        // --- Construcción dinámica del SQL ---
        StringBuilder sql = new StringBuilder("SELECT * FROM " + tableName);

        // Filtro simple tipo columna=valor (mejorar en el futuro)
        List<String> whereClauses = new ArrayList<>();
        if (filter != null && filter.contains("=")) {
            String[] parts = filter.split("=", 2);
            String col = parts[0].trim();
            String val = parts[1].trim();
            whereClauses.add(col + " = ?");
        }

        // Tags: no aplicados en SQL, sí en memoria (más abajo)

        if (!whereClauses.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
        }
        // Order by
        if (sort != null && !sort.isEmpty()) {
            sql.append(" ORDER BY ").append(sort); // Valida sort en producción para evitar SQLi
        }
        sql.append(" LIMIT ?");

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int paramIdx = 1;
            if (filter != null && filter.contains("=")) {
                String[] parts = filter.split("=", 2);
                String val = parts[1].trim();
                stmt.setString(paramIdx++, val);
            }
            stmt.setInt(paramIdx, fetchLimit);

            ResultSet rs = stmt.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            DatabaseMetaData dbMeta = conn.getMetaData();
            ResultSet pkRs = dbMeta.getPrimaryKeys(null, null, tableName);
            List<String> pkCols = new ArrayList<>();
            while (pkRs.next()) {
                pkCols.add(pkRs.getString("COLUMN_NAME"));
            }
            pkRs.close();

            while (rs.next()) {
                // ID único
                String id;
                if (pkCols.isEmpty()) {
                    id = UUID.randomUUID().toString();
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (String pk : pkCols) {
                        if (sb.length() > 0) sb.append("_");
                        sb.append(rs.getString(pk));
                    }
                    id = sb.toString();
                }

                // MCP text y metadata
                StringBuilder text = new StringBuilder();
                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("table", tableName);

                for (int i = 1; i <= colCount; i++) {
                    String col = meta.getColumnName(i);
                    String val = rs.getString(i);
                    if (text.length() > 0) text.append(" | ");
                    text.append(col).append(": ").append(val);
                    metadata.put(col, val);
                }

                // Tags: filtrado en memoria (mejorar si querés performance)
                boolean matchesTags = true;
                if (tags != null && !tags.isEmpty()) {
                    matchesTags = tags.stream().allMatch(tag -> metadata.values().contains(tag));
                }

                if (matchesTags) {
                    chunks.add(new ChunkDto(
                            id,
                            text.toString(),
                            metadata,
                            null,   // embedding (puede ser null)
                            null,   // source (puede ser null)
                            null    // tags (puede ser null)
                    ));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching chunks for table " + tableName, e);
        }

        return chunks;
    }
}
