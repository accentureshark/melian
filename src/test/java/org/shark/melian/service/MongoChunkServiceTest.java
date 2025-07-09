package org.shark.melian.service;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.shark.melian.model.ChunkDto;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MongoChunkServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private MongoChunkService mongoChunkService;

    private ObjectId testObjectId;
    private Document testDocument;

    @BeforeEach
    void setUp() {
        testObjectId = new ObjectId();
        testDocument = new Document()
                .append("_id", testObjectId)
                .append("title", "Test Movie")
                .append("description", "A test movie description")
                .append("year", 2023);
    }

    @Test
    void testGetChunks_ValidCollection_ReturnsChunks() {
        // Given
        String collection = "movies";
        String source = "mongodb";
        int limit = 10;
        String afterId = null;
        String filter = null;
        List<String> tags = null;
        String sort = null;

        List<Document> documents = List.of(testDocument);
        when(mongoTemplate.find(any(Query.class), eq(Document.class), eq(collection)))
                .thenReturn(documents);

        // When
        List<ChunkDto> result = mongoChunkService.getChunks(collection, source, limit, afterId, filter, tags, sort);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testObjectId.toString(), result.get(0).getId());
        assertNotNull(result.get(0).getText());
        assertTrue(result.get(0).getText().contains("Test Movie"));
        
        verify(mongoTemplate).find(any(Query.class), eq(Document.class), eq(collection));
    }

    @Test
    void testGetChunks_WithAfterId_AppliesPagination() {
        // Given
        String collection = "movies";
        String afterId = testObjectId.toString();
        int limit = 5;

        List<Document> documents = List.of(testDocument);
        when(mongoTemplate.find(any(Query.class), eq(Document.class), eq(collection)))
                .thenReturn(documents);

        // When
        List<ChunkDto> result = mongoChunkService.getChunks(collection, "mongodb", limit, afterId, null, null, null);

        // Then
        assertNotNull(result);
        verify(mongoTemplate).find(argThat(query -> {
            // Check that the query contains an _id greater than condition
            String queryString = query.toString();
            return queryString.contains("_id") && queryString.contains("$gt");
        }), eq(Document.class), eq(collection));
    }

    @Test
    void testGetChunks_WithEqualityFilter_AppliesFilter() {
        // Given
        String collection = "movies";
        String filter = "title=Matrix";
        int limit = 10;

        List<Document> documents = List.of(testDocument);
        when(mongoTemplate.find(any(Query.class), eq(Document.class), eq(collection)))
                .thenReturn(documents);

        // When
        List<ChunkDto> result = mongoChunkService.getChunks(collection, "mongodb", limit, null, filter, null, null);

        // Then
        assertNotNull(result);
        verify(mongoTemplate).find(argThat(query -> {
            String queryString = query.toString();
            return queryString.contains("title") && queryString.contains("Matrix");
        }), eq(Document.class), eq(collection));
    }

    @Test
    void testGetChunks_WithLikeFilter_AppliesRegexFilter() {
        // Given
        String collection = "movies";
        String filter = "title like %Matrix%";
        int limit = 10;

        List<Document> documents = List.of(testDocument);
        when(mongoTemplate.find(any(Query.class), eq(Document.class), eq(collection)))
                .thenReturn(documents);

        // When
        List<ChunkDto> result = mongoChunkService.getChunks(collection, "mongodb", limit, null, filter, null, null);

        // Then
        assertNotNull(result);
        verify(mongoTemplate).find(argThat(query -> {
            String queryString = query.toString();
            return queryString.contains("title") && queryString.contains("$regex");
        }), eq(Document.class), eq(collection));
    }

    @Test
    void testGetChunks_WithCustomSort_AppliesSort() {
        // Given
        String collection = "movies";
        String sortField = "title";
        int limit = 10;

        List<Document> documents = List.of(testDocument);
        when(mongoTemplate.find(any(Query.class), eq(Document.class), eq(collection)))
                .thenReturn(documents);

        // When
        List<ChunkDto> result = mongoChunkService.getChunks(collection, "mongodb", limit, null, null, null, sortField);

        // Then
        assertNotNull(result);
        verify(mongoTemplate).find(argThat(query -> {
            String queryStr = query.toString();
            return queryStr.contains("sort") && queryStr.contains(sortField);
        }), eq(Document.class), eq(collection));
    }

    @Test
    void testGetChunks_WithDefaultSort_AppliesIdSort() {
        // Given
        String collection = "movies";
        int limit = 10;

        List<Document> documents = List.of(testDocument);
        when(mongoTemplate.find(any(Query.class), eq(Document.class), eq(collection)))
                .thenReturn(documents);

        // When
        List<ChunkDto> result = mongoChunkService.getChunks(collection, "mongodb", limit, null, null, null, null);

        // Then
        assertNotNull(result);
        verify(mongoTemplate).find(argThat(query -> {
            String queryStr = query.toString();
            return queryStr.contains("sort") && queryStr.contains("_id");
        }), eq(Document.class), eq(collection));
    }

    @Test
    void testGetChunks_WithLimit_AppliesLimit() {
        // Given
        String collection = "movies";
        int limit = 5;

        List<Document> documents = List.of(testDocument);
        when(mongoTemplate.find(any(Query.class), eq(Document.class), eq(collection)))
                .thenReturn(documents);

        // When
        List<ChunkDto> result = mongoChunkService.getChunks(collection, "mongodb", limit, null, null, null, null);

        // Then
        assertNotNull(result);
        verify(mongoTemplate).find(argThat(query -> query.getLimit() == limit), 
                eq(Document.class), eq(collection));
    }

    @Test
    void testGetChunks_EmptyResult_ReturnsEmptyList() {
        // Given
        String collection = "movies";
        when(mongoTemplate.find(any(Query.class), eq(Document.class), eq(collection)))
                .thenReturn(List.of());

        // When
        List<ChunkDto> result = mongoChunkService.getChunks(collection, "mongodb", 10, null, null, null, null);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetChunks_WithQuotedFilter_CleansQuotes() {
        // Given
        String collection = "movies";
        String filter = "title='Matrix'";
        int limit = 10;

        List<Document> documents = List.of(testDocument);
        when(mongoTemplate.find(any(Query.class), eq(Document.class), eq(collection)))
                .thenReturn(documents);

        // When
        List<ChunkDto> result = mongoChunkService.getChunks(collection, "mongodb", limit, null, filter, null, null);

        // Then
        assertNotNull(result);
        verify(mongoTemplate).find(argThat(query -> {
            String queryString = query.toString();
            // Should contain Matrix without quotes
            return queryString.contains("Matrix") && !queryString.contains("'Matrix'");
        }), eq(Document.class), eq(collection));
    }
}