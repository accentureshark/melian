//package org.shark.melian.controller;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.shark.melian.model.ChunkDto;
//import org.shark.melian.service.ChunkService;
//import org.shark.melian.service.MovieChunkService;
//
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class ChunkControllerTest {
//
//    @Mock
//    private ChunkService sqlChunkService;
//
//    @Mock
//    private ChunkService restApiChunkService;
//
//    @Mock
//    private ChunkService mongoChunkService;
//
//    @Mock
//    private MovieChunkService sqlMovieChunkService;
//
//    @Mock
//    private MovieChunkService mongoMovieChunkService;
//
//    @InjectMocks
//    private ChunkController chunkController;
//
//    private List<ChunkDto> mockChunks;
//
//    @BeforeEach
//    void setUp() {
//        mockChunks = List.of(
//            new ChunkDto("1", "Movie content 1", null, null, null, null),
//            new ChunkDto("2", "Movie content 2", null, null, null, null)
//        );
//    }
//
//    @Test
//    void testGetChunks_SqlSource_CallsSqlService() {
//        // Given
//        String table = "movies";
//        String source = "sql";
//        int limit = 10;
//        when(sqlChunkService.getChunks(eq(table), eq(source), eq(limit), isNull(), isNull(), isNull(), isNull()))
//                .thenReturn(List.of(
//                    new ChunkDto("1", "Movie content 1", null, null, null, null),
//                    new ChunkDto("2", "Movie content 2", null, null, null, null)
//                ));
//
//        // When
//        List<ChunkDto> result = chunkController.getChunks(table, source, limit, null, null, null, null);
//
//        // Then
//        assertNotNull(result);
//        assertEquals(2, result.size());
//        verify(sqlChunkService).getChunks(eq(table), eq(source), eq(limit), isNull(), isNull(), isNull(), isNull());
//        verifyNoInteractions(restApiChunkService, mongoChunkService);
//    }
//
//    @Test
//    void testGetChunks_RestSource_CallsRestService() {
//        // Given
//        String table = "film";
//        String source = "rest";
//        int limit = 5;
//        when(restApiChunkService.getChunks(table, source, limit, null, null, null, null))
//                .thenReturn(mockChunks);
//
//        // When
//        List<ChunkDto> result = chunkController.getChunks(table, source, limit, null, null, null, null);
//
//        // Then
//        assertNotNull(result);
//        verify(restApiChunkService).getChunks(table, source, limit, null, null, null, null);
//        verifyNoInteractions(sqlChunkService, mongoChunkService);
//    }
//
//    @Test
//    void testGetChunks_MongoSource_CallsMongoService() {
//        // Given
//        String table = "movies";
//        String source = "mongodb";
//        int limit = 15;
//        when(mongoChunkService.getChunks(eq(table), eq(source), eq(limit), isNull(), isNull(), isNull(), isNull()))
//                .thenReturn(List.of(
//                    new ChunkDto("1", "Movie content 1", null, null, null, null),
//                    new ChunkDto("2", "Movie content 2", null, null, null, null)
//                ));
//
//        // When
//        List<ChunkDto> result = chunkController.getChunks(table, source, limit, null, null, null, null);
//
//        // Then
//        assertNotNull(result);
//        assertEquals(2, result.size());
//        verify(mongoChunkService).getChunks(eq(table), eq(source), eq(limit), isNull(), isNull(), isNull(), isNull());
//        verifyNoInteractions(sqlChunkService, restApiChunkService);
//    }
//
//    @Test
//    void testGetChunks_MoviesTable_SqlSource_CallsMovieService() {
//        // Given
//        String table = "movies";
//        String source = "sql";
//        int limit = 10;
//        when(sqlMovieChunkService.getMovieChunks(eq(source), eq(limit), isNull(), isNull(), isNull(), isNull()))
//                .thenReturn(List.of(
//                    new ChunkDto("1", "Movie content 1", null, null, null, null),
//                    new ChunkDto("2", "Movie content 2", null, null, null, null)
//                ));
//
//        // When
//        List<ChunkDto> result = chunkController.getChunks(table, source, limit, null, null, null, null);
//
//        // Then
//        assertNotNull(result);
//        assertEquals(2, result.size());
//        verify(sqlMovieChunkService).getMovieChunks(eq(source), eq(limit), isNull(), isNull(), isNull(), isNull());
//        verifyNoInteractions(sqlChunkService, restApiChunkService, mongoChunkService);
//    }
//
//    @Test
//    void testGetChunks_MoviesTable_MongoSource_CallsMongoMovieService() {
//        // Given
//        String table = "movies";
//        String source = "mongodb";
//        int limit = 10;
//        when(mongoMovieChunkService.getMovieChunks(eq(source), eq(limit), isNull(), isNull(), isNull(), isNull()))
//                .thenReturn(List.of(
//                    new ChunkDto("1", "Movie content 1", null, null, null, null),
//                    new ChunkDto("2", "Movie content 2", null, null, null, null)
//                ));
//
//        // When
//        List<ChunkDto> result = chunkController.getChunks(table, source, limit, null, null, null, null);
//
//        // Then
//        assertNotNull(result);
//        assertEquals(2, result.size());
//        verify(mongoMovieChunkService).getMovieChunks(eq(source), eq(limit), isNull(), isNull(), isNull(), isNull());
//        verifyNoInteractions(sqlChunkService, restApiChunkService, mongoChunkService);
//    }
//
//    @Test
//    void testGetChunks_TmdbSource_CallsRestService() {
//        // Given
//        String table = "film";
//        String source = "tmdb";
//        int limit = 5;
//        when(restApiChunkService.getChunks(table, source, limit, null, null, null, null))
//                .thenReturn(mockChunks);
//
//        // When
//        List<ChunkDto> result = chunkController.getChunks(table, source, limit, null, null, null, null);
//
//        // Then
//        assertNotNull(result);
//        verify(restApiChunkService).getChunks(table, source, limit, null, null, null, null);
//    }
//
//    @Test
//    void testGetChunks_InvalidSource_ThrowsException() {
//        // Given
//        String table = "movies";
//        String source = "invalid";
//        int limit = 10;
//
//        // When & Then
//        assertThrows(IllegalArgumentException.class, () ->
//            chunkController.getChunks(table, source, limit, null, null, null, null));
//    }
//
//    @Test
//    void testGetChunks_WithAllParameters() {
//        // Given
//        String table = "movies";
//        String source = "sql";
//        int limit = 20;
//        String afterId = "10";
//        String filter = "title='Matrix'";
//        List<String> tags = List.of("action", "sci-fi");
//        String sort = "title";
//        when(sqlChunkService.getChunks(eq(table), eq(source), eq(limit), eq(afterId), eq(filter), eq(tags), eq(sort)))
//                .thenReturn(List.of(
//                    new ChunkDto("1", "Movie content 1", null, null, null, null),
//                    new ChunkDto("2", "Movie content 2", null, null, null, null)
//                ));
//
//        // When
//        List<ChunkDto> result = chunkController.getChunks(table, source, limit, afterId, filter, tags, sort);
//
//        // Then
//        assertNotNull(result);
//        assertEquals(2, result.size());
//        verify(sqlChunkService).getChunks(eq(table), eq(source), eq(limit), eq(afterId), eq(filter), eq(tags), eq(sort));
//    }
//}