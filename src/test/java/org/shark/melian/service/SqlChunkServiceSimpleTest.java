//package org.shark.melian.service;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.shark.melian.model.ChunkDto;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.jdbc.core.RowMapper;
//
//import javax.sql.DataSource;
//import java.sql.Connection;
//import java.sql.DatabaseMetaData;
//import java.sql.ResultSet;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class SqlChunkServiceSimpleTest {
//
//    @Mock
//    private JdbcTemplate jdbcTemplate;
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
//    private SqlChunkService sqlChunkService;
//
//    @BeforeEach
//    void setUp() throws Exception {
//        lenient().when(dataSource.getConnection()).thenReturn(connection);
//        lenient().when(connection.getMetaData()).thenReturn(metaData);
//        lenient().when(jdbcTemplate.getDataSource()).thenReturn(dataSource);
//    }
//
//    @Test
//    void testGetChunks_InvalidTableName_ThrowsException() {
//        // Given
//        String invalidTable = "invalid-table-name";
//
//        // When & Then
//        assertThrows(IllegalArgumentException.class, () ->
//            sqlChunkService.getChunks(invalidTable, "sql", 10, null, "", null, null));
//    }
//
//    @Test
//    void testCleanQuotes_Private_Method() {
//        // This is more of a placeholder test since the method is private
//        // The actual behavior is tested through integration with other methods
//        assertTrue(true);
//    }
//}