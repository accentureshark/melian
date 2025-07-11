//package org.shark.melian.controller;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class MetadataControllerTest {
//
//    @Mock
//    private MetadataService<DatabaseMetadataDto> sqlMetadataService;
//
//    @Mock
//    private MetadataService<McpMetadataDto> restApiMetadataService;
//
//    @Mock
//    private MetadataService<MongoDatabaseMetadataDto> mongoMetadataService;
//
//    @InjectMocks
//    private MetadataController metadataController;
//
//    private List<TableShortDto> mockTableShorts;
//    private DatabaseMetadataDto mockDatabaseMetadata;
//    private McpMetadataDto mockMcpMetadata;
//    private MongoDatabaseMetadataDto mockMongoMetadata;
//
//    @BeforeEach
//    void setUp() {
//        mockTableShorts = List.of(
//            new TableShortDto("movies", List.of(), List.of()),
//            new TableShortDto("users", List.of(), List.of())
//        );
//
//        mockDatabaseMetadata = new DatabaseMetadataDto(List.of());
//        mockMcpMetadata = new McpMetadataDto("Melian", "Test MCP Server", "1.0", "2024-01-01", List.of(), List.of(), new DatabaseMetadataDto(List.of()));
//        mockMongoMetadata = new MongoDatabaseMetadataDto(List.of());
//    }
//
//    @Test
//    void testGetShortSummary_SqlSource_CallsSqlService() {
//        // Given
//        String source = "sql";
//        when(sqlMetadataService.extractShortSummary()).thenReturn(mockTableShorts);
//
//        // When
//        List<TableShortDto> result = metadataController.getShortSummary(source);
//
//        // Then
//        assertNotNull(result);
//        assertEquals(2, result.size());
//        assertEquals("movies", result.get(0).getName());
//        verify(sqlMetadataService).extractShortSummary();
//        verifyNoInteractions(restApiMetadataService, mongoMetadataService);
//    }
//
//    @Test
//    void testGetShortSummary_RestSource_CallsRestService() {
//        String source = "rest";
//        when(restApiMetadataService.extractShortSummary()).thenReturn(mockTableShorts);
//
//        List<TableShortDto> result = metadataController.getShortSummary(source);
//
//        assertNotNull(result);
//        assertEquals(2, result.size());
//        verify(restApiMetadataService).extractShortSummary();
//        verifyNoInteractions(sqlMetadataService, mongoMetadataService);
//    }
//
//    @Test
//    void testGetShortSummary_MongodbSource_CallsMongoService() {
//        // Given
//        String source = "mongodb";
//        when(mongoMetadataService.extractShortSummary()).thenReturn(mockTableShorts);
//
//        // When
//        List<TableShortDto> result = metadataController.getShortSummary(source);
//
//        // Then
//        assertNotNull(result);
//        assertEquals(2, result.size());
//        verify(mongoMetadataService, atLeastOnce()).extractShortSummary();
//        verifyNoInteractions(sqlMetadataService, restApiMetadataService);
//    }
//
//    @Test
//    void testGetShortSummary_DefaultSource_CallsSqlService() {
//        // Given - no source parameter (uses default)
//        when(sqlMetadataService.extractShortSummary()).thenReturn(mockTableShorts);
//
//        // When
//        List<TableShortDto> result = metadataController.getShortSummary("sql");
//
//        // Then
//        assertNotNull(result);
//        assertEquals(2, result.size());
//        verify(sqlMetadataService, atLeastOnce()).extractShortSummary();
//    }
//
//    @Test
//    void testGetMetadata_SqlSource_ReturnsDatabaseMetadata() {
//        // Given
//        String source = "sql";
//        when(sqlMetadataService.extractMetadata()).thenReturn(mockDatabaseMetadata);
//
//        // When
//        Object result = metadataController.getMetadata(source);
//
//        // Then
//        assertNotNull(result);
//        assertInstanceOf(DatabaseMetadataDto.class, result);
//        verify(sqlMetadataService).extractMetadata();
//        verifyNoInteractions(restApiMetadataService, mongoMetadataService);
//    }
//
//    @Test
//    void testGetMetadata_RestSource_ReturnsMcpMetadata() {
//        // Given
//        String source = "rest";
//        when(restApiMetadataService.extractMetadata()).thenReturn(mockMcpMetadata);
//
//        // When
//        Object result = metadataController.getMetadata(source);
//
//        // Then
//        assertNotNull(result);
//        assertInstanceOf(McpMetadataDto.class, result);
//        verify(restApiMetadataService).extractMetadata();
//        verifyNoInteractions(sqlMetadataService, mongoMetadataService);
//    }
//
//    @Test
//    void testGetMetadata_MongodbSource_ReturnsMongoMetadata() {
//        // Given
//        String source = "mongodb";
//        when(mongoMetadataService.extractMetadata()).thenReturn(mockMongoMetadata);
//
//        // When
//        Object result = metadataController.getMetadata(source);
//
//        // Then
//        assertNotNull(result);
//        assertInstanceOf(MongoDatabaseMetadataDto.class, result);
//        verify(mongoMetadataService).extractMetadata();
//        verifyNoInteractions(sqlMetadataService, restApiMetadataService);
//    }
//
//    @Test
//    void testGetMetadata_DefaultSource_ReturnsDatabaseMetadata() {
//        // Given
//        when(sqlMetadataService.extractMetadata()).thenReturn(mockDatabaseMetadata);
//
//        // When
//        Object result = metadataController.getMetadata("sql");
//
//        // Then
//        assertNotNull(result);
//        assertInstanceOf(DatabaseMetadataDto.class, result);
//        verify(sqlMetadataService).extractMetadata();
//    }
//
//    @Test
//    void testGetShortSummary_MongodbSource_ReturnsMongoMetadata() {
//        String source = "mongodb";
//        when(mongoMetadataService.extractShortSummary()).thenReturn(mockTableShorts);
//
//        List<TableShortDto> result = metadataController.getShortSummary(source);
//
//        assertNotNull(result);
//        assertEquals(2, result.size());
//        verify(mongoMetadataService).extractShortSummary();
//        verifyNoInteractions(sqlMetadataService, restApiMetadataService);
//    }
//
//}