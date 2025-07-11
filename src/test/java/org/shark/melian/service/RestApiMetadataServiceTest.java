//package org.shark.melian.service;
//
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@ExtendWith(MockitoExtension.class)
//class RestApiMetadataServiceTest {
//
//    @InjectMocks
//    private RestApiMetadataService restApiMetadataService;
//
//    @Test
//    void testExtractMetadata_ReturnsVirtualTables() {
//        // When
//        McpMetadataDto result = restApiMetadataService.extractMetadata();
//
//        // Then
//        assertNotNull(result);
//        assertNotNull(result.getDatabase());
//        assertNotNull(result.getDatabase().getTables());
//        assertFalse(result.getDatabase().getTables().isEmpty());
//
//        // Verify film table exists
//        boolean filmTableExists = result.getDatabase().getTables().stream()
//                .anyMatch(table -> "film".equals(table.getName()));
//        assertTrue(filmTableExists);
//    }
//
//    @Test
//    void testExtractShortSummary_ReturnsShortSummary() {
//        // When
//        List<TableShortDto> result = restApiMetadataService.extractShortSummary();
//
//        // Then
//        assertNotNull(result);
//        assertFalse(result.isEmpty());
//
//        // Verify film table exists in short summary
//        boolean filmTableExists = result.stream()
//                .anyMatch(table -> "film".equals(table.getName()));
//        assertTrue(filmTableExists);
//
//        // Verify columns exist for film table
//        TableShortDto filmTable = result.stream()
//                .filter(table -> "film".equals(table.getName()))
//                .findFirst()
//                .orElse(null);
//
//        assertNotNull(filmTable);
//        assertNotNull(filmTable.getColumns());
//        assertFalse(filmTable.getColumns().isEmpty());
//
//        // Check for expected columns
//        List<String> columnNames = filmTable.getColumns().stream()
//                .map(col -> col.getName())
//                .toList();
//        assertTrue(columnNames.contains("film_id"));
//        assertTrue(columnNames.contains("title"));
//        assertTrue(columnNames.contains("description"));
//    }
//
//    @Test
//    void testFilmTableMetadata_HasCorrectStructure() {
//        // When
//        McpMetadataDto result = restApiMetadataService.extractMetadata();
//
//        // Then
//        assertNotNull(result.getDatabase().getTables());
//
//        // Find film table
//        var filmTable = result.getDatabase().getTables().stream()
//                .filter(table -> "film".equals(table.getName()))
//                .findFirst()
//                .orElse(null);
//
//        assertNotNull(filmTable);
//        assertNotNull(filmTable.getColumns());
//
//        // Verify primary key exists
//        boolean hasPrimaryKey = filmTable.getColumns().stream()
//                .anyMatch(col -> col.isPrimaryKey());
//        assertTrue(hasPrimaryKey);
//
//        // Verify film_id is the primary key
//        boolean filmIdIsPrimaryKey = filmTable.getColumns().stream()
//                .anyMatch(col -> "film_id".equals(col.getName()) && col.isPrimaryKey());
//        assertTrue(filmIdIsPrimaryKey);
//    }
//}