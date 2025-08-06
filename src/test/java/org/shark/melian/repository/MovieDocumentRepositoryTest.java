package org.shark.melian.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shark.melian.document.MovieDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@Testcontainers
class MovieDocumentRepositoryTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0.5");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private MovieDocumentRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        repository.save(new MovieDocument("Alpha Movie", "Overview1", "2001-01-01", 7.0));
        repository.save(new MovieDocument("Alpha Another", "Overview2", "2002-02-02", 8.0));
        repository.save(new MovieDocument("Beta Film", "Overview3", "2003-03-03", 6.0));
    }

    @Test
    void searchByTitle_shouldPaginateResults() {
        Pageable firstPage = PageRequest.of(0, 1);
        Pageable secondPage = PageRequest.of(1, 1);

        List<MovieDocument> page1 = repository.searchByTitle("Alpha", firstPage);
        List<MovieDocument> page2 = repository.searchByTitle("Alpha", secondPage);

        assertEquals(1, page1.size());
        assertEquals(1, page2.size());
        assertNotEquals(page1.get(0).getId(), page2.get(0).getId());
    }

    @Test
    void searchByTitleFuzzy_shouldPaginateAndSortResults() {
        Pageable firstPage = PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "title"));
        Pageable secondPage = PageRequest.of(1, 1, Sort.by(Sort.Direction.ASC, "title"));

        List<MovieDocument> page1 = repository.searchByTitleFuzzy("Alpha", firstPage);
        List<MovieDocument> page2 = repository.searchByTitleFuzzy("Alpha", secondPage);

        assertEquals("Alpha Another", page1.get(0).getTitle());
        assertEquals("Alpha Movie", page2.get(0).getTitle());
    }
}

