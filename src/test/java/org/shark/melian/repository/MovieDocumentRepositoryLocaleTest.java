package org.shark.melian.repository;

import com.mongodb.client.MongoClients;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.junit.jupiter.api.*;
import org.shark.melian.document.MovieDocument;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.io.IOException;
import java.util.List;

/**
 * Tests for locale-aware search in MovieDocumentRepository.
 */
public class MovieDocumentRepositoryLocaleTest {

    private static MongodExecutable mongodExecutable;
    private static int port;

    private MongoTemplate mongoTemplate;
    private CustomMovieDocumentRepositoryImpl repository;

    @BeforeAll
    static void beforeAll() throws IOException {
        port = Network.getFreeServerPort();
        MongodConfig config = MongodConfig.builder()
                .version(Version.Main.V6_0)
                .net(new Net(port, Network.localhostIsIPv6()))
                .build();
        mongodExecutable = MongodStarter.getDefaultInstance().prepare(config);
        mongodExecutable.start();
    }

    @AfterAll
    static void afterAll() {
        if (mongodExecutable != null) {
            mongodExecutable.stop();
        }
    }

    @BeforeEach
    void setUp() {
        mongoTemplate = new MongoTemplate(MongoClients.create("mongodb://localhost:" + port), "test");
        repository = new CustomMovieDocumentRepositoryImpl();
        try {
            var field = CustomMovieDocumentRepositoryImpl.class.getDeclaredField("mongoTemplate");
            field.setAccessible(true);
            field.set(repository, mongoTemplate);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        mongoTemplate.dropCollection(MovieDocument.class);
        MovieDocument movie = new MovieDocument("El niño", "desc", "2024", 8.5);
        mongoTemplate.save(movie);
    }

    @Test
    void searchSpanishLocale() {
        List<MovieDocument> results = repository.searchByTitle("nino", PageRequest.of(0, 10), "es");
        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals("El niño", results.get(0).getTitle());
    }

    @Test
    void searchEnglishLocale() {
        List<MovieDocument> results = repository.searchByTitle("nino", PageRequest.of(0, 10), "en");
        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals("El niño", results.get(0).getTitle());
    }
}
