package org.shark.melian.service;

import org.shark.melian.controller.ChunkPageDto;
import org.shark.melian.model.ChunkDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static String cleanQuotes(String val) {
        val = val.trim();
        if ((val.startsWith("'") && val.endsWith("'")) || (val.startsWith("\"") && val.endsWith("\""))) {
            return val.substring(1, val.length() - 1);
        }
        return val;
    }

    public ChunkPageDto findChunks(String table, String filter, int limit, String afterId) {
        log.info("[SqlChunkService] findChunks params: table=" + table + ", filter=" + filter + ", limit=" + limit + ", afterId=" + afterId);
        filter = java.net.URLDecoder.decode(filter, java.nio.charset.StandardCharsets.UTF_8);

        if (!table.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Tabla inválida: " + table);
        }

        String pkColumn = getPrimaryKeyColumn(table);
        if (pkColumn == null) {
            String msg = "[SqlChunkService] ❌ No se encontró PK para la tabla " + table + ", abortando.";
            log.warning(msg);
            throw new RuntimeException(msg);
        }
        log.info("[SqlChunkService] PK detected: " + pkColumn);

        List<String> whereClauses = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (afterId != null && !afterId.isBlank()) {
            whereClauses.add(pkColumn + " > ?");
            params.add(afterId);
        }

        if (filter != null && !filter.isBlank()) {
            String filterLower = filter.toLowerCase();
            if (filterLower.contains(" like ")) {
                String[] parts = filter.split("(?i)like", 2);
                String col = parts[0].trim();
                String val = cleanQuotes(parts[1].trim());
                if (!col.matches("^[a-zA-Z0-9_]+$")) throw new IllegalArgumentException("Columna inválida: " + col);
                whereClauses.add("LOWER(" + col + ") LIKE LOWER(?)");
                params.add(val);
                log.info("[SqlChunkService] Case-insensitive LIKE: LOWER(" + col + ") LIKE LOWER(" + val + ")");
            } else if (filter.contains("=")) {
                String[] parts = filter.split("=", 2);
                String col = parts[0].trim();
                String val = cleanQuotes(parts[1].trim());
                if (!col.matches("^[a-zA-Z0-9_]+$")) throw new IllegalArgumentException("Columna inválida: " + col);
                whereClauses.add("LOWER(" + col + ") = LOWER(?)");
                params.add(val);
                log.info("[SqlChunkService] Case-insensitive '=': LOWER(" + col + ") = LOWER(" + val + ")");
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
                chunk.setId(rs.getString(pkColumn));

                String text = buildTextFromResultSet(rs, rsmd);
                chunk.setText(text);

                Map<String, Object> metadata = new HashMap<>();
                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                    String col = rsmd.getColumnName(i);
                    metadata.put(col, rs.getObject(col));
                }
                chunk.setMetadata(metadata);

                log.info("[SqlChunkService] ✅ Chunk generado: id=" + chunk.getId());
                log.info("[SqlChunkService] ➕ text: " + text);
                log.info("[SqlChunkService] ➕ metadata: " + metadata);

                chunks.add(chunk);
            }
        } catch (Exception e) {
            log.severe("[SqlChunkService] ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        boolean hasMore = chunks.size() == limit;
        String nextAfterId = hasMore ? chunks.get(chunks.size() - 1).getId() : null;
        return new ChunkPageDto(chunks, hasMore, nextAfterId);
    }

    @Override
    public List<ChunkDto> getChunks(String table, String source, int limit, String afterId, String filter, List<String> tags, String sort) {
        ChunkPageDto page = findChunks(table, filter, limit, afterId);
        return page.getChunks();
    }

    private String getPrimaryKeyColumn(String table) {
        try (Connection conn = jdbcTemplate.getDataSource().getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet pk = meta.getPrimaryKeys(null, null, table);
            if (pk.next()) {
                return pk.getString("COLUMN_NAME");
            }
        } catch (SQLException e) {
            log.warning("[SqlChunkService] No se pudo detectar PK para tabla " + table);
        }
        return null; // antes devolvía "id"
    }

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
