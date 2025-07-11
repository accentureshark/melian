//package org.shark.melian.service;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import javax.sql.DataSource;
//import java.sql.Connection;
//import java.sql.DatabaseMetaData;
//import java.sql.ResultSet;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class SqlMetadataServiceTest {
//
//    @Mock
//    private DataSource dataSource;
//
//    @Mock
//    private Connection connection;
//
//    @Mock
//    private DatabaseMetaData metaData;
//
//    @Mock
//    private ResultSet resultSet;
//
//    @InjectMocks
//    private SqlMetadataService sqlMetadataService;
//
//    @BeforeEach
//    void setUp() throws Exception {
//        lenient().when(dataSource.getConnection()).thenReturn(connection);
//        lenient().when(connection.getMetaData()).thenReturn(metaData);
//
//        // Mock tables result set
//        lenient().when(metaData.getTables(isNull(), isNull(), eq("%"), eq(new String[]{"TABLE"}))).thenReturn(resultSet);
//        lenient().when(resultSet.next()).thenReturn(true, true, false); // Two tables
//        lenient().when(resultSet.getString("TABLE_NAME")).thenReturn("movies", "users");
//    }
//
//    @Test
//    void testExtractShortSummary_ReturnsTables() throws Exception {
//        // Given - mock columns for each table
//        ResultSet columnsResultSet = mock(ResultSet.class);
//        when(metaData.getColumns(isNull(), isNull(), anyString(), eq("%"))).thenReturn(columnsResultSet);
//        when(columnsResultSet.next()).thenReturn(false); // No columns for simplicity
//
//        ResultSet fkResultSet = mock(ResultSet.class);
//        when(metaData.getImportedKeys(isNull(), isNull(), anyString())).thenReturn(fkResultSet);
//        when(fkResultSet.next()).thenReturn(false); // No foreign keys
//
//        // When
//        List<TableShortDto> result = sqlMetadataService.extractShortSummary();
//
//        // Then
//        assertNotNull(result);
//        assertEquals(2, result.size());
//
//        List<String> tableNames = result.stream()
//                .map(TableShortDto::getName)
//                .toList();
//        assertTrue(tableNames.contains("movies"));
//        assertTrue(tableNames.contains("users"));
//    }
//
//    @Test
//    void testExtractMetadata_ReturnsCompleteMetadata() throws Exception {
//        // Given - mock columns for each table
//        ResultSet columnsResultSet = mock(ResultSet.class);
//        when(metaData.getColumns(isNull(), isNull(), anyString(), eq("%"))).thenReturn(columnsResultSet);
//        when(columnsResultSet.next()).thenReturn(false); // No columns for simplicity
//
//        ResultSet pkResultSet = mock(ResultSet.class);
//        when(metaData.getPrimaryKeys(isNull(), isNull(), anyString())).thenReturn(pkResultSet);
//        when(pkResultSet.next()).thenReturn(false); // No primary keys
//
//        ResultSet fkResultSet = mock(ResultSet.class);
//        when(metaData.getImportedKeys(isNull(), isNull(), anyString())).thenReturn(fkResultSet);
//        when(fkResultSet.next()).thenReturn(false); // No foreign keys
//
//        // When
//        DatabaseMetadataDto result = sqlMetadataService.extractMetadata();
//
//        // Then
//        assertNotNull(result);
//        assertNotNull(result.getTables());
//        assertEquals(2, result.getTables().size());
//    }
//
//    @Test
//    void testConstructor_WithDataSource() {
//        // When
//        SqlMetadataService service = new SqlMetadataService(dataSource);
//
//        // Then
//        assertNotNull(service);
//    }
//}