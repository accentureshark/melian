package org.shark.melian.service;

import org.shark.melian.controller.ChunkPageDto;
import org.shark.melian.model.ChunkDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * MCP-compliant implementation for serving data chunks from SQL DBs.
 * Implements org.shark.melian.service.ChunkService interface as per MCP standard.
 */
@Service
public class SqlChunkService implements ChunkService {

    private static final Logger log = Logger.getLogger(SqlChunkService.class.getName());

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * MCP-Compliant, main method for retrieving data chunks from a table.
     *
     * @param table   - Table name (validated)
     * @param filter  - Optional SQL filter, e.g. "title='SOMETHING'"
     * @param limit   - Max number of results per page
     * @param afterId - For pagination, id to continue from (should match PK)
     * @return ChunkPageDto - List of ChunkDto + paging info
     */
    public ChunkPageDto findChunks(String table, String filter, int limit, String afterId) {
        log.info("[SqlChunkService] findChunks params: table=" + table + ", filter=" + filter + ", limit=" + limit + ", afterId=" + afterId);

        // Validate table name
        if (!table.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Tabla inválida: " + table);
        }

        // Determine primary key for pagination dynamically
        String pkColumn = getPrimaryKeyColumn(table);
        log.info("[SqlChunkService] PK detected: " + pkColumn);

        List<String> whereClauses = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        // afterId paginación dinámica por PK
        if (afterId != null && !afterId.isBlank()) {
            whereClauses.add(pkColumn + " > ?");
            params.add(afterId);
        }

        // Filtro (LIKE o '=')
        if (filter != null && !filter.isBlank()) {
            String filterLower = filter.toLowerCase();
            if (filterLower.contains(" like ")) {
                String[] parts = filter.split("(?i)like", 2);
                String col = parts[0].trim();
                String val = cleanQuotes(parts[1].trim());
                if (!col.matches("^[a-zA-Z0-9_]+$")) throw new IllegalArgumentException("Columna inválida: " + col);
                whereClauses.add(col + " LIKE ?");
                params.add(val);
                log.info("[SqlChunkService] Filtro LIKE: " + col + " LIKE " + val);
            } else if (filter.contains("=")) {
                String[] parts = filter.split("=", 2);
                String col = parts[0].trim();
                String val = cleanQuotes(parts[1].trim());
                if (!col.matches("^[a-zA-Z0-9_]+$")) throw new IllegalArgumentException("Columna inválida: " + col);
                whereClauses.add(col + " = ?");
                params.add(val);
                log.info("[SqlChunkService] Filtro '=': " + col + " = " + val);
            }
        }

        String where = whereClauses.isEmpty() ? "" : "WHERE " + String.join(" AND ", whereClauses);
        String sql = "SELECT * FROM " + table + " " + where + " LIMIT ?";
        params.add(limit);

        log.info("[SqlChunkService] Query final: " + sql);
        log.info("[SqlChunkService] Params: " + params);

        List<ChunkDto> chunks = new ArrayList<>();

        try (Connection conn = jdbcTemplate.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            ResultSet rs = stmt.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();
            while (rs.next()) {
                ChunkDto chunk = new ChunkDto();
                // Set id using detected PK column
                chunk.setId(rs.getString(pkColumn));
                // Text: dynamically build from all columns
                chunk.setText(buildTextFromResultSet(rs, rsmd));
                // Metadata: full row map
                Map<String, Object> metadata = new HashMap<>();
                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                    String col = rsmd.getColumnName(i);
                    metadata.put(col, rs.getObject(col));
                }
                chunk.setMetadata(metadata);
                chunks.add(chunk);
            }
        } catch (Exception e) {
            log.severe("[SqlChunkService] ERROR: " + e.getMessage());
            e.printStackTrace();
            // Opcional: throw una excepción MCP si quieres que suba como 4xx/5xx.
        }

        boolean hasMore = chunks.size() == limit;
        String nextAfterId = hasMore ? chunks.get(chunks.size() - 1).getId() : null;
        return new ChunkPageDto(chunks, hasMore, nextAfterId);
    }

    /**
     * MCP standard interface, compatible for compliance and legacy.
     *
     * @see ChunkService#getChunks
     */
    @Override
    public List<ChunkDto> getChunks(String table, String source, int limit, String afterId, String filter, List<String> tags, String sort) {
        // MCP puro: ignora params que no usa, responde igual
        ChunkPageDto page = findChunks(table, filter, limit, afterId);
        return page.getChunks();
    }

    // UTILS

    /** Detección dinámica de PK por tabla (asume PK simple, mejora según tu metadata si es compuesta) */
    private String getPrimaryKeyColumn(String table) {
        try (Connection conn = jdbcTemplate.getDataSource().getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet pk = meta.getPrimaryKeys(null, null, table);
            if (pk.next()) {
                return pk.getString("COLUMN_NAME");
            }
        } catch (SQLException e) {
            log.warning("[SqlChunkService] No se pudo detectar PK para tabla " + table + ", usando 'id' por default");
        }
        // Fallback por convención
        return "id";
    }

    /** Limpia comillas en el filtro SQL */
    private static String cleanQuotes(String val) {
        val = val.trim();
        if ((val.startsWith("'") && val.endsWith("'")) || (val.startsWith("\"") && val.endsWith("\""))) {
            return val.substring(1, val.length() - 1);
        }
        return val;
    }

    /** Arma el texto de chunk usando TODAS las columnas de la fila */
    private String buildTextFromResultSet(ResultSet rs, ResultSetMetaData rsmd) throws SQLException {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= rsmd.getColumnCount(); i++) {
            String col = rsmd.getColumnName(i);
            sb.append(col).append(": ").append(rs.getString(col));
            if (i < rsmd.getColumnCount()) sb.append(" | ");
        }
        return sb.toString();
    }
}
