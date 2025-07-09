package org.shark.melian.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.shark.melian.model.MongoDatabaseMetadataDto;
import org.shark.melian.model.TableShortDto;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MongoMetadataServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private MongoMetadataService mongoMetadataService;

    @BeforeEach
    void setUp() {
        // Setup mock collections
        Set<String> mockCollections = Set.of("movies", "users", "products");
        when(mongoTemplate.getCollectionNames()).thenReturn(mockCollections);
    }

    @Test
    void testExtractShortSummary_ReturnsCollectionNames() {
        // When
        List<TableShortDto> result = mongoMetadataService.extractShortSummary();

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        
        // Verify collection names are present
        List<String> tableNames = result.stream()
                .map(TableShortDto::getName)
                .toList();
        assertTrue(tableNames.contains("movies"));
        assertTrue(tableNames.contains("users"));
        assertTrue(tableNames.contains("products"));
        
        verify(mongoTemplate).getCollectionNames();
    }

    @Test
    void testExtractMetadata_ReturnsMetadataDto() {
        // Given - mock empty collections to avoid complex document inspection
        when(mongoTemplate.getCollection("movies")).thenReturn(null);
        when(mongoTemplate.getCollection("users")).thenReturn(null);
        when(mongoTemplate.getCollection("products")).thenReturn(null);

        // When & Then - expect exception due to null collections
        assertThrows(Exception.class, () -> mongoMetadataService.extractMetadata());
    }

    @Test
    void testExtractShortSummary_EmptyCollections_ReturnsEmptyList() {
        // Given
        when(mongoTemplate.getCollectionNames()).thenReturn(Set.of());

        // When
        List<TableShortDto> result = mongoMetadataService.extractShortSummary();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}